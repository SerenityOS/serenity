/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.dtd;

import java.util.Iterator;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.RevalidationHandler;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.dtd.models.ContentModelValidator;
import com.sun.org.apache.xerces.internal.impl.dv.DTDDVFactory;
import com.sun.org.apache.xerces.internal.impl.dv.DatatypeValidator;
import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationState;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentFilter;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;

/**
 * The DTD validator. The validator implements a document
 * filter: receiving document events from the scanner; validating
 * the content and structure; augmenting the InfoSet, if applicable;
 * and notifying the parser of the information resulting from the
 * validation process.
 * <p> Formerly, this component also handled DTD events and grammar construction.
 * To facilitate the development of a meaningful DTD grammar caching/preparsing
 * framework, this functionality has been moved into the XMLDTDLoader
 * class.  Therefore, this class no longer implements the DTDFilter
 * or DTDContentModelFilter interfaces.
 * <p>
 * This component requires the following features and properties from the
 * component manager that uses it:
 * <ul>
 *  <li>http://xml.org/sax/features/namespaces</li>
 *  <li>http://xml.org/sax/features/validation</li>
 *  <li>http://apache.org/xml/features/validation/dynamic</li>
 *  <li>http://apache.org/xml/properties/internal/symbol-table</li>
 *  <li>http://apache.org/xml/properties/internal/error-reporter</li>
 *  <li>http://apache.org/xml/properties/internal/grammar-pool</li>
 *  <li>http://apache.org/xml/properties/internal/datatype-validator-factory</li>
 * </ul>
 *
 * @xerces.internal
 *
 * @author Eric Ye, IBM
 * @author Andy Clark, IBM
 * @author Jeffrey Rodriguez IBM
 * @author Neil Graham, IBM
 *
 * @LastModified: May 2018
 */
