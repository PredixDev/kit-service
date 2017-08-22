/*
 * Copyright (c) 2017 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
 
package com.ge.predix.solsvc.kitservice.boot.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;

import com.ge.predix.entity.assetfilter.AssetFilter;
import com.ge.predix.entity.field.Field;
import com.ge.predix.entity.field.fieldidentifier.FieldIdentifier;
import com.ge.predix.entity.fielddata.FieldData;
import com.ge.predix.entity.fielddata.PredixString;
import com.ge.predix.entity.fielddatacriteria.FieldDataCriteria;
import com.ge.predix.entity.fieldselection.FieldSelection;
import com.ge.predix.entity.getfielddata.GetFieldDataRequest;
import com.ge.predix.entity.putfielddata.PutFieldDataCriteria;
import com.ge.predix.entity.putfielddata.PutFieldDataRequest;

/**
 * 
 * @author 212421693 -
 */
public class FdhUtils
{
    
    
  /**
   * 
   * @param device -
   * @param group - 
   * @param userGroup -
   * @return -
   */
    public static PutFieldDataRequest createRegisterPutRequest(String device, String group, String userGroup)
    {
        

        // Request
        PutFieldDataRequest putFieldDataRequest = new PutFieldDataRequest();
        putFieldDataRequest.setCorrelationId("string"); //$NON-NLS-1$
        putFieldDataRequest.getPutFieldDataCriteria().add( getFieldCriteria(device));
        if(StringUtils.isNotEmpty(group))
            putFieldDataRequest.getPutFieldDataCriteria().add( getFieldCriteria(group));
        if(StringUtils.isNotEmpty(userGroup))
            putFieldDataRequest.getPutFieldDataCriteria().add( getFieldCriteria(userGroup));
     
        return putFieldDataRequest;
    }


    /**
     * @param device
     * @return -
     */
    private static PutFieldDataCriteria getFieldCriteria(String dataString)
    {
        FieldData fieldData = new FieldData();
        Field field = new Field();
        FieldIdentifier fieldIdentifier = new FieldIdentifier();
        field.setFieldIdentifier(fieldIdentifier);
        fieldData.getField().add(field);
        
        //device
        PredixString data = new PredixString();
        data.setString(dataString);
        fieldData.setData(data);

        // Criteria 
        PutFieldDataCriteria fieldDataCriteria = new PutFieldDataCriteria();
        fieldDataCriteria.setFieldData(fieldData);
        return fieldDataCriteria;
    }


    /**
     * @param filterFieldValue -
     * @param expectedDataType -
     * @param deviceIdentifier -
     * @param string - 
     * @param userId -
     * @param deviceAddress -
     * @return -
     * @throws UnsupportedEncodingException -
     */
    public static GetFieldDataRequest createGetUserDeviceRequest(String filterFieldValue, String expectedDataType, String userId,
            String deviceAddress) throws UnsupportedEncodingException
    {
        GetFieldDataRequest getFieldDataRequest = new GetFieldDataRequest();

        FieldDataCriteria fieldDataCriteria = new FieldDataCriteria();

        // SELECT
        FieldSelection fieldSelection = new FieldSelection();
        FieldIdentifier fieldIdentifier = new FieldIdentifier();
        fieldIdentifier.setId("/PredixString"); //$NON-NLS-1$ default needed by the system
        fieldSelection.setFieldIdentifier(fieldIdentifier);
        fieldSelection.setExpectedDataType(expectedDataType);

        // FILTER
        AssetFilter assetFilter = new AssetFilter();
        assetFilter.setUri(filterFieldValue);
       
        if(StringUtils.isEmpty(userId) && !StringUtils.isEmpty(deviceAddress)) {
            assetFilter.setFilterString("deviceAddress="+deviceAddress); //$NON-NLS-1$ 
        }
        else if(StringUtils.isEmpty(deviceAddress)) {
            String encodedUrl = URLEncoder.encode("uaaUsers="+userId+"<userGroup","UTF-8");//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assetFilter.setFilterString(encodedUrl); 
        } else {
           String encodedUrl = URLEncoder.encode("uaaUsers="+userId+"<userGroup:deviceAddress="+deviceAddress,"UTF-8");//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assetFilter.setFilterString(encodedUrl); 
        }

        // SELECT
        fieldDataCriteria.getFieldSelection().add(fieldSelection);
        // WHERE
        fieldDataCriteria.setFilter(assetFilter);

        getFieldDataRequest.getFieldDataCriteria().add(fieldDataCriteria);
        return getFieldDataRequest;
    }
    
