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

import com.sun.org.apache.xerces.internal.util.SecurityManager;
import java.util.concurrent.CopyOnWriteArrayList;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkProperty.State;
import jdk.xml.internal.JdkProperty.ImplPropMap;
import jdk.xml.internal.SecuritySupport;
import org.xml.sax.SAXException;

/**
 * This class manages standard and implementation-specific limitations.
 *
 */
public final class XMLSecurityManager {

    /**
     * Limits managed by the security manager
     */
    @SuppressWarnings("deprecation")
    public static enum Limit {

        ENTITY_EXPANSION_LIMIT("EntityExpansionLimit",
                JdkConstants.JDK_ENTITY_EXPANSION_LIMIT, JdkConstants.SP_ENTITY_EXPANSION_LIMIT, 0, 64000),
        MAX_OCCUR_NODE_LIMIT("MaxOccurLimit",
                JdkConstants.JDK_MAX_OCCUR_LIMIT, JdkConstants.SP_MAX_OCCUR_LIMIT, 0, 5000),
        ELEMENT_ATTRIBUTE_LIMIT("ElementAttributeLimit",
                JdkConstants.JDK_ELEMENT_ATTRIBUTE_LIMIT, JdkConstants.SP_ELEMENT_ATTRIBUTE_LIMIT, 0, 10000),
        TOTAL_ENTITY_SIZE_LIMIT("TotalEntitySizeLimit",
                JdkConstants.JDK_TOTAL_ENTITY_SIZE_LIMIT, JdkConstants.SP_TOTAL_ENTITY_SIZE_LIMIT, 0, 50000000),
        GENERAL_ENTITY_SIZE_LIMIT("MaxEntitySizeLimit",
                JdkConstants.JDK_GENERAL_ENTITY_SIZE_LIMIT, JdkConstants.SP_GENERAL_ENTITY_SIZE_LIMIT, 0, 0),
        PARAMETER_ENTITY_SIZE_LIMIT("MaxEntitySizeLimit",
                JdkConstants.JDK_PARAMETER_ENTITY_SIZE_LIMIT, JdkConstants.SP_PARAMETER_ENTITY_SIZE_LIMIT, 0, 1000000),
        MAX_ELEMENT_DEPTH_LIMIT("MaxElementDepthLimit",
                JdkConstants.JDK_MAX_ELEMENT_DEPTH, JdkConstants.SP_MAX_ELEMENT_DEPTH, 0, 0),
        MAX_NAME_LIMIT("MaxXMLNameLimit",
                JdkConstants.JDK_XML_NAME_LIMIT, JdkConstants.SP_XML_NAME_LIMIT, 1000, 1000),
        ENTITY_REPLACEMENT_LIMIT("EntityReplacementLimit",
                JdkConstants.JDK_ENTITY_REPLACEMENT_LIMIT, JdkConstants.SP_ENTITY_REPLACEMENT_LIMIT, 0, 3000000);

        final String key;
        final String apiProperty;
        final String systemProperty;
        final int defaultValue;
        final int secureValue;

        Limit(String key, String apiProperty, String systemProperty, int value, int secureValue) {
            this.key = key;
            this.apiProperty = apiProperty;
            this.systemProperty = systemProperty;
            this.defaultValue = value;
            this.secureValue = secureValue;
        }

        /**
         * Checks whether the specified name is a limit. Checks both the
         * property and System Property which is now the new property name.
         *
         * @param name the specified name
         * @return true if there is a match, false otherwise
         */
        public boolean is(String name) {
            // current spec: new property name == systemProperty
            return (systemProperty != null && systemProperty.equals(name)) ||
                   // current spec: apiProperty is legacy
                   (apiProperty.equals(name));
        }

        /**
         * Returns the state of a property name. By the specification as of JDK 17,
         * the "jdk.xml." prefixed System property name is also the current API
         * name. The URI-based qName is legacy.
         *
         * @param name the property name
         * @return the state of the property name, null if no match
         */
        public State getState(String name) {
            if (systemProperty != null && systemProperty.equals(name)) {
                return State.APIPROPERTY;
            } else if (apiProperty.equals(name)) {
                //the URI-style qName is legacy
                return State.LEGACY_APIPROPERTY;
            }
            return null;
        }

        public String key() {
            return key;
        }

        public String apiProperty() {
            return apiProperty;
        }

        public String systemProperty() {
            return systemProperty;
        }

        public int defaultValue() {
            return defaultValue;
        }

        int secureValue() {
            return secureValue;
        }
    }

    /**
     * Map old property names with the new ones
     */
    public static enum NameMap {

