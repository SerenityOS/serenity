/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.dom;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.dv.DTDDVFactory;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.util.DOMEntityResolverWrapper;
import com.sun.org.apache.xerces.internal.util.DOMErrorHandlerWrapper;
import com.sun.org.apache.xerces.internal.util.MessageFormatter;
import com.sun.org.apache.xerces.internal.util.ParserConfigurationSettings;
import com.sun.org.apache.xerces.internal.util.PropertyState;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.XMLDTDContentModelHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDTDHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParserConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.ls.LSResourceResolver;



/**
 * Xerces implementation of DOMConfiguration that maintains a table of recognized parameters.
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 * @author Neeraj Bajaj, Sun Microsystems.
 * @LastModified: May 2021
 */
public class DOMConfigurationImpl extends ParserConfigurationSettings
    implements XMLParserConfiguration, DOMConfiguration {

    //
    // Constants
    //

    protected static final String XML11_DATATYPE_VALIDATOR_FACTORY =
        "com.sun.org.apache.xerces.internal.impl.dv.dtd.XML11DTDDVFactoryImpl";

    // feature identifiers

    /** Feature identifier: validation. */
    protected static final String XERCES_VALIDATION =
        Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** Feature identifier: namespaces. */
    protected static final String XERCES_NAMESPACES =
        Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

    protected static final String SCHEMA =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

    protected static final String SCHEMA_FULL_CHECKING =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

    protected static final String DYNAMIC_VALIDATION =
        Constants.XERCES_FEATURE_PREFIX + Constants.DYNAMIC_VALIDATION_FEATURE;

    protected static final String NORMALIZE_DATA =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_NORMALIZED_VALUE;

    /** Feature identifier: send element default value via characters() */
    protected static final String SCHEMA_ELEMENT_DEFAULT =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_ELEMENT_DEFAULT;

    /** sending psvi in the pipeline */
    protected static final String SEND_PSVI =
        Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_AUGMENT_PSVI;

    /** Feature: generate synthetic annotations */
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.GENERATE_SYNTHETIC_ANNOTATIONS_FEATURE;

    /** Feature identifier: validate annotations */
    protected static final String VALIDATE_ANNOTATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.VALIDATE_ANNOTATIONS_FEATURE;

    /** Feature identifier: honour all schemaLocations */
    protected static final String HONOUR_ALL_SCHEMALOCATIONS =
        Constants.XERCES_FEATURE_PREFIX + Constants.HONOUR_ALL_SCHEMALOCATIONS_FEATURE;

    /** Feature identifier: use grammar pool only */
    protected static final String USE_GRAMMAR_POOL_ONLY =
        Constants.XERCES_FEATURE_PREFIX + Constants.USE_GRAMMAR_POOL_ONLY_FEATURE;

    /** Feature identifier: load external DTD. */
    protected static final String DISALLOW_DOCTYPE_DECL_FEATURE =
        Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

    /** Feature identifier: balance syntax trees. */
    protected static final String BALANCE_SYNTAX_TREES =
        Constants.XERCES_FEATURE_PREFIX + Constants.BALANCE_SYNTAX_TREES;

    /** Feature identifier: warn on duplicate attribute definition. */
    protected static final String WARN_ON_DUPLICATE_ATTDEF =
        Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ATTDEF_FEATURE;

    /** Feature identifier: namespace growth */
    protected static final String NAMESPACE_GROWTH =
        Constants.XERCES_FEATURE_PREFIX + Constants.NAMESPACE_GROWTH_FEATURE;

    protected static final String TOLERATE_DUPLICATES =
        Constants.XERCES_FEATURE_PREFIX + Constants.TOLERATE_DUPLICATES_FEATURE;

    // property identifiers

    /** Property identifier: entity manager. */
    protected static final String ENTITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_MANAGER_PROPERTY;

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: xml string. */
    protected static final String XML_STRING =
        Constants.SAX_PROPERTY_PREFIX + Constants.XML_STRING_PROPERTY;

    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property id: Grammar pool. */
    protected static final String GRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Property identifier: error handler. */
    protected static final String ERROR_HANDLER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;

    /** Property identifier: entity resolver. */
    protected static final String ENTITY_RESOLVER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Property identifier: JAXP schema language / DOM schema-type. */
    protected static final String JAXP_SCHEMA_LANGUAGE =
        Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE;

    /** Property identifier: JAXP schema source/ DOM schema-location. */
    protected static final String JAXP_SCHEMA_SOURCE =
        Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE;

    /** Property identifier: DTD validator. */
    protected final static String DTD_VALIDATOR_PROPERTY =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_VALIDATOR_PROPERTY;

    /** Property identifier: datatype validator factory. */
    protected static final String DTD_VALIDATOR_FACTORY_PROPERTY =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DATATYPE_VALIDATOR_FACTORY_PROPERTY;

    protected static final String VALIDATION_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    /** Property identifier: schema location. */
    protected static final String SCHEMA_LOCATION =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_LOCATION;

    /** Property identifier: no namespace schema location. */
    protected static final String SCHEMA_NONS_LOCATION =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_NONS_LOCATION;

    /** Property identifier: Schema DV Factory */
    protected static final String SCHEMA_DV_FACTORY =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_DV_FACTORY_PROPERTY;

    /** Property identifier: Security manager. */
    private static final String SECURITY_MANAGER = Constants.SECURITY_MANAGER;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    //
    // Data
    //
    XMLDocumentHandler fDocumentHandler;

    /** Normalization features*/
    protected short features = 0;

    protected final static short NAMESPACES          = 0x1<<0;
    protected final static short DTNORMALIZATION     = 0x1<<1;
    protected final static short ENTITIES            = 0x1<<2;
    protected final static short CDATA               = 0x1<<3;
    protected final static short SPLITCDATA          = 0x1<<4;
    protected final static short COMMENTS            = 0x1<<5;
    protected final static short VALIDATE            = 0x1<<6;
    protected final static short PSVI                = 0x1<<7;
    protected final static short WELLFORMED          = 0x1<<8;
    protected final static short NSDECL              = 0x1<<9;

    protected final static short INFOSET_TRUE_PARAMS = NAMESPACES | COMMENTS | WELLFORMED | NSDECL;
    protected final static short INFOSET_FALSE_PARAMS = ENTITIES | DTNORMALIZATION | CDATA;
    protected final static short INFOSET_MASK = INFOSET_TRUE_PARAMS | INFOSET_FALSE_PARAMS;

    // components

    /** Symbol table. */
    protected SymbolTable fSymbolTable;

    /** Components. */
    protected List<XMLComponent> fComponents;

    protected ValidationManager fValidationManager;

    /** Locale. */
    protected Locale fLocale;

    /** Error reporter */
    protected XMLErrorReporter fErrorReporter;

    protected final DOMErrorHandlerWrapper fErrorHandlerWrapper =
                new DOMErrorHandlerWrapper();

    /** Current Datatype validator factory. */
    protected DTDDVFactory fCurrentDVFactory;

    /** The XML 1.0 Datatype validator factory. */
    protected DTDDVFactory fDatatypeValidatorFactory;

    /** The XML 1.1 Datatype validator factory. **/
    protected DTDDVFactory fXML11DatatypeFactory;

    // private data

    private String fSchemaLocation = null;
    private DOMStringList fRecognizedParameters;


    //
    // Constructors
    //

    /** Default Constructor. */
    protected DOMConfigurationImpl() {
        this(null, null);
    } // <init>()

    /**
     * Constructs a parser configuration using the specified symbol table.
     *
     * @param symbolTable The symbol table to use.
     */
    protected DOMConfigurationImpl(SymbolTable symbolTable) {
        this(symbolTable, null);
    } // <init>(SymbolTable)

    /**
     * Constructs a parser configuration using the specified symbol table
     * and parent settings.
     *
     * @param symbolTable    The symbol table to use.
     * @param parentSettings The parent settings.
     */
    protected DOMConfigurationImpl(SymbolTable symbolTable,
                                    XMLComponentManager parentSettings) {
        super(parentSettings);


        // create table for features and properties
        fFeatures = new HashMap<>();
        fProperties = new HashMap<>();

        // add default recognized features
        final String[] recognizedFeatures = {
            XERCES_VALIDATION,
            XERCES_NAMESPACES,
            SCHEMA,
            SCHEMA_FULL_CHECKING,
            DYNAMIC_VALIDATION,
            NORMALIZE_DATA,
            SCHEMA_ELEMENT_DEFAULT,
            SEND_PSVI,
            GENERATE_SYNTHETIC_ANNOTATIONS,
            VALIDATE_ANNOTATIONS,
            HONOUR_ALL_SCHEMALOCATIONS,
            USE_GRAMMAR_POOL_ONLY,
            DISALLOW_DOCTYPE_DECL_FEATURE,
            BALANCE_SYNTAX_TREES,
            WARN_ON_DUPLICATE_ATTDEF,
            PARSER_SETTINGS,
            NAMESPACE_GROWTH,
            TOLERATE_DUPLICATES,
            XMLConstants.USE_CATALOG,
            JdkConstants.OVERRIDE_PARSER
        };
        addRecognizedFeatures(recognizedFeatures);

        // set state for default features
        setFeature(XERCES_VALIDATION, false);
        setFeature(SCHEMA, false);
        setFeature(SCHEMA_FULL_CHECKING, false);
        setFeature(DYNAMIC_VALIDATION, false);
        setFeature(NORMALIZE_DATA, false);
        setFeature(SCHEMA_ELEMENT_DEFAULT, false);
        setFeature(XERCES_NAMESPACES, true);
        setFeature(SEND_PSVI, true);
        setFeature(GENERATE_SYNTHETIC_ANNOTATIONS, false);
        setFeature(VALIDATE_ANNOTATIONS, false);
        setFeature(HONOUR_ALL_SCHEMALOCATIONS, false);
        setFeature(USE_GRAMMAR_POOL_ONLY, false);
        setFeature(DISALLOW_DOCTYPE_DECL_FEATURE, false);
        setFeature(BALANCE_SYNTAX_TREES, false);
        setFeature(WARN_ON_DUPLICATE_ATTDEF, false);
        setFeature(PARSER_SETTINGS, true);
        setFeature(NAMESPACE_GROWTH, false);
        setFeature(TOLERATE_DUPLICATES, false);
        setFeature(XMLConstants.USE_CATALOG, JdkXmlUtils.USE_CATALOG_DEFAULT);
        setFeature(JdkConstants.OVERRIDE_PARSER, JdkConstants.OVERRIDE_PARSER_DEFAULT);

        // add default recognized properties
        final String[] recognizedProperties = {
            XML_STRING,
            SYMBOL_TABLE,
            ERROR_HANDLER,
            ENTITY_RESOLVER,
            ERROR_REPORTER,
            ENTITY_MANAGER,
            VALIDATION_MANAGER,
            GRAMMAR_POOL,
            JAXP_SCHEMA_SOURCE,
            JAXP_SCHEMA_LANGUAGE,
            SCHEMA_LOCATION,
            SCHEMA_NONS_LOCATION,
            DTD_VALIDATOR_PROPERTY,
            DTD_VALIDATOR_FACTORY_PROPERTY,
            SCHEMA_DV_FACTORY,
            SECURITY_MANAGER,
            XML_SECURITY_PROPERTY_MANAGER,
            JdkXmlUtils.CATALOG_DEFER,
            JdkXmlUtils.CATALOG_FILES,
            JdkXmlUtils.CATALOG_PREFER,
            JdkXmlUtils.CATALOG_RESOLVE,
            JdkConstants.CDATA_CHUNK_SIZE
        };
        addRecognizedProperties(recognizedProperties);

        // set default values for normalization features
        features |= NAMESPACES;
        features |= ENTITIES;
        features |= COMMENTS;
        features |= CDATA;
        features |= SPLITCDATA;
        features |= WELLFORMED;
        features |= NSDECL;

        if (symbolTable == null) {
            symbolTable = new SymbolTable();
        }
        fSymbolTable = symbolTable;

        fComponents = new ArrayList<>();

        setProperty(SYMBOL_TABLE, fSymbolTable);
        fErrorReporter = new XMLErrorReporter();
        setProperty(ERROR_REPORTER, fErrorReporter);
        addComponent(fErrorReporter);

        fDatatypeValidatorFactory = DTDDVFactory.getInstance();
        fXML11DatatypeFactory = DTDDVFactory.getInstance(XML11_DATATYPE_VALIDATOR_FACTORY);
        fCurrentDVFactory = fDatatypeValidatorFactory;
        setProperty(DTD_VALIDATOR_FACTORY_PROPERTY, fCurrentDVFactory);

        XMLEntityManager manager =  new XMLEntityManager();
        setProperty(ENTITY_MANAGER, manager);
        addComponent(manager);

        fValidationManager = createValidationManager();
        setProperty(VALIDATION_MANAGER, fValidationManager);

        setProperty(SECURITY_MANAGER, new XMLSecurityManager(true));

        setProperty(JdkConstants.XML_SECURITY_PROPERTY_MANAGER,
                new XMLSecurityPropertyManager());

        // add message formatters
        if (fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN) == null) {
            XMLMessageFormatter xmft = new XMLMessageFormatter();
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, xmft);
            fErrorReporter.putMessageFormatter(XMLMessageFormatter.XMLNS_DOMAIN, xmft);
        }

        // REVISIT: try to include XML Schema formatter.
        //          This is a hack to allow DTD configuration to be build.
        //
        if (fErrorReporter.getMessageFormatter("http://www.w3.org/TR/xml-schema-1") == null) {
            MessageFormatter xmft = null;
            try {
               xmft = new com.sun.org.apache.xerces.internal.impl.xs.XSMessageFormatter();
            } catch (Exception exception){
            }

             if (xmft !=  null) {
                 fErrorReporter.putMessageFormatter("http://www.w3.org/TR/xml-schema-1", xmft);
             }
        }


        // set locale
        try {
            setLocale(Locale.getDefault());
        }
        catch (XNIException e) {
            // do nothing
            // REVISIT: What is the right thing to do? -Ac
        }

        // Initialize Catalog features
        for( CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
            setProperty(f.getPropertyName(), null);
        }

        setProperty(JdkConstants.CDATA_CHUNK_SIZE, JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);
    } // <init>(SymbolTable)


    //
    // XMLParserConfiguration methods
    //

    /**
     * Parse an XML document.
     * <p>
     * The parser can use this method to instruct this configuration
     * to begin parsing an XML document from any valid input source
     * (a character stream, a byte stream, or a URI).
     * <p>
     * Parsers may not invoke this method while a parse is in progress.
     * Once a parse is complete, the parser may then parse another XML
     * document.
     * <p>
     * This method is synchronous: it will not return until parsing
     * has ended.  If a client application wants to terminate
     * parsing early, it should throw an exception.
     *
     * @param inputSource The input source for the top-level of the
     *                    XML document.
     *
     * @exception XNIException Any XNI exception, possibly wrapping
     *                         another exception.
     * @exception IOException  An IO exception from the parser, possibly
     *                         from a byte stream or character stream
     *                         supplied by the parser.
     */
    public void parse(XMLInputSource inputSource)
        throws XNIException, IOException{
        // no-op
    }

    /**
     * Sets the document handler on the last component in the pipeline
     * to receive information about the document.
     *
     * @param documentHandler   The document handler.
     */
    public void setDocumentHandler(XMLDocumentHandler documentHandler) {
        fDocumentHandler = documentHandler;
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
        //no-op
    } // setDTDHandler(XMLDTDHandler)

    /** Returns the registered DTD handler. */
    public XMLDTDHandler getDTDHandler() {
        return null;
    } // getDTDHandler():XMLDTDHandler

    /**
     * Sets the DTD content model handler.
     *
     * @param handler The DTD content model handler.
     */
    public void setDTDContentModelHandler(XMLDTDContentModelHandler handler) {
        //no-op

    } // setDTDContentModelHandler(XMLDTDContentModelHandler)

    /** Returns the registered DTD content model handler. */
    public XMLDTDContentModelHandler getDTDContentModelHandler() {
        return null;
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
        if (errorHandler != null) {
            fProperties.put(ERROR_HANDLER, errorHandler);
        }
    } // setErrorHandler(XMLErrorHandler)

    /**
     * Return the current error handler.
     *
     * @return The current error handler, or null if none
     *         has been registered.
     * @see #setErrorHandler
     */
    public XMLErrorHandler getErrorHandler() {
        return (XMLErrorHandler)fProperties.get(ERROR_HANDLER);
    } // getErrorHandler():XMLErrorHandler

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
    public boolean getFeature(String featureId)
        throws XMLConfigurationException {
        if (featureId.equals(PARSER_SETTINGS)) {
            return true;
        }
        return super.getFeature(featureId);
    }

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

        // store value if noone "objects"
        super.setProperty(propertyId, value);

    } // setProperty(String,Object)

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

    /** Returns the locale. */
    public Locale getLocale() {
        return fLocale;
    } // getLocale():Locale

    /**
     * DOM Level 3 WD - Experimental.
     * setParameter
     */
    public void setParameter(String name, Object value) throws DOMException {
        boolean found = true;

        // REVISIT: Recognizes DOM L3 default features only.
        //          Does not yet recognize Xerces features.
        if(value instanceof Boolean){
            boolean state = ((Boolean)value).booleanValue();

            if (name.equalsIgnoreCase(Constants.DOM_COMMENTS)) {
                features = (short) (state ? features | COMMENTS : features & ~COMMENTS);
            }
            else if (name.equalsIgnoreCase(Constants.DOM_DATATYPE_NORMALIZATION)) {
                setFeature(NORMALIZE_DATA, state);
                features =
                    (short) (state ? features | DTNORMALIZATION : features & ~DTNORMALIZATION);
                if (state) {
                    features = (short) (features | VALIDATE);
                }
            }
            else if (name.equalsIgnoreCase(Constants.DOM_NAMESPACES)) {
                features = (short) (state ? features | NAMESPACES : features & ~NAMESPACES);
            }
            else if (name.equalsIgnoreCase(Constants.DOM_CDATA_SECTIONS)) {
                features = (short) (state ? features | CDATA : features & ~CDATA);
            }
            else if (name.equalsIgnoreCase(Constants.DOM_ENTITIES)) {
                features = (short) (state ? features | ENTITIES : features & ~ENTITIES);
            }
            else if (name.equalsIgnoreCase(Constants.DOM_SPLIT_CDATA)) {
                features = (short) (state ? features | SPLITCDATA : features & ~SPLITCDATA);
            }
            else if (name.equalsIgnoreCase(Constants.DOM_VALIDATE)) {
                features = (short) (state ? features | VALIDATE : features & ~VALIDATE);
            }
            else if (name.equalsIgnoreCase(Constants.DOM_WELLFORMED)) {
                features = (short) (state ? features | WELLFORMED : features & ~WELLFORMED );
            }
            else if (name.equalsIgnoreCase(Constants.DOM_NAMESPACE_DECLARATIONS)) {
                features = (short) (state ? features | NSDECL : features & ~NSDECL);
            }
            else if (name.equalsIgnoreCase(Constants.DOM_INFOSET)) {
                // Setting to false has no effect.
                if (state) {
                    features = (short) (features | INFOSET_TRUE_PARAMS);
                    features = (short) (features & ~INFOSET_FALSE_PARAMS);
                    setFeature(NORMALIZE_DATA, false);
                }
            }
            else if (name.equalsIgnoreCase(Constants.DOM_NORMALIZE_CHARACTERS)
                    || name.equalsIgnoreCase(Constants.DOM_CANONICAL_FORM)
                    || name.equalsIgnoreCase(Constants.DOM_VALIDATE_IF_SCHEMA)
                    || name.equalsIgnoreCase(Constants.DOM_CHECK_CHAR_NORMALIZATION)
                    ) {
                if (state) { // true is not supported
                    throw newFeatureNotSupportedError(name);
                }
            }
                        else if ( name.equalsIgnoreCase(Constants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
                if (!state) { // false is not supported
                    throw newFeatureNotSupportedError(name);
                }
            }
            else if (name.equalsIgnoreCase(SEND_PSVI) ){
                // REVISIT: turning augmentation of PSVI is not support,
                // because in this case we won't be able to retrieve element
                // default value.
                if (!state) { // false is not supported
                    throw newFeatureNotSupportedError(name);
                }
            }
            else if (name.equalsIgnoreCase(Constants.DOM_PSVI)){
                  features = (short) (state ? features | PSVI : features & ~PSVI);
            }
            else {
                found = false;
                /*
                String msg =
                    DOMMessageFormatter.formatMessage(
                        DOMMessageFormatter.DOM_DOMAIN,
                        "FEATURE_NOT_FOUND",
                        new Object[] { name });
                throw new DOMException(DOMException.NOT_FOUND_ERR, msg);
                */
            }

        }

        if (!found || !(value instanceof Boolean))  { // set properties
                found = true;

            if (name.equalsIgnoreCase(Constants.DOM_ERROR_HANDLER)) {
                if (value instanceof DOMErrorHandler || value == null) {
                    fErrorHandlerWrapper.setErrorHandler((DOMErrorHandler)value);
                    setErrorHandler(fErrorHandlerWrapper);
                }
                else {
                    throw newTypeMismatchError(name);
                }
            }
            else if (name.equalsIgnoreCase(Constants.DOM_RESOURCE_RESOLVER)) {
                if (value instanceof LSResourceResolver || value == null) {
                    try {
                        setEntityResolver(new DOMEntityResolverWrapper((LSResourceResolver) value));
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    throw newTypeMismatchError(name);
                }
            }
            else if (name.equalsIgnoreCase(Constants.DOM_SCHEMA_LOCATION)) {
                if (value instanceof String || value == null) {
                    try {
                        if (value == null) {
                            fSchemaLocation = null;
                            setProperty (
                                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE,
                                null);
                        }
                        else {
                            fSchemaLocation = (String) value;
                            // map DOM schema-location to JAXP schemaSource property
                            // tokenize location string
                            StringTokenizer t = new StringTokenizer(fSchemaLocation, " \n\t\r");
                            if (t.hasMoreTokens()) {
                                List<String> locations = new ArrayList<>();
                                locations.add(t.nextToken());
                                while (t.hasMoreTokens()) {
                                    locations.add (t.nextToken());
                                }
                                setProperty (
                                    Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE,
                                    locations.toArray(new String[locations.size()]));
                            }
                            else {
                                setProperty (
                                    Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE,
                                    new String [] {(String) value});
                            }
                        }
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    throw newTypeMismatchError(name);
                }
            }
            else if (name.equalsIgnoreCase(Constants.DOM_SCHEMA_TYPE)) {
                if (value instanceof String || value == null) {
                    try {
                        if (value == null) {
                            setProperty(
                                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE,
                                null);
                        }
                        else if (value.equals(Constants.NS_XMLSCHEMA)) {
                            // REVISIT: when add support to DTD validation
                            setProperty(
                                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE,
                                Constants.NS_XMLSCHEMA);
                        }
                        else if (value.equals(Constants.NS_DTD)) {
                            // Added support for revalidation against DTDs
                                setProperty(Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE,
                                                Constants.NS_DTD);
                        }
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    throw newTypeMismatchError(name);
                }
            }
            else if (name.equalsIgnoreCase(ENTITY_RESOLVER)) {
                if (value instanceof XMLEntityResolver || value == null) {
                    try {
                        setEntityResolver((XMLEntityResolver) value);
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    throw newTypeMismatchError(name);
                }
            }
            else if (name.equalsIgnoreCase(SYMBOL_TABLE)) {
                // Xerces Symbol Table
                if (value instanceof SymbolTable){
                    setProperty(SYMBOL_TABLE, value);
                }
                else {
                    throw newTypeMismatchError(name);
                }
            }
            else if (name.equalsIgnoreCase (GRAMMAR_POOL)) {
                if (value instanceof XMLGrammarPool || value == null) {
                    setProperty(GRAMMAR_POOL, value);
                }
                else {
                    throw newTypeMismatchError(name);
                }
            }
                else {
                // REVISIT: check if this is a boolean parameter -- type mismatch should be thrown.
                //parameter is not recognized
                throw newFeatureNotFoundError(name);
            }
        }
    }


    /**
     * DOM Level 3 WD - Experimental.
     * getParameter
     */
        public Object getParameter(String name) throws DOMException {

                // REVISIT: Recognizes DOM L3 default features only.
                //          Does not yet recognize Xerces features.

                if (name.equalsIgnoreCase(Constants.DOM_COMMENTS)) {
                        return ((features & COMMENTS) != 0) ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_NAMESPACES)) {
                        return (features & NAMESPACES) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_DATATYPE_NORMALIZATION)) {
                        // REVISIT: datatype-normalization only takes effect if validation is on
                        return (features & DTNORMALIZATION) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_CDATA_SECTIONS)) {
                        return (features & CDATA) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_ENTITIES)) {
                        return (features & ENTITIES) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_SPLIT_CDATA)) {
                        return (features & SPLITCDATA) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_VALIDATE)) {
                        return (features & VALIDATE) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_WELLFORMED)) {
                        return (features & WELLFORMED) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_NAMESPACE_DECLARATIONS)) {
                    return (features & NSDECL) != 0 ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_INFOSET)) {
                        return (features & INFOSET_MASK) == INFOSET_TRUE_PARAMS ? Boolean.TRUE : Boolean.FALSE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_NORMALIZE_CHARACTERS)
                                || name.equalsIgnoreCase(Constants.DOM_CANONICAL_FORM)
                                || name.equalsIgnoreCase(Constants.DOM_VALIDATE_IF_SCHEMA)
                                || name.equalsIgnoreCase(Constants.DOM_CHECK_CHAR_NORMALIZATION)
                ) {
                        return Boolean.FALSE;
                }
        else if (name.equalsIgnoreCase(SEND_PSVI)) {
            return Boolean.TRUE;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_PSVI)) {
            return (features & PSVI) != 0 ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
                        return Boolean.TRUE;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_ERROR_HANDLER)) {
            return fErrorHandlerWrapper.getErrorHandler();
                }
                else if (name.equalsIgnoreCase(Constants.DOM_RESOURCE_RESOLVER)) {
                        XMLEntityResolver entityResolver = getEntityResolver();
                        if (entityResolver != null && entityResolver instanceof DOMEntityResolverWrapper) {
                                return ((DOMEntityResolverWrapper) entityResolver).getEntityResolver();
                        }
                        return null;
                }
                else if (name.equalsIgnoreCase(Constants.DOM_SCHEMA_TYPE)) {
                        return getProperty(Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE);
                }
                else if (name.equalsIgnoreCase(Constants.DOM_SCHEMA_LOCATION)) {
            return fSchemaLocation;
                }
        else if (name.equalsIgnoreCase(ENTITY_RESOLVER)) {
            return getEntityResolver();
        }
        else if (name.equalsIgnoreCase(SYMBOL_TABLE)) {
            return getProperty(SYMBOL_TABLE);
        }
        else if (name.equalsIgnoreCase(GRAMMAR_POOL)) {
            return getProperty(GRAMMAR_POOL);
        }
        else if (name.equalsIgnoreCase(SECURITY_MANAGER)) {
            return getProperty(SECURITY_MANAGER);
        }
                else {
                    throw newFeatureNotFoundError(name);
                }
        }

    /**
     * DOM Level 3 WD - Experimental.
     * Check if setting a parameter to a specific value is supported.
     *
     * @param name The name of the parameter to check.
     *
     * @param value An object. if null, the returned value is true.
     *
     * @return true if the parameter could be successfully set to the
     * specified value, or false if the parameter is not recognized or
     * the requested value is not supported. This does not change the
     * current value of the parameter itself.
     */
        public boolean canSetParameter(String name, Object value) {

        if (value == null){
            //if null, the returned value is true.
            //REVISIT: I dont like this --- even for unrecognized parameter it would
            //return 'true'. I think it should return false in that case.
            // Application will be surprised to find that setParameter throws not
            //recognized exception when canSetParameter returns 'true' Then what is the use
            //of having canSetParameter ??? - nb.
            return true ;
        }
        if( value instanceof Boolean ){
            //features whose parameter value can be set either 'true' or 'false'
            // or they accept any boolean value -- so we just need to check that
            // its a boolean value..
            if (name.equalsIgnoreCase(Constants.DOM_COMMENTS)
                || name.equalsIgnoreCase(Constants.DOM_DATATYPE_NORMALIZATION)
                || name.equalsIgnoreCase(Constants.DOM_CDATA_SECTIONS)
                || name.equalsIgnoreCase(Constants.DOM_ENTITIES)
                || name.equalsIgnoreCase(Constants.DOM_SPLIT_CDATA)
                || name.equalsIgnoreCase(Constants.DOM_NAMESPACES)
                || name.equalsIgnoreCase(Constants.DOM_VALIDATE)
                || name.equalsIgnoreCase(Constants.DOM_WELLFORMED)
                || name.equalsIgnoreCase(Constants.DOM_INFOSET)
                || name.equalsIgnoreCase(Constants.DOM_NAMESPACE_DECLARATIONS)
                ) {
                return true;
            }//features whose parameter value can not be set to 'true'
            else if (
                name.equalsIgnoreCase(Constants.DOM_NORMALIZE_CHARACTERS)
                    || name.equalsIgnoreCase(Constants.DOM_CANONICAL_FORM)
                    || name.equalsIgnoreCase(Constants.DOM_VALIDATE_IF_SCHEMA)
                    || name.equalsIgnoreCase(Constants.DOM_CHECK_CHAR_NORMALIZATION)
                    ) {
                    return (value.equals(Boolean.TRUE)) ? false : true;
            }//features whose parameter value can not be set to 'false'
            else if( name.equalsIgnoreCase(Constants.DOM_ELEMENT_CONTENT_WHITESPACE)
                    || name.equalsIgnoreCase(SEND_PSVI)
                    ) {
                    return (value.equals(Boolean.TRUE)) ? true : false;
            }// if name is not among the above listed above -- its not recognized. return false
            else {
                return false ;
            }
        }
                else if (name.equalsIgnoreCase(Constants.DOM_ERROR_HANDLER)) {
            return (value instanceof DOMErrorHandler) ? true : false ;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_RESOURCE_RESOLVER)) {
            return (value instanceof LSResourceResolver) ? true : false ;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_SCHEMA_LOCATION)) {
            return (value instanceof String) ? true : false ;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_SCHEMA_TYPE)) {
            // REVISIT: should null value be supported?
            // as of now we are only supporting W3C XML Schema and DTD.
            return ((value instanceof String) &&
                    (value.equals(Constants.NS_XMLSCHEMA) || value.equals(Constants.NS_DTD))) ? true : false;
        }
        else if (name.equalsIgnoreCase(ENTITY_RESOLVER)) {
            return (value instanceof XMLEntityResolver) ? true : false;
        }
        else if (name.equalsIgnoreCase(SYMBOL_TABLE)) {
            // Xerces Symbol Table
            return (value instanceof SymbolTable) ? true : false;
        }
        else if (name.equalsIgnoreCase (GRAMMAR_POOL)) {
            return (value instanceof XMLGrammarPool) ? true : false;
        }
        else {
            //false if the parameter is not recognized or the requested value is not supported.
            return false ;
        }

        } //canSetParameter

    /**
     *  DOM Level 3 CR - Experimental.
     *
     *  The list of the parameters supported by this
     * <code>DOMConfiguration</code> object and for which at least one value
     * can be set by the application. Note that this list can also contain
     * parameter names defined outside this specification.
     */
        public DOMStringList getParameterNames() {
            if (fRecognizedParameters == null){
            List<String> parameters = new ArrayList<>();

                //Add DOM recognized parameters
                //REVISIT: Would have been nice to have a list of
                //recognized paramters.
                parameters.add(Constants.DOM_COMMENTS);
                parameters.add(Constants.DOM_DATATYPE_NORMALIZATION);
                parameters.add(Constants.DOM_CDATA_SECTIONS);
                parameters.add(Constants.DOM_ENTITIES);
                parameters.add(Constants.DOM_SPLIT_CDATA);
                parameters.add(Constants.DOM_NAMESPACES);
                parameters.add(Constants.DOM_VALIDATE);

                parameters.add(Constants.DOM_INFOSET);
                parameters.add(Constants.DOM_NORMALIZE_CHARACTERS);
                parameters.add(Constants.DOM_CANONICAL_FORM);
                parameters.add(Constants.DOM_VALIDATE_IF_SCHEMA);
                parameters.add(Constants.DOM_CHECK_CHAR_NORMALIZATION);
                parameters.add(Constants.DOM_WELLFORMED);

                parameters.add(Constants.DOM_NAMESPACE_DECLARATIONS);
                parameters.add(Constants.DOM_ELEMENT_CONTENT_WHITESPACE);

                parameters.add(Constants.DOM_ERROR_HANDLER);
                parameters.add(Constants.DOM_SCHEMA_TYPE);
                parameters.add(Constants.DOM_SCHEMA_LOCATION);
                parameters.add(Constants.DOM_RESOURCE_RESOLVER);

                //Add recognized xerces features and properties
                parameters.add(ENTITY_RESOLVER);
                parameters.add(GRAMMAR_POOL);
                parameters.add(SECURITY_MANAGER);
                parameters.add(SYMBOL_TABLE);
                parameters.add(SEND_PSVI);

                fRecognizedParameters = new DOMStringListImpl(parameters);

            }

            return fRecognizedParameters;
        }//getParameterNames

    //
    // Protected methods
    //

    /**
     * reset all components before parsing
     */
    protected void reset() throws XNIException {

        if (fValidationManager != null)
            fValidationManager.reset();

        int count = fComponents.size();
        for (int i = 0; i < count; i++) {
            XMLComponent c = fComponents.get(i);
            c.reset(this);
        }

    } // reset()

    /**
     * Check a property. If the property is known and supported, this method
     * simply returns. Otherwise, the appropriate exception is thrown.
     *
     * @param propertyId The unique identifier (URI) of the property
     *                   being set.
     * @exception com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException If the
     *            requested feature is not known or supported.
     */
    @Override
    protected PropertyState checkProperty(String propertyId)
        throws XMLConfigurationException {

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

        // check property
        return super.checkProperty(propertyId);

    } // checkProperty(String)


    protected void addComponent(XMLComponent component) {

        // don't add a component more than once
        if (fComponents.contains(component)) {
            return;
        }
        fComponents.add(component);

        // register component's recognized features
        String[] recognizedFeatures = component.getRecognizedFeatures();
        addRecognizedFeatures(recognizedFeatures);

        // register component's recognized properties
        String[] recognizedProperties = component.getRecognizedProperties();
        addRecognizedProperties(recognizedProperties);

    } // addComponent(XMLComponent)

    protected ValidationManager createValidationManager(){
        return new ValidationManager();
    }

    protected final void setDTDValidatorFactory(String version) {
        if ("1.1".equals(version)) {
            if (fCurrentDVFactory != fXML11DatatypeFactory) {
                fCurrentDVFactory = fXML11DatatypeFactory;
                setProperty(DTD_VALIDATOR_FACTORY_PROPERTY, fCurrentDVFactory);
            }
        }
        else if (fCurrentDVFactory != fDatatypeValidatorFactory) {
            fCurrentDVFactory = fDatatypeValidatorFactory;
            setProperty(DTD_VALIDATOR_FACTORY_PROPERTY, fCurrentDVFactory);
        }
    }

    private static DOMException newFeatureNotSupportedError(String name) {
        String msg =
            DOMMessageFormatter.formatMessage(
                DOMMessageFormatter.DOM_DOMAIN,
                "FEATURE_NOT_SUPPORTED",
                new Object[] { name });
        return new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
    }

    private static DOMException newFeatureNotFoundError(String name) {
        String msg =
            DOMMessageFormatter.formatMessage(
                DOMMessageFormatter.DOM_DOMAIN,
                "FEATURE_NOT_FOUND",
                new Object[] { name });
        return new DOMException(DOMException.NOT_FOUND_ERR, msg);
    }

    private static DOMException newTypeMismatchError(String name) {
        String msg =
            DOMMessageFormatter.formatMessage(
                DOMMessageFormatter.DOM_DOMAIN,
                "TYPE_MISMATCH_ERR",
                new Object[] { name });
        return new DOMException(DOMException.TYPE_MISMATCH_ERR, msg);
    }

} // class DOMConfigurationImpl
