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

import java.io.Serializable;

/**
 * 
 * @author 212421693 -
 */
public class GeoLocation implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = -3350292912815020657L;
    /**
     * 
     */
    String latitude;
    
    /**
     * 
     */
    String longitude;
    
    /**
     * @return the latitude
     */
    public String getLatitude()
    {
        return this.latitude;
    }

    /**
     * @param latitude the latitude to set
     */
    public void setLatitude(String latitude)
    {
        this.latitude = latitude;
    }

    /**
     * @return the longitude
     */
    public String getLongitude()
    {
        return this.longitude;
    }

    /**
     * @param longitude the longitude to set
     */
    public void setLongitude(String longitude)
    {
        this.longitude = longitude;
    }

  
}