        ENTITY_EXPANSION_LIMIT(JdkConstants.SP_ENTITY_EXPANSION_LIMIT, JdkConstants.ENTITY_EXPANSION_LIMIT),
        MAX_OCCUR_NODE_LIMIT(JdkConstants.SP_MAX_OCCUR_LIMIT, JdkConstants.MAX_OCCUR_LIMIT),
        ELEMENT_ATTRIBUTE_LIMIT(JdkConstants.SP_ELEMENT_ATTRIBUTE_LIMIT, JdkConstants.ELEMENT_ATTRIBUTE_LIMIT);
        final String newName;
        final String oldName;

        NameMap(String newName, String oldName) {
            this.newName = newName;
            this.oldName = oldName;
        }

        String getOldName(String newName) {
            if (newName.equals(this.newName)) {
                return oldName;
            }
            return null;
        }
    }
    private static final int NO_LIMIT = 0;
    /**
     * Values of the properties
     */
    private final int[] values;
    /**
     * States of the settings for each property
     */
    private State[] states;
    /**
     * Flag indicating if secure processing is set
     */
    boolean secureProcessing;

    /**
     * States that determine if properties are set explicitly
     */
    private boolean[] isSet;


    /**
     * Index of the special entityCountInfo property
     */
    private final int indexEntityCountInfo = 10000;
    private String printEntityCountInfo = "";

    /**
     * Default constructor. Establishes default values for known security
     * vulnerabilities.
     */
    public XMLSecurityManager() {
        this(false);
    }

    /**
     * Instantiate Security Manager in accordance with the status of
     * secure processing
     * @param secureProcessing
     */
    public XMLSecurityManager(boolean secureProcessing) {
        values = new int[Limit.values().length];
        states = new State[Limit.values().length];
        isSet = new boolean[Limit.values().length];
        this.secureProcessing = secureProcessing;
        for (Limit limit : Limit.values()) {
            if (secureProcessing) {
                values[limit.ordinal()] = limit.secureValue;
                states[limit.ordinal()] = State.FSP;
            } else {
                values[limit.ordinal()] = limit.defaultValue();
                states[limit.ordinal()] = State.DEFAULT;
            }
        }
        //read system properties or jaxp.properties
        readSystemProperties();
    }

    /**
     * Setting FEATURE_SECURE_PROCESSING explicitly
     */
    public void setSecureProcessing(boolean secure) {
        secureProcessing = secure;
        for (Limit limit : Limit.values()) {
            if (secure) {
                setLimit(limit.ordinal(), State.FSP, limit.secureValue());
            } else {
                setLimit(limit.ordinal(), State.FSP, limit.defaultValue());
            }
        }
    }

    /**
     * Return the state of secure processing
     * @return the state of secure processing
     */
    public boolean isSecureProcessing() {
        return secureProcessing;
    }

    /**
     * Finds a limit's new name with the given property name.
     * @param propertyName the property name specified
     * @return the limit's new name if found, null otherwise
     */
    public String find(String propertyName) {
        for (Limit limit : Limit.values()) {
            if (limit.is(propertyName)) {
                // current spec: new property name == systemProperty
                return limit.systemProperty();
            }
        }
        //ENTITYCOUNT's new name is qName
        if (ImplPropMap.ENTITYCOUNT.is(propertyName)) {
            return ImplPropMap.ENTITYCOUNT.qName();
        }
        return null;
    }

    /**
     * Set limit by property name and state
     * @param propertyName property name
     * @param state the state of the property
     * @param value the value of the property
     * @return true if the property is managed by the security manager; false
     *              if otherwise.
     */
    public boolean setLimit(String propertyName, State state, Object value) {
        int index = getIndex(propertyName);
        if (index > -1) {
            State pState = state;
            if (index != indexEntityCountInfo && state == State.APIPROPERTY) {
                pState = (Limit.values()[index]).getState(propertyName);
            }
            setLimit(index, pState, value);
            return true;
        }
        return false;
    }

    /**
     * Set the value for a specific limit.
     *
     * @param limit the limit
     * @param state the state of the property
     * @param value the value of the property
     */
    public void setLimit(Limit limit, State state, int value) {
        setLimit(limit.ordinal(), state, value);
    }

    /**
     * Set the value of a property by its index
     *
     * @param index the index of the property
     * @param state the state of the property
     * @param value the value of the property
     */
    public void setLimit(int index, State state, Object value) {
        if (index == indexEntityCountInfo) {
            printEntityCountInfo = (String)value;
        } else {
            int temp;
            if (value instanceof Integer) {
                temp = (Integer)value;
            } else {
                temp = Integer.parseInt((String) value);
                if (temp < 0) {
                    temp = 0;
                }
            }
            setLimit(index, state, temp);
        }
    }

