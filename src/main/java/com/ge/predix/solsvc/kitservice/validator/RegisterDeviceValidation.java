/*
 * Copyright (c) 2017 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
 
package com.ge.predix.solsvc.kitservice.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.ge.predix.solsvc.kitservice.model.RegisterDevice;

/**
 * 
 * @author 212421693 -
 */
public class RegisterDeviceValidation implements Validator
{
    

    private Pattern pattern;
    private Pattern deviceTypePattern;
    private Pattern groupRefPattern;

    @SuppressWarnings("javadoc")
    public static final String DEVICE_NAME_PATTERN = "^[A-Za-z0-9-]*$"; //$NON-NLS-1$
    @SuppressWarnings("javadoc")
    public static final String DEVICE_TYPE_PATTERN = "^[A-Za-z0-9]*$"; //$NON-NLS-1$
    @SuppressWarnings("javadoc")
    public static final String GROUP_REF_PATTERN = "^[A-Za-z0-9-/]*$"; //$NON-NLS-1$
    @SuppressWarnings("javadoc")
    public static final int DEVICE_ADDRESS_LENGTH = 75;
    @SuppressWarnings("javadoc")
    public static final int DEVICE_NAME_LENGTH = 50;

    
    /**
     *  -
     */
    public RegisterDeviceValidation()
    {
        this.pattern = Pattern.compile(DEVICE_NAME_PATTERN);
        this.deviceTypePattern = Pattern.compile(DEVICE_TYPE_PATTERN);
        this.groupRefPattern = Pattern.compile(GROUP_REF_PATTERN);
    }


    /* (non-Javadoc)
     * @see org.springframework.validation.Validator#supports(java.lang.Class)
     */
    @Override
    public boolean supports(Class<?> clazz)
    {
        return RegisterDevice.class.equals(clazz);
    }

    /* (non-Javadoc)
     * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
     */
    @Override
    public void validate(Object target, Errors errors)
    {
        RegisterDevice device = (RegisterDevice) target;
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "deviceName", "field.required"); //$NON-NLS-1$//$NON-NLS-2$
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "deviceAddress", "field.required"); //$NON-NLS-1$//$NON-NLS-2$
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "deviceType", "field.required"); //$NON-NLS-1$//$NON-NLS-2$
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userId", "field.required"); //$NON-NLS-1$//$NON-NLS-2$
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "groupRef", "field.required"); //$NON-NLS-1$//$NON-NLS-2$
        
        
        if(!validate(device.getDeviceName() , this.pattern)){
            errors.rejectValue("DeviceName", "Device Name should match ^[A-Za-z0-9-]*");  //$NON-NLS-1$//$NON-NLS-2$
        }
        if(!validate(device.getDeviceAddress(),this.pattern)){
            errors.rejectValue("DeviceAddress", "Device Address should match ^[A-Za-z0-9-]*");  //$NON-NLS-1$//$NON-NLS-2$
        }
        if(!validate(device.getDeviceType(),this.deviceTypePattern)){
            errors.rejectValue("DeviceType", "Device Type should match ^[A-Za-z0-9]*");  //$NON-NLS-1$//$NON-NLS-2$
        }
        if(!validate(device.getGroupRef(),this.groupRefPattern)){
            errors.rejectValue("GroupRef", "GroupRef should match ^[A-Za-z0-9-/]*");  //$NON-NLS-1$//$NON-NLS-2$
        }
        
        if(DEVICE_NAME_LENGTH <= device.getDeviceName().length() ){
            errors.rejectValue("DeviceName", "Device Name is limited to max 50");  //$NON-NLS-1$//$NON-NLS-2$
        }
        if( DEVICE_ADDRESS_LENGTH <= device.getDeviceAddress().length()){
            errors.rejectValue("DeviceAddress", "Device Address is limited to max 75");  //$NON-NLS-1$//$NON-NLS-2$
        }
    }


    /**
     * @param deviceName -
     * @param pattern 
     * @return -
     */
    public static boolean validate(String deviceName, Pattern pattern)
    {
        Matcher matcher = pattern.matcher(deviceName);
        return matcher.matches();
    }

}
