/*
 * Copyright (c) 2017 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
 
package com.ge.predix.solsvc.kitservice.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.ge.predix.entity.asset.AssetTag;

/**
 * 
 * @author 212421693 -
 */

@XmlRootElement(name="device")
public class RegisterDevice
{
    /**
     *  -
     */
    public RegisterDevice()
    {
        super();
    }
    /**
     * 
     */
    String uri;
   
    /**
     * 
     */
   
    String deviceName;
    /**
     * 
     */
    String deviceAddress;
    /**
     * 
     */
    String deviceType;
    /**
     * 
     */
    String activationDate;
    
    /**
     * 
     */
    String expirationDate;
    /**
     * 
     */
    
    String deviceGroup;
    
    /**
     * 
     */
    String userGroup;
    
    /**
     * 
     */
    String createdDate;
    
    /**
     * 
     */
    String updateDate;
    
    /**
     * 
     */
    String ipAddress;
    
    /**
     * 
     */
    private GeoLocation geoLocation = new GeoLocation();
    
    /**
     * @return the ipAddress
     */
    public String getIpAddress()
    {
        return this.ipAddress;
    }
    /**
     * @param ipAddress the ipAddress to set
     */
    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }
    /**
     * 
     */
    Set<AssetTag> tags = new HashSet<AssetTag>();
    
    private Map<?, ?> deviceConfig;
    /**
     * @return the uri
     */
    public String getUri()
    {
        return this.uri;
    }
    /**
     * @param uri the uri to set
     */
    public void setUri(String uri)
    {
        this.uri = uri;
    }
    /**
     * @return the deviceName
     */
    public String getDeviceName()
    {
        return this.deviceName;
    }
    /**
     * @param deviceName the deviceName to set
     */
    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }
    /**
     * @return the deviceAddress
     */
    public String getDeviceAddress()
    {
        return this.deviceAddress;
    }
    /**
     * @param deviceAddress the deviceAddress to set
     */
    public void setDeviceAddress(String deviceAddress)
    {
        this.deviceAddress = deviceAddress;
    }
    /**
     * @return the deviceType
     */
    public String getDeviceType()
    {
        return this.deviceType;
    }
    /**
     * @param deviceType the deviceType to set
     */
    public void setDeviceType(String deviceType)
    {
        this.deviceType = deviceType;
    }
    /**
     * @return the activationDate
     */
    public String getActivationDate()
    {
        return this.activationDate;
    }
    /**
     * @param activationDate the activationDate to set
     */
    public void setActivationDate(String activationDate)
    {
        this.activationDate = activationDate;
    }
    
    /**
     * @return the deviceGroupRef
     */
    public String getDeviceGroup()
    {
        return this.deviceGroup;
    }
    /**
     * @param deviceGroupRef the deviceGroupRef to set
     */
    public void setDeviceGroup(String deviceGroupRef)
    {
        this.deviceGroup = deviceGroupRef;
    }
    /**
     * @return the userGroupRef
     */
    public String getUserGroup()
    {
        return this.userGroup;
    }
    /**
     * @param userGroupRef the userGroupRef to set
     */
    public void setUserGroup(String userGroupRef)
    {
        this.userGroup = userGroupRef;
    }
    /**
     * @return the createdDate
     */
    public String getCreatedDate()
    {
        return this.createdDate;
    }
    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(String createdDate)
    {
        this.createdDate = createdDate;
    }
    /**
     * @return the tags
     */
    public Set<AssetTag> getTags()
    {
        return this.tags;
    }
    /**
     * @param tags the tags to set
     */
    public void setTags(Set<AssetTag> tags)
    {
        this.tags = tags;
    }
    
    /**
     * @return the deviceConfig
     */
    public Map<?, ?> getDeviceConfig()
    {
        return this.deviceConfig;
    }
    /**
     * @param deviceConfig the deviceConfig to set
     */
    public void setDeviceConfig(Map<?, ?> deviceConfig)
    {
        this.deviceConfig = deviceConfig;
    }
    /**
     * @return the updateDate
     */
    public String getUpdateDate()
    {
        return this.updateDate;
    }
    /**
     * @param updateDate the updateDate to set
     */
    public void setUpdateDate(String updateDate)
    {
        this.updateDate = updateDate;
    }
	
    /**
     * 
     * @return -
     */
	public String getExpirationDate() {
		return this.expirationDate;
	}
	
	/**
	 * 
	 * @param expirationDate -
	 */
	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}
    /**
     * @return the geLocation
     */
    public GeoLocation getGeoLocation()
    {
        return this.geoLocation;
    }
    /**
     * @param geLocation the geLocation to set
     */
    public void setGeoLocation(GeoLocation geLocation)
    {
        this.geoLocation = geLocation;
    }
    
    
}
