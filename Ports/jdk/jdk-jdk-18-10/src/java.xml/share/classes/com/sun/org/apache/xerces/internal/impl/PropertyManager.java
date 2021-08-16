/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl;

import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.xml.internal.stream.StaxEntityResolverWrapper;
import java.util.HashMap;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLResolver;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkProperty;
import jdk.xml.internal.JdkXmlUtils;

/**
 * This class manages different properties related to Stax specification and its implementation.
 * This class constructor also takes itself (PropertyManager object) as parameter and initializes the
 * object with the property taken from the object passed.
 *
 * @author Neeraj Bajaj
 * @author K Venugopal
 * @author Sunitha Reddy
 */

public class PropertyManager {


    public static final String STAX_NOTATIONS = "javax.xml.stream.notations";
    public static final String STAX_ENTITIES = "javax.xml.stream.entities";

    private static final String STRING_INTERNING = "http://xml.org/sax/features/string-interning";

    /** Property identifier: Security manager. */
    private static final String SECURITY_MANAGER = Constants.SECURITY_MANAGER;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    HashMap<String, Object> supportedProps = new HashMap<>();

    private XMLSecurityManager fSecurityManager;
    private XMLSecurityPropertyManager fSecurityPropertyMgr;

    public static final int CONTEXT_READER = 1;
    public static final int CONTEXT_WRITER = 2;

    /** Creates a new instance of PropertyManager */
    public PropertyManager(int context) {
        switch(context){
            case CONTEXT_READER:{
                initConfigurableReaderProperties();
                break;
            }
            case CONTEXT_WRITER:{
                initWriterProps();
                break;
            }
        }
    }

    /**
     * Initialize this object with the properties taken from passed PropertyManager object.
     */
    public PropertyManager(PropertyManager propertyManager){

        HashMap<String, Object> properties = propertyManager.getProperties();
        supportedProps.putAll(properties);
        fSecurityManager = (XMLSecurityManager)getProperty(SECURITY_MANAGER);
        fSecurityPropertyMgr = (XMLSecurityPropertyManager)getProperty(XML_SECURITY_PROPERTY_MANAGER);
    }

    private HashMap<String, Object> getProperties(){
        return supportedProps ;
    }


