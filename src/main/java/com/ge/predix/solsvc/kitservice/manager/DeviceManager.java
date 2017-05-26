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
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.text.StrSubstitutor;
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
    private static final String GROUP = "group"; //$NON-NLS-1$
   
    
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
     * @return RegisterDevice
     * @throws DeviceRegistrationError -
     */
    public RegisterDevice registerDevice(RegisterDevice device) throws DeviceRegistrationError
    {
        // reactivation TBD 
        log.info("Calling registerDevice for device="+device.getUri()+" User = "+device.getUserId()); //$NON-NLS-1$ //$NON-NLS-2$
        return createorUpdateDevice(device);
    }
   
   
    /**
     * @param device -
     * @return RegisterDevice
     * @throws DeviceRegistrationError 
     */
    private RegisterDevice createorUpdateDevice(RegisterDevice device) throws DeviceRegistrationError
    {
       
        List<Header> headers = getServiceHeaders();
        log.debug("In here to get Service Headers "+this.jsonMapper.toJson(device));
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
        
        if(!StringUtils.isEmpty(device.getGroupRef()) && !device.getGroupRef().startsWith("/group")){ //$NON-NLS-1$
            String groupRef= device.getGroupRef();
            device.setGroupRef("/"+GROUP+"/"+groupRef); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        
        // add default Asset tags
        List<AssetTag> tags = getDefaultTags(device.getDeviceAddress());
        device.setTags(new HashSet<AssetTag>(tags));
       
        // User and user group
        UserGroup userGroup = this.groupManagementService.getOrCreateUserGroup(headers,device.getUserId());
        String  userGroupString = this.groupManagementService.getUserGroupString(userGroup);
        String group = this.groupManagementService.getOrCreateGroup(headers,userGroup.getUri(),device.getGroupRef());
        
        log.debug("With group"+group + "userGroup" + userGroupString); //$NON-NLS-1$ //$NON-NLS-2$
       
        
        List<Object> kitModels = new ArrayList<>();
        kitModels.add(device);
        String deviceString = this.jsonMapper.toJson(kitModels);
        PutFieldDataRequest putFieldDataRequest = FdhUtils.createRegisterPutRequest(deviceString,group,userGroupString);
        PutFieldDataResult result = this.assetPutFieldDataHandler.putData(putFieldDataRequest, null, headers,
                HttpPost.METHOD_NAME);
        log.debug(this.jsonMapper.toJson(result));
        if ( !CollectionUtils.isEmpty(result.getErrorEvent()) )
        {
            log.info("Error: registering/Updating Device"); //$NON-NLS-1$
            //TBD: do something
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
        PredixString data = (PredixString) predixString;
        String deviceString = StringEscapeUtils.unescapeJava(data.getString());
        deviceString = deviceString.substring(1, deviceString.length() - 1);
        deviceString = deviceString.substring(0, deviceString.length());
        List<RegisterDevice> registeredDevice = this.jsonMapper.fromJsonArray("["+deviceString+"]", RegisterDevice.class); //$NON-NLS-1$ //$NON-NLS-2$
        if(CollectionUtils.isNotEmpty(registeredDevice)) 
            return registeredDevice.get(0);
        return null;
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
        GetFieldDataRequest request = FdhUtils.createGetUserDeviceRequest("/"+DEVICE, "PredixString",userId,null); //$NON-NLS-1$ //$NON-NLS-2$
        List<FieldData> fieldDatas= getFieldDataResult(request,headers);
        
        if(CollectionUtils.isNotEmpty(fieldDatas)) {
            for(FieldData fieldData:fieldDatas){
                devices.add( getDeviceInfoFromData(fieldData.getData()));
            }
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
        GetFieldDataRequest request = FdhUtils.createGetUserDeviceRequest(deviceIdentifier, "PredixString",userId,deviceAddress); //$NON-NLS-1$
        List<FieldData> fieldData = getFieldDataResult(request,headers);
        if(CollectionUtils.isNotEmpty(fieldData)) {
            return getDeviceInfoFromData(fieldData.get(0).getData());
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
        return deviceConfig;
        
    }


    /**
     * @param device -
     * @param originalDevice  -
     * @throws DeviceRegistrationError -
     */
    public void updateDevice(RegisterDevice device, RegisterDevice originalDevice) throws DeviceRegistrationError
    {
        log.info("Calling updateDevice for device="+device.getUri()+" User = "+device.getUserId()); //$NON-NLS-1$ //$NON-NLS-2$
        
        if(StringUtils.isNotEmpty(device.getDeviceName()) && ! originalDevice.getDeviceName().equalsIgnoreCase(device.getDeviceName())) {
            originalDevice.setDeviceName(device.getDeviceName());
        }
        if(StringUtils.isNotEmpty(device.getGroupRef()) && ! originalDevice.getGroupRef().equalsIgnoreCase(device.getGroupRef())) {
            originalDevice.setGroupRef(device.getGroupRef());
        }
        if(CollectionUtils.isNotEmpty(device.getTags())) {
            originalDevice.getTags().addAll(device.getTags());
        }
        originalDevice.setUpdateDate(String.valueOf(Instant.now().toEpochMilli()));
        this.createorUpdateDevice(originalDevice);
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
}
