/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.jaxp.validation;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.util.DOMEntityResolverWrapper;
import com.sun.org.apache.xerces.internal.util.DOMInputSource;
import com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper;
import com.sun.org.apache.xerces.internal.util.SAXInputSource;
import com.sun.org.apache.xerces.internal.util.SAXMessageFormatter;
import com.sun.org.apache.xerces.internal.util.StAXInputSource;
import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures.Feature;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkProperty;
import jdk.xml.internal.JdkProperty.ImplPropMap;
import jdk.xml.internal.JdkXmlFeatures;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

/**
 * {@link SchemaFactory} for XML Schema.
 *
 * @author Kohsuke Kawaguchi
 *
 * @LastModified: May 2021
 */
public final class XMLSchemaFactory extends SchemaFactory {

    // feature identifiers

    /** JAXP Source feature prefix. */
    private static final String JAXP_SOURCE_FEATURE_PREFIX = "http://javax.xml.transform";

    /** Feature identifier: schema full checking. */
    private static final String SCHEMA_FULL_CHECKING =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

    /** Feature identifier: use grammar pool only. */
    private static final String USE_GRAMMAR_POOL_ONLY =
        Constants.XERCES_FEATURE_PREFIX + Constants.USE_GRAMMAR_POOL_ONLY_FEATURE;

    // property identifiers

    /** Property identifier: grammar pool. */
    private static final String XMLGRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Property identifier: XMLSecurityManager. */
    private static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;


    //
    // Data
    //

    /** The XMLSchemaLoader */
    private final XMLSchemaLoader fXMLSchemaLoader = new XMLSchemaLoader();

    /** User-specified ErrorHandler; can be null. */
    private ErrorHandler fErrorHandler;

    /** The LSResrouceResolver */
    private LSResourceResolver fLSResourceResolver;

    /** The DOMEntityResolverWrapper */
    private final DOMEntityResolverWrapper fDOMEntityResolverWrapper;

    /** The ErrorHandlerWrapper */
    private final ErrorHandlerWrapper fErrorHandlerWrapper;

    /** The SecurityManager. */
    private XMLSecurityManager fSecurityManager;

    /** The Security property manager. */
    private XMLSecurityPropertyManager fSecurityPropertyMgr;

    /** The container for the real grammar pool. */
    private final XMLGrammarPoolWrapper fXMLGrammarPoolWrapper;

    /** Whether or not to allow new schemas to be added to the grammar pool */
    private boolean fUseGrammarPoolOnly;

    private final JdkXmlFeatures fXmlFeatures;
    /**
     * Indicates whether 3rd party parser may be used to override the system-default
     * Note the default value (false) is the safe option.
     * Note same as the old property useServicesMechanism
     */
    private final boolean fOverrideDefaultParser;


    public XMLSchemaFactory() {
        fErrorHandlerWrapper = new ErrorHandlerWrapper(DraconianErrorHandler.getInstance());
        fDOMEntityResolverWrapper = new DOMEntityResolverWrapper();
        fXMLGrammarPoolWrapper = new XMLGrammarPoolWrapper();
        fXMLSchemaLoader.setFeature(SCHEMA_FULL_CHECKING, true);
        fXMLSchemaLoader.setProperty(XMLGRAMMAR_POOL, fXMLGrammarPoolWrapper);
        fXMLSchemaLoader.setEntityResolver(fDOMEntityResolverWrapper);
        fXMLSchemaLoader.setErrorHandler(fErrorHandlerWrapper);
        fUseGrammarPoolOnly = true;

        // Enable secure processing feature by default
        fSecurityManager = new XMLSecurityManager(true);
        fXMLSchemaLoader.setProperty(SECURITY_MANAGER, fSecurityManager);

        fSecurityPropertyMgr = new XMLSecurityPropertyManager();
        fXMLSchemaLoader.setProperty(XML_SECURITY_PROPERTY_MANAGER,
                fSecurityPropertyMgr);

        // use catalog
        fXMLSchemaLoader.setFeature(XMLConstants.USE_CATALOG, JdkXmlUtils.USE_CATALOG_DEFAULT);
        for (Feature f : Feature.values()) {
            fXMLSchemaLoader.setProperty(f.getPropertyName(), null);
        }

        fXMLSchemaLoader.setProperty(JdkConstants.CDATA_CHUNK_SIZE, JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);
        fXmlFeatures = new JdkXmlFeatures(fSecurityManager.isSecureProcessing());
        fOverrideDefaultParser = fXmlFeatures.getFeature(
                JdkXmlFeatures.XmlFeature.JDK_OVERRIDE_PARSER);
        fXMLSchemaLoader.setFeature(JdkConstants.OVERRIDE_PARSER, fOverrideDefaultParser);
    }