    /**
     * Important point:
     * 1. We are not exposing Xerces namespace property. Application should configure namespace through
     * Stax specific property.
     *
     */
    private void initConfigurableReaderProperties(){
        //spec default values
        supportedProps.put(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        supportedProps.put(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        supportedProps.put(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        supportedProps.put(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.TRUE);
        supportedProps.put(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        supportedProps.put(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
        supportedProps.put(XMLInputFactory.REPORTER, null);
        supportedProps.put(XMLInputFactory.RESOLVER, null);
        supportedProps.put(XMLInputFactory.ALLOCATOR, null);
        supportedProps.put(STAX_NOTATIONS,null );

        //zephyr (implementation) specific properties which can be set by the application.
        //interning is always done
        supportedProps.put(Constants.SAX_FEATURE_PREFIX + Constants.STRING_INTERNING_FEATURE , true);
        //recognizing java encoding names by default
        supportedProps.put(Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE, true) ;
        //in stax mode, namespace declarations are not added as attributes
        supportedProps.put(Constants.ADD_NAMESPACE_DECL_AS_ATTRIBUTE ,  Boolean.FALSE) ;
        supportedProps.put(Constants.READER_IN_DEFINED_STATE, true);
        supportedProps.put(Constants.REUSE_INSTANCE, true);
        supportedProps.put(Constants.ZEPHYR_PROPERTY_PREFIX + Constants.STAX_REPORT_CDATA_EVENT , false);
        supportedProps.put(Constants.ZEPHYR_PROPERTY_PREFIX + Constants.IGNORE_EXTERNAL_DTD, Boolean.FALSE);
        supportedProps.put(Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ATTDEF_FEATURE, false);
        supportedProps.put(Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ENTITYDEF_FEATURE, false);
        supportedProps.put(Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_UNDECLARED_ELEMDEF_FEATURE, false);

        fSecurityManager = new XMLSecurityManager(true);
        supportedProps.put(SECURITY_MANAGER, fSecurityManager);
        fSecurityPropertyMgr = new XMLSecurityPropertyManager();
        supportedProps.put(XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);

        // Initialize Catalog features
        supportedProps.put(XMLConstants.USE_CATALOG, JdkXmlUtils.USE_CATALOG_DEFAULT);
        for( CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
            supportedProps.put(f.getPropertyName(), null);
        }

        supportedProps.put(JdkConstants.CDATA_CHUNK_SIZE, JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);
    }

    private void initWriterProps(){
        supportedProps.put(XMLOutputFactory.IS_REPAIRING_NAMESPACES , Boolean.FALSE);
        //default value of escaping characters is 'true'
        supportedProps.put(Constants.ESCAPE_CHARACTERS , Boolean.TRUE);
        supportedProps.put(Constants.REUSE_INSTANCE, true);
    }

    /**
     * public void reset(){
     * supportedProps.clear() ;
     * }
     */
    public boolean containsProperty(String property){
        return supportedProps.containsKey(property) ||
                (fSecurityManager != null && fSecurityManager.getIndex(property) > -1) ||
                (fSecurityPropertyMgr!=null && fSecurityPropertyMgr.getIndex(property) > -1) ;
    }

    public Object getProperty(String property){
        /** Check to see if the property is managed by the security manager **/
        String propertyValue = (fSecurityManager != null) ?
                fSecurityManager.getLimitAsString(property) : null;
        return propertyValue != null ? propertyValue : supportedProps.get(property);
    }

    public void setProperty(String property, Object value){
        String equivalentProperty = null ;
        if(property.equals(XMLInputFactory.IS_NAMESPACE_AWARE)){
            equivalentProperty = Constants.XERCES_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE ;
        }
        else if(property.equals(XMLInputFactory.IS_VALIDATING)){
            if( (value instanceof Boolean) && ((Boolean)value).booleanValue()){
                throw new java.lang.IllegalArgumentException("true value of isValidating not supported") ;
            }
        }
        else if(property.equals(STRING_INTERNING)){
            if( (value instanceof Boolean) && !((Boolean)value).booleanValue()){
                throw new java.lang.IllegalArgumentException("false value of " + STRING_INTERNING + "feature is not supported") ;
            }
        }
        else if(property.equals(XMLInputFactory.RESOLVER)){
            //add internal stax property
            supportedProps.put( Constants.XERCES_PROPERTY_PREFIX + Constants.STAX_ENTITY_RESOLVER_PROPERTY , new StaxEntityResolverWrapper((XMLResolver)value)) ;
        }

        /**
         * It's possible for users to set a security manager through the interface.
         * If it's the old SecurityManager, convert it to the new XMLSecurityManager
         */
        if (property.equals(Constants.SECURITY_MANAGER)) {
            fSecurityManager = XMLSecurityManager.convert(value, fSecurityManager);
            supportedProps.put(Constants.SECURITY_MANAGER, fSecurityManager);
            return;
        }
        if (property.equals(JdkConstants.XML_SECURITY_PROPERTY_MANAGER)) {
            if (value == null) {
                fSecurityPropertyMgr = new XMLSecurityPropertyManager();
            } else {
                fSecurityPropertyMgr = (XMLSecurityPropertyManager)value;
            }
            supportedProps.put(JdkConstants.XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);
            return;
        }

        //check if the property is managed by security manager
        if (fSecurityManager == null ||
                !fSecurityManager.setLimit(property, JdkProperty.State.APIPROPERTY, value)) {
            //check if the property is managed by security property manager
            if (fSecurityPropertyMgr == null ||
                    !fSecurityPropertyMgr.setValue(property, XMLSecurityPropertyManager.State.APIPROPERTY, value)) {
                //fall back to the existing property manager
                supportedProps.put(property, value);
            }
        }

        if(equivalentProperty != null){
            supportedProps.put(equivalentProperty, value ) ;
        }
    }

    public String toString(){
        return supportedProps.toString();
    }

}//PropertyManager
