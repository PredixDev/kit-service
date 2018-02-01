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
import java.util.Set;

/**
 * User Group reference
 * 
 * @author 212421693 -
 */
public class UserGroup {

	private String uri;
	/**
	 * 
	 */
	Set<String> uaaUsers = new HashSet<String>();
	/**
	 * 
	 */
	Set<String> uaaOwners = new HashSet<String>();

	/**
	 * 
	 */
	String createdDate;
	/**
	 * 
	 */
	String updatedDate;

	/**
	 * @return the uri
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the users
	 */
	public Set<String> getUaaUsers() {
		return this.uaaUsers;
	}

	/**
	 * @param users
	 *            the users to set
	 */
	public void setUaaUsers(Set<String> users) {
		this.uaaUsers = users;
	}

	/**
	 * @return the owners
	 */
	public Set<String> getUaaOwners() {
		return this.uaaOwners;
	}

	/**
	 * @param owners
	 *            the owners to set
	 */
	public void setUaaOwners(Set<String> owners) {
		this.uaaOwners = owners;
	}

	/**
	 * @return the createdDate
	 */
	public String getCreatedDate() {
		return this.createdDate;
	}

	/**
	 * @param createdDate
	 *            the createdDate to set
	 */
	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}

	/**
	 * @return the updatedDate
	 */
	public String getUpdatedDate() {
		return this.updatedDate;
	}

	/**
	 * @param updatedDate
	 *            the updatedDate to set
	 */
	public void setUpdatedDate(String updatedDate) {
		this.updatedDate = updatedDate;
	}

}