    /**
     * <p>Is specified schema supported by this <code>SchemaFactory</code>?</p>
     *
     * @param schemaLanguage Specifies the schema language which the returned <code>SchemaFactory</code> will understand.
     *    <code>schemaLanguage</code> must specify a <a href="#schemaLanguage">valid</a> schema language.
     *
     * @return <code>true</code> if <code>SchemaFactory</code> supports <code>schemaLanguage</code>, else <code>false</code>.
     *
     * @throws NullPointerException If <code>schemaLanguage</code> is <code>null</code>.
     * @throws IllegalArgumentException If <code>schemaLanguage.length() == 0</code>
     *   or <code>schemaLanguage</code> does not specify a <a href="#schemaLanguage">valid</a> schema language.
     */
    public boolean isSchemaLanguageSupported(String schemaLanguage) {
        if (schemaLanguage == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "SchemaLanguageNull", null));
        }
        if (schemaLanguage.length() == 0) {
            throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "SchemaLanguageLengthZero", null));
        }
        // only W3C XML Schema 1.0 is supported
        return schemaLanguage.equals(XMLConstants.W3C_XML_SCHEMA_NS_URI) ||
                schemaLanguage.equals(Constants.W3C_XML_SCHEMA10_NS_URI);
    }

    public LSResourceResolver getResourceResolver() {
        return fLSResourceResolver;
    }

    public void setResourceResolver(LSResourceResolver resourceResolver) {
        fLSResourceResolver = resourceResolver;
        fDOMEntityResolverWrapper.setEntityResolver(resourceResolver);
        fXMLSchemaLoader.setEntityResolver(fDOMEntityResolverWrapper);
    }

    public ErrorHandler getErrorHandler() {
        return fErrorHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        fErrorHandler = errorHandler;
        fErrorHandlerWrapper.setErrorHandler(errorHandler != null ? errorHandler : DraconianErrorHandler.getInstance());
        fXMLSchemaLoader.setErrorHandler(fErrorHandlerWrapper);
    }

    public Schema newSchema( Source[] schemas ) throws SAXException {

        // this will let the loader store parsed Grammars into the pool.
        XMLGrammarPoolImplExtension pool = new XMLGrammarPoolImplExtension();
        fXMLGrammarPoolWrapper.setGrammarPool(pool);

        XMLInputSource[] xmlInputSources = new XMLInputSource[schemas.length];
        InputStream inputStream;
        Reader reader;
        for (int i = 0; i < schemas.length; ++i) {
            Source source = schemas[i];
            if (source instanceof StreamSource) {
                StreamSource streamSource = (StreamSource) source;
                String publicId = streamSource.getPublicId();
                String systemId = streamSource.getSystemId();
                inputStream = streamSource.getInputStream();
                reader = streamSource.getReader();
                XMLInputSource xmlInputSource = new XMLInputSource(publicId, systemId, null, false);
                xmlInputSource.setByteStream(inputStream);
                xmlInputSource.setCharacterStream(reader);
                xmlInputSources[i] = xmlInputSource;
            }
            else if (source instanceof SAXSource) {
                SAXSource saxSource = (SAXSource) source;
                InputSource inputSource = saxSource.getInputSource();
                if (inputSource == null) {
                    throw new SAXException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                            "SAXSourceNullInputSource", null));
                }
                xmlInputSources[i] = new SAXInputSource(saxSource.getXMLReader(), inputSource);
            }
            else if (source instanceof DOMSource) {
                DOMSource domSource = (DOMSource) source;
                Node node = domSource.getNode();
                String systemID = domSource.getSystemId();
                xmlInputSources[i] = new DOMInputSource(node, systemID);
            }
             else if (source instanceof StAXSource) {
                StAXSource staxSource = (StAXSource) source;
                XMLEventReader eventReader = staxSource.getXMLEventReader();
                if (eventReader != null) {
                    xmlInputSources[i] = new StAXInputSource(eventReader);
                }
                else {
                    xmlInputSources[i] = new StAXInputSource(staxSource.getXMLStreamReader());
                }
            }
            else if (source == null) {
                throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "SchemaSourceArrayMemberNull", null));
            }
            else {
                throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "SchemaFactorySourceUnrecognized",
                        new Object [] {source.getClass().getName()}));
            }
        }

        try {
            fXMLSchemaLoader.loadGrammar(xmlInputSources);
        }
        catch (XNIException e) {
            // this should have been reported to users already.
            throw Util.toSAXException(e);
        }
        catch (IOException e) {
            // this hasn't been reported, so do so now.
            SAXParseException se = new SAXParseException(e.getMessage(),null,e);
            if (fErrorHandler != null) {
                fErrorHandler.error(se);
            }
            throw se; // and we must throw it.
        }

        // Clear reference to grammar pool.
        fXMLGrammarPoolWrapper.setGrammarPool(null);

        // Select Schema implementation based on grammar count.
        final int grammarCount = pool.getGrammarCount();
        AbstractXMLSchema schema = null;
        if (fUseGrammarPoolOnly) {
            if (grammarCount > 1) {
                schema = new XMLSchema(new ReadOnlyGrammarPool(pool));
            }
            else if (grammarCount == 1) {
                Grammar[] grammars = pool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
                schema = new SimpleXMLSchema(grammars[0]);
            }
            else {
                schema = new EmptyXMLSchema();
            }
        }
        else {
            schema = new XMLSchema(new ReadOnlyGrammarPool(pool), false);
        }
        propagateFeatures(schema);
        propagateProperties(schema);
        return schema;
    }

    public Schema newSchema() throws SAXException {
        /*
         * It would make sense to return an EmptyXMLSchema object here, if
         * fUseGrammarPoolOnly is set to true. However, because the default
         * value of this feature is true, doing so would change the default
         * behaviour of this method. Thus, we return a WeakReferenceXMLSchema
         * regardless of the value of fUseGrammarPoolOnly. -PM
         */

        // Use a Schema that uses the system id as the equality source.
        AbstractXMLSchema schema = new WeakReferenceXMLSchema();
        propagateFeatures(schema);
        propagateProperties(schema);
        return schema;
    }

    public Schema newSchema(XMLGrammarPool pool) throws SAXException {
        // If the "use-grammar-pool-only" feature is set to true
        // prevent the application's grammar pool from being mutated
        // by wrapping it in a ReadOnlyGrammarPool.
        final AbstractXMLSchema schema = (fUseGrammarPoolOnly) ?
            new XMLSchema(new ReadOnlyGrammarPool(pool)) :
            new XMLSchema(pool, false);
        propagateFeatures(schema);
        return schema;
    }

    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "FeatureNameNull", null));
        }
        if (name.startsWith(JAXP_SOURCE_FEATURE_PREFIX)) {
            // Indicates to the caller that this SchemaFactory supports a specific JAXP Source.
            if (name.equals(StreamSource.FEATURE) ||
                name.equals(SAXSource.FEATURE) ||
                name.equals(DOMSource.FEATURE) ||
                name.equals(StAXSource.FEATURE)) {
                return true;
            }
        }
        if (name.equals(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            return (fSecurityManager != null && fSecurityManager.isSecureProcessing());
        }
        else if (name.equals(USE_GRAMMAR_POOL_ONLY)) {
            return fUseGrammarPoolOnly;
        }
        /** Check to see if the property is managed by the JdkXmlFeatues **/
        int index = fXmlFeatures.getIndex(name);
        if (index > -1) {
            return fXmlFeatures.getFeature(index);
        }
        try {
            return fXMLSchemaLoader.getFeature(name);
        }
        catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == Status.NOT_RECOGNIZED) {
                throw new SAXNotRecognizedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "feature-not-recognized", new Object [] {identifier}));
            }
            else {
                throw new SAXNotSupportedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "feature-not-supported", new Object [] {identifier}));
            }
        }
    }

    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "ProperyNameNull", null));
        }
        if (name.equals(SECURITY_MANAGER)) {
            return fSecurityManager;
        }
        else if (name.equals(XMLGRAMMAR_POOL)) {
            throw new SAXNotSupportedException(
                    SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "property-not-supported", new Object [] {name}));
        }
        try {
            /** Check to see if the property is managed by the security manager **/
            String propertyValue = (fSecurityManager != null) ?
                    fSecurityManager.getLimitAsString(name) : null;
            return propertyValue != null ? propertyValue :
                    fXMLSchemaLoader.getProperty(name);
        }
        catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == Status.NOT_RECOGNIZED) {
                throw new SAXNotRecognizedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "property-not-recognized", new Object [] {identifier}));
            }
            else {
                throw new SAXNotSupportedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "property-not-supported", new Object [] {identifier}));
            }
        }
    }

    @SuppressWarnings({"removal","deprecation"})
    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "FeatureNameNull", null));
        }
        if (name.startsWith(JAXP_SOURCE_FEATURE_PREFIX)) {
            if (name.equals(StreamSource.FEATURE) ||
                name.equals(SAXSource.FEATURE) ||
                name.equals(DOMSource.FEATURE) ||
                name.equals(StAXSource.FEATURE)) {
                throw new SAXNotSupportedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "feature-read-only", new Object [] {name}));
            }
        }
        if (name.equals(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            if (System.getSecurityManager() != null && (!value)) {
                throw new SAXNotSupportedException(
                        SAXMessageFormatter.formatMessage(null,
                        "jaxp-secureprocessing-feature", null));
            }

            fSecurityManager.setSecureProcessing(value);
            if (value) {
                fSecurityPropertyMgr.setValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD,
                        XMLSecurityPropertyManager.State.FSP, JdkConstants.EXTERNAL_ACCESS_DEFAULT_FSP);
                fSecurityPropertyMgr.setValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_SCHEMA,
                        XMLSecurityPropertyManager.State.FSP, JdkConstants.EXTERNAL_ACCESS_DEFAULT_FSP);
            }

            fXMLSchemaLoader.setProperty(SECURITY_MANAGER, fSecurityManager);
            return;
        }
        else if (name.equals(USE_GRAMMAR_POOL_ONLY)) {
            fUseGrammarPoolOnly = value;
            return;
        }
        else if (name.equals(JdkConstants.ORACLE_FEATURE_SERVICE_MECHANISM)) {
            //in secure mode, let useServicesMechanism be determined by the constructor
            if (System.getSecurityManager() != null)
                return;
        }

        if ((fXmlFeatures != null) &&
                    fXmlFeatures.setFeature(name, JdkProperty.State.APIPROPERTY, value)) {
            if ((ImplPropMap.OVERRIDEPARSER.is(name))
                    || name.equals(JdkXmlUtils.USE_CATALOG)) {
                fXMLSchemaLoader.setFeature(name, value);
            }
            return;
        }
        try {
            fXMLSchemaLoader.setFeature(name, value);
        }
        catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == Status.NOT_RECOGNIZED) {
                throw new SAXNotRecognizedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "feature-not-recognized", new Object [] {identifier}));
            }
            else {
                throw new SAXNotSupportedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "feature-not-supported", new Object [] {identifier}));
            }
        }
    }

    public void setProperty(String name, Object object)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException(JAXPValidationMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "ProperyNameNull", null));
        }
        if (name.equals(SECURITY_MANAGER)) {
            fSecurityManager = XMLSecurityManager.convert(object, fSecurityManager);
            fXMLSchemaLoader.setProperty(SECURITY_MANAGER, fSecurityManager);
            return;
        } else if (name.equals(JdkConstants.XML_SECURITY_PROPERTY_MANAGER)) {
            if (object == null) {
                fSecurityPropertyMgr = new XMLSecurityPropertyManager();
            } else {
                fSecurityPropertyMgr = (XMLSecurityPropertyManager)object;
            }
            fXMLSchemaLoader.setProperty(JdkConstants.XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);
            return;
        }
        else if (name.equals(XMLGRAMMAR_POOL)) {
            throw new SAXNotSupportedException(
                    SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                    "property-not-supported", new Object [] {name}));
        }
        try {
            //check if the property is managed by security manager
            if (fSecurityManager == null ||
                    !fSecurityManager.setLimit(name, JdkProperty.State.APIPROPERTY, object)) {
                //check if the property is managed by security property manager
                if (fSecurityPropertyMgr == null ||
                        !fSecurityPropertyMgr.setValue(name, XMLSecurityPropertyManager.State.APIPROPERTY, object)) {
                    //fall back to the existing property manager
                    fXMLSchemaLoader.setProperty(name, object);
                }
            }
        }
        catch (XMLConfigurationException e) {
            String identifier = e.getIdentifier();
            if (e.getType() == Status.NOT_RECOGNIZED) {
                throw new SAXNotRecognizedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "property-not-recognized", new Object [] {identifier}));
            }
            else {
                throw new SAXNotSupportedException(
                        SAXMessageFormatter.formatMessage(fXMLSchemaLoader.getLocale(),
                        "property-not-supported", new Object [] {identifier}));
            }
        }
    }

    private void propagateFeatures(AbstractXMLSchema schema) {
        schema.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,
                (fSecurityManager != null && fSecurityManager.isSecureProcessing()));
        schema.setFeature(JdkConstants.OVERRIDE_PARSER, fOverrideDefaultParser);
        String[] features = fXMLSchemaLoader.getRecognizedFeatures();
        for (int i = 0; i < features.length; ++i) {
            boolean state = fXMLSchemaLoader.getFeature(features[i]);
            schema.setFeature(features[i], state);
        }
    }

    private void propagateProperties(AbstractXMLSchema schema) {
        String[] properties = fXMLSchemaLoader.getRecognizedProperties();
        for (int i = 0; i < properties.length; ++i) {
            Object state = fXMLSchemaLoader.getProperty(properties[i]);
            schema.setProperty(properties[i], state);
        }
    }


    /**
     * Extension of XMLGrammarPoolImpl which exposes the number of
     * grammars stored in the grammar pool.
     */
    static class XMLGrammarPoolImplExtension extends XMLGrammarPoolImpl {

        /** Constructs a grammar pool with a default number of buckets. */
        public XMLGrammarPoolImplExtension() {
            super();
        }

        /** Constructs a grammar pool with a specified number of buckets. */
        public XMLGrammarPoolImplExtension(int initialCapacity) {
            super(initialCapacity);
        }

        /** Returns the number of grammars contained in this pool. */
        int getGrammarCount() {
            return fGrammarCount;
        }

    } // XMLSchemaFactory.XMLGrammarPoolImplExtension

    /**
     * A grammar pool which wraps another.
     */
    static class XMLGrammarPoolWrapper implements XMLGrammarPool {

        private XMLGrammarPool fGrammarPool;

        /*
         * XMLGrammarPool methods
         */

        public Grammar[] retrieveInitialGrammarSet(String grammarType) {
            return fGrammarPool.retrieveInitialGrammarSet(grammarType);
        }

        public void cacheGrammars(String grammarType, Grammar[] grammars) {
            fGrammarPool.cacheGrammars(grammarType, grammars);
        }

        public Grammar retrieveGrammar(XMLGrammarDescription desc) {
            return fGrammarPool.retrieveGrammar(desc);
        }

        public void lockPool() {
            fGrammarPool.lockPool();
        }

        public void unlockPool() {
            fGrammarPool.unlockPool();
        }

        public void clear() {
            fGrammarPool.clear();
        }

        /*
         * Other methods
         */

        void setGrammarPool(XMLGrammarPool grammarPool) {
            fGrammarPool = grammarPool;
        }

        XMLGrammarPool getGrammarPool() {
            return fGrammarPool;
        }

    } // XMLSchemaFactory.XMLGrammarPoolWrapper

} // XMLSchemaFactory
