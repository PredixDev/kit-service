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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.ge.predix.solsvc.bootstrap.ams.dto.Group;

/**
 * 
 * @author 212421693 -
 */
public class KitGroup extends Group
{
    /**
     * 
     */
    Set<String> groupRef = new HashSet<String>();

    /**
     * @return the groupRef
     */
    public Set<String> getGroupRef()
    {
        return this.groupRef;
    }

    /**
     * @param groupRef the groupRef to set
     */
    public void setGroupRef(Set<String> groupRef)
    {
        this.groupRef = groupRef;
    }

    /* (non-Javadoc)
     * @see com.ge.predix.solsvc.bootstrap.ams.dto.Group#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other)
    {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    
    /* (non-Javadoc)
     * @see com.ge.predix.solsvc.bootstrap.ams.dto.Group#hashCode()
     */
    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
