/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaValidator;
import com.sun.org.apache.xerces.internal.impl.xs.XSMessageFormatter;
import com.sun.org.apache.xerces.internal.util.DOMEntityResolverWrapper;
import com.sun.org.apache.xerces.internal.util.ErrorHandlerWrapper;
import com.sun.org.apache.xerces.internal.util.FeatureState;
import com.sun.org.apache.xerces.internal.util.NamespaceSupport;
import com.sun.org.apache.xerces.internal.util.ParserConfigurationSettings;
import com.sun.org.apache.xerces.internal.util.PropertyState;
import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import javax.xml.catalog.CatalogFeatures;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkProperty;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;

/**
 * <p>An implementation of XMLComponentManager for a schema validator.</p>
 *
 * @author Michael Glavassevich, IBM
 * @LastModified: May 2021
 */
final class XMLSchemaValidatorComponentManager extends ParserConfigurationSettings implements
        XMLComponentManager {

    // feature identifiers

    /** Feature identifier: schema validation. */
    private static final String SCHEMA_VALIDATION =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

    /** Feature identifier: validation. */
    private static final String VALIDATION =
        Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** Feature identifier: use grammar pool only. */
    private static final String USE_GRAMMAR_POOL_ONLY =
        Constants.XERCES_FEATURE_PREFIX + Constants.USE_GRAMMAR_POOL_ONLY_FEATURE;

    /** Feature identifier: whether to ignore xsi:type attributes until a global element declaration is encountered */
    protected static final String IGNORE_XSI_TYPE =
        Constants.XERCES_FEATURE_PREFIX + Constants.IGNORE_XSI_TYPE_FEATURE;

    /** Feature identifier: whether to ignore ID/IDREF errors */
    protected static final String ID_IDREF_CHECKING =
        Constants.XERCES_FEATURE_PREFIX + Constants.ID_IDREF_CHECKING_FEATURE;

    /** Feature identifier: whether to ignore unparsed entity errors */
    protected static final String UNPARSED_ENTITY_CHECKING =
        Constants.XERCES_FEATURE_PREFIX + Constants.UNPARSED_ENTITY_CHECKING_FEATURE;

    /** Feature identifier: whether to ignore identity constraint errors */
    protected static final String IDENTITY_CONSTRAINT_CHECKING =
        Constants.XERCES_FEATURE_PREFIX + Constants.IDC_CHECKING_FEATURE;

    /** Feature identifier: disallow DOCTYPE declaration */
    private static final String DISALLOW_DOCTYPE_DECL_FEATURE =
        Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

    /** Feature identifier: expose schema normalized value */
    private static final String NORMALIZE_DATA =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_NORMALIZED_VALUE;

    /** Feature identifier: send element default value via characters() */
    private static final String SCHEMA_ELEMENT_DEFAULT =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_ELEMENT_DEFAULT;

    /** Feature identifier: augment PSVI */
    private static final String SCHEMA_AUGMENT_PSVI =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_AUGMENT_PSVI;

    // property identifiers

    /** Property identifier: entity manager. */
    private static final String ENTITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY;

    /** Property identifier: entity resolver. */
    private static final String ENTITY_RESOLVER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Property identifier: error handler. */
    private static final String ERROR_HANDLER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;

    /** Property identifier: error reporter. */
    private static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: namespace context. */
    private static final String NAMESPACE_CONTEXT =
        Constants.XERCES_PROPERTY_PREFIX + Constants.NAMESPACE_CONTEXT_PROPERTY;

    /** Property identifier: XML Schema validator. */
    private static final String SCHEMA_VALIDATOR =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_VALIDATOR_PROPERTY;

    /** Property identifier: security manager. */
    private static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    /** Property identifier: security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    /** Property identifier: symbol table. */
    private static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: validation manager. */
    private static final String VALIDATION_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    /** Property identifier: grammar pool. */
    private static final String XMLGRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Property identifier: locale. */
    private static final String LOCALE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.LOCALE_PROPERTY;

    //
    // Data
    //
    /**
     * <p>State of secure mode.</p>
     */
    private boolean _isSecureMode = false;

    /**
     * fConfigUpdated is set to true if there has been any change to the configuration settings,
     * i.e a feature or a property was changed.
     */
    private boolean fConfigUpdated = true;

    /**
     * Tracks whether the validator should use components from
     * the grammar pool to the exclusion of all others.
     */
    private boolean fUseGrammarPoolOnly;

    /** Lookup map for components required for validation. **/
    private final HashMap<String, Object> fComponents = new HashMap<>();

    //
    // Components
    //

    /** Entity manager. */
    private XMLEntityManager fEntityManager;

    /** Error reporter. */
    private XMLErrorReporter fErrorReporter;

    /** Namespace context. */
    private NamespaceContext fNamespaceContext;

    /** XML Schema validator. */
    private XMLSchemaValidator fSchemaValidator;

    /** Validation manager. */
    private ValidationManager fValidationManager;

    //
    // Configuration
    //

    /** Stores initial feature values for validator reset. */
    private final HashMap<String, Boolean> fInitFeatures = new HashMap<>();

    /** Stores initial property values for validator reset. */
    private final HashMap<String, Object> fInitProperties = new HashMap<>();

    /** Stores the initial security manager. */
    private XMLSecurityManager fInitSecurityManager;

    /** Stores the initial security property manager. */
    private final XMLSecurityPropertyManager fSecurityPropertyMgr;

    //
    // User Objects
    //

    /** Application's ErrorHandler. **/
    private ErrorHandler fErrorHandler = null;

    /** Application's LSResourceResolver. */
    private LSResourceResolver fResourceResolver = null;

    /** Locale chosen by the application. */
    private Locale fLocale = null;

    /** Constructs a component manager suitable for Xerces' schema validator. */
    @SuppressWarnings("removal")
    public XMLSchemaValidatorComponentManager(XSGrammarPoolContainer grammarContainer) {
        // setup components
        fEntityManager = new XMLEntityManager();
        fComponents.put(ENTITY_MANAGER, fEntityManager);

        fErrorReporter = new XMLErrorReporter();
        fComponents.put(ERROR_REPORTER, fErrorReporter);

        fNamespaceContext = new NamespaceSupport();
        fComponents.put(NAMESPACE_CONTEXT, fNamespaceContext);

        fSchemaValidator = new XMLSchemaValidator();
        fComponents.put(SCHEMA_VALIDATOR, fSchemaValidator);

        fValidationManager = new ValidationManager();
        fComponents.put(VALIDATION_MANAGER, fValidationManager);

        // setup other properties
        fComponents.put(ENTITY_RESOLVER, null);
        fComponents.put(ERROR_HANDLER, null);

        fComponents.put(SYMBOL_TABLE, new SymbolTable());

        // setup grammar pool
        fComponents.put(XMLGRAMMAR_POOL, grammarContainer.getGrammarPool());
        fUseGrammarPoolOnly = grammarContainer.isFullyComposed();

        // add schema message formatter to error reporter
        fErrorReporter.putMessageFormatter(XSMessageFormatter.SCHEMA_DOMAIN, new XSMessageFormatter());

        // add all recognized features and properties and apply their defaults
        final String [] recognizedFeatures = {
                DISALLOW_DOCTYPE_DECL_FEATURE,
                NORMALIZE_DATA,
                SCHEMA_ELEMENT_DEFAULT,
                SCHEMA_AUGMENT_PSVI,
                XMLConstants.USE_CATALOG,
                JdkConstants.OVERRIDE_PARSER
        };
        addRecognizedFeatures(recognizedFeatures);
        fFeatures.put(DISALLOW_DOCTYPE_DECL_FEATURE, Boolean.FALSE);
        fFeatures.put(NORMALIZE_DATA, Boolean.FALSE);
        fFeatures.put(SCHEMA_ELEMENT_DEFAULT, Boolean.FALSE);
        fFeatures.put(SCHEMA_AUGMENT_PSVI, Boolean.TRUE);
        fFeatures.put(XMLConstants.USE_CATALOG, grammarContainer.getFeature(XMLConstants.USE_CATALOG));
        fFeatures.put(JdkConstants.OVERRIDE_PARSER, grammarContainer.getFeature(JdkConstants.OVERRIDE_PARSER));

        addRecognizedParamsAndSetDefaults(fEntityManager, grammarContainer);
        addRecognizedParamsAndSetDefaults(fErrorReporter, grammarContainer);
        addRecognizedParamsAndSetDefaults(fSchemaValidator, grammarContainer);

        /* TODO: are other XMLSchemaValidator default values never set?
         * Initial investigation indicates that they aren't set, but
         * that they all have default values of false, so it works out
         * anyway -PM
         */
        fFeatures.put(IGNORE_XSI_TYPE, Boolean.FALSE);
        fFeatures.put(ID_IDREF_CHECKING, Boolean.TRUE);
        fFeatures.put(IDENTITY_CONSTRAINT_CHECKING, Boolean.TRUE);
        fFeatures.put(UNPARSED_ENTITY_CHECKING, Boolean.TRUE);

        boolean secureProcessing = grammarContainer.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING);
        if (System.getSecurityManager() != null) {
            _isSecureMode = true;
            secureProcessing = true;
        }

        fInitSecurityManager = (XMLSecurityManager)
                grammarContainer.getProperty(SECURITY_MANAGER);
        if (fInitSecurityManager != null ) {
            fInitSecurityManager.setSecureProcessing(secureProcessing);
        } else {
            fInitSecurityManager = new XMLSecurityManager(secureProcessing);
        }

        setProperty(SECURITY_MANAGER, fInitSecurityManager);

        //pass on properties set on SchemaFactory
        fSecurityPropertyMgr = (XMLSecurityPropertyManager)
                grammarContainer.getProperty(JdkConstants.XML_SECURITY_PROPERTY_MANAGER);
        setProperty(XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);

        //initialize Catalog properties
        for( CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
            setProperty(f.getPropertyName(), grammarContainer.getProperty(f.getPropertyName()));
        }

        setProperty(JdkConstants.CDATA_CHUNK_SIZE,
                grammarContainer.getProperty(JdkConstants.CDATA_CHUNK_SIZE));
    }

    /**
     * Returns the state of a feature.
     *
     * @param featureId The feature identifier.
     * @return true if the feature is supported
     *
     * @throws XMLConfigurationException Thrown for configuration error.
     *                                   In general, components should
     *                                   only throw this exception if
     *                                   it is <strong>really</strong>
     *                                   a critical error.
     */
    public FeatureState getFeatureState(String featureId)
            throws XMLConfigurationException {
        if (PARSER_SETTINGS.equals(featureId)) {
            return FeatureState.is(fConfigUpdated);
        }
        else if (VALIDATION.equals(featureId) || SCHEMA_VALIDATION.equals(featureId)) {
            return FeatureState.is(true);
        }
        else if (USE_GRAMMAR_POOL_ONLY.equals(featureId)) {
            return FeatureState.is(fUseGrammarPoolOnly);
        }
        else if (XMLConstants.FEATURE_SECURE_PROCESSING.equals(featureId)) {
            return FeatureState.is(fInitSecurityManager.isSecureProcessing());
        }
        else if (SCHEMA_ELEMENT_DEFAULT.equals(featureId)) {
            return FeatureState.is(true); //pre-condition: VALIDATION and SCHEMA_VALIDATION are always true
        }
        return super.getFeatureState(featureId);
    }

    /**
     * Set the state of a feature.
     *
     * @param featureId The unique identifier (URI) of the feature.
     * @param state The requested state of the feature (true or false).
     *
     * @exception XMLConfigurationException If the requested feature is not known.
     */
    public void setFeature(String featureId, boolean value) throws XMLConfigurationException {
        if (PARSER_SETTINGS.equals(featureId)) {
            throw new XMLConfigurationException(Status.NOT_SUPPORTED, featureId);
        }
        else if (value == false && (VALIDATION.equals(featureId) || SCHEMA_VALIDATION.equals(featureId))) {
            throw new XMLConfigurationException(Status.NOT_SUPPORTED, featureId);
        }
        else if (USE_GRAMMAR_POOL_ONLY.equals(featureId) && value != fUseGrammarPoolOnly) {
            throw new XMLConfigurationException(Status.NOT_SUPPORTED, featureId);
        }
        if (XMLConstants.FEATURE_SECURE_PROCESSING.equals(featureId)) {
            if (_isSecureMode && !value) {
                throw new XMLConfigurationException(Status.NOT_ALLOWED, XMLConstants.FEATURE_SECURE_PROCESSING);
            }

            fInitSecurityManager.setSecureProcessing(value);
            setProperty(SECURITY_MANAGER, fInitSecurityManager);

            if (value) {
                fSecurityPropertyMgr.setValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD,
                        XMLSecurityPropertyManager.State.FSP, JdkConstants.EXTERNAL_ACCESS_DEFAULT_FSP);
                fSecurityPropertyMgr.setValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_SCHEMA,
                        XMLSecurityPropertyManager.State.FSP, JdkConstants.EXTERNAL_ACCESS_DEFAULT_FSP);
                setProperty(XML_SECURITY_PROPERTY_MANAGER, fSecurityPropertyMgr);
            }

            return;
        }
        fConfigUpdated = true;
        fEntityManager.setFeature(featureId, value);
        fErrorReporter.setFeature(featureId, value);
        fSchemaValidator.setFeature(featureId, value);
        if (!fInitFeatures.containsKey(featureId)) {
            boolean current = super.getFeature(featureId);
            fInitFeatures.put(featureId, current ? Boolean.TRUE : Boolean.FALSE);
        }
        super.setFeature(featureId, value);
    }

    /**
     * Returns the value of a property.
     *
     * @param propertyId The property identifier.
     * @return the value of the property
     *
     * @throws XMLConfigurationException Thrown for configuration error.
     *                                   In general, components should
     *                                   only throw this exception if
     *                                   it is <strong>really</strong>
     *                                   a critical error.
     */
    public PropertyState getPropertyState(String propertyId)
            throws XMLConfigurationException {
        if (LOCALE.equals(propertyId)) {
            return PropertyState.is(getLocale());
        }
        final Object component = fComponents.get(propertyId);
        if (component != null) {
            return PropertyState.is(component);
        }
        else if (fComponents.containsKey(propertyId)) {
            return PropertyState.is(null);
        }
        return super.getPropertyState(propertyId);
    }

    /**
     * Sets the state of a property.
     *
     * @param propertyId The unique identifier (URI) of the property.
     * @param value The requested state of the property.
     *
     * @exception XMLConfigurationException If the requested property is not known.
     */
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
        if ( ENTITY_MANAGER.equals(propertyId) || ERROR_REPORTER.equals(propertyId) ||
             NAMESPACE_CONTEXT.equals(propertyId) || SCHEMA_VALIDATOR.equals(propertyId) ||
             SYMBOL_TABLE.equals(propertyId) || VALIDATION_MANAGER.equals(propertyId) ||
             XMLGRAMMAR_POOL.equals(propertyId)) {
            throw new XMLConfigurationException(Status.NOT_SUPPORTED, propertyId);
        }
        fConfigUpdated = true;
        fEntityManager.setProperty(propertyId, value);
        fErrorReporter.setProperty(propertyId, value);
        fSchemaValidator.setProperty(propertyId, value);
        if (ENTITY_RESOLVER.equals(propertyId) || ERROR_HANDLER.equals(propertyId) ||
                SECURITY_MANAGER.equals(propertyId)) {
            fComponents.put(propertyId, value);
            return;
        }
        else if (LOCALE.equals(propertyId)) {
            setLocale((Locale) value);
            fComponents.put(propertyId, value);
            return;
        }
        //check if the property is managed by security manager
        if (fInitSecurityManager == null ||
                !fInitSecurityManager.setLimit(propertyId, JdkProperty.State.APIPROPERTY, value)) {
            //check if the property is managed by security property manager
            if (fSecurityPropertyMgr == null ||
                    !fSecurityPropertyMgr.setValue(propertyId, XMLSecurityPropertyManager.State.APIPROPERTY, value)) {
                //fall back to the existing property manager
                if (!fInitProperties.containsKey(propertyId)) {
                    fInitProperties.put(propertyId, super.getProperty(propertyId));
                }
                super.setProperty(propertyId, value);
            }
        }
    }

    /**
     * Adds all of the component's recognized features and properties
     * to the list of default recognized features and properties, and
     * sets default values on the configuration for features and
     * properties which were previously absent from the configuration.
     *
     * @param component The component whose recognized features
     * and properties will be added to the configuration
     */
    public void addRecognizedParamsAndSetDefaults(XMLComponent component, XSGrammarPoolContainer grammarContainer) {

        // register component's recognized features
        final String[] recognizedFeatures = component.getRecognizedFeatures();
        addRecognizedFeatures(recognizedFeatures);

        // register component's recognized properties
        final String[] recognizedProperties = component.getRecognizedProperties();
        addRecognizedProperties(recognizedProperties);

        // set default values
        setFeatureDefaults(component, recognizedFeatures, grammarContainer);
        setPropertyDefaults(component, recognizedProperties);
    }

    /** Calls reset on each of the components owned by this component manager. **/
    public void reset() throws XNIException {
        fNamespaceContext.reset();
        fValidationManager.reset();
        fEntityManager.reset(this);
        fErrorReporter.reset(this);
        fSchemaValidator.reset(this);
        // Mark configuration as fixed.
        fConfigUpdated = false;
    }

    void setErrorHandler(ErrorHandler errorHandler) {
        fErrorHandler = errorHandler;
        setProperty(ERROR_HANDLER, (errorHandler != null) ? new ErrorHandlerWrapper(errorHandler) :
                new ErrorHandlerWrapper(DraconianErrorHandler.getInstance()));
    }

    ErrorHandler getErrorHandler() {
        return fErrorHandler;
    }

    void setResourceResolver(LSResourceResolver resourceResolver) {
        fResourceResolver = resourceResolver;
        setProperty(ENTITY_RESOLVER, new DOMEntityResolverWrapper(resourceResolver));
    }

    LSResourceResolver getResourceResolver() {
        return fResourceResolver;
    }

    void setLocale(Locale locale) {
        fLocale = locale;
        fErrorReporter.setLocale(locale);
    }

    Locale getLocale() {
        return fLocale;
    }

    /** Cleans out configuration, restoring it to its initial state. */
    void restoreInitialState() {
        fConfigUpdated = true;

        // Remove error resolver and error handler
        fComponents.put(ENTITY_RESOLVER, null);
        fComponents.put(ERROR_HANDLER, null);

        // Set the Locale back to null.
        setLocale(null);
        fComponents.put(LOCALE, null);

        // Restore initial security manager
        fInitSecurityManager.setSecureProcessing(true);
        fComponents.put(SECURITY_MANAGER, fInitSecurityManager);

        // Set the Locale back to null.
        setLocale(null);
        fComponents.put(LOCALE, null);

        // Reset feature and property values to their initial values
        if (!fInitFeatures.isEmpty()) {
            for (Map.Entry<String, Boolean> entry : fInitFeatures.entrySet()) {
                String name = entry.getKey();
                boolean value = entry.getValue();
                super.setFeature(name, value);
            }
            fInitFeatures.clear();
        }
        if (!fInitProperties.isEmpty()) {
            for (Map.Entry<String, Object> entry : fInitProperties.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                super.setProperty(name, value);
            }
            fInitProperties.clear();
        }
    }

    /** Sets feature defaults for the given component on this configuration. */
    private void setFeatureDefaults(final XMLComponent component,
            final String [] recognizedFeatures, XSGrammarPoolContainer grammarContainer) {
        if (recognizedFeatures != null) {
            for (int i = 0; i < recognizedFeatures.length; ++i) {
                String featureId = recognizedFeatures[i];
                Boolean state = grammarContainer.getFeature(featureId);
                if (state == null) {
                    state = component.getFeatureDefault(featureId);
                }
                if (state != null) {
                    // Do not overwrite values already set on the configuration.
                    if (!fFeatures.containsKey(featureId)) {
                        fFeatures.put(featureId, state);
                        // For newly added components who recognize this feature
                        // but did not offer a default value, we need to make
                        // sure these components will get an opportunity to read
                        // the value before parsing begins.
                        fConfigUpdated = true;
                    }
                }
            }
        }
    }

    /** Sets property defaults for the given component on this configuration. */
    private void setPropertyDefaults(final XMLComponent component, final String [] recognizedProperties) {
        if (recognizedProperties != null) {
            for (int i = 0; i < recognizedProperties.length; ++i) {
                String propertyId = recognizedProperties[i];
                Object value = component.getPropertyDefault(propertyId);
                if (value != null) {
                    // Do not overwrite values already set on the configuration.
                    if (!fProperties.containsKey(propertyId)) {
                        fProperties.put(propertyId, value);
                        // For newly added components who recognize this property
                        // but did not offer a default value, we need to make
                        // sure these components will get an opportunity to read
                        // the value before parsing begins.
                        fConfigUpdated = true;
                    }
                }
            }
        }
    }

} // XMLSchemaValidatorComponentManager