    /**
     * Set the value of a property by its index
     *
     * @param index the index of the property
     * @param state the state of the property
     * @param value the value of the property
     */
    public void setLimit(int index, State state, int value) {
        if (index == indexEntityCountInfo) {
            //if it's explicitly set, it's treated as yes no matter the value
            printEntityCountInfo = JdkConstants.JDK_YES;
        } else {
            //only update if it shall override
            if (state.compareTo(states[index]) >= 0) {
                values[index] = value;
                states[index] = state;
                isSet[index] = true;
            }
        }
    }

    /**
     * Return the value of the specified property
     *
     * @param propertyName the property name
     * @return the value of the property as a string. If a property is managed
     * by this manager, its value shall not be null.
     */
    public String getLimitAsString(String propertyName) {
        int index = getIndex(propertyName);
        if (index > -1) {
            return getLimitValueByIndex(index);
        }

        return null;
    }
    /**
     * Return the value of the specified property
     *
     * @param limit the property
     * @return the value of the property
     */
    public int getLimit(Limit limit) {
        return values[limit.ordinal()];
    }

    /**
     * Return the value of a property by its ordinal
     *
     * @param limit the property
     * @return value of a property
     */
    public String getLimitValueAsString(Limit limit) {
        return Integer.toString(values[limit.ordinal()]);
    }

    /**
     * Return the value of a property by its ordinal
     *
     * @param index the index of a property
     * @return limit of a property as a string
     */
    public String getLimitValueByIndex(int index) {
        if (index == indexEntityCountInfo) {
            return printEntityCountInfo;
        }

        return Integer.toString(values[index]);
    }

    /**
     * Return the state of the limit property
     *
     * @param limit the limit
     * @return the state of the limit property
     */
    public State getState(Limit limit) {
        return states[limit.ordinal()];
    }

    /**
     * Return the state of the limit property
     *
     * @param limit the limit
     * @return the state of the limit property
     */
    public String getStateLiteral(Limit limit) {
        return states[limit.ordinal()].literal();
    }

    /**
     * Get the index by property name
     *
     * @param propertyName property name
     * @return the index of the property if found; return -1 if not
     */
    public int getIndex(String propertyName) {
        for (Limit limit : Limit.values()) {
            // see JDK-8265248, accept both the URL and jdk.xml as prefix
            if (limit.is(propertyName)) {
                //internally, ordinal is used as index
                return limit.ordinal();
            }
        }
        //special property to return entity count info
        if (ImplPropMap.ENTITYCOUNT.is(propertyName)) {
            return indexEntityCountInfo;
        }
        return -1;
    }

    /**
     * Check if there's no limit defined by the Security Manager
     * @param limit
     * @return
     */
    public boolean isNoLimit(int limit) {
        return limit==NO_LIMIT;
    }
    /**
     * Check if the size (length or count) of the specified limit property is
     * over the limit
     *
     * @param limit the type of the limit property
     * @param entityName the name of the entity
     * @param size the size (count or length) of the entity
     * @return true if the size is over the limit, false otherwise
     */
    public boolean isOverLimit(Limit limit, String entityName, int size,
            XMLLimitAnalyzer limitAnalyzer) {
        return isOverLimit(limit.ordinal(), entityName, size, limitAnalyzer);
    }

    /**
     * Check if the value (length or count) of the specified limit property is
     * over the limit
     *
     * @param index the index of the limit property
     * @param entityName the name of the entity
     * @param size the size (count or length) of the entity
     * @return true if the size is over the limit, false otherwise
     */
    public boolean isOverLimit(int index, String entityName, int size,
            XMLLimitAnalyzer limitAnalyzer) {
        if (values[index] == NO_LIMIT) {
            return false;
        }
        if (size > values[index]) {
            limitAnalyzer.addValue(index, entityName, size);
            return true;
        }
        return false;
    }

    /**
     * Check against cumulated value
     *
     * @param limit the type of the limit property
     * @param size the size (count or length) of the entity
     * @return true if the size is over the limit, false otherwise
     */
    public boolean isOverLimit(Limit limit, XMLLimitAnalyzer limitAnalyzer) {
        return isOverLimit(limit.ordinal(), limitAnalyzer);
    }

