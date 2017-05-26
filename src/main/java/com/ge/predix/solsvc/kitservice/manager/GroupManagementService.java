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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ge.predix.entity.fielddata.Data;
import com.ge.predix.entity.fielddata.FieldData;
import com.ge.predix.entity.fielddata.PredixString;
import com.ge.predix.entity.getfielddata.GetFieldDataRequest;
import com.ge.predix.solsvc.kitservice.boot.utils.FdhUtils;
import com.ge.predix.solsvc.kitservice.model.KitGroup;
import com.ge.predix.solsvc.kitservice.model.UserGroup;

/**
 * 
 * @author 212421693 -
 */
@Component
public class GroupManagementService extends BaseManager
{
    private static final Logger log          = LoggerFactory.getLogger(GroupManagementService.class);
    private static final String USER_GROUP = "user-group"; //$NON-NLS-1$
    
    
    /**
     * @param headers -
     * @param userId -
     * @return -
     */
    public UserGroup getUserGroup(List<Header> headers, String userId)
    {
        UserGroup userGroup = null;
        if(StringUtils.isNotEmpty(userId)){
            GetFieldDataRequest request = FdhUtils.createGetUserGroupRequest("/"+USER_GROUP, "PredixString",userId); //$NON-NLS-1$ //$NON-NLS-2$
            List<FieldData> fieldData = getFieldDataResult(request,headers);
            if(CollectionUtils.isEmpty(fieldData)) {
                return null;
            }
            Data predixString = fieldData.get(0).getData();
            PredixString data = (PredixString) predixString;
            String deviceString = StringEscapeUtils.unescapeJava(data.getString());
            deviceString = deviceString.substring(1, deviceString.length() - 1);
            deviceString = deviceString.substring(0, deviceString.length());
            List<UserGroup> userGroups = this.jsonMapper.fromJsonArray("["+deviceString+"]", UserGroup.class); //$NON-NLS-1$ //$NON-NLS-2$
            if(CollectionUtils.isNotEmpty(userGroups)) 
                userGroup = userGroups.get(0);
            return userGroup;
       
        }
        return userGroup;
    }

    /**
     * @param headers -
     * @param userGroupRef -
     * @param groupRef  -
     * @param userId -
     * @return -
     */
    
    public String getOrCreateGroup(List<Header> headers, String userGroupRef, String groupRef)
    {
      
       KitGroup group =  getGroup(headers,groupRef);
       if( group == null) {
           group = createGroup(groupRef,userGroupRef);  
       }
        group.getGroupRef().add(userGroupRef);
        
        List<Object> kitModels = new ArrayList<>();
        kitModels.add(group);
        return this.jsonMapper.toJson(kitModels);
       
    }
    
    /**
     * @param headers -
     * @param groupRef -
     * @param userId -
     * @return -
     */
    public UserGroup getOrCreateUserGroup(List<Header> headers, String userId)
    {
      
        UserGroup userGroup = getUserGroup(headers,userId);
        if(userGroup == null) {
            userGroup = createUserGroup(userId);
        }
        else {
            userGroup.getUsers().add(userId); 
            userGroup.setUpdatedDate(String.valueOf(Instant.now().toEpochMilli()));
        }
        return userGroup;
       
        
    }
    
    /**
     * 
     * @param userGroup -
     * @return -
     */
    public String getUserGroupString(final UserGroup userGroup) {
        ArrayList<Object> kitModels = new ArrayList<>();
        kitModels.add(userGroup); 
        return this.jsonMapper.toJson(kitModels);
    }

    /**
     * @param groupRef
     * @param userId
     * @return -
     */
    private UserGroup createUserGroup(String userId)
    {
        UserGroup userGroup = new UserGroup();
        userGroup.setUri("/"+USER_GROUP+"/"+UUID.randomUUID().toString()); //$NON-NLS-1$ //$NON-NLS-2$
        userGroup.getOwners().add(userId);
        userGroup.getUsers().add(userId);
        userGroup.setCreatedDate(String.valueOf(Instant.now().toEpochMilli()));
        return userGroup;
        
    }


    /**
     * 
     * @param groupRef
     * @return -
     */
    private KitGroup createGroup(String groupRef,String userGroupRef)
    {
        KitGroup group = new KitGroup();
        group.setUri(groupRef);
        group.setCreatedate(String.valueOf(Instant.now().toEpochMilli()));
        group.setDescription(groupRef);
        String[] parts = groupRef.split("/"); //$NON-NLS-1$
        if(parts.length > 0 ){
        group.setName(parts[parts.length-1]);
        }
        group.getGroupRef().add(userGroupRef);
        return group;
    }

    /**
     * @param headers 
     * @param groupRef
     * @return -
     */
    private KitGroup getGroup(List<Header> headers, String groupRef)
    {
        log.info("Calling getGroup for "+groupRef); //$NON-NLS-1$
        KitGroup group = null;
        if(StringUtils.isNotEmpty(groupRef)){
            GetFieldDataRequest request = FdhUtils.createGetGroupRequest(groupRef, "PredixString"); //$NON-NLS-1$
            List<FieldData> fieldData = getFieldDataResult(request,headers);
            if(CollectionUtils.isEmpty(fieldData)) {
                return null;
            }
            Data predixString = fieldData.get(0).getData();
            PredixString data = (PredixString) predixString;
            String deviceString = StringEscapeUtils.unescapeJava(data.getString());
            deviceString = deviceString.substring(1, deviceString.length() - 1);
            deviceString = deviceString.substring(0, deviceString.length());
            List<KitGroup> groups = this.jsonMapper.fromJsonArray("["+deviceString+"]", KitGroup.class); //$NON-NLS-1$ //$NON-NLS-2$
            if(CollectionUtils.isNotEmpty(groups)) 
                group = groups.get(0);
            return group;
       
        }
        return group;
        
        
    }

}
