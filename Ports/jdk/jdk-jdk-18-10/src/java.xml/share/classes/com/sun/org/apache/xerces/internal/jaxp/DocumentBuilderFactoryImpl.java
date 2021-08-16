/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.jaxp;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import com.sun.org.apache.xerces.internal.util.SAXMessageFormatter;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import jdk.xml.internal.JdkProperty;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * @author Rajiv Mordani
 * @author Edwin Goei
 * @LastModified: May 2021
 */
public class DocumentBuilderFactoryImpl extends DocumentBuilderFactory {
    /** These are DocumentBuilderFactory attributes not DOM attributes */
    private Map<String, Object> attributes;
    private Map<String, Boolean> features;
    private Schema grammar;
    private boolean isXIncludeAware;

    /**
     * State of the secure processing feature, initially <code>false</code>
     */
    private boolean fSecureProcess = true;

    // used to verify attributes
    XMLSecurityManager fSecurityManager = new XMLSecurityManager(true);
    XMLSecurityPropertyManager fSecurityPropertyMgr = new XMLSecurityPropertyManager();

    /**
     * Creates a new instance of a {@link javax.xml.parsers.DocumentBuilder}
     * using the currently configured parameters.
     */
    public DocumentBuilder newDocumentBuilder()
        throws ParserConfigurationException
    {
        /** Check that if a Schema has been specified that neither of the schema properties have been set. */
        if (grammar != null && attributes != null) {
            if (attributes.containsKey(JAXPConstants.JAXP_SCHEMA_LANGUAGE)) {
                throw new ParserConfigurationException(
                        SAXMessageFormatter.formatMessage(null,
                        "schema-already-specified", new Object[] {JAXPConstants.JAXP_SCHEMA_LANGUAGE}));
            }
            else if (attributes.containsKey(JAXPConstants.JAXP_SCHEMA_SOURCE)) {
                throw new ParserConfigurationException(
                        SAXMessageFormatter.formatMessage(null,
                        "schema-already-specified", new Object[] {JAXPConstants.JAXP_SCHEMA_SOURCE}));
            }
        }

        try {
            return new DocumentBuilderImpl(this, attributes, features, fSecureProcess);
        } catch (SAXException se) {
            // Handles both SAXNotSupportedException, SAXNotRecognizedException
            throw new ParserConfigurationException(se.getMessage());
        }
    }

    /**
     * Allows the user to set specific attributes on the underlying
     * implementation.
     * @param name    name of attribute
     * @param value   null means to remove attribute
     */
    public void setAttribute(String name, Object value)
        throws IllegalArgumentException
    {
        // This handles removal of attributes
        if (value == null) {
            if (attributes != null) {
                attributes.remove(name);
            }
            // Unrecognized attributes do not cause an exception
            return;
        }

        // This is ugly.  We have to collect the attributes and then
        // later create a DocumentBuilderImpl to verify the attributes.

        // Create the Map if none existed before
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        //check if the property is managed by security manager
        String pName;
        if ((pName = fSecurityManager.find(name)) != null) {
            // as the qName is deprecated, let the manager decide whether the
            // value shall be changed
            fSecurityManager.setLimit(name, JdkProperty.State.APIPROPERTY, value);
            attributes.put(pName, fSecurityManager.getLimitAsString(pName));
            // no need to create a DocumentBuilderImpl
            return;
        } else if ((pName = fSecurityPropertyMgr.find(name)) != null) {
            attributes.put(pName, value);
            return;
        }

        attributes.put(name, value);

        // Test the attribute name by possibly throwing an exception
        try {
            new DocumentBuilderImpl(this, attributes, features);
        } catch (Exception e) {
            attributes.remove(name);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Allows the user to retrieve specific attributes on the underlying
     * implementation.
     */
    public Object getAttribute(String name)
        throws IllegalArgumentException
    {

        //check if the property is managed by security manager
        String pName;
        if ((pName = fSecurityManager.find(name)) != null) {
            return attributes.get(pName);
        } else if ((pName = fSecurityPropertyMgr.find(name)) != null) {
            return attributes.get(pName);
        }

        // See if it's in the attributes Map
        if (attributes != null) {
            Object val = attributes.get(name);
            if (val != null) {
                return val;
            }
        }

        DOMParser domParser = null;
        try {
            // We create a dummy DocumentBuilderImpl in case the attribute
            // name is not one that is in the attributes map.
            domParser =
                new DocumentBuilderImpl(this, attributes, features).getDOMParser();
            return domParser.getProperty(name);
        } catch (SAXException se1) {
            // assert(name is not recognized or not supported), try feature
            try {
                boolean result = domParser.getFeature(name);
                // Must have been a feature
                return result ? Boolean.TRUE : Boolean.FALSE;
            } catch (SAXException se2) {
                // Not a property or a feature
                throw new IllegalArgumentException(se1.getMessage());
            }
        }
    }

    public Schema getSchema() {
        return grammar;
    }

    public void setSchema(Schema grammar) {
        this.grammar = grammar;
    }

    public boolean isXIncludeAware() {
        return this.isXIncludeAware;
    }

    public void setXIncludeAware(boolean state) {
        this.isXIncludeAware = state;
    }

    public boolean getFeature(String name)
        throws ParserConfigurationException {
        if (name.equals(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            return fSecureProcess;
        }
        // See if it's in the features map
        if (features != null) {
            Boolean val = features.get(name);
            if (val != null) {
                return val;
            }
        }
        try {
            DOMParser domParser = new DocumentBuilderImpl(this, attributes, features).getDOMParser();
            return domParser.getFeature(name);
        }
        catch (SAXException e) {
            throw new ParserConfigurationException(e.getMessage());
        }
    }

    @SuppressWarnings("removal")
    public void setFeature(String name, boolean value)
        throws ParserConfigurationException {
        if (features == null) {
            features = new HashMap<>();
        }
        // If this is the secure processing feature, save it then return.
        if (name.equals(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            if (System.getSecurityManager() != null && (!value)) {
                throw new ParserConfigurationException(
                        SAXMessageFormatter.formatMessage(null,
                        "jaxp-secureprocessing-feature", null));
            }
            fSecureProcess = value;
            features.put(name, value ? Boolean.TRUE : Boolean.FALSE);
            return;
        }

        features.put(name, value ? Boolean.TRUE : Boolean.FALSE);
        // Test the feature by possibly throwing SAX exceptions
        try {
            new DocumentBuilderImpl(this, attributes, features);
        }
        catch (SAXNotSupportedException e) {
            features.remove(name);
            throw new ParserConfigurationException(e.getMessage());
        }
        catch (SAXNotRecognizedException e) {
            features.remove(name);
            throw new ParserConfigurationException(e.getMessage());
        }
    }
}