    public boolean isOverLimit(int index, XMLLimitAnalyzer limitAnalyzer) {
        if (values[index] == NO_LIMIT) {
            return false;
        }

        if (index == Limit.ELEMENT_ATTRIBUTE_LIMIT.ordinal() ||
                index == Limit.ENTITY_EXPANSION_LIMIT.ordinal() ||
                index == Limit.TOTAL_ENTITY_SIZE_LIMIT.ordinal() ||
                index == Limit.ENTITY_REPLACEMENT_LIMIT.ordinal() ||
                index == Limit.MAX_ELEMENT_DEPTH_LIMIT.ordinal() ||
                index == Limit.MAX_NAME_LIMIT.ordinal()
                ) {
            return (limitAnalyzer.getTotalValue(index) > values[index]);
        } else {
            return (limitAnalyzer.getValue(index) > values[index]);
        }
    }

    public void debugPrint(XMLLimitAnalyzer limitAnalyzer) {
        if (printEntityCountInfo.equals(JdkConstants.JDK_YES)) {
            limitAnalyzer.debugPrint(this);
        }
    }


    /**
     * Indicate if a property is set explicitly
     * @param index
     * @return
     */
    public boolean isSet(int index) {
        return isSet[index];
    }

    public boolean printEntityCountInfo() {
        return printEntityCountInfo.equals(JdkConstants.JDK_YES);
    }

    /**
     * Read from system properties, or those in jaxp.properties
     */
    private void readSystemProperties() {

        for (Limit limit : Limit.values()) {
            if (!getSystemProperty(limit, limit.systemProperty())) {
                //if system property is not found, try the older form if any
                for (NameMap nameMap : NameMap.values()) {
                    String oldName = nameMap.getOldName(limit.systemProperty());
                    if (oldName != null) {
                        getSystemProperty(limit, oldName);
                    }
                }
            }
        }

    }

    // Array list to store printed warnings for each SAX parser used
    private static final CopyOnWriteArrayList<String> printedWarnings = new CopyOnWriteArrayList<>();

    /**
     * Prints out warnings if a parser does not support the specified feature/property.
     *
     * @param parserClassName the name of the parser class
     * @param propertyName the property name
     * @param exception the exception thrown by the parser
     */
    public static void printWarning(String parserClassName, String propertyName, SAXException exception) {
        String key = parserClassName+":"+propertyName;
        if (printedWarnings.addIfAbsent(key)) {
            System.err.println( "Warning: "+parserClassName+": "+exception.getMessage());
        }
    }

    /**
     * Read from system properties, or those in jaxp.properties
     *
     * @param property the type of the property
     * @param sysPropertyName the name of system property
     */
    private boolean getSystemProperty(Limit limit, String sysPropertyName) {
        try {
            String value = SecuritySupport.getSystemProperty(sysPropertyName);
            if (value != null && !value.equals("")) {
                values[limit.ordinal()] = Integer.parseInt(value);
                states[limit.ordinal()] = State.SYSTEMPROPERTY;
                return true;
            }

            value = SecuritySupport.readJAXPProperty(sysPropertyName);
            if (value != null && !value.equals("")) {
                values[limit.ordinal()] = Integer.parseInt(value);
                states[limit.ordinal()] = State.JAXPDOTPROPERTIES;
                return true;
            }
        } catch (NumberFormatException e) {
            //invalid setting
            throw new NumberFormatException("Invalid setting for system property: " + limit.systemProperty());
        }
        return false;
    }


    /**
     * Convert a value set through setProperty to XMLSecurityManager.
     * If the value is an instance of XMLSecurityManager, use it to override the default;
     * If the value is an old SecurityManager, convert to the new XMLSecurityManager.
     *
     * @param value user specified security manager
     * @param securityManager an instance of XMLSecurityManager
     * @return an instance of the new security manager XMLSecurityManager
     */
    public static XMLSecurityManager convert(Object value, XMLSecurityManager securityManager) {
        if (value == null) {
            if (securityManager == null) {
                securityManager = new XMLSecurityManager(true);
            }
            return securityManager;
        }
        if (value instanceof XMLSecurityManager) {
            return (XMLSecurityManager)value;
        } else {
            if (securityManager == null) {
                securityManager = new XMLSecurityManager(true);
            }
            if (value instanceof SecurityManager) {
                SecurityManager origSM = (SecurityManager)value;
                securityManager.setLimit(Limit.MAX_OCCUR_NODE_LIMIT, State.APIPROPERTY, origSM.getMaxOccurNodeLimit());
                securityManager.setLimit(Limit.ENTITY_EXPANSION_LIMIT, State.APIPROPERTY, origSM.getEntityExpansionLimit());
                securityManager.setLimit(Limit.ELEMENT_ATTRIBUTE_LIMIT, State.APIPROPERTY, origSM.getElementAttrLimit());
            }
            return securityManager;
        }
    }
}