public class XMLDTDValidator
        implements XMLComponent, XMLDocumentFilter, XMLDTDValidatorFilter, RevalidationHandler {

    //
    // Constants
    //

    // feature identifiers

    /** Feature identifier: namespaces. */
    protected static final String NAMESPACES =
        Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

    /** Feature identifier: validation. */
    protected static final String VALIDATION =
        Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /** Feature identifier: dynamic validation. */
    protected static final String DYNAMIC_VALIDATION =
        Constants.XERCES_FEATURE_PREFIX + Constants.DYNAMIC_VALIDATION_FEATURE;

    /** Feature identifier: balance syntax trees. */
    protected static final String BALANCE_SYNTAX_TREES =
        Constants.XERCES_FEATURE_PREFIX + Constants.BALANCE_SYNTAX_TREES;

    /** Feature identifier: warn on duplicate attdef */
    protected static final String WARN_ON_DUPLICATE_ATTDEF =
        Constants.XERCES_FEATURE_PREFIX + Constants.WARN_ON_DUPLICATE_ATTDEF_FEATURE;

    protected static final String PARSER_SETTINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS;


    // property identifiers

    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: grammar pool. */
    protected static final String GRAMMAR_POOL =
        Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Property identifier: datatype validator factory. */
    protected static final String DATATYPE_VALIDATOR_FACTORY =
        Constants.XERCES_PROPERTY_PREFIX + Constants.DATATYPE_VALIDATOR_FACTORY_PROPERTY;

    // property identifier:  ValidationManager
    protected static final String VALIDATION_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    // recognized features and properties

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        NAMESPACES,
        VALIDATION,
        DYNAMIC_VALIDATION,
        BALANCE_SYNTAX_TREES
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        SYMBOL_TABLE,
        ERROR_REPORTER,
        GRAMMAR_POOL,
        DATATYPE_VALIDATOR_FACTORY,
        VALIDATION_MANAGER
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
        null,
        null,
        null,
        null,
        null,
    };

    // debugging

    /** Compile to true to debug attributes. */
    private static final boolean DEBUG_ATTRIBUTES = false;

    /** Compile to true to debug element children. */
    private static final boolean DEBUG_ELEMENT_CHILDREN = false;

    //
    // Data
    //

    // updated during reset
    protected ValidationManager fValidationManager = null;

    // validation state
    protected final ValidationState fValidationState = new ValidationState();

    // features

    /** Namespaces. */
    protected boolean fNamespaces;

    /** Validation. */
    protected boolean fValidation;

    /** Validation against only DTD */
    protected boolean fDTDValidation;

    /**
     * Dynamic validation. This state of this feature is only useful when
     * the validation feature is set to <code>true</code>.
     */
    protected boolean fDynamicValidation;

    /** Controls whether the DTD grammar produces balanced syntax trees. */
    protected boolean fBalanceSyntaxTrees;

    /** warn on duplicate attribute definition, this feature works only when validation is true */
    protected boolean fWarnDuplicateAttdef;

    // properties

    /** Symbol table. */
    protected SymbolTable fSymbolTable;

    /** Error reporter. */
    protected XMLErrorReporter fErrorReporter;

    // the grammar pool
    protected XMLGrammarPool fGrammarPool;

    /** Grammar bucket. */
    protected DTDGrammarBucket fGrammarBucket;

    /* location of the document as passed in from startDocument call */
    protected XMLLocator fDocLocation;

    /** Namespace support. */
    protected NamespaceContext fNamespaceContext = null;

    /** Datatype validator factory. */
    protected DTDDVFactory fDatatypeValidatorFactory;

    // handlers

    /** Document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    protected XMLDocumentSource fDocumentSource;
    // grammars

    /** DTD Grammar. */
    protected DTDGrammar fDTDGrammar;

    // state

    /** True if seen DOCTYPE declaration. */
    protected boolean fSeenDoctypeDecl = false;

    /** Perform validation. */
    private boolean fPerformValidation;

    /** Schema type: None, DTD, Schema */
    private String fSchemaType;

    // information regarding the current element

    /** Current element name. */
    private final QName fCurrentElement = new QName();

    /** Current element index. */
    private int fCurrentElementIndex = -1;

    /** Current content spec type. */
    private int fCurrentContentSpecType = -1;

    /** The root element name. */
    private final QName fRootElement = new QName();

    private boolean fInCDATASection = false;
    // element stack

    /** Element index stack. */
    private int[] fElementIndexStack = new int[8];

    /** Content spec type stack. */
    private int[] fContentSpecTypeStack = new int[8];

    /** Element name stack. */
    private QName[] fElementQNamePartsStack = new QName[8];

    // children list and offset stack

    /**
     * Element children. This data structure is a growing stack that
     * holds the children of elements from the root to the current
     * element depth. This structure never gets "deeper" than the
     * deepest element. Space is re-used once each element is closed.
     * <p>
     * <strong>Note:</strong> This is much more efficient use of memory
     * than creating new arrays for each element depth.
     * <p>
     * <strong>Note:</strong> The use of this data structure is for
     * validation "on the way out". If the validation model changes to
     * "on the way in", then this data structure is not needed.
     */
    private QName[] fElementChildren = new QName[32];

    /** Element children count. */
    private int fElementChildrenLength = 0;

    /**
     * Element children offset stack. This stack refers to offsets
     * into the <code>fElementChildren</code> array.
     * @see #fElementChildren
     */
    private int[] fElementChildrenOffsetStack = new int[32];

    /** Element depth. */
    private int fElementDepth = -1;

    // validation states

    /** True if seen the root element. */
    private boolean fSeenRootElement = false;

    /** True if inside of element content. */
    private boolean fInElementContent = false;

    // temporary variables

    /** Temporary element declaration. */
    private final XMLElementDecl fTempElementDecl = new XMLElementDecl();

    /** Temporary atribute declaration. */
    private final XMLAttributeDecl fTempAttDecl = new XMLAttributeDecl();

    /** Temporary entity declaration. */
    private final XMLEntityDecl fEntityDecl = new XMLEntityDecl();

    /** Temporary qualified name. */
    private final QName fTempQName = new QName();

    /** Temporary string buffers. */
    private final StringBuilder fBuffer = new StringBuilder();

    // symbols: general

    // attribute validators

    /** Datatype validator: ID. */
    protected DatatypeValidator fValID;

    /** Datatype validator: IDREF. */
    protected DatatypeValidator fValIDRef;

    /** Datatype validator: IDREFS. */
    protected DatatypeValidator fValIDRefs;

    /** Datatype validator: ENTITY. */
    protected DatatypeValidator fValENTITY;

    /** Datatype validator: ENTITIES. */
    protected DatatypeValidator fValENTITIES;

    /** Datatype validator: NMTOKEN. */
    protected DatatypeValidator fValNMTOKEN;

    /** Datatype validator: NMTOKENS. */
    protected DatatypeValidator fValNMTOKENS;

    /** Datatype validator: NOTATION. */
    protected DatatypeValidator fValNOTATION;

    // to check for duplicate ID or ANNOTATION attribute declare in
    // ATTLIST, and misc VCs

    //
    // Constructors
    //

    /** Default constructor. */
    public XMLDTDValidator() {

        // initialize data
        for (int i = 0; i < fElementQNamePartsStack.length; i++) {
            fElementQNamePartsStack[i] = new QName();
        }
        fGrammarBucket = new DTDGrammarBucket();

    } // <init>()

    DTDGrammarBucket getGrammarBucket() {
        return fGrammarBucket;
    } // getGrammarBucket():  DTDGrammarBucket

    //
    // XMLComponent methods
    //

    /*
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Thrown by component on finitialization error.
     *                      For example, if a feature or property is
     *                      required for the operation of the component, the
     *                      component manager may throw a
     *                      SAXNotRecognizedException or a
     *                      SAXNotSupportedException.
     */
    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

        // clear grammars
        fDTDGrammar = null;
        fSeenDoctypeDecl = false;
        fInCDATASection = false;
        // initialize state
        fSeenRootElement = false;
        fInElementContent = false;
        fCurrentElementIndex = -1;
        fCurrentContentSpecType = -1;

        fRootElement.clear();

                fValidationState.resetIDTables();

                fGrammarBucket.clear();
                fElementDepth = -1;
                fElementChildrenLength = 0;

        boolean parser_settings = componentManager.getFeature(PARSER_SETTINGS, true);

        if (!parser_settings){
                // parser settings have not been changed
                        fValidationManager.addValidationState(fValidationState);
                return;
        }

        // sax features
        fNamespaces = componentManager.getFeature(NAMESPACES, true);
        fValidation = componentManager.getFeature(VALIDATION, false);
        fDTDValidation = !(componentManager.getFeature(Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE, false));

        // Xerces features
        fDynamicValidation = componentManager.getFeature(DYNAMIC_VALIDATION, false);
        fBalanceSyntaxTrees = componentManager.getFeature(BALANCE_SYNTAX_TREES, false);
        fWarnDuplicateAttdef = componentManager.getFeature(WARN_ON_DUPLICATE_ATTDEF, false);

        fSchemaType = (String)componentManager.getProperty (Constants.JAXP_PROPERTY_PREFIX
            + Constants.SCHEMA_LANGUAGE, null);

        fValidationManager= (ValidationManager)componentManager.getProperty(VALIDATION_MANAGER);
        fValidationManager.addValidationState(fValidationState);
        fValidationState.setUsingNamespaces(fNamespaces);

        // get needed components
        fErrorReporter = (XMLErrorReporter)componentManager.getProperty(Constants.XERCES_PROPERTY_PREFIX+Constants.ERROR_REPORTER_PROPERTY);
        fSymbolTable = (SymbolTable)componentManager.getProperty(Constants.XERCES_PROPERTY_PREFIX+Constants.SYMBOL_TABLE_PROPERTY);
        fGrammarPool= (XMLGrammarPool)componentManager.getProperty(GRAMMAR_POOL, null);

        fDatatypeValidatorFactory = (DTDDVFactory)componentManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.DATATYPE_VALIDATOR_FACTORY_PROPERTY);
                init();

    } // reset(XMLComponentManager)

    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES.clone();
    } // getRecognizedFeatures():String[]

    /**
     * Sets the state of a feature. This method is called by the component
     * manager any time after reset when a feature changes state.
     * <p>
     * <strong>Note:</strong> Components should silently ignore features
     * that do not affect the operation of the component.
     *
     * @param featureId The feature identifier.
     * @param state     The state of the feature.
     */
    public void setFeature(String featureId, boolean state)
    throws XMLConfigurationException {
    } // setFeature(String,boolean)

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES.clone();
    } // getRecognizedProperties():String[]

    /**
     * Sets the value of a property. This method is called by the component
     * manager any time after reset when a property changes value.
     * <p>
     * <strong>Note:</strong> Components should silently ignore properties
     * that do not affect the operation of the component.
     *
     * @param propertyId The property identifier.
     * @param value      The value of the property.
     */
    public void setProperty(String propertyId, Object value)
    throws XMLConfigurationException {
    } // setProperty(String,Object)

    /**
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     *
     * @param featureId The feature identifier.
     *
     * @since Xerces 2.2.0
     */
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return null;
    } // getFeatureDefault(String):Boolean

    /**
     * Returns the default state for a property, or null if this
     * component does not want to report a default value for this
     * property.
     *
     * @param propertyId The property identifier.
     *
     * @since Xerces 2.2.0
     */
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    } // getPropertyDefault(String):Object

    //
    // XMLDocumentSource methods
    //

    /** Sets the document handler to receive information about the document. */
    public void setDocumentHandler(XMLDocumentHandler documentHandler) {
        fDocumentHandler = documentHandler;
    } // setDocumentHandler(XMLDocumentHandler)

    /** Returns the document handler */
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    } // getDocumentHandler():  XMLDocumentHandler


    //
    // XMLDocumentHandler methods
    //

    /** Sets the document source */
    public void setDocumentSource(XMLDocumentSource source){
        fDocumentSource = source;
    } // setDocumentSource

    /** Returns the document source */
    public XMLDocumentSource getDocumentSource (){
        return fDocumentSource;
    } // getDocumentSource

    /**
     * The start of the document.
     *
     * @param locator The system identifier of the entity if the entity
     *                 is external, null otherwise.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     * @param namespaceContext
     *                 The namespace context in effect at the
     *                 start of this document.
     *                 This object represents the current context.
     *                 Implementors of this class are responsible
     *                 for copying the namespace bindings from the
     *                 the current context (and its parent contexts)
     *                 if that information is important.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startDocument(XMLLocator locator, String encoding,
                              NamespaceContext namespaceContext, Augmentations augs)
    throws XNIException {

        // call handlers
        // get initial grammars
        if (fGrammarPool != null) {
            Grammar [] grammars = fGrammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_DTD);
            final int length = (grammars != null) ? grammars.length : 0;
            for (int i = 0; i < length; ++i) {
                fGrammarBucket.putGrammar((DTDGrammar)grammars[i]);
            }
        }
        fDocLocation = locator;
        fNamespaceContext = namespaceContext;

        if (fDocumentHandler != null) {
            fDocumentHandler.startDocument(locator, encoding, namespaceContext, augs);
        }

    } // startDocument(XMLLocator,String)

    /**
     * Notifies of the presence of an XMLDecl line in the document. If
     * present, this method will be called immediately following the
     * startDocument call.
     *
     * @param version    The XML version.
     * @param encoding   The IANA encoding name of the document, or null if
     *                   not specified.
     * @param standalone The standalone value, or null if not specified.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs)
    throws XNIException {

        // save standalone state
        fGrammarBucket.setStandalone(standalone != null && standalone.equals("yes"));

        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }

    } // xmlDecl(String,String,String)

    /**
     * Notifies of the presence of the DOCTYPE line in the document.
     *
     * @param rootElement The name of the root element.
     * @param publicId    The public identifier if an external DTD or null
     *                    if the external DTD is specified using SYSTEM.
     * @param systemId    The system identifier if an external DTD, null
     *                    otherwise.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void doctypeDecl(String rootElement, String publicId, String systemId,
                            Augmentations augs)
    throws XNIException {

        // save root element state
        fSeenDoctypeDecl = true;
        fRootElement.setValues(null, rootElement, rootElement, null);
        // find or create grammar:
        String eid = null;
        try {
            eid = XMLEntityManager.expandSystemId(systemId, fDocLocation.getExpandedSystemId(), false);
        } catch (java.io.IOException e) {
        }
        XMLDTDDescription grammarDesc = new XMLDTDDescription(publicId, systemId, fDocLocation.getExpandedSystemId(), eid, rootElement);
        fDTDGrammar = fGrammarBucket.getGrammar(grammarDesc);
        if(fDTDGrammar == null) {
            // give grammar pool a chance...
            //
            // Do not bother checking the pool if no public or system identifier was provided.
            // Since so many different DTDs have roots in common, using only a root name as the
            // key may cause an unexpected grammar to be retrieved from the grammar pool. This scenario
            // would occur when an ExternalSubsetResolver has been queried and the
            // XMLInputSource returned contains an input stream but no external identifier.
            // This can never happen when the instance document specified a DOCTYPE. -- mrglavas
            if (fGrammarPool != null && (systemId != null || publicId != null)) {
                fDTDGrammar = (DTDGrammar)fGrammarPool.retrieveGrammar(grammarDesc);
            }
        }
        if(fDTDGrammar == null) {
            // we'll have to create it...
            if (!fBalanceSyntaxTrees) {
                fDTDGrammar = new DTDGrammar(fSymbolTable, grammarDesc);
            }
            else {
                fDTDGrammar = new BalancedDTDGrammar(fSymbolTable, grammarDesc);
            }
        } else {
            // we've found a cached one;so let's make sure not to read
            // any external subset!
            fValidationManager.setCachedDTD(true);
        }
        fGrammarBucket.setActiveGrammar(fDTDGrammar);

        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.doctypeDecl(rootElement, publicId, systemId, augs);
        }

    } // doctypeDecl(String,String,String, Augmentations)


    /**
     * The start of an element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
    throws XNIException {

        handleStartElement(element, attributes, augs);
        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.startElement(element, attributes, augs);

        }

    } // startElement(QName,XMLAttributes)

    /**
     * An empty element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
    throws XNIException {

        boolean removed = handleStartElement(element, attributes, augs);

        if (fDocumentHandler !=null) {
            fDocumentHandler.emptyElement(element, attributes, augs);
        }
        if (!removed) {
            handleEndElement(element, augs, true);
        }


    } // emptyElement(QName,XMLAttributes)

    /**
     * Character content.
     *
     * @param text The content.
     *
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void characters(XMLString text, Augmentations augs) throws XNIException {

        boolean callNextCharacters = true;

        // REVISIT: [Q] Is there a more efficient way of doing this?
        //          Perhaps if the scanner told us so we don't have to
        //          look at the characters again. -Ac
        boolean allWhiteSpace = true;
        for (int i=text.offset; i< text.offset+text.length; i++) {
            if (!isSpace(text.ch[i])) {
                allWhiteSpace = false;
                break;
            }
        }
        // call the ignoreableWhiteSpace callback
        // never call ignorableWhitespace if we are in cdata section
        if (fInElementContent && allWhiteSpace && !fInCDATASection) {
            if (fDocumentHandler != null) {
                fDocumentHandler.ignorableWhitespace(text, augs);
                callNextCharacters = false;
            }
        }

        // validate
        if (fPerformValidation) {
            if (fInElementContent) {
                if (fGrammarBucket.getStandalone() &&
                    fDTDGrammar.getElementDeclIsExternal(fCurrentElementIndex)) {
                    if (allWhiteSpace) {
                        fErrorReporter.reportError( XMLMessageFormatter.XML_DOMAIN,
                                                    "MSG_WHITE_SPACE_IN_ELEMENT_CONTENT_WHEN_STANDALONE",
                                                    null, XMLErrorReporter.SEVERITY_ERROR);
                    }
                }
                if (!allWhiteSpace) {
                    charDataInContent();
                }

                // For E15.2
                if (augs != null && augs.getItem(Constants.CHAR_REF_PROBABLE_WS) == Boolean.TRUE) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "MSG_CONTENT_INVALID_SPECIFIED",
                                               new Object[]{ fCurrentElement.rawname,
                                                   fDTDGrammar.getContentSpecAsString(fElementDepth),
                                                   "character reference"},
                                               XMLErrorReporter.SEVERITY_ERROR);
                }
            }

            if (fCurrentContentSpecType == XMLElementDecl.TYPE_EMPTY) {
                charDataInContent();
            }
        }

        // call handlers
        if (callNextCharacters && fDocumentHandler != null) {
            fDocumentHandler.characters(text, augs);
        }

    } // characters(XMLString)



    /**
     * Ignorable whitespace. For this method to be called, the document
     * source must have some way of determining that the text containing
     * only whitespace characters should be considered ignorable. For
     * example, the validator can determine if a length of whitespace
     * characters in the document are ignorable based on the element
     * content model.
     *
     * @param text The ignorable whitespace.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {

        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.ignorableWhitespace(text, augs);
        }

    } // ignorableWhitespace(XMLString)

    /**
     * The end of an element.
     *
     * @param element The name of the element.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endElement(QName element, Augmentations augs) throws XNIException {

        handleEndElement(element,  augs, false);

    } // endElement(QName)

    /**
     * The start of a CDATA section.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startCDATA(Augmentations augs) throws XNIException {

        if (fPerformValidation && fInElementContent) {
            charDataInContent();
        }
        fInCDATASection = true;
        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.startCDATA(augs);
        }

    } // startCDATA()

    /**
     * The end of a CDATA section.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endCDATA(Augmentations augs) throws XNIException {

        fInCDATASection = false;
        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.endCDATA(augs);
        }

    } // endCDATA()

    /**
     * The end of the document.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endDocument(Augmentations augs) throws XNIException {

        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.endDocument(augs);
        }

    } // endDocument()

    /**
     * A comment.
     *
     * @param text The text in the comment.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by application to signal an error.
     */
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        // fixes E15.1
        if (fPerformValidation && fElementDepth >= 0 && fDTDGrammar != null) {
            fDTDGrammar.getElementDecl(fCurrentElementIndex, fTempElementDecl);
            if (fTempElementDecl.type == XMLElementDecl.TYPE_EMPTY) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "MSG_CONTENT_INVALID_SPECIFIED",
                                               new Object[]{ fCurrentElement.rawname,
                                                             "EMPTY",
                                                             "comment"},
                                               XMLErrorReporter.SEVERITY_ERROR);
            }
        }
        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.comment(text, augs);
        }

    } // comment(XMLString)


    /**
     * A processing instruction. Processing instructions consist of a
     * target name and, optionally, text data. The data is only meaningful
     * to the application.
     * <p>
     * Typically, a processing instruction's data will contain a series
     * of pseudo-attributes. These pseudo-attributes follow the form of
     * element attributes but are <strong>not</strong> parsed or presented
     * to the application as anything other than text. The application is
     * responsible for parsing the data.
     *
     * @param target The target.
     * @param data   The data or null if none specified.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void processingInstruction(String target, XMLString data, Augmentations augs)
    throws XNIException {

        // fixes E15.1
        if (fPerformValidation && fElementDepth >= 0 && fDTDGrammar != null) {
            fDTDGrammar.getElementDecl(fCurrentElementIndex, fTempElementDecl);
            if (fTempElementDecl.type == XMLElementDecl.TYPE_EMPTY) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "MSG_CONTENT_INVALID_SPECIFIED",
                                               new Object[]{ fCurrentElement.rawname,
                                                             "EMPTY",
                                                             "processing instruction"},
                                               XMLErrorReporter.SEVERITY_ERROR);
            }
        }
        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.processingInstruction(target, data, augs);
        }
    } // processingInstruction(String,XMLString)

    /**
     * This method notifies the start of a general entity.
     * <p>
     * <strong>Note:</strong> This method is not called for entity references
     * appearing as part of attribute values.
     *
     * @param name     The name of the general entity.
     * @param identifier The resource identifier.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     * @param augs     Additional information that may include infoset augmentations
     *
     * @exception XNIException Thrown by handler to signal an error.
     */
    public void startGeneralEntity(String name,
                                   XMLResourceIdentifier identifier,
                                   String encoding,
                                   Augmentations augs) throws XNIException {
        if (fPerformValidation && fElementDepth >= 0 && fDTDGrammar != null) {
            fDTDGrammar.getElementDecl(fCurrentElementIndex, fTempElementDecl);
            // fixes E15.1
            if (fTempElementDecl.type == XMLElementDecl.TYPE_EMPTY) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                           "MSG_CONTENT_INVALID_SPECIFIED",
                                           new Object[]{ fCurrentElement.rawname,
                                                         "EMPTY", "ENTITY"},
                                           XMLErrorReporter.SEVERITY_ERROR);
            }
            if (fGrammarBucket.getStandalone()) {
                XMLDTDLoader.checkStandaloneEntityRef(name, fDTDGrammar, fEntityDecl, fErrorReporter);
            }
        }
        if (fDocumentHandler != null) {
            fDocumentHandler.startGeneralEntity(name, identifier, encoding, augs);
        }
    }

    /**
     * This method notifies the end of a general entity.
     * <p>
     * <strong>Note:</strong> This method is not called for entity references
     * appearing as part of attribute values.
     *
     * @param name   The name of the entity.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.endGeneralEntity(name, augs);
        }
    } // endEntity(String)

    /**
     * Notifies of the presence of a TextDecl line in an entity. If present,
     * this method will be called immediately following the startParameterEntity call.
     * <p>
     * <strong>Note:</strong> This method is only called for external
     * parameter entities referenced in the DTD.
     *
     * @param version  The XML version, or null if not specified.
     * @param encoding The IANA encoding name of the entity.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {

        // call handlers
        if (fDocumentHandler != null) {
            fDocumentHandler.textDecl(version, encoding, augs);
        }
    }


    public final boolean hasGrammar(){

        return (fDTDGrammar != null);
    }

    public final boolean validate(){
        // Do validation if all of the following are true:
        // 1. The JAXP Schema Language property is not XML Schema
        //    REVISIT: since only DTD and Schema are supported at this time,
        //             such checking is sufficient. but if more schema types
        //             are introduced in the future, we'll need to change it
        //             to something like
        //             (fSchemaType == null || fSchemaType == NS_XML_DTD)
        // 2. One of the following is true (validation features)
        // 2.1 Dynamic validation is off, and validation is on
        // 2.2 Dynamic validation is on, and DOCTYPE was seen
        // 3 Xerces schema validation feature is off, or DOCTYPE was seen.
        return (fSchemaType != Constants.NS_XMLSCHEMA) &&
               (!fDynamicValidation && fValidation ||
                fDynamicValidation && fSeenDoctypeDecl) &&
               (fDTDValidation || fSeenDoctypeDecl);
    }

            //REVISIT:we can convert into functions.. adding default attribute values.. and one validating.

    /** Add default attributes and validate. */
    protected void addDTDDefaultAttrsAndValidate(QName elementName, int elementIndex,
                                               XMLAttributes attributes)
    throws XNIException {

        // is there anything to do?
        if (elementIndex == -1 || fDTDGrammar == null) {
            return;
        }

        //
        // Check after all specified attrs are scanned
        // (1) report error for REQUIRED attrs that are missing (V_TAGc)
        // (2) add default attrs (FIXED and NOT_FIXED)
        //
        int attlistIndex = fDTDGrammar.getFirstAttributeDeclIndex(elementIndex);

        while (attlistIndex != -1) {

            fDTDGrammar.getAttributeDecl(attlistIndex, fTempAttDecl);

            if (DEBUG_ATTRIBUTES) {
                if (fTempAttDecl != null) {
                    XMLElementDecl elementDecl = new XMLElementDecl();
                    fDTDGrammar.getElementDecl(elementIndex, elementDecl);
                    System.out.println("element: "+(elementDecl.name.localpart));
                    System.out.println("attlistIndex " + attlistIndex + "\n"+
                                       "attName : '"+(fTempAttDecl.name.localpart) + "'\n"
                                       + "attType : "+fTempAttDecl.simpleType.type + "\n"
                                       + "attDefaultType : "+fTempAttDecl.simpleType.defaultType + "\n"
                                       + "attDefaultValue : '"+fTempAttDecl.simpleType.defaultValue + "'\n"
                                       + attributes.getLength() +"\n"
                                      );
                }
            }
            String attPrefix = fTempAttDecl.name.prefix;
            String attLocalpart = fTempAttDecl.name.localpart;
            String attRawName = fTempAttDecl.name.rawname;
            String attType = getAttributeTypeName(fTempAttDecl);
            int attDefaultType =fTempAttDecl.simpleType.defaultType;
            String attValue = null;

            if (fTempAttDecl.simpleType.defaultValue != null) {
                attValue = fTempAttDecl.simpleType.defaultValue;
            }

            boolean specified = false;
            boolean required = attDefaultType == XMLSimpleType.DEFAULT_TYPE_REQUIRED;
            boolean cdata = attType == XMLSymbols.fCDATASymbol;

            if (!cdata || required || attValue != null) {
                int attrCount = attributes.getLength();
                for (int i = 0; i < attrCount; i++) {
                    if (attributes.getQName(i) == attRawName) {
                        specified = true;
                        break;
                    }
                }
            }

            if (!specified) {
                if (required) {
                    if (fPerformValidation) {
                        Object[] args = {elementName.localpart, attRawName};
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                   "MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED", args,
                                                   XMLErrorReporter.SEVERITY_ERROR);
                    }
                }
                else if (attValue != null) {
                    if (fPerformValidation && fGrammarBucket.getStandalone()) {
                        if (fDTDGrammar.getAttributeDeclIsExternal(attlistIndex)) {

                            Object[] args = { elementName.localpart, attRawName};
                            fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                       "MSG_DEFAULTED_ATTRIBUTE_NOT_SPECIFIED", args,
                                                       XMLErrorReporter.SEVERITY_ERROR);
                        }
                    }

                    // add namespace information
                    if (fNamespaces) {
                        int index = attRawName.indexOf(':');
                        if (index != -1) {
                            attPrefix = attRawName.substring(0, index);
                            attPrefix = fSymbolTable.addSymbol(attPrefix);
                            attLocalpart = attRawName.substring(index + 1);
                            attLocalpart = fSymbolTable.addSymbol(attLocalpart);
                        }
                    }

                    // add attribute
                    fTempQName.setValues(attPrefix, attLocalpart, attRawName, fTempAttDecl.name.uri);
                    attributes.addAttribute(fTempQName, attType, attValue);
                }
            }
            // get next att decl in the Grammar for this element
            attlistIndex = fDTDGrammar.getNextAttributeDeclIndex(attlistIndex);
        }

        // now iterate through the expanded attributes for
        // 1. if every attribute seen is declared in the DTD
        // 2. check if the VC: default_fixed holds
        // 3. validate every attribute.
        int attrCount = attributes.getLength();
        for (int i = 0; i < attrCount; i++) {
            String attrRawName = attributes.getQName(i);
            boolean declared = false;
            if (fPerformValidation) {
                if (fGrammarBucket.getStandalone()) {
                    // check VC: Standalone Document Declaration, entities
                    // references appear in the document.
                    // REVISIT: this can be combined to a single check in
                    // startEntity if we add one more argument in
                    // startEntity, inAttrValue
                    String nonNormalizedValue = attributes.getNonNormalizedValue(i);
                    if (nonNormalizedValue != null) {
                        String entityName = getExternalEntityRefInAttrValue(nonNormalizedValue);
                        if (entityName != null) {
                            fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                       "MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE",
                                                       new Object[]{entityName},
                                                       XMLErrorReporter.SEVERITY_ERROR);
                        }
                    }
                }
            }
            int position =
            fDTDGrammar.getFirstAttributeDeclIndex(elementIndex);
            while (position != -1) {
                fDTDGrammar.getAttributeDecl(position, fTempAttDecl);
                if (fTempAttDecl.name.rawname == attrRawName) {
                    // found the match att decl,
                    declared = true;
                    break;
                }
                position = fDTDGrammar.getNextAttributeDeclIndex(position);
            }
            if (!declared) {
                if (fPerformValidation) {
                    // REVISIT - cache the elem/attr tuple so that we only
                    // give this error once for each unique occurrence
                    Object[] args = { elementName.rawname, attrRawName};

                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "MSG_ATTRIBUTE_NOT_DECLARED",
                                               args,XMLErrorReporter.SEVERITY_ERROR);
                }
                continue;
            }
            // attribute is declared

            // fTempAttDecl should have the right value set now, so
            // the following is not needed
            // fGrammar.getAttributeDecl(attDefIndex,fTempAttDecl);

            String type = getAttributeTypeName(fTempAttDecl);
            attributes.setType(i, type);
            attributes.getAugmentations(i).putItem(Constants.ATTRIBUTE_DECLARED, Boolean.TRUE);

            boolean changedByNormalization = false;
            String oldValue = attributes.getValue(i);
            String attrValue = oldValue;
            if (attributes.isSpecified(i) && type != XMLSymbols.fCDATASymbol) {
                changedByNormalization = normalizeAttrValue(attributes, i);
                attrValue = attributes.getValue(i);
                if (fPerformValidation && fGrammarBucket.getStandalone()
                    && changedByNormalization
                    && fDTDGrammar.getAttributeDeclIsExternal(position)
                   ) {
                    // check VC: Standalone Document Declaration
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "MSG_ATTVALUE_CHANGED_DURING_NORMALIZATION_WHEN_STANDALONE",
                                               new Object[]{attrRawName, oldValue, attrValue},
                                               XMLErrorReporter.SEVERITY_ERROR);
                }
            }
            if (!fPerformValidation) {
                continue;
            }
            if (fTempAttDecl.simpleType.defaultType ==
                XMLSimpleType.DEFAULT_TYPE_FIXED) {
                String defaultValue = fTempAttDecl.simpleType.defaultValue;

                if (!attrValue.equals(defaultValue)) {
                    Object[] args = {elementName.localpart,
                        attrRawName,
                        attrValue,
                        defaultValue};
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "MSG_FIXED_ATTVALUE_INVALID",
                                               args, XMLErrorReporter.SEVERITY_ERROR);
                }
            }

            if (fTempAttDecl.simpleType.type == XMLSimpleType.TYPE_ENTITY ||
                fTempAttDecl.simpleType.type == XMLSimpleType.TYPE_ENUMERATION ||
                fTempAttDecl.simpleType.type == XMLSimpleType.TYPE_ID ||
                fTempAttDecl.simpleType.type == XMLSimpleType.TYPE_IDREF ||
                fTempAttDecl.simpleType.type == XMLSimpleType.TYPE_NMTOKEN ||
                fTempAttDecl.simpleType.type == XMLSimpleType.TYPE_NOTATION
               ) {
                validateDTDattribute(elementName, attrValue, fTempAttDecl);
            }
        } // for all attributes

    } // addDTDDefaultAttrsAndValidate(int,XMLAttrList)

    /** Checks entities in attribute values for standalone VC. */
    protected String getExternalEntityRefInAttrValue(String nonNormalizedValue) {
        int valLength = nonNormalizedValue.length();
        int ampIndex = nonNormalizedValue.indexOf('&');
        while (ampIndex != -1) {
            if (ampIndex + 1 < valLength &&
                nonNormalizedValue.charAt(ampIndex+1) != '#') {
                int semicolonIndex = nonNormalizedValue.indexOf(';', ampIndex+1);
                String entityName = nonNormalizedValue.substring(ampIndex+1, semicolonIndex);
                entityName = fSymbolTable.addSymbol(entityName);
                int entIndex = fDTDGrammar.getEntityDeclIndex(entityName);
                if (entIndex > -1) {
                    fDTDGrammar.getEntityDecl(entIndex, fEntityDecl);
                    if (fEntityDecl.inExternal ||
                        (entityName = getExternalEntityRefInAttrValue(fEntityDecl.value)) != null) {
                        return entityName;
                    }
                }
            }
            ampIndex = nonNormalizedValue.indexOf('&', ampIndex+1);
        }
        return null;
    } // isExternalEntityRefInAttrValue(String):String

    /**
     * Validate attributes in DTD fashion.
     */
    protected void validateDTDattribute(QName element, String attValue,
                                      XMLAttributeDecl attributeDecl)
    throws XNIException {

        switch (attributeDecl.simpleType.type) {
        case XMLSimpleType.TYPE_ENTITY: {
                // NOTE: Save this information because invalidStandaloneAttDef
                boolean isAlistAttribute = attributeDecl.simpleType.list;

                try {
                    if (isAlistAttribute) {
                        fValENTITIES.validate(attValue, fValidationState);
                    }
                    else {
                        fValENTITY.validate(attValue, fValidationState);
                    }
                }
                catch (InvalidDatatypeValueException ex) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               ex.getKey(),
                                               ex.getArgs(),
                                               XMLErrorReporter.SEVERITY_ERROR );

                }
                break;
            }

        case XMLSimpleType.TYPE_NOTATION:
        case XMLSimpleType.TYPE_ENUMERATION: {
                boolean found = false;
                String [] enumVals = attributeDecl.simpleType.enumeration;
                if (enumVals == null) {
                    found = false;
                }
                else
                    for (int i = 0; i < enumVals.length; i++) {
                        if (attValue == enumVals[i] || attValue.equals(enumVals[i])) {
                            found = true;
                            break;
                        }
                    }

                if (!found) {
                    StringBuilder enumValueString = new StringBuilder();
                    if (enumVals != null)
                        for (int i = 0; i < enumVals.length; i++) {
                            enumValueString.append(enumVals[i]+" ");
                        }
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "MSG_ATTRIBUTE_VALUE_NOT_IN_LIST",
                                               new Object[]{attributeDecl.name.rawname, attValue, enumValueString},
                                               XMLErrorReporter.SEVERITY_ERROR);
                }
                break;
            }

        case XMLSimpleType.TYPE_ID: {
                try {
                    fValID.validate(attValue, fValidationState);
                }
                catch (InvalidDatatypeValueException ex) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               ex.getKey(),
                                               ex.getArgs(),
                                               XMLErrorReporter.SEVERITY_ERROR );
                }
                break;
            }

        case XMLSimpleType.TYPE_IDREF: {
                boolean isAlistAttribute = attributeDecl.simpleType.list;//Caveat - Save this information because invalidStandaloneAttDef

                try {
                    if (isAlistAttribute) {
                        fValIDRefs.validate(attValue, fValidationState);
                    }
                    else {
                        fValIDRef.validate(attValue, fValidationState);
                    }
                }
                catch (InvalidDatatypeValueException ex) {
                    if (isAlistAttribute) {
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                   "IDREFSInvalid",
                                                   new Object[]{attValue},
                                                   XMLErrorReporter.SEVERITY_ERROR );
                    }
                    else {
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                   ex.getKey(),
                                                   ex.getArgs(),
                                                   XMLErrorReporter.SEVERITY_ERROR );
                    }

                }
                break;
            }

        case XMLSimpleType.TYPE_NMTOKEN: {
                boolean isAlistAttribute = attributeDecl.simpleType.list;//Caveat - Save this information because invalidStandaloneAttDef
                //changes fTempAttDef
                try {
                    if (isAlistAttribute) {
                        fValNMTOKENS.validate(attValue, fValidationState);
                    }
                    else {
                        fValNMTOKEN.validate(attValue, fValidationState);
                    }
                }
                catch (InvalidDatatypeValueException ex) {
                    if (isAlistAttribute) {
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                   "NMTOKENSInvalid",
                                                   new Object[] { attValue},
                                                   XMLErrorReporter.SEVERITY_ERROR);
                    }
                    else {
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                   "NMTOKENInvalid",
                                                   new Object[] { attValue},
                                                   XMLErrorReporter.SEVERITY_ERROR);
                    }
                }
                break;
            }

        } // switch

    } // validateDTDattribute(QName,String,XMLAttributeDecl)


    /** Returns true if invalid standalone attribute definition. */
    protected boolean invalidStandaloneAttDef(QName element, QName attribute) {
        // REVISIT: This obviously needs to be fixed! -Ac
        boolean state = true;
        /*
       if (fStandaloneReader == -1) {
          return false;
       }
       // we are normalizing a default att value...  this ok?
       if (element.rawname == -1) {
          return false;
       }
       return getAttDefIsExternal(element, attribute);
       */
        return state;
    }


    //
    // Private methods
    //


    /**
     * Normalize the attribute value of a non CDATA attributes collapsing
     * sequences of space characters (x20)
     *
     * @param attributes The list of attributes
     * @param index The index of the attribute to normalize
     */
    private boolean normalizeAttrValue(XMLAttributes attributes, int index) {
        // vars
        boolean leadingSpace = true;
        boolean spaceStart = false;
        boolean readingNonSpace = false;
        int count = 0;
        String attrValue = attributes.getValue(index);
        char[] attValue = new char[attrValue.length()];

        fBuffer.setLength(0);
        attrValue.getChars(0, attrValue.length(), attValue, 0);
        for (int i = 0; i < attValue.length; i++) {

            if (attValue[i] == ' ') {

                // now the tricky part
                if (readingNonSpace) {
                    spaceStart = true;
                    readingNonSpace = false;
                }

                if (spaceStart && !leadingSpace) {
                    spaceStart = false;
                    fBuffer.append(attValue[i]);
                    count++;
                }
            } else {
                readingNonSpace = true;
                spaceStart = false;
                leadingSpace = false;
                fBuffer.append(attValue[i]);
                count++;
            }
        }

        // check if the last appended character is a space.
        if (count > 0 && fBuffer.charAt(count-1) == ' ') {
            fBuffer.setLength(count-1);
        }
        String newValue = fBuffer.toString();
        attributes.setValue(index, newValue);
        return ! attrValue.equals(newValue);
    }

    /** Root element specified. */
    private final void rootElementSpecified(QName rootElement) throws XNIException {
        if (fPerformValidation) {
            String root1 = fRootElement.rawname;
            String root2 = rootElement.rawname;
            if (root1 == null || !root1.equals(root2)) {
                fErrorReporter.reportError( XMLMessageFormatter.XML_DOMAIN,
                                            "RootElementTypeMustMatchDoctypedecl",
                                            new Object[]{root1, root2},
                                            XMLErrorReporter.SEVERITY_ERROR);
            }
        }
    } // rootElementSpecified(QName)

    /**
     * Check that the content of an element is valid.
     * <p>
     * This is the method of primary concern to the validator. This method is called
     * upon the scanner reaching the end tag of an element. At that time, the
     * element's children must be structurally validated, so it calls this method.
     * The index of the element being checked (in the decl pool), is provided as
     * well as an array of element name indexes of the children. The validator must
     * confirm that this element can have these children in this order.
     * <p>
     * This can also be called to do 'what if' testing of content models just to see
     * if they would be valid.
     * <p>
     * Note that the element index is an index into the element decl pool, whereas
     * the children indexes are name indexes, i.e. into the string pool.
     * <p>
     * A value of -1 in the children array indicates a PCDATA node. All other
     * indexes will be positive and represent child elements. The count can be
     * zero, since some elements have the EMPTY content model and that must be
     * confirmed.
     *
     * @param elementIndex The index within the <code>ElementDeclPool</code> of this
     *                     element.
     * @param childCount The number of entries in the <code>children</code> array.
     * @param children The children of this element.
     *
     * @return The value -1 if fully valid, else the 0 based index of the child
     *         that first failed. If the value returned is equal to the number
     *         of children, then additional content is required to reach a valid
     *         ending state.
     *
     * @exception Exception Thrown on error.
     */
    private int checkContent(int elementIndex,
                             QName[] children,
                             int childOffset,
                             int childCount) throws XNIException {

        fDTDGrammar.getElementDecl(elementIndex, fTempElementDecl);

        // Get out the content spec for this element
        final int contentType = fCurrentContentSpecType;


        //
        //  Deal with the possible types of content. We try to optimized here
        //  by dealing specially with content models that don't require the
        //  full DFA treatment.
        //
        if (contentType == XMLElementDecl.TYPE_EMPTY) {
            //
            //  If the child count is greater than zero, then this is
            //  an error right off the bat at index 0.
            //
            if (childCount != 0) {
                return 0;
            }
        }
        else if (contentType == XMLElementDecl.TYPE_ANY) {
            //
            //  This one is open game so we don't pass any judgement on it
            //  at all. Its assumed to fine since it can hold anything.
            //
        }
        else if (contentType == XMLElementDecl.TYPE_MIXED ||
                 contentType == XMLElementDecl.TYPE_CHILDREN) {
            // Get the content model for this element, faulting it in if needed
            ContentModelValidator cmElem = null;
            cmElem = fTempElementDecl.contentModelValidator;
            int result = cmElem.validate(children, childOffset, childCount);
            return result;
        }
        else if (contentType == -1) {
            //REVISIT
            /****
            reportRecoverableXMLError(XMLMessages.MSG_ELEMENT_NOT_DECLARED,
                                      XMLMessages.VC_ELEMENT_VALID,
                                      elementType);
            /****/
        }
        else if (contentType == XMLElementDecl.TYPE_SIMPLE) {

            //REVISIT
            // this should never be reached in the case of DTD validation.

        }
        else {
            //REVISIT
            /****
            fErrorReporter.reportError(fErrorReporter.getLocator(),
                                       ImplementationMessages.XERCES_IMPLEMENTATION_DOMAIN,
                                       ImplementationMessages.VAL_CST,
                                       0,
                                       null,
                                       XMLErrorReporter.ERRORTYPE_FATAL_ERROR);
            /****/
        }

        // We succeeded
        return -1;

    } // checkContent(int,int,QName[]):int

    /** Character data in content. */
    private void charDataInContent() {

        if (DEBUG_ELEMENT_CHILDREN) {
            System.out.println("charDataInContent()");
        }
        if (fElementChildren.length <= fElementChildrenLength) {
            QName[] newarray = new QName[fElementChildren.length * 2];
            System.arraycopy(fElementChildren, 0, newarray, 0, fElementChildren.length);
            fElementChildren = newarray;
        }
        QName qname = fElementChildren[fElementChildrenLength];
        if (qname == null) {
            for (int i = fElementChildrenLength; i < fElementChildren.length; i++) {
                fElementChildren[i] = new QName();
            }
            qname = fElementChildren[fElementChildrenLength];
        }
        qname.clear();
        fElementChildrenLength++;

    } // charDataInCount()

    /** convert attribute type from ints to strings */
    private String getAttributeTypeName(XMLAttributeDecl attrDecl) {

        switch (attrDecl.simpleType.type) {
        case XMLSimpleType.TYPE_ENTITY: {
                return attrDecl.simpleType.list ? XMLSymbols.fENTITIESSymbol : XMLSymbols.fENTITYSymbol;
            }
        case XMLSimpleType.TYPE_ENUMERATION: {
                int totalLength = 2;
                for (int i = 0; i < attrDecl.simpleType.enumeration.length; i++) {
                    totalLength += attrDecl.simpleType.enumeration[i].length() + 1;
                }
                StringBuilder buffer = new StringBuilder(totalLength);
                buffer.append('(');
                for (int i=0; i<attrDecl.simpleType.enumeration.length ; i++) {
                    if (i > 0) {
                        buffer.append('|');
                    }
                    buffer.append(attrDecl.simpleType.enumeration[i]);
                }
                buffer.append(')');
                return fSymbolTable.addSymbol(buffer.toString());
            }
        case XMLSimpleType.TYPE_ID: {
                return XMLSymbols.fIDSymbol;
            }
        case XMLSimpleType.TYPE_IDREF: {
                return attrDecl.simpleType.list ? XMLSymbols.fIDREFSSymbol : XMLSymbols.fIDREFSymbol;
            }
        case XMLSimpleType.TYPE_NMTOKEN: {
                return attrDecl.simpleType.list ? XMLSymbols.fNMTOKENSSymbol : XMLSymbols.fNMTOKENSymbol;
            }
        case XMLSimpleType.TYPE_NOTATION: {
                return XMLSymbols.fNOTATIONSymbol;
            }
        }
        return XMLSymbols.fCDATASymbol;

    } // getAttributeTypeName(XMLAttributeDecl):String

    /** initialization */
    protected void init() {

        // datatype validators
        if (fValidation || fDynamicValidation) {
            try {
                //REVISIT: datatypeRegistry + initialization of datatype
                //         why do we cast to ListDatatypeValidator?
                fValID       = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fIDSymbol);
                fValIDRef    = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fIDREFSymbol);
                fValIDRefs   = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fIDREFSSymbol);
                fValENTITY   = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fENTITYSymbol);
                fValENTITIES = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fENTITIESSymbol);
                fValNMTOKEN  = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fNMTOKENSymbol);
                fValNMTOKENS = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fNMTOKENSSymbol);
                fValNOTATION = fDatatypeValidatorFactory.getBuiltInDV(XMLSymbols.fNOTATIONSymbol);

            }
            catch (Exception e) {
                // should never happen
                e.printStackTrace(System.err);
            }

        }

    } // init()

    /** ensure element stack capacity */
    private void ensureStackCapacity (int newElementDepth) {
        if (newElementDepth == fElementQNamePartsStack.length) {

            QName[] newStackOfQueue = new QName[newElementDepth * 2];
            System.arraycopy(this.fElementQNamePartsStack, 0, newStackOfQueue, 0, newElementDepth );
            fElementQNamePartsStack = newStackOfQueue;

            QName qname = fElementQNamePartsStack[newElementDepth];
            if (qname == null) {
                for (int i = newElementDepth; i < fElementQNamePartsStack.length; i++) {
                    fElementQNamePartsStack[i] = new QName();
                }
            }

            int[] newStack = new int[newElementDepth * 2];
            System.arraycopy(fElementIndexStack, 0, newStack, 0, newElementDepth);
            fElementIndexStack = newStack;

            newStack = new int[newElementDepth * 2];
            System.arraycopy(fContentSpecTypeStack, 0, newStack, 0, newElementDepth);
            fContentSpecTypeStack = newStack;

        }
    } // ensureStackCapacity


    //
    // Protected methods
    //

    /** Handle element
     * @return true if validator is removed from the pipeline
     */
    protected boolean handleStartElement(QName element, XMLAttributes attributes, Augmentations augs)
                        throws XNIException {


        // VC: Root Element Type
        // see if the root element's name matches the one in DoctypeDecl
        if (!fSeenRootElement) {
            // REVISIT: Here are current assumptions about validation features
            //          given that XMLSchema validator is in the pipeline
            //
            // http://xml.org/sax/features/validation = true
            // http://apache.org/xml/features/validation/schema = true
            //
            // [1] XML instance document only has reference to a DTD
            //  Outcome: report validation errors only against dtd.
            //
            // [2] XML instance document has only XML Schema grammars:
            //  Outcome: report validation errors only against schemas (no errors produced from DTD validator)
            //
            // [3] XML instance document has DTD and XML schemas:
            // [a] if schema language is not set outcome - validation errors reported against both grammars: DTD and schemas.
            // [b] if schema language is set to XML Schema - do not report validation errors
            //
            // if dynamic validation is on
            //            validate only against grammar we've found (depending on settings
            //            for schema feature)
            //
            //
            fPerformValidation = validate();
            fSeenRootElement = true;
            fValidationManager.setEntityState(fDTDGrammar);
            fValidationManager.setGrammarFound(fSeenDoctypeDecl);
            rootElementSpecified(element);
        }
        if (fDTDGrammar == null) {

            if (!fPerformValidation) {
                fCurrentElementIndex = -1;
                fCurrentContentSpecType = -1;
                fInElementContent = false;
            }
            if (fPerformValidation) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                           "MSG_GRAMMAR_NOT_FOUND",
                                           new Object[]{ element.rawname},
                                           XMLErrorReporter.SEVERITY_ERROR);
            }
            // modify pipeline
            if (fDocumentSource !=null ) {
                fDocumentSource.setDocumentHandler(fDocumentHandler);
                if (fDocumentHandler != null)
                    fDocumentHandler.setDocumentSource(fDocumentSource);
                return true;
            }
        }
        else {
            //  resolve the element
            fCurrentElementIndex = fDTDGrammar.getElementDeclIndex(element);
            //changed here.. new function for getContentSpecType
            fCurrentContentSpecType = fDTDGrammar.getContentSpecType(fCurrentElementIndex);
            if (fCurrentContentSpecType == -1 && fPerformValidation) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                           "MSG_ELEMENT_NOT_DECLARED",
                                           new Object[]{ element.rawname},
                                           XMLErrorReporter.SEVERITY_ERROR);
            }

            //  0. insert default attributes
            //  1. normalize the attributes
            //  2. validate the attrivute list.
            // TO DO:
            //changed here.. also pass element name,
            addDTDDefaultAttrsAndValidate(element, fCurrentElementIndex, attributes);
        }

        // set element content state
        fInElementContent = fCurrentContentSpecType == XMLElementDecl.TYPE_CHILDREN;

        // increment the element depth, add this element's
        // QName to its enclosing element 's children list
        fElementDepth++;
        if (fPerformValidation) {
            // push current length onto stack
            if (fElementChildrenOffsetStack.length <= fElementDepth) {
                int newarray[] = new int[fElementChildrenOffsetStack.length * 2];
                System.arraycopy(fElementChildrenOffsetStack, 0, newarray, 0, fElementChildrenOffsetStack.length);
                fElementChildrenOffsetStack = newarray;
            }
            fElementChildrenOffsetStack[fElementDepth] = fElementChildrenLength;

            // add this element to children
            if (fElementChildren.length <= fElementChildrenLength) {
                QName[] newarray = new QName[fElementChildrenLength * 2];
                System.arraycopy(fElementChildren, 0, newarray, 0, fElementChildren.length);
                fElementChildren = newarray;
            }
            QName qname = fElementChildren[fElementChildrenLength];
            if (qname == null) {
                for (int i = fElementChildrenLength; i < fElementChildren.length; i++) {
                    fElementChildren[i] = new QName();
                }
                qname = fElementChildren[fElementChildrenLength];
            }
            qname.setValues(element);
            fElementChildrenLength++;
        }

        // save current element information
        fCurrentElement.setValues(element);
        ensureStackCapacity(fElementDepth);
        fElementQNamePartsStack[fElementDepth].setValues(fCurrentElement);
        fElementIndexStack[fElementDepth] = fCurrentElementIndex;
        fContentSpecTypeStack[fElementDepth] = fCurrentContentSpecType;
        startNamespaceScope(element, attributes, augs);
        return false;

    } // handleStartElement(QName,XMLAttributes)

    protected void startNamespaceScope(QName element, XMLAttributes attributes, Augmentations augs){
    }

    /** Handle end element. */
    protected void handleEndElement(QName element,  Augmentations augs, boolean isEmpty)
    throws XNIException {

        // decrease element depth
        fElementDepth--;

        // validate
        if (fPerformValidation) {
            int elementIndex = fCurrentElementIndex;
            if (elementIndex != -1 && fCurrentContentSpecType != -1) {
                QName children[] = fElementChildren;
                int childrenOffset = fElementChildrenOffsetStack[fElementDepth + 1] + 1;
                int childrenLength = fElementChildrenLength - childrenOffset;
                int result = checkContent(elementIndex,
                                          children, childrenOffset, childrenLength);

                if (result != -1) {
                    fDTDGrammar.getElementDecl(elementIndex, fTempElementDecl);
                    if (fTempElementDecl.type == XMLElementDecl.TYPE_EMPTY) {
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                   "MSG_CONTENT_INVALID",
                                                   new Object[]{ element.rawname, "EMPTY"},
                                                   XMLErrorReporter.SEVERITY_ERROR);
                    }
                    else {
                        String messageKey = result != childrenLength ?
                                            "MSG_CONTENT_INVALID" : "MSG_CONTENT_INCOMPLETE";
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                   messageKey,
                                                   new Object[]{ element.rawname,
                                                       fDTDGrammar.getContentSpecAsString(elementIndex)},
                                                   XMLErrorReporter.SEVERITY_ERROR);
                    }
                }
            }
            fElementChildrenLength = fElementChildrenOffsetStack[fElementDepth + 1] + 1;
        }

        endNamespaceScope(fCurrentElement, augs, isEmpty);

        // now pop this element off the top of the element stack
        if (fElementDepth < -1) {
            throw new RuntimeException("FWK008 Element stack underflow");
        }
        if (fElementDepth < 0) {
            fCurrentElement.clear();
            fCurrentElementIndex = -1;
            fCurrentContentSpecType = -1;
            fInElementContent = false;

            // TO DO : fix this
            //
            // Check after document is fully parsed
            // (1) check that there was an element with a matching id for every
            //   IDREF and IDREFS attr (V_IDREF0)
            //
            if (fPerformValidation) {
                Iterator<String> invIdRefs = fValidationState.checkIDRefID();
                if (invIdRefs != null) {
                    while (invIdRefs.hasNext()) {
                        fErrorReporter.reportError( XMLMessageFormatter.XML_DOMAIN,
                                "MSG_ELEMENT_WITH_ID_REQUIRED",
                                new Object[]{invIdRefs.next()},
                                XMLErrorReporter.SEVERITY_ERROR );
                    }
                }
            }
            return;
        }

        // If Namespace enable then localName != rawName
        fCurrentElement.setValues(fElementQNamePartsStack[fElementDepth]);

        fCurrentElementIndex = fElementIndexStack[fElementDepth];
        fCurrentContentSpecType = fContentSpecTypeStack[fElementDepth];
        fInElementContent = (fCurrentContentSpecType == XMLElementDecl.TYPE_CHILDREN);

    } // handleEndElement(QName,boolean)

    protected void endNamespaceScope(QName element,  Augmentations augs, boolean isEmpty){

        // call handlers
        if (fDocumentHandler != null && !isEmpty) {
            // NOTE: The binding of the element doesn't actually happen
            //       yet because the namespace binder does that. However,
            //       if it does it before this point, then the endPrefix-
            //       Mapping calls get made too soon! As long as the
            //       rawnames match, we know it'll have a good binding,
            //       so we can just use the current element. -Ac
            fDocumentHandler.endElement(fCurrentElement, augs);
        }
    }

    // returns whether a character is space according to the
    // version of XML this validator supports.
    protected boolean isSpace(int c) {
        return XMLChar.isSpace(c);
    } // isSpace(int):  boolean

    public boolean characterData(String data, Augmentations augs) {
        characters(new XMLString(data.toCharArray(), 0, data.length()), augs);
        return true;
    }

} // class XMLDTDValidator
