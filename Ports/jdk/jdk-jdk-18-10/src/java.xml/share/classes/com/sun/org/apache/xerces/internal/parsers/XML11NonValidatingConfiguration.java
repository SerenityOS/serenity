/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XML11DTDScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XML11DocumentScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XML11NSDocumentScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLDTDScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLDocumentScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLEntityHandler;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.XMLNSDocumentScannerImpl;
import com.sun.org.apache.xerces.internal.impl.XMLVersionDetector;
import com.sun.org.apache.xerces.internal.impl.dv.DTDDVFactory;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.util.FeatureState;
import com.sun.org.apache.xerces.internal.util.ParserConfigurationSettings;
import com.sun.org.apache.xerces.internal.util.PropertyState;
import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.xni.XMLDTDContentModelHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDTDHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLPullParserConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * This class is the non vlaidating parser configuration
 * used to parse XML 1.0 and XML 1.1 documents.
 *
 * Xerces parser that uses this configuration is <strong>not</strong>
 * <a href="http://www.w3.org/TR/REC-xml#sec-conformance">conformant</a>
 * non-validating XML processor, since conformant non-validating processor is
 * required to process "all the declarations they read in the internal DTD subset
 * ... must use the information in those declarations to normalize attribute values,
 * include the replacement text of internal entities, and supply default attribute values".

 * @author Elena Litani, IBM
 * @author John Kim, IBM
 * @author Michael Glavassevich, IBM
 *
 * @LastModified: Oct 2017
 */
