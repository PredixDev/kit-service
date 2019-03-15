/*
 * Copyright (c) 2017 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.kitservice.manager;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ge.predix.entity.asset.AssetTag;
import com.ge.predix.entity.data.Data;
import com.ge.predix.entity.fielddata.FieldData;
import com.ge.predix.entity.fielddata.PredixString;
import com.ge.predix.entity.getfielddata.GetFieldDataRequest;
import com.ge.predix.entity.putfielddata.PutFieldDataRequest;
import com.ge.predix.entity.putfielddata.PutFieldDataResult;
import com.ge.predix.solsvc.bootstrap.ams.common.AssetConfig;
import com.ge.predix.solsvc.fdh.handler.PutDataHandler;
import com.ge.predix.solsvc.kitservice.boot.utils.FdhUtils;
import com.ge.predix.solsvc.kitservice.error.DeviceRegistrationError;
import com.ge.predix.solsvc.kitservice.model.DeviceGroup;
import com.ge.predix.solsvc.kitservice.model.RegisterDevice;
import com.ge.predix.solsvc.kitservice.model.UserGroup;
import com.ge.predix.solsvc.restclient.config.IOauthRestConfig;
import com.ge.predix.solsvc.restclient.impl.RestClient;
import com.ge.predix.solsvc.timeseries.bootstrap.config.ITimeseriesConfig;
import com.ge.predix.uaa.token.lib.JsonUtils;

/**
 * 
 * @author 212421693 -
 */
@Component
public class DeviceManager extends BaseManager {
	private static final Logger log = LoggerFactory.getLogger(DeviceManager.class);
	private static final String DEVICE = "device"; //$NON-NLS-1$
	private static final String DEVICEGROUP = "deviceGroup"; //$NON-NLS-1$

	@Autowired
	@Qualifier("defaultOauthRestConfig")
	private IOauthRestConfig restConfig;

	@Autowired
	private PutDataHandler assetPutFieldDataHandler;
	
	@Autowired
	protected AssetConfig assetConfig; //TODO delete 

	@Autowired
	private RestClient restClient;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	@Qualifier("defaultTimeseriesConfig")
	private ITimeseriesConfig timeseriesConfig;

	@Autowired
	private GroupManagementService groupManagementService;

	// this number will be in days
	@Value("${register.device.deactivation:#{60}}")
	private String deactivationPeriod;

	@Value("${kit.webapp.url:null}")
	private String kitApplicationUrl;

	@Value("${kit.device.credentials:null}")
	private String deviceCredentials;

	@Value("${kit.device.artifactory.url:null}")
	private String artifactoryConfigUrl;

	/**
	 * 
	 * @param deviceIdentifier
	 *            -
	 * @param userId
	 *            -
	 * @return -
	 */
	public RegisterDevice getDevice(String deviceIdentifier, String userId) {
		log.trace("Calling getDevice"); //$NON-NLS-1$
		return getDevice("/" + DEVICE, userId, deviceIdentifier); //$NON-NLS-1$

	}

	/**
	 * Registers a device
	 * 
	 * @param device
	 *            -
	 * @param userId
	 *            -
	 * @return RegisterDevice
	 * @throws DeviceRegistrationError
	 *             -
	 */
	public RegisterDevice registerDevice(RegisterDevice device, String userId) throws DeviceRegistrationError {
		// reactivation TBD
		log.info("Calling registerDevice for device=" + device.getUri() + " User = " + userId); //$NON-NLS-1$ //$NON-NLS-2$
		return createorUpdateDevice(device, userId);
	}

