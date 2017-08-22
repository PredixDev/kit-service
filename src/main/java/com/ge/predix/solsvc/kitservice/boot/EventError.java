/*
 * Copyright (c) 2016 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.kitservice.boot;

import java.util.Map;

import org.springframework.util.StringUtils;



/**
 * 
 * @author 212421693 -
 */
public class EventError
{
    /**
     * status
     */
    Integer status;
    /**
     * error
     */
    String  error;
    
    /**
     * message
     */
    String  message;
    /**
     * timeStamp
     */
    String  timeStamp;
    
    /**
     * code 
     */
    String code;

    /**
     * @param status
     *            -
     * @param errorAttributes
     *            -
     */
    public EventError(int status, Map<String, Object> errorAttributes)
    {
        this.status = status;
        this.error = (String) errorAttributes.get("error"); //$NON-NLS-1$
        this.message = (String) errorAttributes.get("message"); //$NON-NLS-1$
        this.code = (String) errorAttributes.get("code"); //$NON-NLS-1$
        if(! StringUtils.isEmpty(errorAttributes.get("timestamp"))) {//$NON-NLS-1$
            this.timeStamp = errorAttributes.get("timestamp").toString(); //$NON-NLS-1$
        }

    }

    /**
     * @return the status
     */
    public Integer getStatus()
    {
        return this.status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(Integer status)
    {
        this.status = status;
    }

    /**
     * @return the error
     */
    public String getError()
    {
        return this.error;
    }

    /**
     * @param error the error to set
     */
    public void setError(String error)
    {
        this.error = error;
    }

    /**
     * @return the message
     */
    public String getMessage()
    {
        return this.message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * @return the timeStamp
     */
    public String getTimeStamp()
    {
        return this.timeStamp;
    }

    /**
     * @param timeStamp the timeStamp to set
     */
    public void setTimeStamp(String timeStamp)
    {
        this.timeStamp = timeStamp;
    }
    
    
    
    
}
