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
import com.ge.predix.solsvc.kitservice.model.DeviceGroup;
import com.ge.predix.solsvc.kitservice.model.UserGroup;

/**
 * 
 * @author 212421693 -
 */
@Component
public class GroupManagementService extends BaseManager {
	private static final Logger log = LoggerFactory.getLogger(GroupManagementService.class);
	private static final String USERGROUP = "userGroup"; //$NON-NLS-1$

	/**
	 * @param headers
	 *            -
	 * @param userId
	 *            -
	 * @return -
	 */
	public UserGroup getUserGroupbyUserId(List<Header> headers, String userId) {
		UserGroup userGroup = null;
		if (StringUtils.isNotEmpty(userId)) {
			GetFieldDataRequest request = FdhUtils.createGetUserGroupRequest("/" + USERGROUP, "PredixString", userId); //$NON-NLS-1$ //$NON-NLS-2$
			List<FieldData> fieldData = getFieldDataResult(request, headers);
			if (CollectionUtils.isEmpty(fieldData)) {
				return null;
			}
			Data predixString = fieldData.get(0).getData();
			PredixString data = (PredixString) predixString;
			String deviceString = StringEscapeUtils.unescapeJava(data.getString());
			deviceString = deviceString.substring(1, deviceString.length() - 1);
			deviceString = deviceString.substring(0, deviceString.length());
			List<UserGroup> userGroups = this.jsonMapper.fromJsonArray("[" + deviceString + "]", UserGroup.class); //$NON-NLS-1$ //$NON-NLS-2$
			if (CollectionUtils.isNotEmpty(userGroups))
				userGroup = userGroups.get(0);
			return userGroup;

		}
		return userGroup;
	}

	/**
	 * @param headers
	 *            -
	 * @param deviceGroup
	 *            -
	 * @param userId
	 *            -
	 * @return -
	 */
	public UserGroup createUserGroup(String userId) {

		UserGroup userGroup = new UserGroup();
		userGroup.setUri("/" + USERGROUP + "/" + UUID.randomUUID().toString()); //$NON-NLS-1$ //$NON-NLS-2$
		userGroup.getUaaOwners().add(userId);
		userGroup.getUaaUsers().add(userId);
		userGroup.setCreatedDate(String.valueOf(Instant.now().toEpochMilli()));
		return userGroup;

	}

	/**
	 * @param userGroupUri
	 *            -
	 * @param headers
	 *            -
	 * @param deviceGroup
	 *            -
	 * @param userId
	 *            -
	 * @return -
	 */
	public UserGroup getUserGroup(String userGroupUri, List<Header> headers) {

		UserGroup userGroup = null;

		GetFieldDataRequest request = FdhUtils.createGetUserGroupRequest(userGroupUri, "PredixString", null); //$NON-NLS-1$
		List<FieldData> fieldData = getFieldDataResult(request, headers);
		if (CollectionUtils.isEmpty(fieldData)) {
			return null;
		}
		Data predixString = fieldData.get(0).getData();
		PredixString data = (PredixString) predixString;
		String deviceString = StringEscapeUtils.unescapeJava(data.getString());
		deviceString = deviceString.substring(1, deviceString.length() - 1);
		deviceString = deviceString.substring(0, deviceString.length());
		List<UserGroup> userGroups = this.jsonMapper.fromJsonArray("[" + deviceString + "]", UserGroup.class); //$NON-NLS-1$ //$NON-NLS-2$
		if (CollectionUtils.isNotEmpty(userGroups))
			userGroup = userGroups.get(0);
		return userGroup;

	}

	/**
	 * 
	 * @param userGroup
	 *            -
	 * @return -
	 */
	public String getUserGroupString(final UserGroup userGroup) {
		ArrayList<Object> kitModels = new ArrayList<>();
		kitModels.add(userGroup);
		return this.jsonMapper.toJson(kitModels);
	}

	/**
	 * 
	 * @param deviceGroupRef
	 *            -
	 * @param userGroupRef
	 *            -
	 * @return -
	 */
	DeviceGroup createDeviceGroup(String deviceGroupRef, String userGroupRef) {
		DeviceGroup group = new DeviceGroup();
		group.setUri(deviceGroupRef);
		group.setCreatedate(String.valueOf(Instant.now().toEpochMilli()));
		group.setDescription(deviceGroupRef);
		String[] parts = deviceGroupRef.split("/"); //$NON-NLS-1$
		if (parts.length > 0) {
			group.setName(parts[parts.length - 1]);
		}
		group.getUserGroup().add(userGroupRef);
		return group;
	}

	/**
	 * 
	 * @param headers
	 *            -
	 * @param deviceGroupRef
	 *            -
	 * @return -
	 */
	DeviceGroup getDeviceGroup(List<Header> headers, String deviceGroupRef) {
		log.info("Calling deviceGroupRef for " + deviceGroupRef); //$NON-NLS-1$
		DeviceGroup group = null;
		if (StringUtils.isNotEmpty(deviceGroupRef)) {
			GetFieldDataRequest request = FdhUtils.createGetGroupRequest(deviceGroupRef, "PredixString"); //$NON-NLS-1$
			List<FieldData> fieldData = getFieldDataResult(request, headers);
			if (CollectionUtils.isEmpty(fieldData)) {
				return null;
			}
			Data predixString = fieldData.get(0).getData();
			PredixString data = (PredixString) predixString;
			String deviceString = StringEscapeUtils.unescapeJava(data.getString());
			deviceString = deviceString.substring(1, deviceString.length() - 1);
			deviceString = deviceString.substring(0, deviceString.length());
			List<DeviceGroup> groups = this.jsonMapper.fromJsonArray("[" + deviceString + "]", DeviceGroup.class); //$NON-NLS-1$ //$NON-NLS-2$
			if (CollectionUtils.isNotEmpty(groups))
				group = groups.get(0);
			return group;

		}
		return group;

	}

	/**
	 * @param deviceGroup
	 *            -
	 * @return -
	 */
	public String getDeviceGroupString(DeviceGroup deviceGroup) {
		List<Object> kitModels = new ArrayList<>();
		kitModels.add(deviceGroup);
		return this.jsonMapper.toJson(kitModels);
	}

}
