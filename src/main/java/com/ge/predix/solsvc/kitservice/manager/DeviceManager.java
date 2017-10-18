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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
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
import org.springframework.stereotype.Component;

import com.ge.predix.entity.asset.AssetTag;
import com.ge.predix.entity.fielddata.Data;
import com.ge.predix.entity.fielddata.FieldData;
import com.ge.predix.entity.fielddata.PredixString;
import com.ge.predix.entity.getfielddata.GetFieldDataRequest;
import com.ge.predix.entity.putfielddata.PutFieldDataRequest;
import com.ge.predix.entity.putfielddata.PutFieldDataResult;
import com.ge.predix.solsvc.fdh.handler.PutDataHandler;
import com.ge.predix.solsvc.kitservice.boot.utils.FdhUtils;
import com.ge.predix.solsvc.kitservice.error.DeviceRegistrationError;
import com.ge.predix.solsvc.kitservice.model.DeviceGroup;
import com.ge.predix.solsvc.kitservice.model.RegisterDevice;
import com.ge.predix.solsvc.kitservice.model.UserGroup;
import com.ge.predix.solsvc.restclient.config.IOauthRestConfig;
import com.ge.predix.solsvc.restclient.impl.RestClient;
import com.ge.predix.solsvc.timeseries.bootstrap.config.ITimeseriesConfig;

/**
 * 
 * @author 212421693 -
 */
@Component
public class DeviceManager extends BaseManager
{
    private static final Logger log          = LoggerFactory.getLogger(DeviceManager.class);
    private static final String DEVICE = "device"; //$NON-NLS-1$
    private static final String DEVICEGROUP = "deviceGroup"; //$NON-NLS-1$
   
    
    @Autowired
    @Qualifier("defaultOauthRestConfig")
    private IOauthRestConfig    restConfig;

    @Autowired
    private PutDataHandler      assetPutFieldDataHandler;
   
    @Autowired
    private RestClient          restClient;
    
    @Autowired
    private
    ResourceLoader resourceLoader;
    
    @Autowired
    @Qualifier("defaultTimeseriesConfig")
    private ITimeseriesConfig timeseriesConfig;
    
    @Autowired
    private GroupManagementService          groupManagementService;
    
    // this number will be in days
    @Value("${register.device.deactivation:#{60}}")
    private String deactivationPeriod;
    
    @SuppressWarnings("javadoc")
    @Value("${kit.webapp.url:null}")
    String kitApplicationUrl; 
    
    @SuppressWarnings("javadoc")
    @Value("${kit.device.credentials:null}")
    String deviceCredentials; 
    
    @SuppressWarnings("javadoc")
    @Value("${kit.device.artifactory.url:null}")
    String artifactoryConfigUrl; 
    
    
    
    
  /**
   *   
   * @param deviceIdentifier -
   * @param userId  -
   * @return -
   */
    public RegisterDevice getDevice(String deviceIdentifier, String userId)
    {
       log.info("Calling getDevice"); //$NON-NLS-1$
       return getDevice("/"+DEVICE,userId, deviceIdentifier); //$NON-NLS-1$
       
    }
    