   /**
    * 
    * @param groupRef - 
    * @param expectedDataType -
    * @param userId -
    * @return -
    */
    public static GetFieldDataRequest createGetGroupRequest(String groupRef, String expectedDataType)
    {
        GetFieldDataRequest getFieldDataRequest = new GetFieldDataRequest();

        FieldDataCriteria fieldDataCriteria = new FieldDataCriteria();

        // SELECT
        FieldSelection fieldSelection = new FieldSelection();
        FieldIdentifier fieldIdentifier = new FieldIdentifier();
        fieldIdentifier.setId("/PredixString"); //$NON-NLS-1$ default needed by the system
        fieldSelection.setFieldIdentifier(fieldIdentifier);
        fieldSelection.setExpectedDataType(expectedDataType);

        // FILTER
        AssetFilter assetFilter = new AssetFilter();
        assetFilter.setUri(groupRef);
        
        // SELECT
        fieldDataCriteria.getFieldSelection().add(fieldSelection);
        // WHERE
        fieldDataCriteria.setFilter(assetFilter);

        getFieldDataRequest.getFieldDataCriteria().add(fieldDataCriteria);
        return getFieldDataRequest;
    }


/**
 * @param userGroupRef -
 * @param deviceGroupRef -
 * @param expectedDataType -
 * @param userId -
 * @return -
 */
public static GetFieldDataRequest createGetUserGroupRequest(String userGroupRef, String expectedDataType, String userId)
{
    GetFieldDataRequest getFieldDataRequest = new GetFieldDataRequest();

    FieldDataCriteria fieldDataCriteria = new FieldDataCriteria();

    // SELECT
    FieldSelection fieldSelection = new FieldSelection();
    FieldIdentifier fieldIdentifier = new FieldIdentifier();
    fieldIdentifier.setId("/PredixString"); //$NON-NLS-1$ default needed by the system
    fieldSelection.setFieldIdentifier(fieldIdentifier);
    fieldSelection.setExpectedDataType(expectedDataType);

    // FILTER
    AssetFilter assetFilter = new AssetFilter();
    assetFilter.setUri(userGroupRef);
    
    if(StringUtils.isNotEmpty(userId)) {
        assetFilter.setFilterString("uaaUsers="+userId); //$NON-NLS-1$
    }
  
    // SELECT
    fieldDataCriteria.getFieldSelection().add(fieldSelection);
    // WHERE
    fieldDataCriteria.setFilter(assetFilter);

    getFieldDataRequest.getFieldDataCriteria().add(fieldDataCriteria);
    return getFieldDataRequest;
}

/**
 * 
 * @param filterFieldValue -
 * @param expectedDataType -
 * @return GetFieldDataRequest
 * @throws UnsupportedEncodingException -
 */
public static GetFieldDataRequest createGetAdminDeviceRequest(String filterFieldValue, String expectedDataType) throws UnsupportedEncodingException
{
    GetFieldDataRequest getFieldDataRequest = new GetFieldDataRequest();

    FieldDataCriteria fieldDataCriteria = new FieldDataCriteria();

    // SELECT
    FieldSelection fieldSelection = new FieldSelection();
    FieldIdentifier fieldIdentifier = new FieldIdentifier();
    fieldIdentifier.setId("/PredixString"); //$NON-NLS-1$ default needed by the system
    fieldSelection.setFieldIdentifier(fieldIdentifier);
    fieldSelection.setExpectedDataType(expectedDataType);

    // FILTER
    AssetFilter assetFilter = new AssetFilter();
    assetFilter.setUri(filterFieldValue);
   
   
    // SELECT
    fieldDataCriteria.getFieldSelection().add(fieldSelection);
    // WHERE
    fieldDataCriteria.setFilter(assetFilter);

    getFieldDataRequest.getFieldDataCriteria().add(fieldDataCriteria);
    return getFieldDataRequest;
}
}
