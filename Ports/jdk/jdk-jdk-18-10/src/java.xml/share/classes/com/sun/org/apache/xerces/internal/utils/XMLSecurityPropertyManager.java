/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.utils;

import javax.xml.XMLConstants;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.SecuritySupport;

/**
 * This class manages security related properties
 *
 */
public final class XMLSecurityPropertyManager {

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
     * Limits managed by the security manager
     */
    public static enum Property {
        ACCESS_EXTERNAL_DTD(XMLConstants.ACCESS_EXTERNAL_DTD,
                JdkConstants.EXTERNAL_ACCESS_DEFAULT),
        ACCESS_EXTERNAL_SCHEMA(XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                JdkConstants.EXTERNAL_ACCESS_DEFAULT);

        final String name;
        final String defaultValue;

        Property(String name, String value) {
            this.name = name;
            this.defaultValue = value;
        }

        public boolean equalsName(String propertyName) {
            return (propertyName == null) ? false : name.equals(propertyName);
        }

        public String propertyName() {
            return name;
        }

        String defaultValue() {
            return defaultValue;
        }
    }

    /**
     * Values of the properties as defined in enum Properties
     */
    private final String[] values;
    /**
     * States of the settings for each property in Properties above
     */
    private State[] states = {State.DEFAULT, State.DEFAULT};

    /**
     * Default constructor. Establishes default values
     */
    public XMLSecurityPropertyManager() {
        values = new String[Property.values().length];
        for (Property property : Property.values()) {
            values[property.ordinal()] = property.defaultValue();
        }
        //read system properties or jaxp.properties
        readSystemProperties();
    }

    /**
     * Finds the property with the given name.
     * @param propertyName the property name specified
     * @return the property name if found, null otherwise
     */
    public String find(String propertyName) {
        for (Property property : Property.values()) {
            if (property.equalsName(propertyName)) {
                return property.propertyName();
            }
        }
        return null;
    }

    /**
     * Set limit by property name and state
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
     * Set the value for a specific property.
     *
     * @param property the property
     * @param state the state of the property
     * @param value the value of the property
     */
    public void setValue(Property property, State state, String value) {
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
     * Return the value of the specified property
     *
     * @param propertyName the property name
     * @return the value of the property as a string
     */
    public String getValue(String propertyName) {
        int index = getIndex(propertyName);
        if (index > -1) {
            return getValueByIndex(index);
        }

        return null;
    }

    /**
     * Return the value of the specified property
     *
     * @param property the property
     * @return the value of the property
     */
    public String getValue(Property property) {
        return values[property.ordinal()];
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
    public int getIndex(String propertyName){
        for (Property property : Property.values()) {
            if (property.equalsName(propertyName)) {
                //internally, ordinal is used as index
                return property.ordinal();
            }
        }
        return -1;
    }

    /**
     * Read from system properties, or those in jaxp.properties
     */
    private void readSystemProperties() {
        getSystemProperty(Property.ACCESS_EXTERNAL_DTD,
                JdkConstants.SP_ACCESS_EXTERNAL_DTD);
        getSystemProperty(Property.ACCESS_EXTERNAL_SCHEMA,
                JdkConstants.SP_ACCESS_EXTERNAL_SCHEMA);
    }

    /**
     * Read from system properties, or those in jaxp.properties
     *
     * @param property the property
     * @param systemProperty the name of the system property
     */
    private void getSystemProperty(Property property, String systemProperty) {
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
