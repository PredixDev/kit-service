/*
 * Copyright (c) 2017 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
 
package com.ge.predix.solsvc.kitservice.error;

/**
 * 
 * @author 212421693 -
 */

public class DeviceRegistrationError extends Exception
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     *  -
     */
    public DeviceRegistrationError()
    {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message -
     * @param cause -
     * @param enableSuppression -
     * @param writableStackTrace -
     */
    public DeviceRegistrationError(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param message -
     * @param cause -
     */
    public DeviceRegistrationError(String message, Throwable cause)
    {
        super(message, cause);
        
    }

    /**
     * @param message -
     */
    public DeviceRegistrationError(String message)
    {
        super(message);
       
    }

    /**
     * @param cause -
     */
    public DeviceRegistrationError(Throwable cause)
    {
        super(cause);
       
    }
    

}
