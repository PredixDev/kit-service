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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ge.predix.entity.fielddata.FieldData;
import com.ge.predix.entity.getfielddata.GetFieldDataRequest;
import com.ge.predix.entity.getfielddata.GetFieldDataResult;
import com.ge.predix.solsvc.ext.util.JsonMapper;
import com.ge.predix.solsvc.fdh.handler.GetDataHandler;

/**
 * 
 * @author 212421693 -
 */
public abstract class BaseManager {
	private static final Logger log = LoggerFactory.getLogger(BaseManager.class);

	/**
	 * 
	 */
	@Autowired
	protected JsonMapper jsonMapper;

	@Autowired
	private GetDataHandler assetGetFieldDataHandler;

	/**
	 * @param request
	 *            -
	 * @param headers
	 *            -
	 * @return -
	 */
	protected List<FieldData> getFieldDataResult(GetFieldDataRequest request, List<Header> headers) {
		GetFieldDataResult getResult = this.assetGetFieldDataHandler.getData(request, null, headers);
		log.debug(this.jsonMapper.toJson(getResult));
		if (!CollectionUtils.isEmpty(getResult.getErrorEvent())) {
			log.info("Error: fetching data" + this.jsonMapper.toJson(getResult)); //$NON-NLS-1$
			return null;
			// TBD do something
		}
		if (CollectionUtils.isEmpty(getResult.getFieldData()) || (CollectionUtils.isNotEmpty(getResult.getFieldData())
				&& getResult.getFieldData().get(0).getData() == null)) {
			log.info("Data Not found for " + this.jsonMapper.toJson(getResult)); //$NON-NLS-1$
			return null;

		}
		return getResult.getFieldData();

	}

	/**
	 * @return the jsonMapper
	 */
	public JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	/**
	 * @param jsonMapper
	 *            the jsonMapper to set
	 */
	public void setJsonMapper(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/**
	 * @return the assetGetFieldDataHandler
	 */
	public GetDataHandler getAssetGetFieldDataHandler() {
		return this.assetGetFieldDataHandler;
	}

	/**
	 * @param assetGetFieldDataHandler
	 *            the assetGetFieldDataHandler to set
	 */
	public void setAssetGetFieldDataHandler(GetDataHandler assetGetFieldDataHandler) {
		this.assetGetFieldDataHandler = assetGetFieldDataHandler;
	}

}