public class XML11NonValidatingConfiguration extends ParserConfigurationSettings
    implements XMLPullParserConfiguration, XML11Configurable {

    //
    // Constants
    //
    protected final static String XML11_DATATYPE_VALIDATOR_FACTORY =
        "com.sun.org.apache.xerces.internal.impl.dv.dtd.XML11DTDDVFactoryImpl";

    // feature identifiers

    /** Feature identifier: validation. */
    protected static final String VALIDATION =
        Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** Feature identifier: namespaces. */
    protected static final String NAMESPACES =
        Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

    /** Feature identifier: external general entities. */
    protected static final String EXTERNAL_GENERAL_ENTITIES =
        Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE;

    /** Feature identifier: external parameter entities. */
    protected static final String EXTERNAL_PARAMETER_ENTITIES =
        Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE;


    /** Feature identifier: continue after fatal error. */
    protected static final String CONTINUE_AFTER_FATAL_ERROR =
        Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE;


    // property identifiers

        /** Property identifier: xml string. */
        protected static final String XML_STRING =
                Constants.SAX_PROPERTY_PREFIX + Constants.XML_STRING_PROPERTY;

        /** Property identifier: symbol table. */
        protected static final String SYMBOL_TABLE =
                Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

        /** Property identifier: error handler. */
        protected static final String ERROR_HANDLER =
                Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;

        /** Property identifier: entity resolver. */
        protected static final String ENTITY_RESOLVER =
                Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

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

    // debugging

    /** Set to true and recompile to print exception stack trace. */
    protected static final boolean PRINT_EXCEPTION_STACK_TRACE = false;

    //
    // Data
    //
    protected SymbolTable fSymbolTable;
    protected XMLInputSource fInputSource;
    protected ValidationManager fValidationManager;
    protected XMLVersionDetector fVersionDetector;
    protected XMLLocator fLocator;
    protected Locale fLocale;

    /** XML 1.0 Components. */
    protected List<XMLComponent> fComponents;

    /** XML 1.1. Components. */
    protected List<XMLComponent> fXML11Components = null;

    /** Common components: XMLEntityManager, XMLErrorReporter */
    protected List<XMLComponent> fCommonComponents = null;

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** The DTD handler. */
    protected XMLDTDHandler fDTDHandler;

    /** The DTD content model handler. */
    protected XMLDTDContentModelHandler fDTDContentModelHandler;

    /** Last component in the document pipeline */
    protected XMLDocumentSource fLastComponent;

    /**
     * True if a parse is in progress. This state is needed because
     * some features/properties cannot be set while parsing (e.g.
     * namespaces).
     */
    protected boolean fParseInProgress = false;

    /** fConfigUpdated is set to true if there has been any change to the configuration settings,
     * i.e a feature or a property was changed.
     */
    protected boolean fConfigUpdated = false;

    //
    // XML 1.0 components
    //

    /** The XML 1.0 Datatype validator factory. */
    protected DTDDVFactory fDatatypeValidatorFactory;

    /** The XML 1.0 Document scanner that does namespace binding. */
    protected XMLNSDocumentScannerImpl fNamespaceScanner;

    /** The XML 1.0 Non-namespace implementation of scanner */
    protected XMLDocumentScannerImpl fNonNSScanner;

    /** The XML 1.0 DTD scanner. */
    protected XMLDTDScanner fDTDScanner;

    //
    // XML 1.1 components
    //

    /** The XML 1.1 datatype factory. **/
    protected DTDDVFactory fXML11DatatypeFactory = null;

    /** The XML 1.1 document scanner that does namespace binding. **/
    protected XML11NSDocumentScannerImpl fXML11NSDocScanner = null;

    /** The XML 1.1 document scanner that does not do namespace binding. **/
    protected XML11DocumentScannerImpl fXML11DocScanner = null;

    /** The XML 1.1 DTD scanner. **/
    protected XML11DTDScannerImpl fXML11DTDScanner = null;

    //
    // Common components
    //

    /** Grammar pool. */
    protected XMLGrammarPool fGrammarPool;

    /** Error reporter. */
    protected XMLErrorReporter fErrorReporter;

    /** Entity manager. */
    protected XMLEntityManager fEntityManager;

    /** Current scanner */
    protected XMLDocumentScanner fCurrentScanner;

    /** Current Datatype validator factory. */
    protected DTDDVFactory fCurrentDVFactory;

    /** Current DTD scanner. */
    protected XMLDTDScanner fCurrentDTDScanner;


    /** Flag indiciating whether XML11 components have been initialized. */
    private boolean f11Initialized = false;

    //
    // Constructors
    //

    /** Default constructor. */
    public XML11NonValidatingConfiguration() {
        this(null, null, null);
    } // <init>()

    /**
     * Constructs a parser configuration using the specified symbol table.
     *
     * @param symbolTable The symbol table to use.
     */
    public XML11NonValidatingConfiguration(SymbolTable symbolTable) {
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
    public XML11NonValidatingConfiguration(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
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
    public XML11NonValidatingConfiguration(
        SymbolTable symbolTable,
        XMLGrammarPool grammarPool,
        XMLComponentManager parentSettings) {

                super(parentSettings);

                // create a vector to hold all the components in use
                // XML 1.0 specialized components
                fComponents = new ArrayList<>();
                // XML 1.1 specialized components
                fXML11Components = new ArrayList<>();
                // Common components for XML 1.1. and XML 1.0
                fCommonComponents = new ArrayList<>();

                // create table for features and properties
                fFeatures = new HashMap<>();
                fProperties = new HashMap<>();

        // add default recognized features
        final String[] recognizedFeatures =
            {
                CONTINUE_AFTER_FATAL_ERROR, // from XMLDTDScannerImpl
                                VALIDATION,
                                NAMESPACES,
                                EXTERNAL_GENERAL_ENTITIES,
                                EXTERNAL_PARAMETER_ENTITIES,
                                PARSER_SETTINGS
                        };
        addRecognizedFeatures(recognizedFeatures);

                // set state for default features
                fFeatures.put(VALIDATION, Boolean.FALSE);
                fFeatures.put(NAMESPACES, Boolean.TRUE);
                fFeatures.put(EXTERNAL_GENERAL_ENTITIES, Boolean.TRUE);
                fFeatures.put(EXTERNAL_PARAMETER_ENTITIES, Boolean.TRUE);
                fFeatures.put(CONTINUE_AFTER_FATAL_ERROR, Boolean.FALSE);
                fFeatures.put(PARSER_SETTINGS, Boolean.TRUE);

        // add default recognized properties
        final String[] recognizedProperties =
            {
                XML_STRING,
                SYMBOL_TABLE,
                                ERROR_HANDLER,
                                ENTITY_RESOLVER,
                ERROR_REPORTER,
                ENTITY_MANAGER,
                DOCUMENT_SCANNER,
                DTD_SCANNER,
                DTD_VALIDATOR,
                                DATATYPE_VALIDATOR_FACTORY,
                                VALIDATION_MANAGER,
                                XML_STRING,
                XMLGRAMMAR_POOL, };
        addRecognizedProperties(recognizedProperties);

                if (symbolTable == null) {
                        symbolTable = new SymbolTable();
                }
                fSymbolTable = symbolTable;
                fProperties.put(SYMBOL_TABLE, fSymbolTable);

        fGrammarPool = grammarPool;
        if (fGrammarPool != null) {
                        fProperties.put(XMLGRAMMAR_POOL, fGrammarPool);
        }

        fEntityManager = new XMLEntityManager();
                fProperties.put(ENTITY_MANAGER, fEntityManager);
        addCommonComponent(fEntityManager);

        fErrorReporter = new XMLErrorReporter();
        fErrorReporter.setDocumentLocator(fEntityManager.getEntityScanner());
                fProperties.put(ERROR_REPORTER, fErrorReporter);
        addCommonComponent(fErrorReporter);

        fNamespaceScanner = new XMLNSDocumentScannerImpl();
                fProperties.put(DOCUMENT_SCANNER, fNamespaceScanner);
        addComponent((XMLComponent) fNamespaceScanner);

        fDTDScanner = new XMLDTDScannerImpl();
                fProperties.put(DTD_SCANNER, fDTDScanner);
        addComponent((XMLComponent) fDTDScanner);

        fDatatypeValidatorFactory = DTDDVFactory.getInstance();
                fProperties.put(DATATYPE_VALIDATOR_FACTORY, fDatatypeValidatorFactory);

        fValidationManager = new ValidationManager();
                fProperties.put(VALIDATION_MANAGER, fValidationManager);

        fVersionDetector = new XMLVersionDetector();

        // add message formatters
        if (fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN) == null) {
            XMLMessageFormatter xmft = new XMLMessageFormatter();
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, xmft);
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XMLNS_DOMAIN, xmft);
        }

        // set locale
        try {
            setLocale(Locale.getDefault());
        } catch (XNIException e) {
            // do nothing
            // REVISIT: What is the right thing to do? -Ac
        }

                fConfigUpdated = false;

    } // <init>(SymbolTable,XMLGrammarPool)

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
     * Set the locale to use for messages.
     *
     * @param locale The locale object to use for localization of messages.
     *
     * @exception XNIException Thrown if the parser does not support the
     *                         specified locale.
     */
    public void setLocale(Locale locale) throws XNIException {
        fLocale = locale;
        fErrorReporter.setLocale(locale);
    } // setLocale(Locale)

        /**
         * Sets the document handler on the last component in the pipeline
         * to receive information about the document.
         *
         * @param documentHandler   The document handler.
         */
        public void setDocumentHandler(XMLDocumentHandler documentHandler) {
                fDocumentHandler = documentHandler;
                if (fLastComponent != null) {
                        fLastComponent.setDocumentHandler(fDocumentHandler);
                        if (fDocumentHandler !=null){
                                fDocumentHandler.setDocumentSource(fLastComponent);
                        }
                }
        } // setDocumentHandler(XMLDocumentHandler)

        /** Returns the registered document handler. */
        public XMLDocumentHandler getDocumentHandler() {
                return fDocumentHandler;
        } // getDocumentHandler():XMLDocumentHandler

        /**
         * Sets the DTD handler.
         *
         * @param dtdHandler The DTD handler.
         */
        public void setDTDHandler(XMLDTDHandler dtdHandler) {
                fDTDHandler = dtdHandler;
        } // setDTDHandler(XMLDTDHandler)

        /** Returns the registered DTD handler. */
        public XMLDTDHandler getDTDHandler() {
                return fDTDHandler;
        } // getDTDHandler():XMLDTDHandler

        /**
         * Sets the DTD content model handler.
         *
         * @param handler The DTD content model handler.
         */
        public void setDTDContentModelHandler(XMLDTDContentModelHandler handler) {
                fDTDContentModelHandler = handler;
        } // setDTDContentModelHandler(XMLDTDContentModelHandler)

        /** Returns the registered DTD content model handler. */
        public XMLDTDContentModelHandler getDTDContentModelHandler() {
                return fDTDContentModelHandler;
        } // getDTDContentModelHandler():XMLDTDContentModelHandler

        /**
         * Sets the resolver used to resolve external entities. The EntityResolver
         * interface supports resolution of public and system identifiers.
         *
         * @param resolver The new entity resolver. Passing a null value will
         *                 uninstall the currently installed resolver.
         */
        public void setEntityResolver(XMLEntityResolver resolver) {
                fProperties.put(ENTITY_RESOLVER, resolver);
        } // setEntityResolver(XMLEntityResolver)

        /**
         * Return the current entity resolver.
         *
         * @return The current entity resolver, or null if none
         *         has been registered.
         * @see #setEntityResolver
         */
        public XMLEntityResolver getEntityResolver() {
                return (XMLEntityResolver)fProperties.get(ENTITY_RESOLVER);
        } // getEntityResolver():XMLEntityResolver

        /**
         * Allow an application to register an error event handler.
         *
         * <p>If the application does not register an error handler, all
         * error events reported by the SAX parser will be silently
         * ignored; however, normal processing may not continue.  It is
         * highly recommended that all SAX applications implement an
         * error handler to avoid unexpected bugs.</p>
         *
         * <p>Applications may register a new or different handler in the
         * middle of a parse, and the SAX parser must begin using the new
         * handler immediately.</p>
         *
         * @param errorHandler The error handler.
         * @exception java.lang.NullPointerException If the handler
         *            argument is null.
         * @see #getErrorHandler
         */
        public void setErrorHandler(XMLErrorHandler errorHandler) {
                fProperties.put(ERROR_HANDLER, errorHandler);
        } // setErrorHandler(XMLErrorHandler)

        /**
         * Return the current error handler.
         *
         * @return The current error handler, or null if none
         *         has been registered.
         * @see #setErrorHandler
         */
        public XMLErrorHandler getErrorHandler() {
                // REVISIT: Should this be a property?
                return (XMLErrorHandler)fProperties.get(ERROR_HANDLER);
        } // getErrorHandler():XMLErrorHandler


    /**
     * If the application decides to terminate parsing before the xml document
     * is fully parsed, the application should call this method to free any
     * resource allocated during parsing. For example, close all opened streams.
     */
    public void cleanup() {
        fEntityManager.closeReaders();
    }

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
        } catch (XNIException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        } catch (IOException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        } catch (RuntimeException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        } catch (Exception ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw new XNIException(ex);
        } finally {
            fParseInProgress = false;
            // close all streams opened by xerces
            this.cleanup();
        }

    } // parse(InputSource)

    public boolean parse(boolean complete) throws XNIException, IOException {
        //
        // reset and configure pipeline and set InputSource.
        if (fInputSource != null) {
            try {
                                fValidationManager.reset();
                fVersionDetector.reset(this);
                resetCommon();

                short version = fVersionDetector.determineDocVersion(fInputSource);
                if (version == Constants.XML_VERSION_1_1) {
                    initXML11Components();
                    configureXML11Pipeline();
                    resetXML11();
                } else {
                    configurePipeline();
                    reset();
                }

                // mark configuration as fixed
                fConfigUpdated = false;

                // resets and sets the pipeline.
                fVersionDetector.startDocumentParsing((XMLEntityHandler) fCurrentScanner, version);
                fInputSource = null;
            } catch (XNIException ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw ex;
            } catch (IOException ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw ex;
            } catch (RuntimeException ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw ex;
            } catch (Exception ex) {
                if (PRINT_EXCEPTION_STACK_TRACE)
                    ex.printStackTrace();
                throw new XNIException(ex);
            }
        }

        try {
            return fCurrentScanner.scanDocument(complete);
        } catch (XNIException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        } catch (IOException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        } catch (RuntimeException ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw ex;
        } catch (Exception ex) {
            if (PRINT_EXCEPTION_STACK_TRACE)
                ex.printStackTrace();
            throw new XNIException(ex);
        }

    } // parse(boolean):boolean

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
                        // make this feature special
        if (featureId.equals(PARSER_SETTINGS)){
                return FeatureState.is(fConfigUpdated);
        }
        return super.getFeatureState(featureId);

        } // getFeature(String):boolean

        /**
         * Set the state of a feature.
         *
         * Set the state of any feature in a SAX2 parser.  The parser
         * might not recognize the feature, and if it does recognize
         * it, it might not be able to fulfill the request.
         *
         * @param featureId The unique identifier (URI) of the feature.
         * @param state The requested state of the feature (true or false).
         *
         * @exception com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException If the
         *            requested feature is not known.
         */
        public void setFeature(String featureId, boolean state)
                throws XMLConfigurationException {
                fConfigUpdated = true;
                // forward to every XML 1.0 component
                for (XMLComponent c : fComponents) {
                    c.setFeature(featureId, state);
                }
                // forward it to common components
                for (XMLComponent c : fCommonComponents) {
                    c.setFeature(featureId, state);
                }
                // forward to every XML 1.1 component
                for (XMLComponent c : fXML11Components) {
                    try {
                        c.setFeature(featureId, state);
                    }
                    catch (Exception e){
                        // no op
                    }
                }
                // save state if noone "objects"
                super.setFeature(featureId, state);

        } // setFeature(String,boolean)

        /**
         * setProperty
         *
         * @param propertyId
         * @param value
         */
        public void setProperty(String propertyId, Object value)
                throws XMLConfigurationException {
                fConfigUpdated = true;
                // forward to every XML 1.0 component
                for (XMLComponent c : fComponents) {
                    c.setProperty(propertyId, value);
                }
                // forward it to every common Component
                for (XMLComponent c : fCommonComponents) {
                    c.setProperty(propertyId, value);
                }
                // forward it to every XML 1.1 component
                for (XMLComponent c : fXML11Components) {
                    try {
                        c.setProperty(propertyId, value);
                    }
                    catch (Exception e){
                        // no op
                    }
                }
                // store value if noone "objects"
                super.setProperty(propertyId, value);

        } // setProperty(String,Object)


        /** Returns the locale. */
        public Locale getLocale() {
                return fLocale;
        } // getLocale():Locale

        /**
         * reset all XML 1.0 components before parsing and namespace context
         */
        protected void reset() throws XNIException {
            for (XMLComponent c : fComponents) {
                c.reset(this);
            }
        } // reset()

        /**
         * reset all common components before parsing
         */
        protected void resetCommon() throws XNIException {
            // reset common components
            for (XMLComponent c : fCommonComponents) {
                c.reset(this);
            }
        } // resetCommon()


        /**
         * reset all components before parsing and namespace context
         */
        protected void resetXML11() throws XNIException {
            // reset every component
            for (XMLComponent c : fXML11Components) {
                c.reset(this);
            }
        } // resetXML11()


    /**
     *  Configures the XML 1.1 pipeline.
     *  Note: this method also resets the new XML11 components.
     */
    protected void configureXML11Pipeline() {
        if (fCurrentDVFactory != fXML11DatatypeFactory) {
            fCurrentDVFactory = fXML11DatatypeFactory;
            setProperty(DATATYPE_VALIDATOR_FACTORY, fCurrentDVFactory);
        }

        // setup DTD pipeline
        if (fCurrentDTDScanner != fXML11DTDScanner) {
            fCurrentDTDScanner = fXML11DTDScanner;
            setProperty(DTD_SCANNER, fCurrentDTDScanner);
        }
        fXML11DTDScanner.setDTDHandler(fDTDHandler);
        fXML11DTDScanner.setDTDContentModelHandler(fDTDContentModelHandler);

        // setup XML 1.1 document pipeline
        if (fFeatures.get(NAMESPACES) == Boolean.TRUE) {
            if (fCurrentScanner != fXML11NSDocScanner) {
                fCurrentScanner = fXML11NSDocScanner;
                setProperty(DOCUMENT_SCANNER, fXML11NSDocScanner);
            }

            fXML11NSDocScanner.setDTDValidator(null);
            fXML11NSDocScanner.setDocumentHandler(fDocumentHandler);
            if (fDocumentHandler != null) {
                fDocumentHandler.setDocumentSource(fXML11NSDocScanner);
            }
            fLastComponent = fXML11NSDocScanner;

        } else {
                        // create components
                          if (fXML11DocScanner == null) {
                                        // non namespace document pipeline
                                        fXML11DocScanner = new XML11DocumentScannerImpl();
                                        addXML11Component(fXML11DocScanner);
                          }
            if (fCurrentScanner != fXML11DocScanner) {
                fCurrentScanner = fXML11DocScanner;
                setProperty(DOCUMENT_SCANNER, fXML11DocScanner);
            }
            fXML11DocScanner.setDocumentHandler(fDocumentHandler);

            if (fDocumentHandler != null) {
                fDocumentHandler.setDocumentSource(fXML11DocScanner);
            }
            fLastComponent = fXML11DocScanner;
        }

    } // configureXML11Pipeline()

    /** Configures the pipeline. */
    protected void configurePipeline() {
        if (fCurrentDVFactory != fDatatypeValidatorFactory) {
            fCurrentDVFactory = fDatatypeValidatorFactory;
            // use XML 1.0 datatype library
            setProperty(DATATYPE_VALIDATOR_FACTORY, fCurrentDVFactory);
        }

        // setup DTD pipeline
        if (fCurrentDTDScanner != fDTDScanner) {
            fCurrentDTDScanner = fDTDScanner;
            setProperty(DTD_SCANNER, fCurrentDTDScanner);
        }
        fDTDScanner.setDTDHandler(fDTDHandler);
        fDTDScanner.setDTDContentModelHandler(fDTDContentModelHandler);

        // setup document pipeline
        if (fFeatures.get(NAMESPACES) == Boolean.TRUE) {
            if (fCurrentScanner != fNamespaceScanner) {
                fCurrentScanner = fNamespaceScanner;
                setProperty(DOCUMENT_SCANNER, fNamespaceScanner);
            }
            fNamespaceScanner.setDTDValidator(null);
            fNamespaceScanner.setDocumentHandler(fDocumentHandler);
            if (fDocumentHandler != null) {
                fDocumentHandler.setDocumentSource(fNamespaceScanner);
            }
            fLastComponent = fNamespaceScanner;
        } else {
            // create components
            if (fNonNSScanner == null) {
                fNonNSScanner = new XMLDocumentScannerImpl();
                // add components
                addComponent((XMLComponent) fNonNSScanner);

            }
            if (fCurrentScanner != fNonNSScanner) {
                fCurrentScanner = fNonNSScanner;
                setProperty(DOCUMENT_SCANNER, fNonNSScanner);

            }

            fNonNSScanner.setDocumentHandler(fDocumentHandler);
            if (fDocumentHandler != null) {
                fDocumentHandler.setDocumentSource(fNonNSScanner);
            }
            fLastComponent = fNonNSScanner;
        }

    } // configurePipeline()


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
    protected FeatureState checkFeature(String featureId) throws XMLConfigurationException {

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

            // special performance feature: only component manager is allowed to set it.
            if (suffixLength == Constants.PARSER_SETTINGS.length() &&
                featureId.endsWith(Constants.PARSER_SETTINGS)) {
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
    protected PropertyState checkProperty(String propertyId) throws XMLConfigurationException {

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

        if (propertyId.startsWith(Constants.JAXP_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.JAXP_PROPERTY_PREFIX.length();

            if (suffixLength == Constants.SCHEMA_SOURCE.length() &&
                propertyId.endsWith(Constants.SCHEMA_SOURCE)) {
                return PropertyState.RECOGNIZED;
            }
        }

        // special cases
        if (propertyId.startsWith(Constants.SAX_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.SAX_PROPERTY_PREFIX.length();

            //
            // http://xml.org/sax/properties/xml-string
            // Value type: String
            // Access: read-only
            //   Get the literal string of characters associated with the
            //   current event.  If the parser recognises and supports this
            //   property but is not currently parsing text, it should return
            //   null (this is a good way to check for availability before the
            //   parse begins).
            //
            if (suffixLength == Constants.XML_STRING_PROPERTY.length() &&
                propertyId.endsWith(Constants.XML_STRING_PROPERTY)) {
                // REVISIT - we should probably ask xml-dev for a precise
                // definition of what this is actually supposed to return, and
                // in exactly which circumstances.
                return PropertyState.NOT_SUPPORTED;
            }
        }

        //
        // Not recognized
        //

        return super.checkProperty(propertyId);

    } // checkProperty(String)


    /**
     * Adds a component to the parser configuration. This method will
     * also add all of the component's recognized features and properties
     * to the list of default recognized features and properties.
     *
     * @param component The component to add.
     */
    protected void addComponent(XMLComponent component) {

        // don't add a component more than once
        if (fComponents.contains(component)) {
            return;
        }
        fComponents.add(component);
        addRecognizedParamsAndSetDefaults(component);

    } // addComponent(XMLComponent)

    /**
     * Adds common component to the parser configuration. This method will
     * also add all of the component's recognized features and properties
     * to the list of default recognized features and properties.
     *
     * @param component The component to add.
     */
    protected void addCommonComponent(XMLComponent component) {

        // don't add a component more than once
        if (fCommonComponents.contains(component)) {
            return;
        }
        fCommonComponents.add(component);
        addRecognizedParamsAndSetDefaults(component);

    } // addCommonComponent(XMLComponent)

    /**
     * Adds an XML 1.1 component to the parser configuration. This method will
     * also add all of the component's recognized features and properties
     * to the list of default recognized features and properties.
     *
     * @param component The component to add.
     */
    protected void addXML11Component(XMLComponent component) {

        // don't add a component more than once
        if (fXML11Components.contains(component)) {
            return;
        }
        fXML11Components.add(component);
        addRecognizedParamsAndSetDefaults(component);

    } // addXML11Component(XMLComponent)

    /**
     * Adds all of the component's recognized features and properties
     * to the list of default recognized features and properties, and
     * sets default values on the configuration for features and
     * properties which were previously absent from the configuration.
     *
     * @param component The component whose recognized features
     * and properties will be added to the configuration
     */
    protected void addRecognizedParamsAndSetDefaults(XMLComponent component) {

        // register component's recognized features
        String[] recognizedFeatures = component.getRecognizedFeatures();
        addRecognizedFeatures(recognizedFeatures);

        // register component's recognized properties
        String[] recognizedProperties = component.getRecognizedProperties();
        addRecognizedProperties(recognizedProperties);

        // set default values
        if (recognizedFeatures != null) {
            for (int i = 0; i < recognizedFeatures.length; ++i) {
                String featureId = recognizedFeatures[i];
                Boolean state = component.getFeatureDefault(featureId);
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

    private void initXML11Components() {
        if (!f11Initialized) {

            // create datatype factory
            fXML11DatatypeFactory = DTDDVFactory.getInstance(XML11_DATATYPE_VALIDATOR_FACTORY);

            // setup XML 1.1 DTD pipeline
            fXML11DTDScanner = new XML11DTDScannerImpl();
            addXML11Component(fXML11DTDScanner);

            // setup XML 1.1. document pipeline - namespace aware
            fXML11NSDocScanner = new XML11NSDocumentScannerImpl();
            addXML11Component(fXML11NSDocScanner);

            f11Initialized = true;
        }
    }

} // class XML11NonValidatingConfiguration
