/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.org.apache.xalan.internal.utils;

import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.SecuritySupport;

/**
 * This is the base class for features and properties
 *
 * @LastModified: May 2021
 */
public abstract class FeaturePropertyBase {

    /**
     * States of the settings of a property, in the order: default value, value
     * set by FEATURE_SECURE_PROCESSING, jaxp.properties file, jaxp system
     * properties, and jaxp api properties
     */
    public static enum State {
        //this order reflects the overriding order
        DEFAULT, FSP, JAXPDOTPROPERTIES, SYSTEMPROPERTY, APIPROPERTY
    }


    /**
     * Values of the properties as defined in enum Properties
     */
    String[] values = null;
    /**
     * States of the settings for each property in Properties above
     */
    State[] states = {State.DEFAULT, State.DEFAULT};


    /**
     * Set the value for a specific property.
     *
     * @param property the property
     * @param state the state of the property
     * @param value the value of the property
     */
    public void setValue(Enum<?> property, State state, String value) {
        //only update if it shall override
        if (state.compareTo(states[property.ordinal()]) >= 0) {
            values[property.ordinal()] = value;
            states[property.ordinal()] = state;
        }
    }

    /**
     * Set the value of a property by its index
     * @param index the index of the property
     * @param state the state of the property
     * @param value the value of the property
     */
    public void setValue(int index, State state, String value) {
        //only update if it shall override
        if (state.compareTo(states[index]) >= 0) {
            values[index] = value;
            states[index] = state;
        }
    }

     /**
     * Set value by property name and state
     * @param propertyName property name
     * @param state the state of the property
     * @param value the value of the property
     * @return true if the property is managed by the security property manager;
     *         false if otherwise.
     */
    public boolean setValue(String propertyName, State state, Object value) {
        int index = getIndex(propertyName);
        if (index > -1) {
            setValue(index, state, (String)value);
            return true;
        }
        return false;
    }

     /**
     * Set value by property name and state
     * @param propertyName property name
     * @param state the state of the property
     * @param value the value of the property
     * @return true if the property is managed by the security property manager;
     *         false if otherwise.
     */
    public boolean setValue(String propertyName, State state, boolean value) {
        int index = getIndex(propertyName);
        if (index > -1) {
            if (value) {
                setValue(index, state, JdkConstants.FEATURE_TRUE);
            } else {
                setValue(index, state, JdkConstants.FEATURE_FALSE);
            }
            return true;
        }
        return false;
    }

    /**
     * Return the value of the specified property
     *
     * @param property the property
     * @return the value of the property
     */
    public String getValue(Enum<?> property) {
        return values[property.ordinal()];
    }

    /**
     * Return the value of the specified property
     *
     * @param property the property
     * @return the value of the property
     */
    public String getValue(String property) {
        int index = getIndex(property);
        if (index > -1) {
            return getValueByIndex(index);
        }
        return null;
    }

    /**
     * Return the value of the specified property.
     *
     * @param propertyName the property name
     * @return the value of the property as a string. If a property is managed
     * by this manager, its value shall not be null.
     */
    public String getValueAsString(String propertyName) {
        int index = getIndex(propertyName);
        if (index > -1) {
            return getValueByIndex(index);
        }

        return null;
    }

    /**
     * Return the value of a property by its ordinal
     * @param index the index of a property
     * @return value of a property
     */
    public String getValueByIndex(int index) {
        return values[index];
    }

    /**
     * Get the index by property name
     * @param propertyName property name
     * @return the index of the property if found; return -1 if not
     */
    public abstract int getIndex(String propertyName);

    public <E extends Enum<E>> int getIndex(Class<E> property, String propertyName) {
        for (Enum<E> enumItem : property.getEnumConstants()) {
            if (enumItem.toString().equals(propertyName)) {
                //internally, ordinal is used as index
                return enumItem.ordinal();
            }
        }
        return -1;
    };


    /**
     * Read from system properties, or those in jaxp.properties
     *
     * @param property the property
     * @param systemProperty the name of the system property
     */
    void getSystemProperty(Enum<?> property, String systemProperty) {
        try {
            String value = SecuritySupport.getSystemProperty(systemProperty);
            if (value != null) {
                values[property.ordinal()] = value;
                states[property.ordinal()] = State.SYSTEMPROPERTY;
                return;
            }

            value = SecuritySupport.readJAXPProperty(systemProperty);
            if (value != null) {
                values[property.ordinal()] = value;
                states[property.ordinal()] = State.JAXPDOTPROPERTIES;
            }
        } catch (NumberFormatException e) {
            //invalid setting ignored
        }
    }
}