	/**
	 * @param device
	 *            -
	 * @param userId
	 *            -
	 * @return RegisterDevice
	 * @throws DeviceRegistrationError
	 *             -
	 */
	@SuppressWarnings("nls")
    public RegisterDevice createorUpdateDevice(RegisterDevice device, String userId) throws DeviceRegistrationError {

		List<Header> headers = getServiceHeaders();
		// log.debug("In here to get Service Headers
		// "+this.jsonMapper.toJson(device));
		if (StringUtils.isEmpty(device.getUri())) {
			device.setUri("/" + DEVICE + "/" + device.getDeviceAddress()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (device.getCreatedDate() == null) {
			device.setCreatedDate(String.valueOf(Instant.now().toEpochMilli()));
		}
		if (device.getUpdateDate() == null) {
			device.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
		}

		if (device.getActivationDate() == null) {
			device.setActivationDate(String.valueOf(Instant.now().toEpochMilli()));
		}

		if (!StringUtils.isEmpty(device.getDeviceGroup())
				&& !device.getDeviceGroup().toLowerCase().startsWith("/devicegroup")) { //$NON-NLS-1$
			String groupRef = device.getDeviceGroup();
			device.setDeviceGroup("/" + DEVICEGROUP + "/" + groupRef); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (!StringUtils.isEmpty(device.getDeviceGroup()) && device.getDeviceGroup().contains("group")) { //$NON-NLS-1$
			device.getDeviceGroup().replaceFirst("group", DEVICEGROUP); //$NON-NLS-1$
		}

		// add default Asset tags
		List<AssetTag> tags = getDefaultTags(device.getDeviceAddress());
		device.setTags(new HashSet<AssetTag>(tags));

		// User and user group , device group
		// 1. Check if the userGroup exists for the device
		// 2. if userGroup, then add userId to users only, if not both places
		// owner and users
		// 3. add this new userGroup to list of device-group. userGroup list.
		// 4. check if the deviceGroup name exists if yes use it add userGroup
		// to the list
		// 5 .update the device with deviceGroup and userGroup.

		UserGroup userGroup = null;
		if (!StringUtils.isEmpty(device.getUserGroup())) {
			userGroup = this.groupManagementService.getUserGroup(device.getUserGroup(), headers);
		}
		if (userGroup != null) {
			// this means that user group found adding the user to this user
			// group
			userGroup.getUaaUsers().add(userId);
			userGroup.setUpdatedDate(String.valueOf(Instant.now().toEpochMilli()));
		} else {
			// user group not found creating a new userGroup adding user as a
			// owner
			userGroup = this.groupManagementService.createUserGroup(userId);
		}
		device.setUserGroup(userGroup.getUri());
		// ****END user group setup

		DeviceGroup deviceGroup = null;
		if (!StringUtils.isEmpty(device.getDeviceGroup())) {
			deviceGroup = this.groupManagementService.getDeviceGroup(headers, device.getDeviceGroup());
		}
		if (deviceGroup == null) {
			deviceGroup = this.groupManagementService.createDeviceGroup(device.getDeviceGroup(), userGroup.getUri());
		} else {
			/**
			 * @212672942: check for userGroup inside the deviceGroup because if
			 * the device is reset then deviceGroup might not have a userGroup
			 * attached.
			 */
			/**
			 * this also takes care of registering with same device group name
			 * from various devices. If the deviceGroup exists the userID will
			 * get added to it.
			 */

			log.info("Adding the userID to the deviceGroup's userGroup " + userGroup.getUri());
			deviceGroup.getUserGroup().add(userGroup.getUri());
		}

		String userGroupString = this.groupManagementService.getUserGroupString(userGroup);
		String group = this.groupManagementService.getDeviceGroupString(deviceGroup);
		// log.info("With devicegroup" + deviceGroup.getUri() + "userGroup" +
		// userGroupString); //$NON-NLS-1$ //$NON-NLS-2$
		List<Object> kitModels = new ArrayList<>();
		kitModels.add(device);
		String deviceString = this.jsonMapper.toJson(kitModels);
		PutFieldDataRequest putFieldDataRequest = FdhUtils.createRegisterPutRequest(deviceString, group,
				userGroupString);
		PutFieldDataResult result = this.assetPutFieldDataHandler.putData(putFieldDataRequest, null, headers,
				HttpPost.METHOD_NAME);
		log.debug(this.jsonMapper.toJson(result));
		if (!CollectionUtils.isEmpty(result.getErrorEvent())) {
			log.error("Error: registering/Updating Device for With devicegroup" + deviceGroup.getUri() + "userGroup" //$NON-NLS-1$ //$NON-NLS-2$
					+ userGroupString + " for User with Id" + userId); //$NON-NLS-1$
			throw new DeviceRegistrationError(result.getErrorEvent().get(0));
		}
		return device;

	}

	/**
	 * @param data
	 * @return -
	 */
	private RegisterDevice getDeviceInfoFromData(Data predixString) {
		RegisterDevice device = null;
		PredixString data = (PredixString) predixString;
		String deviceString = StringEscapeUtils.unescapeJava(data.getString());
		deviceString = deviceString.substring(1, deviceString.length() - 1);
		deviceString = deviceString.substring(0, deviceString.length());
		List<RegisterDevice> registeredDevice = this.jsonMapper.fromJsonArray("[" + deviceString + "]", //$NON-NLS-1$ //$NON-NLS-2$
				RegisterDevice.class);
		if (CollectionUtils.isNotEmpty(registeredDevice)) {
			device = registeredDevice.get(0);
			if (device.getActivationDate() == null) {
				device.setExpirationDate(calculateExpiryDate(String.valueOf(Instant.now().toEpochMilli())));
			} else {
				device.setExpirationDate(calculateExpiryDate(device.getActivationDate()));
			}
		}

		return device;
	}

	/**
	 * @param accessToken
	 *            -
	 * @return -
	 */
	public String getUserId(Jwt accessToken) {
		if (accessToken == null)
			return null;
		Map<String, Object> claims = JsonUtils.readValue(accessToken.getClaims(),
				new TypeReference<Map<String, Object>>() {//
				});
		if (claims == null)
			return null;
		return (String) claims.get("user_id"); //$NON-NLS-1$
	}

	/**
	 * 
	 * @param activatationDate
	 * @return -
	 */
	private String calculateExpiryDate(String activatationDate) {
		Date date = new Date();
		date.setTime(Long.valueOf(activatationDate));
		Date expiry = DateUtils.addDays(date, Integer.valueOf(this.deactivationPeriod));
		return String.valueOf(expiry.getTime());
	}

	/**
	 * @return -
	 */
	private List<AssetTag> getDefaultTags(String deviceAddress) {
		Map<String, String> valuesMap = new HashMap<String, String>();
		valuesMap.put("DEVICE_ADDRESS", deviceAddress);//$NON-NLS-1$
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String jsonTemplate = sub.replace(jsonTagTemplate());

		// log.info("read Json is " +jsonTemplate ); //$NON-NLS-1$
		return this.jsonMapper.fromJsonArray(jsonTemplate, AssetTag.class);

	}

	/**
	 * TBD either cache this or use the vault service
	 * 
	 * @return -
	 */
	private List<Header> getServiceHeaders() {
		List<Header> headers = this.restClient.getSecureTokenForClientId();
		headers.add(new BasicHeader("Content-Type", "application/json")); //$NON-NLS-1$//$NON-NLS-2$
		return headers;
	}

	/**
	 * @param userId
	 *            -
	 * @return -
	 */
	public List<RegisterDevice> getDevices(String userId) {
		log.info("Calling get All Device for:" + userId); //$NON-NLS-1$

		return getDevice(userId);
	}

	/**
	 * @param userId
	 *            -
	 * @return -
	 */
	public List<RegisterDevice> getDevice(String userId) {
		log.trace("Calling getDevice"); //$NON-NLS-1$
		List<RegisterDevice> devices = new ArrayList<RegisterDevice>();
		List<Header> headers = getServiceHeaders();
		GetFieldDataRequest request;
		try {
			request = FdhUtils.createGetUserDeviceRequest("/" + DEVICE, "PredixString", userId, null); //$NON-NLS-1$//$NON-NLS-2$
			List<FieldData> fieldDatas = getFieldDataResult(request, headers);

			if (CollectionUtils.isNotEmpty(fieldDatas)) {
				for (FieldData fieldData : fieldDatas) {
					devices.add(getDeviceInfoFromData(fieldData.getData()));
				}
			}

		} catch (UnsupportedEncodingException e) {
			log.info("Error with decoding the String Asset filter " + e.toString(), e); //$NON-NLS-1$
		}

		return devices;
	}

	/**
	 * @param deviceIdentifier
	 *            -
	 * @param userId
	 *            -
	 * @param deviceAddress
	 *            -
	 * @return - returns the device
	 */
	public RegisterDevice getDevice(String deviceIdentifier, String userId, String deviceAddress) {

		log.trace("Calling getDevice by user and device address"); //$NON-NLS-1$
		RegisterDevice device = null;
		List<Header> headers = getServiceHeaders();
		GetFieldDataRequest request;
		try {
			request = FdhUtils.createGetUserDeviceRequest(deviceIdentifier, "PredixString", userId, deviceAddress);//$NON-NLS-1$
			List<FieldData> fieldData = getFieldDataResult(request, headers);
			if (CollectionUtils.isNotEmpty(fieldData)) {
				device = getDeviceInfoFromData(fieldData.get(0).getData());
			}
		} catch (UnsupportedEncodingException e) {

			log.info("Error with decoding the String Asset filter " + e.toString(), e); //$NON-NLS-1$

		}

		return device;

	}

	/**
	 * 
	 * @return -
	 */
	private String jsonTagTemplate() {
		String tagModelTemplate = null;
		try {
			log.info("DeviceManager: Loading asset-model..."); //$NON-NLS-1$
			Resource resource = getResourceLoader().getResource("classpath:asset-model/device-asset-tags.json"); //$NON-NLS-1$
			File dbAsFile = resource.getFile();
			tagModelTemplate = FileUtils.readFileToString(dbAsFile, "UTF-8"); //$NON-NLS-1$
			return tagModelTemplate;
		} catch (IOException | NullPointerException e) {
			log.error("DeviceManager: Asset model could not be initialized. ", e); //$NON-NLS-1$
		}
		return tagModelTemplate;
	}

	/**
	 * @return the resourceLoader
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * @param resourceLoader
	 *            the resourceLoader to set
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 
	 * @return -
	 */
	@SuppressWarnings({ "unchecked", "nls" })
	public Map<String, String> getDeviceConfig() {
		@SuppressWarnings("rawtypes")
		Map deviceConfig = new LinkedHashMap();
		deviceConfig.put("predixUaaIssuer", this.restConfig.getOauthIssuerId()); //$NON-NLS-1$

		deviceConfig.put("client", this.deviceCredentials);
		deviceConfig.put("predixTimeSeriesIngestUri", this.timeseriesConfig.getWsUri()); //$NON-NLS-1$
		deviceConfig.put("predixTimeSeriesZoneid", this.timeseriesConfig.getZoneId());
		deviceConfig.put("deviceDeactivationPeriod", this.deactivationPeriod);
		deviceConfig.put("cloudApplicationUrl", this.kitApplicationUrl);
		deviceConfig.put("artifactoryConfigUrl", this.artifactoryConfigUrl);
		return deviceConfig;

	}

	/**
	 * @param device
	 *            -
	 * @param originalDevice
	 *            -
	 * @param userId
	 *            -
	 * @throws DeviceRegistrationError
	 *             -
	 */
	public void updateDevice(RegisterDevice device, RegisterDevice originalDevice, String userId)
			throws DeviceRegistrationError {
		log.info("Calling updateDevice for device=" + originalDevice.getUri() + " User = " + userId); //$NON-NLS-1$ //$NON-NLS-2$

		if (StringUtils.isNotEmpty(device.getDeviceName())
				&& !originalDevice.getDeviceName().equalsIgnoreCase(device.getDeviceName())) {
			originalDevice.setDeviceName(device.getDeviceName());
		}

		if (StringUtils.isNotEmpty(device.getDeviceGroup())) {

			originalDevice.setDeviceGroup(device.getDeviceGroup());
		}

		if (CollectionUtils.isNotEmpty(device.getTags())) {
			originalDevice.getTags().addAll(device.getTags());
		}
		if (device.getGeoLocation() != null && StringUtils.isNotEmpty(device.getGeoLocation().getLatitude())) {
			originalDevice.getGeoLocation().setLatitude(device.getGeoLocation().getLatitude());
		}
		if (device.getGeoLocation() != null && StringUtils.isNotEmpty(device.getGeoLocation().getLongitude())) {
			originalDevice.getGeoLocation().setLongitude(device.getGeoLocation().getLongitude());
		}

		originalDevice.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
		this.createorUpdateDevice(originalDevice, userId);
	}

	/**
	 * @param device
	 *            -
	 * @throws DeviceRegistrationError
	 *             -
	 */
	public void checkDeviceExpiry(RegisterDevice device) throws DeviceRegistrationError {
		Long currentTime = Instant.now().toEpochMilli();
		// Date currentDate = new Date(currentTime);
		Long activationTime = Long.valueOf(device.getActivationDate());
		// Date activationDate = new Date(activationTime);

		DateTime currentDate = new DateTime(currentTime);
		DateTime activationDate = new DateTime(activationTime);

		int days = Days.daysBetween(currentDate, activationDate).getDays();

		if (Math.abs(days) > Integer.valueOf(this.deactivationPeriod)) {
			throw new DeviceRegistrationError("Device has past its activation period."); //$NON-NLS-1$
		}
	}

	/**
	 * @param device
	 *            -
	 * @param isAdmin
	 *            -
	 * @param userid
	 *            -
	 * @throws DeviceRegistrationError
	 *             -
	 * @throws IOException
	 *             if deleting the device's userGroup from the API end point URL
	 *             during reset of the device causes issues
	 * 
	 * 
	 */
	public void resetDeviceActivation(RegisterDevice device, Boolean isAdmin, String userid)
			throws DeviceRegistrationError, IOException {

		/**
		 * @author 212672942 Resetting the device - On the device, blank out
		 *         both deviceGroup and the userGroup. Also remove the userID
		 *         from the userGroup. If this is the last userID in that
		 *         userGroup,remove the userGroup from the deviceGroup as well
		 *         as remove the userGroup itself from the backend.
		 *
		 */

		log.info("DeviceManager: ... resetting device" + device.getUri()); //$NON-NLS-1$
		List<Header> headers = getServiceHeaders();
		if (StringUtils.isEmpty(device.getUserGroup())) {
			// Device has no userGroup which means it was reset.
			device.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
			device.setActivationDate(device.getUpdateDate());
			device.setDeviceGroup(null);
			device.setUserGroup(null);
			this.updateDeviceDuringReset(device, null, null, headers);
		} else {
			resetDeviceGroupAndUserGroup(device, userid, headers);
		}
	}

	private void updateDeviceDuringReset(RegisterDevice device, DeviceGroup deviceGroup, UserGroup userGroup,
			List<Header> headers) throws DeviceRegistrationError {
		List<Object> kitModels = new ArrayList<>();
		String userGroupString = null;
		String deviceGroupString = null;
		kitModels.add(device);
		String deviceString = this.jsonMapper.toJson(kitModels);

		if (deviceGroup != null)
			deviceGroupString = this.groupManagementService.getDeviceGroupString(deviceGroup);

		if (userGroup != null)
			userGroupString = this.groupManagementService.getUserGroupString(userGroup);

		PutFieldDataRequest putFieldDataRequest = FdhUtils.createRegisterPutRequest(deviceString, deviceGroupString,
				userGroupString);
		PutFieldDataResult result = this.assetPutFieldDataHandler.putData(putFieldDataRequest, null, headers,
				HttpPost.METHOD_NAME);
		log.debug(this.jsonMapper.toJson(result));
		if (!CollectionUtils.isEmpty(result.getErrorEvent())) {
			log.error(
					"Error: Updating the device while resetting the device with address: " + device.getDeviceAddress()); //$NON-NLS-1$
			throw new DeviceRegistrationError(result.getErrorEvent().get(0));
		}
	}

	private void resetDeviceGroupAndUserGroup(RegisterDevice device, String userId, List<Header> headers)
			throws DeviceRegistrationError, IOException {
		UserGroup userGrp = null;
		DeviceGroup deviceGrp = null;
		String userGrpUri = null;
		Set<String> uaaUsers = null;
		Set<String> uaaOwners = null;
		Set<String> deviceUserGroups = null;
		String assetUri = null;

		userGrpUri = device.getUserGroup();
		userGrp = this.groupManagementService.getUserGroup(userGrpUri, headers);
		uaaUsers = userGrp.getUaaUsers();
		uaaOwners = userGrp.getUaaOwners();
		/**
		 * Remove the userID both from deviceGroup's userGroup and the userGroup
		 * only if this is the last user on the device resetting. Else remove
		 * userID only from userGroup.
		 */
		if (uaaUsers.size() == 1) {
			// log.info("Only one user in the user group " + userGrpUri);

			deviceGrp = this.groupManagementService.getDeviceGroup(headers, device.getDeviceGroup());
			deviceUserGroups = deviceGrp.getUserGroup();
			deviceUserGroups.remove(userGrpUri);

			if (deviceUserGroups.isEmpty()) {

				deviceGrp.setUserGroup(null);
			} else
				deviceGrp.setUserGroup(deviceUserGroups);
		}
		uaaUsers.remove(userId);
		uaaOwners.remove(userId);

		if (uaaUsers.isEmpty()) {
			// delete the asset userGroup if the last user in this group resets
			// the device
			// this.assetPutFieldDataHandler.deleteData(userGrp.getUri(),
			// headers);

			assetUri = this.assetConfig.getAssetUri();

			if (!assetUri.endsWith("/") && !userGrp.getUri().startsWith("/"))
				assetUri += "/" + userGrp.getUri();
			else
				assetUri += userGrp.getUri();
			// log.info("Asset URI is: " + assetUri);
			CloseableHttpResponse response = null;
			try {
			    //TODO should use the assetClient not the restClient
				this.restClient.delete(assetUri, headers, this.restConfig.getDefaultConnectionTimeout(),
						this.restConfig.getDefaultSocketTimeout());
			} finally {
				if (response != null)
					response.close();
			}

			// reset the device
			device.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
			device.setActivationDate(device.getUpdateDate());
			device.setDeviceGroup(null);
			device.setUserGroup(null);
			this.updateDeviceDuringReset(device, deviceGrp, null, headers);

		} else {

			userGrp.setUaaUsers(uaaUsers);
			userGrp.setUaaOwners(uaaOwners);
			userGrp.setUpdatedDate(String.valueOf(Instant.now().toEpochMilli()));
			device.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
			device.setActivationDate(device.getUpdateDate());
			device.setDeviceGroup(null);
			device.setUserGroup(null);
			this.updateDeviceDuringReset(device, deviceGrp, userGrp, headers);
		}
	}

	/**
	 * -
	 * 
	 * @return -
	 */
	public List<RegisterDevice> getAllAdminDevices() {
		log.info("Calling getAdminDevice"); //$NON-NLS-1$
		List<RegisterDevice> devices = new ArrayList<RegisterDevice>();
		List<Header> headers = getServiceHeaders();
		GetFieldDataRequest request;

		try {
			request = FdhUtils.createGetAdminDeviceRequest("/" + DEVICE, "PredixString"); //$NON-NLS-1$//$NON-NLS-2$
			List<FieldData> fieldDatas = getFieldDataResult(request, headers);

			if (CollectionUtils.isNotEmpty(fieldDatas)) {
				for (FieldData fieldData : fieldDatas) {
					devices.add(getDeviceInfoFromData(fieldData.getData()));
				}
			}

		} catch (UnsupportedEncodingException e) {
			log.info("Error with decoding the String Asset filter " + e.toString(), e); //$NON-NLS-1$
		}

		return devices;

	}

}
