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

package com.sun.org.apache.xerces.internal.parsers;

import java.io.IOException;
import java.util.Locale;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLDTDScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.XMLNamespaceBinder;
import com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDProcessor;
import com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDValidator;
import com.sun.org.apache.xerces.internal.impl.dv.DTDDVFactory;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.util.FeatureState;
import com.sun.org.apache.xerces.internal.util.PropertyState;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLPullParserConfiguration;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;

/**
 * This is the DTD-only parser configuration.  It extends the basic
 * configuration with a standard set of parser components appropriate
 * to DTD-centric validation. Since
 * the Xerces2 reference implementation document and DTD scanner
 * implementations are capable of acting as pull parsers, this
 * configuration implements the
 * <code>XMLPullParserConfiguration</code> interface.
 * <p>
 * In addition to the features and properties recognized by the base
 * parser configuration, this class recognizes these additional
 * features and properties:
 * <ul>
 * <li>Features
 *  <ul>
 *   <li>http://apache.org/xml/features/validation/warn-on-duplicate-attdef</li>
 *   <li>http://apache.org/xml/features/validation/warn-on-undeclared-elemdef</li>
 *   <li>http://apache.org/xml/features/allow-java-encodings</li>
 *   <li>http://apache.org/xml/features/continue-after-fatal-error</li>
 *   <li>http://apache.org/xml/features/load-external-dtd</li>
 *  </ul>
 * <li>Properties
 *  <ul>
 *   <li>http://apache.org/xml/properties/internal/error-reporter</li>
 *   <li>http://apache.org/xml/properties/internal/entity-manager</li>
 *   <li>http://apache.org/xml/properties/internal/document-scanner</li>
 *   <li>http://apache.org/xml/properties/internal/dtd-scanner</li>
 *   <li>http://apache.org/xml/properties/internal/grammar-pool</li>
 *   <li>http://apache.org/xml/properties/internal/validator/dtd</li>
 *   <li>http://apache.org/xml/properties/internal/datatype-validator-factory</li>
 *  </ul>
 * </ul>
 *
 * @author Arnaud  Le Hors, IBM
 * @author Andy Clark, IBM
 * @author Neil Graham, IBM
 * @LastModified: May 2021
 */