    /**
     * Registers a device
     * @param device -
     * @param userId -
     * @return RegisterDevice
     * @throws DeviceRegistrationError -
     */
    public RegisterDevice registerDevice(RegisterDevice device, String userId) throws DeviceRegistrationError
    {
        // reactivation TBD 
        log.info("Calling registerDevice for device="+device.getUri()+" User = "+userId); //$NON-NLS-1$ //$NON-NLS-2$
        return createorUpdateDevice(device,userId);
    }
   
   
    /**
     * @param device -
     * @return RegisterDevice
     * @throws DeviceRegistrationError 
     */
    private RegisterDevice createorUpdateDevice(RegisterDevice device, String userId) throws DeviceRegistrationError
    {
       
        List<Header> headers = getServiceHeaders();
       // log.debug("In here to get Service Headers "+this.jsonMapper.toJson(device));
        if(StringUtils.isEmpty(device.getUri())){
            device.setUri("/"+DEVICE+"/"+device.getDeviceAddress()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        if(device.getCreatedDate() == null ) {
            device.setCreatedDate(String.valueOf(Instant.now().toEpochMilli()));
        }
        if(device.getUpdateDate() == null ) {
            device.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
        }
        
        if(device.getActivationDate() == null ) {
            device.setActivationDate(String.valueOf(Instant.now().toEpochMilli()));
        }
        
        if(!StringUtils.isEmpty(device.getDeviceGroup()) && !device.getDeviceGroup().toLowerCase().startsWith("/devicegroup")){ //$NON-NLS-1$
            String groupRef= device.getDeviceGroup();
            device.setDeviceGroup("/"+DEVICEGROUP+"/"+groupRef); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (!StringUtils.isEmpty(device.getDeviceGroup()) && device.getDeviceGroup().contains("group")) { //$NON-NLS-1$
            device.getDeviceGroup().replaceFirst("group", DEVICEGROUP); //$NON-NLS-1$
        }
        
        
        // add default Asset tags
        List<AssetTag> tags = getDefaultTags(device.getDeviceAddress());
        device.setTags(new HashSet<AssetTag>(tags));
       
        // User and user group , device group
        //1. Check if the userGroup exists for the device
        //2. if userGroup, then add userId to users only, if not both places owner and users
        //3. add this new userGroup to list of device-group. userGroup list. 
        //4. check if the deviceGroup name exists if yes use it add userGroup to the list
        //5 .update the device with deviceGroup and userGroup.
        
        UserGroup userGroup = null;
        if(!StringUtils.isEmpty(device.getUserGroup())) { 
             userGroup = this.groupManagementService.getUserGroup(device.getUserGroup(),headers);
        }
        if( userGroup !=null  ) {
            // this means that user group found adding the user to this user group
            userGroup.getUaaUsers().add(userId);
            userGroup.setUpdatedDate(String.valueOf(Instant.now().toEpochMilli()));
        } else {
            // user group not found creating a new userGroup adding user as a owner
            userGroup = this.groupManagementService.createUserGroup(userId);
        }
        device.setUserGroup(userGroup.getUri());
        //****END user group setup
       
        DeviceGroup deviceGroup = null;
        if(!StringUtils.isEmpty(device.getDeviceGroup())) { 
            deviceGroup = this.groupManagementService.getDeviceGroup(headers,device.getDeviceGroup());
       } 
        if( deviceGroup == null ){
            // this means that device group not ** found adding the user to this user group
            deviceGroup = this.groupManagementService.createDeviceGroup(device.getDeviceGroup(),userGroup.getUri());
        }
        
        
        String  userGroupString = this.groupManagementService.getUserGroupString(userGroup);
        String group = this.groupManagementService.getDeviceGroupString(deviceGroup);
        log.debug("With devicegroup"+deviceGroup.getUri() + "userGroup" + userGroupString); //$NON-NLS-1$ //$NON-NLS-2$
        
        List<Object> kitModels = new ArrayList<>();
        kitModels.add(device);
        String deviceString = this.jsonMapper.toJson(kitModels);
        PutFieldDataRequest putFieldDataRequest = FdhUtils.createRegisterPutRequest(deviceString,group,userGroupString);
        PutFieldDataResult result = this.assetPutFieldDataHandler.putData(putFieldDataRequest, null, headers,
                HttpPost.METHOD_NAME);
        log.debug(this.jsonMapper.toJson(result));
        if ( !CollectionUtils.isEmpty(result.getErrorEvent()) )
        {
            log.error("Error: registering/Updating Device for With devicegroup"+deviceGroup.getUri() + "userGroup" + userGroupString +" for User with Id" + userId); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
           throw new DeviceRegistrationError(result.getErrorEvent().get(0));
        }
        return device;

    }
    
    /**
     * @param data
     * @return -
     */
    private RegisterDevice getDeviceInfoFromData(Data predixString)
    {
        RegisterDevice device = null;
        PredixString data = (PredixString) predixString;
        String deviceString = StringEscapeUtils.unescapeJava(data.getString());
        deviceString = deviceString.substring(1, deviceString.length() - 1);
        deviceString = deviceString.substring(0, deviceString.length());
        List<RegisterDevice> registeredDevice = this.jsonMapper.fromJsonArray("["+deviceString+"]", RegisterDevice.class); //$NON-NLS-1$ //$NON-NLS-2$
        if(CollectionUtils.isNotEmpty(registeredDevice))  {
            device = registeredDevice.get(0);
            if(device.getActivationDate() == null ) {
                device.setExpirationDate(calculateExpiryDate(String.valueOf(Instant.now().toEpochMilli())));
            } else {
                device.setExpirationDate(calculateExpiryDate(device.getActivationDate()));
            }
        }
            
        return device;
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
    private List<AssetTag> getDefaultTags(String deviceAddress)
    {
        Map<String, String> valuesMap = new HashMap<String, String>();
        valuesMap.put("DEVICE_ADDRESS", deviceAddress);//$NON-NLS-1$
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        String jsonTemplate  = sub.replace(jsonTagTemplate());
        
        //log.info("read Json is " +jsonTemplate ); //$NON-NLS-1$
        return this.jsonMapper.fromJsonArray(jsonTemplate, AssetTag.class);
        
    }

    /**
     * TBD either cache this or use the vault service
     * 
     * @return -
     */
    private List<Header> getServiceHeaders()
    {
        List<Header> headers = this.restClient.getSecureTokenForClientId();
        headers.add(new BasicHeader("Content-Type", "application/json"));  //$NON-NLS-1$//$NON-NLS-2$
        return headers;
    }

    /**
     * @param userId -
     * @return -
     */
    public List<RegisterDevice> getDevices(String userId)
    {
        log.info("Calling get All Device for"); //$NON-NLS-1$
        
        return getDevice(userId); 
    }
    
    /**
     * @param deviceIdentifier -
     * @param userId -
     * @param deviceAddress -  
     * @param deviceName -
     * @return -
     */
    public List<RegisterDevice> getDevice(String userId)
    {
        log.info("Calling getDevice"); //$NON-NLS-1$
        List<RegisterDevice> devices= new ArrayList <RegisterDevice>();
        List<Header> headers = getServiceHeaders();
        GetFieldDataRequest request;
        try
        {
            request = FdhUtils.createGetUserDeviceRequest("/"+DEVICE, "PredixString",userId,null);  //$NON-NLS-1$//$NON-NLS-2$
            List<FieldData> fieldDatas= getFieldDataResult(request,headers);
            
            if(CollectionUtils.isNotEmpty(fieldDatas)) {
                for(FieldData fieldData:fieldDatas){
                    devices.add( getDeviceInfoFromData(fieldData.getData()));
                }
            }
            
        }
        catch (UnsupportedEncodingException e)
        {
            log.info("Error with decoding the String Asset filter "+e.toString(),e); //$NON-NLS-1$
        } 
        
        return devices;
    }
    
    

    /**
     * @param deviceIdentifier -
     * @param userId -
     * @param deviceAddress -  
     * @param deviceName -
     * @return -
     */
    public RegisterDevice getDevice(String deviceIdentifier, String userId, String deviceAddress)
    {
       
        log.info("Calling getDevice by user and device address"); //$NON-NLS-1$
        RegisterDevice device =null;
        List<Header> headers = getServiceHeaders();
        GetFieldDataRequest request;
        try
        {
            request = FdhUtils.createGetUserDeviceRequest(deviceIdentifier, "PredixString",userId,deviceAddress);//$NON-NLS-1$
            List<FieldData> fieldData = getFieldDataResult(request,headers);
            if(CollectionUtils.isNotEmpty(fieldData)) {
                device = getDeviceInfoFromData(fieldData.get(0).getData());
            }
        }
        catch (UnsupportedEncodingException e)
        {
           
            log.info("Error with decoding the String Asset filter "+e.toString(),e); //$NON-NLS-1$
  
        } 
        
        return device;
        
    }
    
  
    /**
     * 
     * @return -
     */
    private String jsonTagTemplate() {
        String tagModelTemplate = null ;
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
    public ResourceLoader getResourceLoader()
    {
        return this.resourceLoader;
    }

    /**
     * @param resourceLoader the resourceLoader to set
     */
    public void setResourceLoader(ResourceLoader resourceLoader)
    {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 
     * @return -
     */
    @SuppressWarnings({
            "unchecked", "nls"
    })
    public Map<String, String> getDeviceConfig()
    {
        @SuppressWarnings("rawtypes")
        Map deviceConfig = new LinkedHashMap();
        deviceConfig.put("predixUaaIssuer", this.restConfig.getOauthIssuerId()); //$NON-NLS-1$
        
        deviceConfig.put("client",this.deviceCredentials);
        deviceConfig.put("predixTimeSeriesIngestUri", this.timeseriesConfig.getWsUri()); //$NON-NLS-1$
        deviceConfig.put("predixTimeSeriesZoneid", this.timeseriesConfig.getZoneId());
        deviceConfig.put("deviceDeactivationPeriod", this.deactivationPeriod);
        deviceConfig.put("cloudApplicationUrl", this.kitApplicationUrl);
        deviceConfig.put("artifactoryConfigUrl", this.artifactoryConfigUrl);
        return deviceConfig;
        
    }


    /**
     * @param device -
     * @param originalDevice  -
     * @param userId -
     * @throws DeviceRegistrationError -
     */
    public void updateDevice(RegisterDevice device, RegisterDevice originalDevice , String userId) throws DeviceRegistrationError
    {
        log.info("Calling updateDevice for device="+originalDevice.getUri()+" User = "+userId); //$NON-NLS-1$ //$NON-NLS-2$
        
        if(StringUtils.isNotEmpty(device.getDeviceName()) && ! originalDevice.getDeviceName().equalsIgnoreCase(device.getDeviceName())) {
            originalDevice.setDeviceName(device.getDeviceName());
        }
        if(StringUtils.isNotEmpty(device.getDeviceGroup()) && ! originalDevice.getDeviceGroup().equalsIgnoreCase(device.getDeviceGroup())) {
            originalDevice.setDeviceGroup(device.getDeviceGroup());
        }
        if(CollectionUtils.isNotEmpty(device.getTags())) {
            originalDevice.getTags().addAll(device.getTags());
        }
        if(device.getGeoLocation() !=null && StringUtils.isNotEmpty(device.getGeoLocation().getLatitude())) {
            originalDevice.getGeoLocation().setLatitude(device.getGeoLocation().getLatitude());
        }
        if(device.getGeoLocation() !=null && StringUtils.isNotEmpty(device.getGeoLocation().getLongitude())) {
            originalDevice.getGeoLocation().setLongitude(device.getGeoLocation().getLongitude());
        }
        
        
        originalDevice.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
        this.createorUpdateDevice(originalDevice,userId);
    }

    /**
     * @param device -
     * @throws DeviceRegistrationError -
     */
    public void checkDeviceExpiry(RegisterDevice device) throws DeviceRegistrationError
    {
    	Long currentTime = Instant.now().toEpochMilli();
        //Date currentDate = new Date(currentTime);
        Long activationTime = Long.valueOf(device.getActivationDate());
       // Date activationDate = new Date(activationTime);
        
        DateTime currentDate = new DateTime(currentTime);
        DateTime activationDate = new DateTime(activationTime);
        
        int days = Days.daysBetween(currentDate, activationDate).getDays();
       
        
        if(Math.abs(days) > Integer.valueOf(this.deactivationPeriod)){
            throw new DeviceRegistrationError("Device has past its activation period."); //$NON-NLS-1$
        }
    }

    /**
     * @param device -
     * @param isAdmin - 
     * @param userid -
     * @throws DeviceRegistrationError  -
     */
    public void resetDevice(RegisterDevice device, Boolean isAdmin, String userid) throws DeviceRegistrationError
    {
        if(isAdmin) {
            log.info("DeviceManager: admin access... resetting device"+device.getUri()); //$NON-NLS-1$
                device.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
                device.setActivationDate(device.getUpdateDate());
                createorUpdateDevice(device,userid);
        }
        
    }

    /**
     *  -
     * @return -
     */
    public List<RegisterDevice> getAllAdminDevices()
    {
        log.info("Calling getAdminDevice"); //$NON-NLS-1$
        List<RegisterDevice> devices= new ArrayList <RegisterDevice>();
        List<Header> headers = getServiceHeaders();
        GetFieldDataRequest request;
        try
        {
            request = FdhUtils.createGetAdminDeviceRequest("/"+DEVICE, "PredixString");  //$NON-NLS-1$//$NON-NLS-2$
            List<FieldData> fieldDatas= getFieldDataResult(request,headers);
            
            if(CollectionUtils.isNotEmpty(fieldDatas)) {
                for(FieldData fieldData:fieldDatas){
                    devices.add( getDeviceInfoFromData(fieldData.getData()));
                }
            }
            
        }
        catch (UnsupportedEncodingException e)
        {
            log.info("Error with decoding the String Asset filter "+e.toString(),e); //$NON-NLS-1$
        } 
        
        return devices;
        
    }
}
