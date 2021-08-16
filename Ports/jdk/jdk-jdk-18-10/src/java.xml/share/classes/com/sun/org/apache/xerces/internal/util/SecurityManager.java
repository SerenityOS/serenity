/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
 */
 /*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.util;

import jdk.xml.internal.JdkConstants;

/**
 * This class is a container for parser settings that relate to security, or
 * more specifically, it is intended to be used to prevent denial-of-service
 * attacks from being launched against a system running Xerces. Any component
 * that is aware of a denial-of-service attack that can arise from its
 * processing of a certain kind of document may query its Component Manager for
 * the property (http://apache.org/xml/properties/security-manager) whose value
 * will be an instance of this class. If no value has been set for the property,
 * the component should proceed in the "usual" (spec-compliant) manner. If a
 * value has been set, then it must be the case that the component in question
 * needs to know what method of this class to query. This class will provide
 * defaults for all known security issues, but will also provide setters so that
 * those values can be tailored by applications that care.
 *
 * @author Neil Graham, IBM
 * @LastModified: May 2021
 */
public final class SecurityManager {

    //
    // Constants
    //
    // default value for entity expansion limit
    private final static int DEFAULT_ENTITY_EXPANSION_LIMIT = 64000;

    /**
     * Default value of number of nodes created. *
     */
    private final static int DEFAULT_MAX_OCCUR_NODE_LIMIT = 5000;

    //
    // Data
    //
    private final static int DEFAULT_ELEMENT_ATTRIBUTE_LIMIT = 10000;

    /**
     * Entity expansion limit. *
     */
    private int entityExpansionLimit;

    /**
     * W3C XML Schema maxOccurs limit. *
     */
    private int maxOccurLimit;

    private int fElementAttributeLimit;

    // default constructor.  Establishes default values for
    // all known security holes.
    /**
     * Default constructor. Establishes default values for known security
     * vulnerabilities.
     */
    public SecurityManager() {
        entityExpansionLimit = DEFAULT_ENTITY_EXPANSION_LIMIT;
        maxOccurLimit = DEFAULT_MAX_OCCUR_NODE_LIMIT;
        fElementAttributeLimit = DEFAULT_ELEMENT_ATTRIBUTE_LIMIT;
        //We are reading system properties only once ,
        //at the time of creation of this object ,
        readSystemProperties();
    }

    /**
     * <p>
     * Sets the number of entity expansions that the parser should permit in a
     * document.</p>
     *
     * @param limit the number of entity expansions permitted in a document
     */
    public void setEntityExpansionLimit(int limit) {
        entityExpansionLimit = limit;
    }

    /**
     * <p>
     * Returns the number of entity expansions that the parser permits in a
     * document.</p>
     *
     * @return the number of entity expansions permitted in a document
     */
    public int getEntityExpansionLimit() {
        return entityExpansionLimit;
    }

    /**
     * <p>
     * Sets the limit of the number of content model nodes that may be created
     * when building a grammar for a W3C XML Schema that contains maxOccurs
     * attributes with values other than "unbounded".</p>
     *
     * @param limit the maximum value for maxOccurs other than "unbounded"
     */
    public void setMaxOccurNodeLimit(int limit) {
        maxOccurLimit = limit;
    }

    /**
     * <p>
     * Returns the limit of the number of content model nodes that may be
     * created when building a grammar for a W3C XML Schema that contains
     * maxOccurs attributes with values other than "unbounded".</p>
     *
     * @return the maximum value for maxOccurs other than "unbounded"
     */
    public int getMaxOccurNodeLimit() {
        return maxOccurLimit;
    }

    public int getElementAttrLimit() {
        return fElementAttributeLimit;
    }

    public void setElementAttrLimit(int limit) {
        fElementAttributeLimit = limit;
    }

    private void readSystemProperties() {

        try {
            String value = System.getProperty(JdkConstants.ENTITY_EXPANSION_LIMIT);
            if (value != null && !value.equals("")) {
                entityExpansionLimit = Integer.parseInt(value);
                if (entityExpansionLimit < 0) {
                    entityExpansionLimit = DEFAULT_ENTITY_EXPANSION_LIMIT;
                }
            } else {
                entityExpansionLimit = DEFAULT_ENTITY_EXPANSION_LIMIT;
            }
        } catch (Exception ex) {
        }

        try {
            String value = System.getProperty(JdkConstants.MAX_OCCUR_LIMIT);
            if (value != null && !value.equals("")) {
                maxOccurLimit = Integer.parseInt(value);
                if (maxOccurLimit < 0) {
                    maxOccurLimit = DEFAULT_MAX_OCCUR_NODE_LIMIT;
                }
            } else {
                maxOccurLimit = DEFAULT_MAX_OCCUR_NODE_LIMIT;
            }
        } catch (Exception ex) {
        }

        try {
            String value = System.getProperty(JdkConstants.ELEMENT_ATTRIBUTE_LIMIT);
            if (value != null && !value.equals("")) {
                fElementAttributeLimit = Integer.parseInt(value);
                if (fElementAttributeLimit < 0) {
                    fElementAttributeLimit = DEFAULT_ELEMENT_ATTRIBUTE_LIMIT;
                }
            } else {
                fElementAttributeLimit = DEFAULT_ELEMENT_ATTRIBUTE_LIMIT;
            }

        } catch (Exception ex) {
        }

    }

} // class SecurityManager