public class DTDConfiguration
    extends BasicParserConfiguration
    implements XMLPullParserConfiguration {

    //
    // Constants
    //

    // feature identifiers

    /** Feature identifier: warn on duplicate attribute definition. */
    protected static final String WARN_ON_DUPLICATE_ATTDEF =
        Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ATTDEF_FEATURE;

    /** Feature identifier: warn on duplicate entity definition. */
    protected static final String WARN_ON_DUPLICATE_ENTITYDEF =
        Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ENTITYDEF_FEATURE;

    /** Feature identifier: warn on undeclared element definition. */
    protected static final String WARN_ON_UNDECLARED_ELEMDEF =
        Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_UNDECLARED_ELEMDEF_FEATURE;

    /** Feature identifier: allow Java encodings. */
    protected static final String ALLOW_JAVA_ENCODINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE;

    /** Feature identifier: continue after fatal error. */
    protected static final String CONTINUE_AFTER_FATAL_ERROR =
        Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE;

    /** Feature identifier: load external DTD. */
    protected static final String LOAD_EXTERNAL_DTD =
        Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE;

    /** Feature identifier: notify built-in refereces. */
    protected static final String NOTIFY_BUILTIN_REFS =
        Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_BUILTIN_REFS_FEATURE;

    /** Feature identifier: notify character refereces. */
    protected static final String NOTIFY_CHAR_REFS =
        Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_CHAR_REFS_FEATURE;


    // property identifiers

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: entity manager. */
    protected static final String ENTITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY;

    /** Property identifier document scanner: */
    protected static final String DOCUMENT_SCANNER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DOCUMENT_SCANNER_PROPERTY;

    /** Property identifier: DTD scanner. */
    protected static final String DTD_SCANNER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_SCANNER_PROPERTY;

    /** Property identifier: grammar pool. */
    protected static final String XMLGRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Property identifier: DTD loader. */
    protected static final String DTD_PROCESSOR =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_PROCESSOR_PROPERTY;

    /** Property identifier: DTD validator. */
    protected static final String DTD_VALIDATOR =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_VALIDATOR_PROPERTY;

    /** Property identifier: namespace binder. */
    protected static final String NAMESPACE_BINDER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.NAMESPACE_BINDER_PROPERTY;

    /** Property identifier: datatype validator factory. */
    protected static final String DATATYPE_VALIDATOR_FACTORY =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DATATYPE_VALIDATOR_FACTORY_PROPERTY;

    protected static final String VALIDATION_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    /** Property identifier: JAXP schema language / DOM schema-type. */
    protected static final String JAXP_SCHEMA_LANGUAGE =
        Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE;

    /** Property identifier: JAXP schema source/ DOM schema-location. */
    protected static final String JAXP_SCHEMA_SOURCE =
        Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE;

    /** Property identifier: locale. */
    protected static final String LOCALE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.LOCALE_PROPERTY;

      /** Property identifier: Security property manager. */
      protected static final String XML_SECURITY_PROPERTY_MANAGER =
              JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

     /** Property identifier: Security manager. */
     private static final String SECURITY_MANAGER = Constants.SECURITY_MANAGER;

    // debugging

    /** Set to true and recompile to print exception stack trace. */
    protected static final boolean PRINT_EXCEPTION_STACK_TRACE = false;

    //
    // Data
    //

    // components (non-configurable)

    /** Grammar pool. */
    protected XMLGrammarPool fGrammarPool;

    /** Datatype validator factory. */
    protected DTDDVFactory fDatatypeValidatorFactory;

    // components (configurable)

    /** Error reporter. */
    protected XMLErrorReporter fErrorReporter;

    /** Entity manager. */
    protected XMLEntityManager fEntityManager;

    /** Document scanner. */
    protected XMLDocumentScanner fScanner;

    /** Input Source */
    protected XMLInputSource fInputSource;

    /** DTD scanner. */
    protected XMLDTDScanner fDTDScanner;

    /** DTD Processor . */
    protected XMLDTDProcessor fDTDProcessor;

    /** DTD Validator. */
    protected XMLDTDValidator fDTDValidator;

    /** Namespace binder. */
    protected XMLNamespaceBinder fNamespaceBinder;

    protected ValidationManager fValidationManager;
    // state

    /** Locator */
    protected XMLLocator fLocator;

    /**
     * True if a parse is in progress. This state is needed because
     * some features/properties cannot be set while parsing (e.g.
     * validation and namespaces).
     */
    protected boolean fParseInProgress = false;

    //
    // Constructors
    //

    /** Default constructor. */
    public DTDConfiguration() {
        this(null, null, null);
    } // <init>()

    /**
     * Constructs a parser configuration using the specified symbol table.
     *
     * @param symbolTable The symbol table to use.
     */
    public DTDConfiguration(SymbolTable symbolTable) {
        this(symbolTable, null, null);
    } // <init>(SymbolTable)

    /**
     * Constructs a parser configuration using the specified symbol table and
     * grammar pool.
     * <p>
     * <strong>REVISIT:</strong>
     * Grammar pool will be updated when the new validation engine is
     * implemented.
     *
     * @param symbolTable The symbol table to use.
     * @param grammarPool The grammar pool to use.
     */
    public DTDConfiguration(SymbolTable symbolTable,
                                       XMLGrammarPool grammarPool) {
        this(symbolTable, grammarPool, null);
    } // <init>(SymbolTable,XMLGrammarPool)

    /**
     * Constructs a parser configuration using the specified symbol table,
     * grammar pool, and parent settings.
     * <p>
     * <strong>REVISIT:</strong>
     * Grammar pool will be updated when the new validation engine is
     * implemented.
     *
     * @param symbolTable    The symbol table to use.
     * @param grammarPool    The grammar pool to use.
     * @param parentSettings The parent settings.
     */
    public DTDConfiguration(SymbolTable symbolTable,
                                       XMLGrammarPool grammarPool,
                                       XMLComponentManager parentSettings) {
        super(symbolTable, parentSettings);

        // add default recognized features
        final String[] recognizedFeatures = {
            //WARN_ON_DUPLICATE_ATTDEF,     // from XMLDTDScannerImpl
            //WARN_ON_UNDECLARED_ELEMDEF,   // from XMLDTDScannerImpl
            //ALLOW_JAVA_ENCODINGS,         // from XMLEntityManager
            CONTINUE_AFTER_FATAL_ERROR,
            LOAD_EXTERNAL_DTD,    // from XMLDTDScannerImpl
            //NOTIFY_BUILTIN_REFS,  // from XMLDocumentFragmentScannerImpl
            //NOTIFY_CHAR_REFS,         // from XMLDocumentFragmentScannerImpl
            //WARN_ON_DUPLICATE_ENTITYDEF,  // from XMLEntityManager
            XMLConstants.USE_CATALOG,
            JdkConstants.OVERRIDE_PARSER
        };
        addRecognizedFeatures(recognizedFeatures);

        // set state for default features
        //setFeature(WARN_ON_DUPLICATE_ATTDEF, false);  // from XMLDTDScannerImpl
        //setFeature(WARN_ON_UNDECLARED_ELEMDEF, false);  // from XMLDTDScannerImpl
        //setFeature(ALLOW_JAVA_ENCODINGS, false);      // from XMLEntityManager
        setFeature(CONTINUE_AFTER_FATAL_ERROR, false);
        setFeature(LOAD_EXTERNAL_DTD, true);      // from XMLDTDScannerImpl
        //setFeature(NOTIFY_BUILTIN_REFS, false);   // from XMLDocumentFragmentScannerImpl
        //setFeature(NOTIFY_CHAR_REFS, false);      // from XMLDocumentFragmentScannerImpl
        //setFeature(WARN_ON_DUPLICATE_ENTITYDEF, false);   // from XMLEntityManager
        fFeatures.put(XMLConstants.USE_CATALOG, JdkXmlUtils.USE_CATALOG_DEFAULT);
        fFeatures.put(JdkConstants.OVERRIDE_PARSER, JdkConstants.OVERRIDE_PARSER_DEFAULT);

        // add default recognized properties
        final String[] recognizedProperties = {
            ERROR_REPORTER,
            ENTITY_MANAGER,
            DOCUMENT_SCANNER,
            DTD_SCANNER,
            DTD_PROCESSOR,
            DTD_VALIDATOR,
            NAMESPACE_BINDER,
            XMLGRAMMAR_POOL,
            DATATYPE_VALIDATOR_FACTORY,
            VALIDATION_MANAGER,
            JAXP_SCHEMA_SOURCE,
            JAXP_SCHEMA_LANGUAGE,
            LOCALE,
            SECURITY_MANAGER,
            XML_SECURITY_PROPERTY_MANAGER,
            JdkXmlUtils.CATALOG_DEFER,
            JdkXmlUtils.CATALOG_FILES,
            JdkXmlUtils.CATALOG_PREFER,
            JdkXmlUtils.CATALOG_RESOLVE,
            JdkConstants.CDATA_CHUNK_SIZE
        };
        addRecognizedProperties(recognizedProperties);

        fGrammarPool = grammarPool;
        if(fGrammarPool != null){
            setProperty(XMLGRAMMAR_POOL, fGrammarPool);
        }

        fEntityManager = createEntityManager();
        setProperty(ENTITY_MANAGER, fEntityManager);
        addComponent(fEntityManager);

        fErrorReporter = createErrorReporter();
        fErrorReporter.setDocumentLocator(fEntityManager.getEntityScanner());
        setProperty(ERROR_REPORTER, fErrorReporter);
        addComponent(fErrorReporter);

        fScanner = createDocumentScanner();
        setProperty(DOCUMENT_SCANNER, fScanner);
        if (fScanner instanceof XMLComponent) {
            addComponent((XMLComponent)fScanner);
        }

        fDTDScanner = createDTDScanner();
        if (fDTDScanner != null) {
            setProperty(DTD_SCANNER, fDTDScanner);
            if (fDTDScanner instanceof XMLComponent) {
                addComponent((XMLComponent)fDTDScanner);
            }
        }

        fDTDProcessor = createDTDProcessor();
        if (fDTDProcessor != null) {
            setProperty(DTD_PROCESSOR, fDTDProcessor);
            if (fDTDProcessor instanceof XMLComponent) {
                addComponent((XMLComponent)fDTDProcessor);
            }
        }

        fDTDValidator = createDTDValidator();
        if (fDTDValidator != null) {
            setProperty(DTD_VALIDATOR, fDTDValidator);
            addComponent(fDTDValidator);
        }

        fNamespaceBinder = createNamespaceBinder();
        if (fNamespaceBinder != null) {
            setProperty(NAMESPACE_BINDER, fNamespaceBinder);
            addComponent(fNamespaceBinder);
        }

        fDatatypeValidatorFactory = createDatatypeValidatorFactory();
        if (fDatatypeValidatorFactory != null) {
            setProperty(DATATYPE_VALIDATOR_FACTORY,
                        fDatatypeValidatorFactory);
        }
        fValidationManager = createValidationManager();

        if (fValidationManager != null) {
            setProperty (VALIDATION_MANAGER, fValidationManager);
        }
        // add message formatters
        if (fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN) == null) {
            XMLMessageFormatter xmft = new XMLMessageFormatter();
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, xmft);
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XMLNS_DOMAIN, xmft);
        }

        // set locale
        try {
            setLocale(Locale.getDefault());
        }
        catch (XNIException e) {
            // do nothing
            // REVISIT: What is the right thing to do? -Ac
        }

        setProperty(XML_SECURITY_PROPERTY_MANAGER, new XMLSecurityPropertyManager());

        // Initialize Catalog features
        for( CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
            setProperty(f.getPropertyName(), null);
        }

        setProperty(JdkConstants.CDATA_CHUNK_SIZE, JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);
    } // <init>(SymbolTable,XMLGrammarPool)

    //
    // Public methods
    //

    public PropertyState getPropertyState(String propertyId)
        throws XMLConfigurationException {
        if (LOCALE.equals(propertyId)) {
            return PropertyState.is(getLocale());
        }
        return super.getPropertyState(propertyId);
    }

    public void setProperty(String propertyId, Object value)
        throws XMLConfigurationException {
        if (LOCALE.equals(propertyId)) {
            setLocale((Locale) value);
        }
        super.setProperty(propertyId, value);
    }

    /**
     * Set the locale to use for messages.
     *
     * @param locale The locale object to use for localization of messages.
     *
     * @exception XNIException Thrown if the parser does not support the
     *                         specified locale.
     */
    public void setLocale(Locale locale) throws XNIException {
        super.setLocale(locale);
        fErrorReporter.setLocale(locale);
    } // setLocale(Locale)

    //
    // XMLPullParserConfiguration methods
    //

    // parsing

    /**
     * Sets the input source for the document to parse.
     *
     * @param inputSource The document's input source.
     *
     * @exception XMLConfigurationException Thrown if there is a
     *                        configuration error when initializing the
     *                        parser.
     * @exception IOException Thrown on I/O error.
     *
     * @see #parse(boolean)
     */
    public void setInputSource(XMLInputSource inputSource)
        throws XMLConfigurationException, IOException {

        // REVISIT: this method used to reset all the components and
        //          construct the pipeline. Now reset() is called
        //          in parse (boolean) just before we parse the document
        //          Should this method still throw exceptions..?

        fInputSource = inputSource;

    } // setInputSource(XMLInputSource)

    /**
     * Parses the document in a pull parsing fashion.
     *
     * @param complete True if the pull parser should parse the
     *                 remaining document completely.
     *
     * @return True if there is more document to parse.
     *
     * @exception XNIException Any XNI exception, possibly wrapping
     *                         another exception.
     * @exception IOException  An IO exception from the parser, possibly
     *                         from a byte stream or character stream
     *                         supplied by the parser.
     *
     * @see #setInputSource
     */
    public boolean parse(boolean complete) throws XNIException, IOException {
        //
        // reset and configure pipeline and set InputSource.
        if (fInputSource !=null) {
            try {
                // resets and sets the pipeline.
                reset();
                fScanner.setInputSource(fInputSource);
                fInputSource = null;
            }
            catch (XNIException ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw ex;
            }
            catch (IOException ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw ex;
            }
            catch (RuntimeException ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw ex;
            }
            catch (Exception ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw new XNIException(ex);
            }
        }

        try {
            return fScanner.scanDocument(complete);
        }
        catch (XNIException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        }
        catch (IOException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        }
        catch (RuntimeException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        }
        catch (Exception ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw new XNIException(ex);
        }

    } // parse(boolean):boolean

    /**
     * If the application decides to terminate parsing before the xml document
     * is fully parsed, the application should call this method to free any
     * resource allocated during parsing. For example, close all opened streams.
     */
    public void cleanup() {
        fEntityManager.closeReaders();
    }

    //
    // XMLParserConfiguration methods
    //

    /**
     * Parses the specified input source.
     *
     * @param source The input source.
     *
     * @exception XNIException Throws exception on XNI error.
     * @exception java.io.IOException Throws exception on i/o error.
     */
    public void parse(XMLInputSource source) throws XNIException, IOException {

        if (fParseInProgress) {
            // REVISIT - need to add new error message
            throw new XNIException("FWK005 parse may not be called while parsing.");
        }
        fParseInProgress = true;

        try {
            setInputSource(source);
            parse(true);
        }
        catch (XNIException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        }
        catch (IOException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        }
        catch (RuntimeException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        }
        catch (Exception ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw new XNIException(ex);
        }
        finally {
            fParseInProgress = false;
            // close all streams opened by xerces
            this.cleanup();
        }

    } // parse(InputSource)

    //
    // Protected methods
    //

    /**
     * Reset all components before parsing.
     *
     * @throws XNIException Thrown if an error occurs during initialization.
     */
    protected void reset() throws XNIException {

        if (fValidationManager != null)
            fValidationManager.reset();
        // configure the pipeline and initialize the components
        configurePipeline();
        super.reset();
    } // reset()

    /** Configures the pipeline. */
        protected void configurePipeline() {

                // REVISIT: This should be better designed. In other words, we
                //          need to figure out what is the best way for people to
                //          re-use *most* of the standard configuration but do
                //          things common things such as remove a component (e.g.
                //          the validator), insert a new component (e.g. XInclude),
                //          etc... -Ac

                // setup document pipeline
                if (fDTDValidator != null) {
                        fScanner.setDocumentHandler(fDTDValidator);
                        if (fFeatures.get(NAMESPACES) == Boolean.TRUE) {

                                // filters
                                fDTDValidator.setDocumentHandler(fNamespaceBinder);
                                fDTDValidator.setDocumentSource(fScanner);
                                fNamespaceBinder.setDocumentHandler(fDocumentHandler);
                                fNamespaceBinder.setDocumentSource(fDTDValidator);
                                fLastComponent = fNamespaceBinder;
                        }
                        else {
                                fDTDValidator.setDocumentHandler(fDocumentHandler);
                                fDTDValidator.setDocumentSource(fScanner);
                                fLastComponent = fDTDValidator;
                        }
                }
                else {
                        if (fFeatures.get(NAMESPACES) == Boolean.TRUE) {
                                fScanner.setDocumentHandler(fNamespaceBinder);
                                fNamespaceBinder.setDocumentHandler(fDocumentHandler);
                                fNamespaceBinder.setDocumentSource(fScanner);
                                fLastComponent = fNamespaceBinder;
                        }
                        else {
                                fScanner.setDocumentHandler(fDocumentHandler);
                                fLastComponent = fScanner;
                        }
                }

        configureDTDPipeline();
        } // configurePipeline()

    protected void configureDTDPipeline (){

        // setup dtd pipeline
        if (fDTDScanner != null) {
            fProperties.put(DTD_SCANNER, fDTDScanner);
            if (fDTDProcessor != null) {
                fProperties.put(DTD_PROCESSOR, fDTDProcessor);
                fDTDScanner.setDTDHandler(fDTDProcessor);
                fDTDProcessor.setDTDSource(fDTDScanner);
                fDTDProcessor.setDTDHandler(fDTDHandler);
                if (fDTDHandler != null) {
                    fDTDHandler.setDTDSource(fDTDProcessor);
                }

                fDTDScanner.setDTDContentModelHandler(fDTDProcessor);
                fDTDProcessor.setDTDContentModelSource(fDTDScanner);
                fDTDProcessor.setDTDContentModelHandler(fDTDContentModelHandler);
                if (fDTDContentModelHandler != null) {
                    fDTDContentModelHandler.setDTDContentModelSource(fDTDProcessor);
                }
            }
            else {
                fDTDScanner.setDTDHandler(fDTDHandler);
                if (fDTDHandler != null) {
                    fDTDHandler.setDTDSource(fDTDScanner);
                }
                fDTDScanner.setDTDContentModelHandler(fDTDContentModelHandler);
                if (fDTDContentModelHandler != null) {
                    fDTDContentModelHandler.setDTDContentModelSource(fDTDScanner);
                }
            }
        }


    }

    // features and properties

    /**
     * Check a feature. If feature is know and supported, this method simply
     * returns. Otherwise, the appropriate exception is thrown.
     *
     * @param featureId The unique identifier (URI) of the feature.
     *
     * @throws XMLConfigurationException Thrown for configuration error.
     *                                   In general, components should
     *                                   only throw this exception if
     *                                   it is <strong>really</strong>
     *                                   a critical error.
     */
    protected FeatureState checkFeature(String featureId)
        throws XMLConfigurationException {

        //
        // Xerces Features
        //

        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            final int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();

            //
            // http://apache.org/xml/features/validation/dynamic
            //   Allows the parser to validate a document only when it
            //   contains a grammar. Validation is turned on/off based
            //   on each document instance, automatically.
            //
            if (suffixLength == Constants.DYNAMIC_VALIDATION_FEATURE.length() &&
                featureId.endsWith(Constants.DYNAMIC_VALIDATION_FEATURE)) {
                return FeatureState.RECOGNIZED;
            }

            //
            // http://apache.org/xml/features/validation/default-attribute-values
            //
            if (suffixLength == Constants.DEFAULT_ATTRIBUTE_VALUES_FEATURE.length() &&
                featureId.endsWith(Constants.DEFAULT_ATTRIBUTE_VALUES_FEATURE)) {
                // REVISIT
                return FeatureState.NOT_SUPPORTED;
            }
            //
            // http://apache.org/xml/features/validation/default-attribute-values
            //
            if (suffixLength == Constants.VALIDATE_CONTENT_MODELS_FEATURE.length() &&
                featureId.endsWith(Constants.VALIDATE_CONTENT_MODELS_FEATURE)) {
                // REVISIT
                return FeatureState.NOT_SUPPORTED;
            }
            //
            // http://apache.org/xml/features/validation/nonvalidating/load-dtd-grammar
            //
            if (suffixLength == Constants.LOAD_DTD_GRAMMAR_FEATURE.length() &&
                featureId.endsWith(Constants.LOAD_DTD_GRAMMAR_FEATURE)) {
                return FeatureState.RECOGNIZED;
            }
            //
            // http://apache.org/xml/features/validation/nonvalidating/load-external-dtd
            //
            if (suffixLength == Constants.LOAD_EXTERNAL_DTD_FEATURE.length() &&
                featureId.endsWith(Constants.LOAD_EXTERNAL_DTD_FEATURE)) {
                return FeatureState.RECOGNIZED;
            }

            //
            // http://apache.org/xml/features/validation/default-attribute-values
            //
            if (suffixLength == Constants.VALIDATE_DATATYPES_FEATURE.length() &&
                featureId.endsWith(Constants.VALIDATE_DATATYPES_FEATURE)) {
                return FeatureState.NOT_SUPPORTED;
            }
        }

        //
        // Not recognized
        //

        return super.checkFeature(featureId);

    } // checkFeature(String)

    /**
     * Check a property. If the property is know and supported, this method
     * simply returns. Otherwise, the appropriate exception is thrown.
     *
     * @param propertyId The unique identifier (URI) of the property
     *                   being set.
     *
     * @throws XMLConfigurationException Thrown for configuration error.
     *                                   In general, components should
     *                                   only throw this exception if
     *                                   it is <strong>really</strong>
     *                                   a critical error.
     */
    protected PropertyState checkProperty(String propertyId)
        throws XMLConfigurationException {

        //
        // Xerces Properties
        //

        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();

            if (suffixLength == Constants.DTD_SCANNER_PROPERTY.length() &&
                propertyId.endsWith(Constants.DTD_SCANNER_PROPERTY)) {
                return PropertyState.RECOGNIZED;
            }
        }

        //
        // Not recognized
        //

        return super.checkProperty(propertyId);

    } // checkProperty(String)

    // factory methods

    /** Creates an entity manager. */
    protected XMLEntityManager createEntityManager() {
        return new XMLEntityManager();
    } // createEntityManager():XMLEntityManager

    /** Creates an error reporter. */
    protected XMLErrorReporter createErrorReporter() {
        return new XMLErrorReporter();
    } // createErrorReporter():XMLErrorReporter

    /** Create a document scanner. */
    protected XMLDocumentScanner createDocumentScanner() {
        return new XMLDocumentScannerImpl();
    } // createDocumentScanner():XMLDocumentScanner

    /** Create a DTD scanner. */
    protected XMLDTDScanner createDTDScanner() {
        return new XMLDTDScannerImpl();
    } // createDTDScanner():XMLDTDScanner

    /** Create a DTD loader . */
    protected XMLDTDProcessor createDTDProcessor() {
        return new XMLDTDProcessor();
    } // createDTDProcessor():XMLDTDProcessor

    /** Create a DTD validator. */
    protected XMLDTDValidator createDTDValidator() {
        return new XMLDTDValidator();
    } // createDTDValidator():XMLDTDValidator

    /** Create a namespace binder. */
    protected XMLNamespaceBinder createNamespaceBinder() {
        return new XMLNamespaceBinder();
    } // createNamespaceBinder():XMLNamespaceBinder

    /** Create a datatype validator factory. */
    protected DTDDVFactory createDatatypeValidatorFactory() {
        return DTDDVFactory.getInstance();
    } // createDatatypeValidatorFactory():DatatypeValidatorFactory
    protected ValidationManager createValidationManager(){
        return new ValidationManager();
    }

} // class DTDConfiguration
