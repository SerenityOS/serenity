/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl;

import com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.AugmentationsImpl;
import com.sun.org.apache.xerces.internal.util.XMLAttributesIteratorImpl;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager.Limit;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.xml.internal.stream.XMLBufferListener;
import com.sun.xml.internal.stream.XMLEntityStorage;
import com.sun.xml.internal.stream.dtd.DTDGrammarUtil;
import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.SecuritySupport;

/**
 *
 * This class is responsible for scanning the structure and content
 * of document fragments.
 *
 * This class has been modified as per the new design which is more suited to
 * efficiently build pull parser. Lot of improvements have been done and
 * the code has been added to support stax functionality/features.
 *
 * @author Neeraj Bajaj SUN Microsystems
 * @author K.Venugopal SUN Microsystems
 * @author Glenn Marcy, IBM
 * @author Andy Clark, IBM
 * @author Arnaud  Le Hors, IBM
 * @author Eric Ye, IBM
 * @author Sunitha Reddy, SUN Microsystems
 *
 * @LastModified: May 2021
 */
public class XMLDocumentFragmentScannerImpl
        extends XMLScanner
        implements XMLDocumentScanner, XMLComponent, XMLEntityHandler, XMLBufferListener {

    //
    // Constants
    //

    protected int fElementAttributeLimit, fXMLNameLimit;

    /** External subset resolver. **/
    protected ExternalSubsetResolver fExternalSubsetResolver;

    // scanner states

    //XXX this should be divided into more states.
    /** Scanner state: start of markup. */
    protected static final int SCANNER_STATE_START_OF_MARKUP = 21;

    /** Scanner state: content. */
    protected static final int SCANNER_STATE_CONTENT = 22;

    /** Scanner state: processing instruction. */
    protected static final int SCANNER_STATE_PI = 23;

    /** Scanner state: DOCTYPE. */
    protected static final int SCANNER_STATE_DOCTYPE = 24;

    /** Scanner state: XML Declaration */
    protected static final int SCANNER_STATE_XML_DECL = 25;

    /** Scanner state: root element. */
    protected static final int SCANNER_STATE_ROOT_ELEMENT = 26;

    /** Scanner state: comment. */
    protected static final int SCANNER_STATE_COMMENT = 27;

    /** Scanner state: reference. */
    protected static final int SCANNER_STATE_REFERENCE = 28;

    // <book type="hard"> reading attribute name 'type'
    protected static final int SCANNER_STATE_ATTRIBUTE = 29;

    // <book type="hard"> //reading attribute value.
    protected static final int SCANNER_STATE_ATTRIBUTE_VALUE = 30;

    /** Scanner state: trailing misc. USED BY DOCUMENT_SCANNER_IMPL*/
    //protected static final int SCANNER_STATE_TRAILING_MISC = 32;

    /** Scanner state: end of input. */
    protected static final int SCANNER_STATE_END_OF_INPUT = 33;

    /** Scanner state: terminated. */
    protected static final int SCANNER_STATE_TERMINATED = 34;

    /** Scanner state: CDATA section. */
    protected static final int SCANNER_STATE_CDATA = 35;

    /** Scanner state: Text declaration. */
    protected static final int SCANNER_STATE_TEXT_DECL = 36;

    /** Scanner state: Text declaration. */
    protected static final int SCANNER_STATE_CHARACTER_DATA = 37;

    //<book type="hard">foo</book>
    protected static final int SCANNER_STATE_START_ELEMENT_TAG = 38;

    //<book type="hard">foo</book> reading </book>
    protected static final int SCANNER_STATE_END_ELEMENT_TAG = 39;

    protected static final int SCANNER_STATE_CHAR_REFERENCE = 40;
    protected static final int SCANNER_STATE_BUILT_IN_REFS = 41;

    // feature identifiers


    /** Feature identifier: notify built-in refereces. */
    protected static final String NOTIFY_BUILTIN_REFS =
            Constants.XERCES_FEATURE_PREFIX + Constants.NOTIFY_BUILTIN_REFS_FEATURE;

    /** Property identifier: entity resolver. */
    protected static final String ENTITY_RESOLVER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    /** Feature identifier: standard uri conformant */
    protected static final String STANDARD_URI_CONFORMANT =
            Constants.XERCES_FEATURE_PREFIX +Constants.STANDARD_URI_CONFORMANT_FEATURE;

    /** Feature id: create entity ref nodes. */
    protected static final String CREATE_ENTITY_REF_NODES =
            Constants.XERCES_FEATURE_PREFIX + Constants.CREATE_ENTITY_REF_NODES_FEATURE;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    /** access external dtd: file protocol
     *  For DOM/SAX, the secure feature is set to true by default
     */
    final static String EXTERNAL_ACCESS_DEFAULT = JdkConstants.EXTERNAL_ACCESS_DEFAULT;

    // recognized features and properties

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
                NAMESPACES,
                VALIDATION,
                NOTIFY_BUILTIN_REFS,
                NOTIFY_CHAR_REFS,
                Constants.STAX_REPORT_CDATA_EVENT,
                XMLConstants.USE_CATALOG
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
                Boolean.TRUE,
                null,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                JdkXmlUtils.USE_CATALOG_DEFAULT
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
                SYMBOL_TABLE,
                ERROR_REPORTER,
                ENTITY_MANAGER,
                XML_SECURITY_PROPERTY_MANAGER,
                JdkXmlUtils.CATALOG_DEFER,
                JdkXmlUtils.CATALOG_FILES,
                JdkXmlUtils.CATALOG_PREFER,
                JdkXmlUtils.CATALOG_RESOLVE,
                JdkConstants.CDATA_CHUNK_SIZE
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT
    };


    private static final char [] CDATA = {'[','C','D','A','T','A','['};
    static final char [] XMLDECL = {'<','?','x','m','l'};
    // private static final char [] endTag = {'<','/'};
    // debugging

    /** Debug scanner state. */
    private static final boolean DEBUG_SCANNER_STATE = false;

    /** Debug driver. */
    private static final boolean DEBUG_DISPATCHER = false;

    /** Debug content driver scanning. */
    protected static final boolean DEBUG_START_END_ELEMENT = false;

    /** Debug driver next */
    protected static final boolean DEBUG = false;

    //
    // Data
    //

    // protected data

    /** Document handler. */
    protected XMLDocumentHandler fDocumentHandler;
    protected int fScannerLastState ;

    /** Entity Storage */
    protected XMLEntityStorage fEntityStore;

    /** Entity stack. */
    protected int[] fEntityStack = new int[4];

    /** Markup depth. */
    protected int fMarkupDepth;

    //is the element empty
    protected boolean fEmptyElement ;

    //track if we are reading attributes, this is usefule while
    //there is a callback
    protected boolean fReadingAttributes = false;

    /** Scanner state. */
    protected int fScannerState;

    /** SubScanner state: inside scanContent method. */
    protected boolean fInScanContent = false;
    protected boolean fLastSectionWasCData = false;
    protected boolean fCDataStart = false;
    protected boolean fInCData = false;
    protected boolean fCDataEnd = false;
    protected boolean fLastSectionWasEntityReference = false;
    protected boolean fLastSectionWasCharacterData = false;

    /** has external dtd */
    protected boolean fHasExternalDTD;

    /** Standalone. */
    protected boolean fStandaloneSet;
    protected boolean fStandalone;
    protected String fVersion;

    // element information

    /** Current element. */
    protected QName fCurrentElement;

    /** Element stack. */
    protected ElementStack fElementStack = new ElementStack();
    protected ElementStack2 fElementStack2 = new ElementStack2();

    // other info

    /** Document system identifier.
     * REVISIT:  So what's this used for?  - NG
     * protected String fDocumentSystemId;
     ******/

    protected String fPITarget ;

    //xxx do we need to create an extra XMLString object... look for using fTempString for collecting all the data values
    protected XMLString fPIData  = new XMLString();

    // features


    /** Notify built-in references. */
    protected boolean fNotifyBuiltInRefs = false;

    //STAX related properties
    //defaultValues.
    protected boolean fSupportDTD = true;
    protected boolean fReplaceEntityReferences = true;
    protected boolean fSupportExternalEntities = false;
    protected boolean fReportCdataEvent = false ;
    protected boolean fIsCoalesce = false ;
    protected String fDeclaredEncoding =  null;
    /** Xerces Feature: Disallow doctype declaration. */
    protected boolean fDisallowDoctype = false;

    /** Create entity reference nodes. */
    protected boolean fCreateEntityRefNodes = false;

    /**
     * CDATA chunk size limit
     */
    private int fChunkSize;

    /**
     * comma-delimited list of protocols that are allowed for the purpose
     * of accessing external dtd or entity references
     */
    protected String fAccessExternalDTD = EXTERNAL_ACCESS_DEFAULT;

    /**
     * standard uri conformant (strict uri).
     * http://apache.org/xml/features/standard-uri-conformant
     */
    protected boolean fStrictURI;

    // drivers

    /** Active driver. */
    protected Driver fDriver;

    /** Content driver. */
    protected Driver fContentDriver = createContentDriver();

    // temporary variables

    /** Element QName. */
    protected QName fElementQName = new QName();

    /** Attribute QName. */
    protected QName fAttributeQName = new QName();

    /**
     * CHANGED: Using XMLAttributesIteratorImpl instead of XMLAttributesImpl. This class
     * implements Iterator interface so we can directly give Attributes in the form of
     * iterator.
     */
    protected XMLAttributesIteratorImpl fAttributes = new XMLAttributesIteratorImpl();


    /** String. */
    protected XMLString fTempString = new XMLString();

    /** String. */
    protected XMLString fTempString2 = new XMLString();

    /** Array of 3 strings. */
    private final String[] fStrings = new String[3];

    /** Making the buffer accessible to derived class -- String buffer. */
    protected XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    /** Making the buffer accessible to derived class -- String buffer. */
    protected XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();

    /** stores character data. */
    /** Making the buffer accessible to derived class -- stores PI data */
    protected XMLStringBuffer fContentBuffer = new XMLStringBuffer();

    /** Single character array. */
    private final char[] fSingleChar = new char[1];
    private String fCurrentEntityName = null;

    // New members
    protected boolean fScanToEnd = false;

    protected DTDGrammarUtil dtdGrammarUtil= null;

    protected boolean fAddDefaultAttr = false;

    protected boolean foundBuiltInRefs = false;

    /** Built-in reference character event */
    protected boolean builtInRefCharacterHandled = false;

    //skip element algorithm
    static final short MAX_DEPTH_LIMIT = 5 ;
    static final short ELEMENT_ARRAY_LENGTH = 200 ;
    static final short MAX_POINTER_AT_A_DEPTH = 4 ;
    static final boolean DEBUG_SKIP_ALGORITHM = false;
    //create a elemnet array of length equal to ELEMENT_ARRAY_LENGTH
    String [] fElementArray = new String[ELEMENT_ARRAY_LENGTH] ;
    //pointer location where last element was skipped
    short fLastPointerLocation = 0 ;
    short fElementPointer = 0 ;
    //2D array to store pointer info
    short [] [] fPointerInfo = new short[MAX_DEPTH_LIMIT] [MAX_POINTER_AT_A_DEPTH] ;
    protected String fElementRawname ;
    protected boolean fShouldSkip = false;
    protected boolean fAdd = false ;
    protected boolean fSkip = false;

    /** Reusable Augmentations. */
    private Augmentations fTempAugmentations = null;
    //
    // Constructors
    //

    /** Default constructor. */
    public XMLDocumentFragmentScannerImpl() {
    } // <init>()

    //
    // XMLDocumentScanner methods
    //

    /**
     * Sets the input source.
     *
     * @param inputSource The input source.
     *
     * @throws IOException Thrown on i/o error.
     */
    public void setInputSource(XMLInputSource inputSource) throws IOException {
        fEntityManager.setEntityHandler(this);
        fEntityManager.startEntity(false, "$fragment$", inputSource, false, true);
        // fDocumentSystemId = fEntityManager.expandSystemId(inputSource.getSystemId());
    } // setInputSource(XMLInputSource)

    /**
     * Scans a document.
     *
     * @param complete True if the scanner should scan the document
     *                 completely, pushing all events to the registered
     *                 document handler. A value of false indicates that
     *                 that the scanner should only scan the next portion
     *                 of the document and return. A scanner instance is
     *                 permitted to completely scan a document if it does
     *                 not support this "pull" scanning model.
     *
     * @return True if there is more to scan, false otherwise.
     */
    public boolean scanDocument(boolean complete)
    throws IOException, XNIException {

        // keep dispatching "events"
        fEntityManager.setEntityHandler(this);
        //System.out.println(" get Document Handler in NSDocumentHandler " + fDocumentHandler );

        int event = next();
        do {
            switch (event) {
                case XMLStreamConstants.START_DOCUMENT :
                    //fDocumentHandler.startDocument(fEntityManager.getEntityScanner(),fEntityManager.getEntityScanner().getVersion(),fNamespaceContext,null);// not able to get
                    break;
                case XMLStreamConstants.START_ELEMENT :
                    //System.out.println(" in scann element");
                    //fDocumentHandler.startElement(getElementQName(),fAttributes,null);
                    break;
                case XMLStreamConstants.CHARACTERS :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    fDocumentHandler.characters(getCharacterData(),null);
                    break;
                case XMLStreamConstants.SPACE:
                    //check if getCharacterData() is the right function to retrieve ignorableWhitespace information.
                    //System.out.println("in the space");
                    //fDocumentHandler.ignorableWhitespace(getCharacterData(), null);
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    //entity reference callback are given in startEntity
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    fDocumentHandler.processingInstruction(getPITarget(),getPIData(),null);
                    break;
                case XMLStreamConstants.COMMENT :
                    fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    fDocumentHandler.comment(getCharacterData(),null);
                    break;
                case XMLStreamConstants.DTD :
                    //all DTD related callbacks are handled in DTDScanner.
                    //1. Stax doesn't define DTD states as it does for XML Document.
                    //therefore we don't need to take care of anything here. So Just break;
                    break;
                case XMLStreamConstants.CDATA:
                   fEntityScanner.checkNodeCount(fEntityScanner.fCurrentEntity);
                    if (fCDataStart) {
                        fDocumentHandler.startCDATA(null);
                        fCDataStart = false;
                        fInCData = true;
                    }

                    fDocumentHandler.characters(getCharacterData(),null);
                    if (fCDataEnd) {
                        fDocumentHandler.endCDATA(null);
                        fCDataEnd = false;
                    }
                    break;
                case XMLStreamConstants.NOTATION_DECLARATION :
                    break;
                case XMLStreamConstants.ENTITY_DECLARATION :
                    break;
                case XMLStreamConstants.NAMESPACE :
                    break;
                case XMLStreamConstants.ATTRIBUTE :
                    break;
                case XMLStreamConstants.END_ELEMENT :
                    //do not give callback here.
                    //this callback is given in scanEndElement function.
                    //fDocumentHandler.endElement(getElementQName(),null);
                    break;
                default :
                    // Errors should have already been handled by the Scanner
                    return false;

            }
            //System.out.println("here in before calling next");
            event = next();
            //System.out.println("here in after calling next");
        } while (event!=XMLStreamConstants.END_DOCUMENT && complete);

        if(event == XMLStreamConstants.END_DOCUMENT) {
            fDocumentHandler.endDocument(null);
            return false;
        }

        return true;

    } // scanDocument(boolean):boolean



    public com.sun.org.apache.xerces.internal.xni.QName getElementQName(){
        if(fScannerLastState == XMLEvent.END_ELEMENT){
            fElementQName.setValues(fElementStack.getLastPoppedElement());
        }
        return fElementQName ;
    }

    /** return the next state on the input
     * @return int
     */

    public int next() throws IOException, XNIException {
        return fDriver.next();
    }

    //
    // XMLComponent methods
    //

    /**
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Thrown by component on initialization error.
     *                      For example, if a feature or property is
     *                      required for the operation of the component, the
     *                      component manager may throw a
     *                      SAXNotRecognizedException or a
     *                      SAXNotSupportedException.
     */

    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

        super.reset(componentManager);

        // other settings
        // fDocumentSystemId = null;

        // sax features
        //fAttributes.setNamespaces(fNamespaces);

        // xerces features
        fReportCdataEvent = componentManager.getFeature(Constants.STAX_REPORT_CDATA_EVENT, true);
        fSecurityManager = (XMLSecurityManager)componentManager.getProperty(Constants.SECURITY_MANAGER, null);
        fNotifyBuiltInRefs = componentManager.getFeature(NOTIFY_BUILTIN_REFS, false);

        fCreateEntityRefNodes = componentManager.getFeature(CREATE_ENTITY_REF_NODES, fCreateEntityRefNodes);

        Object resolver = componentManager.getProperty(ENTITY_RESOLVER, null);
        fExternalSubsetResolver = (resolver instanceof ExternalSubsetResolver) ?
                (ExternalSubsetResolver) resolver : null;

        //attribute
        fReadingAttributes = false;
        //xxx: external entities are supported in Xerces
        // it would be good to define feature for this case
        fSupportExternalEntities = true;
        fReplaceEntityReferences = true;
        fIsCoalesce = false;

        // setup Driver
        setScannerState(SCANNER_STATE_CONTENT);
        setDriver(fContentDriver);

        // JAXP 1.5 features and properties
        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)
                componentManager.getProperty(XML_SECURITY_PROPERTY_MANAGER, null);
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        fStrictURI = componentManager.getFeature(STANDARD_URI_CONFORMANT, false);
        fChunkSize = JdkXmlUtils.getValue(componentManager.getProperty(JdkConstants.CDATA_CHUNK_SIZE),
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);

        resetCommon();
        //fEntityManager.test();
    } // reset(XMLComponentManager)


    public void reset(PropertyManager propertyManager){

        super.reset(propertyManager);

        // other settings
        // fDocumentSystemId = null;
        fNamespaces = ((Boolean)propertyManager.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        fNotifyBuiltInRefs = false ;

        //fElementStack2.clear();
        //fReplaceEntityReferences = true;
        //fSupportExternalEntities = true;
        Boolean bo = (Boolean)propertyManager.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES);
        fReplaceEntityReferences = bo;
        bo = (Boolean)propertyManager.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES);
        fSupportExternalEntities = bo;
        Boolean cdata = (Boolean)propertyManager.getProperty(
                Constants.ZEPHYR_PROPERTY_PREFIX + Constants.STAX_REPORT_CDATA_EVENT) ;
        if(cdata != null)
            fReportCdataEvent = cdata ;
        Boolean coalesce = (Boolean)propertyManager.getProperty(XMLInputFactory.IS_COALESCING) ;
        if(coalesce != null)
            fIsCoalesce = coalesce;
        fReportCdataEvent = fIsCoalesce ? false : (fReportCdataEvent && true) ;
        //if fIsCoalesce is set to true, set the value of fReplaceEntityReferences to true,
        //if fIsCoalesce is set to false, take the value of fReplaceEntityReferences as set by application
        fReplaceEntityReferences = fIsCoalesce ? true : fReplaceEntityReferences;
        // setup Driver
        //we dont need to do this -- nb.
        //setScannerState(SCANNER_STATE_CONTENT);
        //setDriver(fContentDriver);
        //fEntityManager.test();

         // JAXP 1.5 features and properties
        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)
                propertyManager.getProperty(XML_SECURITY_PROPERTY_MANAGER);
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        fSecurityManager = (XMLSecurityManager)propertyManager.getProperty(Constants.SECURITY_MANAGER);
        fChunkSize = JdkXmlUtils.getValue(propertyManager.getProperty(JdkConstants.CDATA_CHUNK_SIZE),
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);
        resetCommon();
    } // reset(XMLComponentManager)

    void resetCommon() {
        // initialize vars
        fMarkupDepth = 0;
        fCurrentElement = null;
        fElementStack.clear();
        fHasExternalDTD = false;
        fStandaloneSet = false;
        fStandalone = false;
        fInScanContent = false;
        //skipping algorithm
        fShouldSkip = false;
        fAdd = false;
        fSkip = false;

        fEntityStore = fEntityManager.getEntityStore();
        dtdGrammarUtil = null;

        if (fSecurityManager != null) {
            fElementAttributeLimit = fSecurityManager.getLimit(XMLSecurityManager.Limit.ELEMENT_ATTRIBUTE_LIMIT);
            fXMLNameLimit = fSecurityManager.getLimit(XMLSecurityManager.Limit.MAX_NAME_LIMIT);
        } else {
            fElementAttributeLimit = 0;
            fXMLNameLimit = XMLSecurityManager.Limit.MAX_NAME_LIMIT.defaultValue();
        }
        fLimitAnalyzer = fEntityManager.fLimitAnalyzer;
    }

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
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setFeature(String featureId, boolean state)
    throws XMLConfigurationException {

        super.setFeature(featureId, state);

        // Xerces properties
        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            String feature = featureId.substring(Constants.XERCES_FEATURE_PREFIX.length());
            if (feature.equals(Constants.NOTIFY_BUILTIN_REFS_FEATURE)) {
                fNotifyBuiltInRefs = state;
            }
        }

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
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setProperty(String propertyId, Object value)
    throws XMLConfigurationException {

        super.setProperty(propertyId, value);

        // Xerces properties
        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();
            if (suffixLength == Constants.ENTITY_MANAGER_PROPERTY.length() &&
                    propertyId.endsWith(Constants.ENTITY_MANAGER_PROPERTY)) {
                fEntityManager = (XMLEntityManager)value;
                return;
            }
            if (suffixLength == Constants.ENTITY_RESOLVER_PROPERTY.length() &&
                    propertyId.endsWith(Constants.ENTITY_RESOLVER_PROPERTY)) {
                fExternalSubsetResolver = (value instanceof ExternalSubsetResolver) ?
                    (ExternalSubsetResolver) value : null;
                return;
            }
        }


                // Xerces properties
        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            String property = propertyId.substring(Constants.XERCES_PROPERTY_PREFIX.length());
            if (property.equals(Constants.ENTITY_MANAGER_PROPERTY)) {
                fEntityManager = (XMLEntityManager)value;
            }
            return;
        }

        //JAXP 1.5 properties
        if (propertyId.equals(XML_SECURITY_PROPERTY_MANAGER))
        {
            XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)value;
            fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);
        }

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

    /**
     * setDocumentHandler
     *
     * @param documentHandler
     */
    public void setDocumentHandler(XMLDocumentHandler documentHandler) {
        fDocumentHandler = documentHandler;
        //System.out.println(" In Set DOCUMENT HANDLER" + fDocumentHandler + " scanner =" + this);
    } // setDocumentHandler(XMLDocumentHandler)


    /** Returns the document handler */
    public XMLDocumentHandler getDocumentHandler(){
        return fDocumentHandler;
    }

    //
    // XMLEntityHandler methods
    //

    /**
     * This method notifies of the start of an entity. The DTD has the
     * pseudo-name of "[dtd]" parameter entity names start with '%'; and
     * general entities are just specified by their name.
     *
     * @param name     The name of the entity.
     * @param identifier The resource identifier.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startEntity(String name,
            XMLResourceIdentifier identifier,
            String encoding, Augmentations augs) throws XNIException {

        // keep track of this entity before fEntityDepth is increased
        if (fEntityDepth == fEntityStack.length) {
            int[] entityarray = new int[fEntityStack.length * 2];
            System.arraycopy(fEntityStack, 0, entityarray, 0, fEntityStack.length);
            fEntityStack = entityarray;
        }
        fEntityStack[fEntityDepth] = fMarkupDepth;

        super.startEntity(name, identifier, encoding, augs);

        // WFC:  entity declared in external subset in standalone doc
        if(fStandalone && fEntityStore.isEntityDeclInExternalSubset(name)) {
            reportFatalError("MSG_REFERENCE_TO_EXTERNALLY_DECLARED_ENTITY_WHEN_STANDALONE",
                    new Object[]{name});
        }

        /** we are not calling the handlers yet.. */
        // call handler
        if (fDocumentHandler != null && !fScanningAttribute) {
            if (!name.equals("[xml]")) {
                fDocumentHandler.startGeneralEntity(name, identifier, encoding, augs);
            }
        }

    } // startEntity(String,XMLResourceIdentifier,String)

    /**
     * This method notifies the end of an entity. The DTD has the pseudo-name
     * of "[dtd]" parameter entity names start with '%'; and general entities
     * are just specified by their name.
     *
     * @param name The name of the entity.
     * @param augs Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endEntity(String name, Augmentations augs) throws IOException, XNIException {

        /**
         * // flush possible pending output buffer - see scanContent
         * if (fInScanContent && fStringBuffer.length != 0
         * && fDocumentHandler != null) {
         * fDocumentHandler.characters(fStringBuffer, null);
         * fStringBuffer.length = 0; // make sure we know it's been flushed
         * }
         */
        super.endEntity(name, augs);

        // make sure markup is properly balanced
        if (fMarkupDepth != fEntityStack[fEntityDepth]) {
            reportFatalError("MarkupEntityMismatch", null);
        }

        /**/
        // call handler
        if (fDocumentHandler != null && !fScanningAttribute) {
            if (!name.equals("[xml]")) {
                fDocumentHandler.endGeneralEntity(name, augs);
            }
        }


    } // endEntity(String)

    //
    // Protected methods
    //

    // Driver factory methods

    /** Creates a content Driver. */
    protected Driver createContentDriver() {
        return new FragmentContentDriver();
    } // createContentDriver():Driver

    // scanning methods

    /**
     * Scans an XML or text declaration.
     * <p>
     * <pre>
     * [23] XMLDecl ::= '&lt;?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
     * [24] VersionInfo ::= S 'version' Eq (' VersionNum ' | " VersionNum ")
     * [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' |  "'" EncName "'" )
     * [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
     * [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'")
     *                 | ('"' ('yes' | 'no') '"'))
     *
     * [77] TextDecl ::= '&lt;?xml' VersionInfo? EncodingDecl S? '?>'
     * </pre>
     *
     * @param scanningTextDecl True if a text declaration is to
     *                         be scanned instead of an XML
     *                         declaration.
     */
    protected void scanXMLDeclOrTextDecl(boolean scanningTextDecl)
    throws IOException, XNIException {

        // scan decl
        super.scanXMLDeclOrTextDecl(scanningTextDecl, fStrings);
        fMarkupDepth--;

        // pseudo-attribute values
        String version = fStrings[0];
        String encoding = fStrings[1];
        String standalone = fStrings[2];
        fDeclaredEncoding = encoding;
        // set standalone
        fStandaloneSet = standalone != null;
        fStandalone = fStandaloneSet && standalone.equals("yes");
        ///xxx see where its used.. this is not used anywhere.
        //it may be useful for entity to store this information
        //but this information is only related with Document Entity.
        fEntityManager.setStandalone(fStandalone);


        // call handler
        if (fDocumentHandler != null) {
            if (scanningTextDecl) {
                fDocumentHandler.textDecl(version, encoding, null);
            } else {
                fDocumentHandler.xmlDecl(version, encoding, standalone, null);
            }
        }

        if(version != null){
            fEntityScanner.setVersion(version);
            fEntityScanner.setXMLVersion(version);
        }
        // set encoding on reader, only if encoding was not specified by the application explicitly
        if (encoding != null && !fEntityScanner.getCurrentEntity().isEncodingExternallySpecified()) {
             fEntityScanner.setEncoding(encoding);
        }

    } // scanXMLDeclOrTextDecl(boolean)

    public String getPITarget(){
        return fPITarget ;
    }

    public XMLStringBuffer getPIData(){
        return fContentBuffer ;
    }

    //XXX: why not this function behave as per the state of the parser?
    public XMLString getCharacterData(){
        if(fUsebuffer){
            return fContentBuffer ;
        }else{
            return fTempString;
        }

    }


    /**
     * Scans a processing data. This is needed to handle the situation
     * where a document starts with a processing instruction whose
     * target name <em>starts with</em> "xml". (e.g. xmlfoo)
     *
     * @param target The PI target
     * @param data The XMLStringBuffer to fill in with the data
     */
    protected void scanPIData(String target, XMLStringBuffer data)
    throws IOException, XNIException {

        super.scanPIData(target, data);

        //set the PI target and values
        fPITarget = target ;

        fMarkupDepth--;

    } // scanPIData(String)

    /**
     * Scans a comment.
     * <p>
     * <pre>
     * [15] Comment ::= '&lt!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!--'
     */
    protected void scanComment() throws IOException, XNIException {
        fContentBuffer.clear();
        scanComment(fContentBuffer);
        //getTextCharacters can also be called for reading comments
        fUsebuffer = true;
        fMarkupDepth--;

    } // scanComment()

    //xxx value returned by this function may not remain valid if another event is scanned.
    public String getComment(){
        return fContentBuffer.toString();
    }

    void addElement(String rawname){
        if(fElementPointer < ELEMENT_ARRAY_LENGTH){
            //storing element raw name in a linear list of array
            fElementArray[fElementPointer] = rawname ;
            //storing elemnetPointer for particular element depth

            if(DEBUG_SKIP_ALGORITHM){
                StringBuffer sb = new StringBuffer() ;
                sb.append(" Storing element information ") ;
                sb.append(" fElementPointer = " + fElementPointer) ;
                sb.append(" fElementRawname = " + fElementQName.rawname) ;
                sb.append(" fElementStack.fDepth = " + fElementStack.fDepth);
                System.out.println(sb.toString()) ;
            }

            //store pointer information only when element depth is less MAX_DEPTH_LIMIT
            if(fElementStack.fDepth < MAX_DEPTH_LIMIT){
                short column = storePointerForADepth(fElementPointer);
                if(column > 0){
                    short pointer = getElementPointer((short)fElementStack.fDepth, (short)(column - 1) );
                    //identity comparison shouldn't take much time and we can rely on this
                    //since its guaranteed to have same object id for same string.
                    if(rawname == fElementArray[pointer]){
                        fShouldSkip = true ;
                        fLastPointerLocation = pointer ;
                        //reset the things and return.
                        resetPointer((short)fElementStack.fDepth , column) ;
                        fElementArray[fElementPointer] = null ;
                        return ;
                    }else{
                        fShouldSkip = false ;
                    }
                }
            }
            fElementPointer++ ;
        }
    }


    void resetPointer(short depth, short column){
        fPointerInfo[depth] [column] = (short)0;
    }

    //returns column information at which pointer was stored.
    short storePointerForADepth(short elementPointer){
        short depth = (short) fElementStack.fDepth ;

        //Stores element pointer locations at particular depth , only 4 pointer locations
        //are stored at particular depth for now.
        for(short i = 0 ; i < MAX_POINTER_AT_A_DEPTH ; i++){

            if(canStore(depth, i)){
                fPointerInfo[depth][i] = elementPointer ;
                if(DEBUG_SKIP_ALGORITHM){
                    StringBuffer sb = new StringBuffer() ;
                    sb.append(" Pointer information ") ;
                    sb.append(" fElementPointer = " + fElementPointer) ;
                    sb.append(" fElementStack.fDepth = " + fElementStack.fDepth);
                    sb.append(" column = " + i ) ;
                    System.out.println(sb.toString()) ;
                }
                return i;
            }
            //else
            //pointer was not stored because we reached the limit
        }
        return -1 ;
    }

    boolean canStore(short depth, short column){
        //colum = 0 , means first element at particular depth
        //column = 1, means second element at particular depth
        //        calle should make sure that it doesn't call for value outside allowed co-ordinates
        return fPointerInfo[depth][column] == 0 ? true : false ;
    }


    short getElementPointer(short depth, short column){
        //colum = 0 , means first element at particular depth
        //column = 1, means second element at particular depth
        //        calle should make sure that it doesn't call for value outside allowed co-ordinates
        return fPointerInfo[depth][column] ;
    }

    //this function assumes that string passed is not null and skips
    //the following string from the buffer this makes sure
    boolean skipFromTheBuffer(String rawname) throws IOException{
        if(fEntityScanner.skipString(rawname)){
            char c = (char)fEntityScanner.peekChar() ;
            //If the start element was completely skipped we should encounter either ' '(space),
            //or '/' (in case of empty element)  or '>'
            if( c == ' ' || c == '/' || c == '>'){
                fElementRawname = rawname ;
                return true ;
            } else{
                return false;
            }
        } else
            return false ;
    }

    boolean skipQElement(String rawname) throws IOException{

        final int c = fEntityScanner.getChar(rawname.length());
        //if this character is still valid element name -- this means string can't match
        if(XMLChar.isName(c)){
            return false;
        }else{
            return fEntityScanner.skipString(rawname);
        }
    }

    protected boolean skipElement() throws IOException {

        if(!fShouldSkip) return false ;

        if(fLastPointerLocation != 0){
            //Look at the next element stored in the array list.. we might just get a match.
            String rawname = fElementArray[fLastPointerLocation + 1] ;
            if(rawname != null && skipFromTheBuffer(rawname)){
                fLastPointerLocation++ ;
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Element " + fElementRawname +
                            " was SKIPPED at pointer location = " + fLastPointerLocation);
                }
                return true ;
            } else{
                //reset it back to zero... we haven't got the correct subset yet.
                fLastPointerLocation = 0 ;

            }
        }
        //xxx: we can put some logic here as from what column it should start looking
        //for now we always start at 0
        //fallback to tolerant algorithm, it would look for differnt element stored at different
        //depth and get us the pointer location.
        return fShouldSkip && skipElement((short)0);

    }

    //start of the column at which it should try searching
    boolean skipElement(short column) throws IOException {
        short depth = (short)fElementStack.fDepth ;

        if(depth > MAX_DEPTH_LIMIT){
            return fShouldSkip = false ;
        }
        for(short i = column ; i < MAX_POINTER_AT_A_DEPTH ; i++){
            short pointer = getElementPointer(depth , i ) ;

            if(pointer == 0){
                return fShouldSkip = false ;
            }

            if(fElementArray[pointer] != null && skipFromTheBuffer(fElementArray[pointer])){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println();
                    System.out.println("Element " + fElementRawname + " was SKIPPED at depth = " +
                            fElementStack.fDepth + " column = " + column );
                    System.out.println();
                }
                fLastPointerLocation = pointer ;
                return fShouldSkip = true ;
            }
        }
        return fShouldSkip = false ;
    }

    /**
     * Scans a start element. This method will handle the binding of
     * namespace information and notifying the handler of the start
     * of the element.
     * <p>
     * <pre>
     * [44] EmptyElemTag ::= '&lt;' Name (S Attribute)* S? '/>'
     * [40] STag ::= '&lt;' Name (S Attribute)* S? '>'
     * </pre>
     * <p>
     * <strong>Note:</strong> This method assumes that the leading
     * '&lt;' character has been consumed.
     * <p>
     * <strong>Note:</strong> This method uses the fElementQName and
     * fAttributes variables. The contents of these variables will be
     * destroyed. The caller should copy important information out of
     * these variables before calling this method.
     * NB: Content in fAttributes is valid only till the state of the parser is XMLEvent.START_ELEMENT
     *
     * @return True if element is empty. (i.e. It matches
     *          production [44].
     */
    // fElementQName will have the details of element just read..
    // fAttributes will have the details of all the attributes.
    protected boolean scanStartElement()
    throws IOException, XNIException {

        if (DEBUG_START_END_ELEMENT) System.out.println( this.getClass().toString() + ">>> scanStartElement()");
        //when skipping is true and no more elements should be added
        if(fSkip && !fAdd){
            //get the stored element -- if everything goes right this should match the
            //token in the buffer

            QName name = fElementStack.getNext();

            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("Trying to skip String = " + name.rawname);
            }

            //Be conservative -- if skipping fails -- stop.
            fSkip = fEntityScanner.skipString(name.rawname);

            if(fSkip){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Element SUCESSFULLY skipped = " + name.rawname);
                }
                fElementStack.push();
                fElementQName = name;
            }else{
                //if skipping fails reposition the stack or fallback to normal way of processing
                fElementStack.reposition();
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Element was NOT skipped, REPOSITIONING stack" );
                }
            }
        }

        //we are still at the stage of adding elements
        //the elements were not matched or
        //fSkip is not set to true
        if(!fSkip || fAdd){
            //get the next element from the stack
            fElementQName = fElementStack.nextElement();
            // name
            if (fNamespaces) {
                fEntityScanner.scanQName(fElementQName, NameType.ELEMENTSTART);
            } else {
                String name = fEntityScanner.scanName(NameType.ELEMENTSTART);
                fElementQName.setValues(null, name, name, null);
            }

            if(DEBUG)System.out.println("Element scanned in start element is " + fElementQName.toString());
            if(DEBUG_SKIP_ALGORITHM){
                if(fAdd){
                    System.out.println("Elements are being ADDED -- elemet added is = " +
                            fElementQName.rawname + " at count = " + fElementStack.fCount);
                }
            }

        }

        //when the elements are being added , we need to check if we are set for skipping the elements
        if(fAdd){
            //this sets the value of fAdd variable
            fElementStack.matchElement(fElementQName);
        }


        //xxx: We dont need another pointer, fCurrentElement, we can use fElementQName
        fCurrentElement = fElementQName;

        String rawname = fElementQName.rawname;

        fEmptyElement = false;

        fAttributes.removeAllAttributes();

        checkDepth(rawname);
        if(!seekCloseOfStartTag()){
            fReadingAttributes = true;
            fAttributeCacheUsedCount =0;
            fStringBufferIndex =0;
            fAddDefaultAttr = true;
            do {
                scanAttribute(fAttributes);
                if (fSecurityManager != null && !fSecurityManager.isNoLimit(fElementAttributeLimit) &&
                        fAttributes.getLength() > fElementAttributeLimit){
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                 "ElementAttributeLimit",
                                                 new Object[]{rawname, fElementAttributeLimit },
                                                 XMLErrorReporter.SEVERITY_FATAL_ERROR );
                }

            } while (!seekCloseOfStartTag());
            fReadingAttributes=false;
        }

        if (fEmptyElement) {
            //decrease the markup depth..
            fMarkupDepth--;

            // check that this element was opened in the same entity
            if (fMarkupDepth < fEntityStack[fEntityDepth - 1]) {
                reportFatalError("ElementEntityMismatch",
                        new Object[]{fCurrentElement.rawname});
            }
            // call handler
            if (fDocumentHandler != null) {
                fDocumentHandler.emptyElement(fElementQName, fAttributes, null);
            }

            //We should not be popping out the context here in endELement becaause the namespace context is still
            //valid when parser is at the endElement state.
            //if (fNamespaces) {
            //  fNamespaceContext.popContext();
            //}

            //pop the element off the stack..
            fElementStack.popElement();

        } else {

            if(dtdGrammarUtil != null)
                dtdGrammarUtil.startElement(fElementQName, fAttributes);
            if(fDocumentHandler != null){
                //complete element and attributes are traversed in this function so we can send a callback
                //here.
                //<strong>we shouldn't be sending callback in scanDocument()</strong>
                fDocumentHandler.startElement(fElementQName, fAttributes, null);
            }
        }


        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +
                "<<< scanStartElement(): "+fEmptyElement);
        return fEmptyElement;

    } // scanStartElement():boolean

    /**
     * Looks for the close of start tag, i.e. if it finds '>' or '/>'
     * Characters are consumed.
     */
    protected boolean seekCloseOfStartTag() throws IOException, XNIException {
        // spaces
        boolean sawSpace = fEntityScanner.skipSpaces();

        // end tag?
        final int c = fEntityScanner.peekChar();
        if (c == '>') {
            fEntityScanner.scanChar(null);
            return true;
        } else if (c == '/') {
            fEntityScanner.scanChar(null);
            if (!fEntityScanner.skipChar('>', NameType.ELEMENTEND)) {
                reportFatalError("ElementUnterminated",
                        new Object[]{fElementQName.rawname});
            }
            fEmptyElement = true;
            return true;
        } else if (!isValidNameStartChar(c) || !sawSpace) {
            // Second chance. Check if this character is a high
            // surrogate of a valid name start character.
            if (!isValidNameStartHighSurrogate(c) || !sawSpace) {
                reportFatalError("ElementUnterminated",
                        new Object[]{fElementQName.rawname});
            }
        }

        return false;
    }

    public boolean hasAttributes(){
        return fAttributes.getLength() > 0;
    }

    /** return the attribute iterator implementation */
    public XMLAttributesIteratorImpl getAttributeIterator(){
        if(dtdGrammarUtil != null && fAddDefaultAttr){
            dtdGrammarUtil.addDTDDefaultAttrs(fElementQName,fAttributes);
            fAddDefaultAttr = false;
        }
        return fAttributes;
    }

    /** return if standalone is set */
    public boolean standaloneSet(){
        return fStandaloneSet;
    }
    /** return if the doucment is standalone */
    public boolean isStandAlone(){
        return fStandalone ;
    }
    /**
     * Scans an attribute name value pair.
     * <p>
     * <pre>
     * [41] Attribute ::= Name Eq AttValue
     * </pre>
     * <p>
     * <strong>Note:</strong> This method assumes that the next
     * character on the stream is the first character of the attribute
     * name.
     * <p>
     * <strong>Note:</strong> This method uses the fAttributeQName and
     * fQName variables. The contents of these variables will be
     * destroyed.
     *
     * @param attributes The attributes list for the scanned attribute.
     */

    protected void scanAttribute(XMLAttributes attributes)
    throws IOException, XNIException {
        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanAttribute()");

        // name
        if (fNamespaces) {
            fEntityScanner.scanQName(fAttributeQName, NameType.ATTRIBUTENAME);
        } else {
            String name = fEntityScanner.scanName(NameType.ATTRIBUTENAME);
            fAttributeQName.setValues(null, name, name, null);
        }

        // equals
        fEntityScanner.skipSpaces();
        if (!fEntityScanner.skipChar('=', NameType.ATTRIBUTE)) {
            reportFatalError("EqRequiredInAttribute",
                new Object[] {fCurrentElement.rawname, fAttributeQName.rawname});
        }
        fEntityScanner.skipSpaces();

        int attIndex = 0 ;
        //REVISIT: one more case needs to be included: external PE and standalone is no
        boolean isVC =  fHasExternalDTD && !fStandalone;
        //fTempString would store attribute value
        ///fTempString2 would store attribute non-normalized value

        //this function doesn't use 'attIndex'. We are adding the attribute later
        //after we have figured out that current attribute is not namespace declaration
        //since scanAttributeValue doesn't use attIndex parameter therefore we
        //can safely add the attribute later..
        XMLString tmpStr = getString();

        scanAttributeValue(tmpStr, fTempString2, fAttributeQName.rawname, attributes,
                attIndex, isVC, fCurrentElement.rawname, false);

        // content
        int oldLen = attributes.getLength();
        //if the attribute name already exists.. new value is replaced with old value
        attIndex = attributes.addAttribute(fAttributeQName, XMLSymbols.fCDATASymbol, null);

        // WFC: Unique Att Spec
        //attributes count will be same if the current attribute  name already exists for this element name.
        //this means there are two duplicate attributes.
        if (oldLen == attributes.getLength()) {
            reportFatalError("AttributeNotUnique",
                    new Object[]{fCurrentElement.rawname,
                            fAttributeQName.rawname});
        }

        //tmpString contains attribute value
        //we are passing null as the attribute value
        attributes.setValue(attIndex, null, tmpStr);

        ///xxx: nonNormalizedValue is not being set as it is not required by SAX & DOM
        //attributes.setNonNormalizedValue(oldLen, fTempString2.toString());
        attributes.setSpecified(attIndex, true);

        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +"<<< scanAttribute()");

    } // scanAttribute(XMLAttributes)

    /**
     * Scans element content.
     *
     * @return Returns the next character on the stream.
     */
    //CHANGED:
    //EARLIER: scanContent()
    //NOW: scanContent(XMLStringBuffer)
    //It makes things easy if this functions takes XMLStringBuffer as parameter..
    //this function appends the data to the buffer.
    protected int scanContent(XMLStringBuffer content) throws IOException, XNIException {
        //set the fTempString length to 0 before passing it on to scanContent
        //scanContent sets the correct co-ordinates as per the content read
        fTempString.length = 0;
        int c = fEntityScanner.scanContent(fTempString);
        content.append(fTempString);
        fTempString.length = 0;
        if (c == '\r') {
            // happens when there is the character reference &#13;
            //xxx: We know the next chracter.. we should just skip it and add ']' directlry
            fEntityScanner.scanChar(null);
            content.append((char)c);
            c = -1;
        } else if (c == ']') {
            //fStringBuffer.clear();
            //xxx: We know the next chracter.. we should just skip it and add ']' directlry
            content.append((char)fEntityScanner.scanChar(null));
            // remember where we are in case we get an endEntity before we
            // could flush the buffer out - this happens when we're parsing an
            // entity which ends with a ]
            fInScanContent = true;
            //
            // We work on a single character basis to handle cases such as:
            // ']]]>' which we might otherwise miss.
            //
            if (fEntityScanner.skipChar(']', null)) {
                content.append(']');
                while (fEntityScanner.skipChar(']', null)) {
                    content.append(']');
                }
                if (fEntityScanner.skipChar('>', null)) {
                    reportFatalError("CDEndInContent", null);
                }
            }
            fInScanContent = false;
            c = -1;
        }
        if (fDocumentHandler != null && content.length > 0) {
            //fDocumentHandler.characters(content, null);
        }
        return c;

    } // scanContent():int


    /**
     * Scans a CDATA section.
     * <p>
     * <strong>Note:</strong> This method uses the fTempString and
     * fStringBuffer variables.
     *
     * @param complete True if the CDATA section is to be scanned
     *                 completely.
     *
     * @return True if CDATA is completely scanned.
     */
    //CHANGED:
    protected boolean scanCDATASection(XMLStringBuffer contentBuffer, boolean complete)
    throws IOException, XNIException {

        // call handler
        if (fDocumentHandler != null) {
            //fDocumentHandler.startCDATA(null);
        }

        while (true) {
            //scanData will fill the contentBuffer
            if (!fEntityScanner.scanData("]]>", contentBuffer, fChunkSize)) {
                fInCData = false;
                fCDataEnd = true;
                fMarkupDepth--;
                break ;
            } else {
                int c = fEntityScanner.peekChar();
                if (c != -1 && isInvalidLiteral(c)) {
                    if (XMLChar.isHighSurrogate(c)) {
                        //contentBuffer.clear();
                        //scan surrogates if any....
                        scanSurrogates(contentBuffer);
                    } else {
                        reportFatalError("InvalidCharInCDSect",
                                new Object[]{Integer.toString(c,16)});
                                fEntityScanner.scanChar(null);
                    }
                } else {
                    //CData partially returned due to the size limit
                    fInCData = true;
                    fCDataEnd = false;
                    break;
                }
                //by this time we have also read surrogate contents if any...
                if (fDocumentHandler != null) {
                    //fDocumentHandler.characters(contentBuffer, null);
                }
            }
        }

        return true;

    } // scanCDATASection(XMLStringBuffer, boolean):boolean

    /**
     * Scans an end element.
     * <p>
     * <pre>
     * [42] ETag ::= '&lt;/' Name S? '>'
     * </pre>
     * <p>
     * <strong>Note:</strong> This method uses the fElementQName variable.
     * The contents of this variable will be destroyed. The caller should
     * copy the needed information out of this variable before calling
     * this method.
     *
     * @return The element depth.
     */
    protected int scanEndElement() throws IOException, XNIException {
        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanEndElement()");

        // pop context
        QName endElementName = fElementStack.popElement();

        String rawname = endElementName.rawname;
        if(DEBUG)System.out.println("endElementName = " + endElementName.toString());
        // Take advantage of the fact that next string _should_ be "fElementQName.rawName",
        //In scanners most of the time is consumed on checks done for XML characters, we can
        // optimize on it and avoid the checks done for endElement,
        //we will also avoid symbol table lookup.

        // this should work both for namespace processing true or false...

        //REVISIT: if the string is not the same as expected.. we need to do better error handling..
        //We can skip this for now... In any case if the string doesn't match -- document is not well formed.

        if (!fEntityScanner.skipString(endElementName.rawname)) {
             reportFatalError("ETagRequired", new Object[]{rawname});
        }

        // end
        fEntityScanner.skipSpaces();
        if (!fEntityScanner.skipChar('>', NameType.ELEMENTEND)) {
            reportFatalError("ETagUnterminated",
                    new Object[]{rawname});
        }
        fMarkupDepth--;

        //we have increased the depth for two markup "<" characters
        fMarkupDepth--;

        // check that this element was opened in the same entity
        if (fMarkupDepth < fEntityStack[fEntityDepth - 1]) {
            reportFatalError("ElementEntityMismatch",
                    new Object[]{rawname});
        }

        //We should not be popping out the context here in endELement becaause the namespace context is still
        //valid when parser is at the endElement state.

        //if (fNamespaces) {
        //  fNamespaceContext.popContext();
        //}

        // call handler
        if (fDocumentHandler != null ) {
            //end element is scanned in this function so we can send a callback
            //here.
            //<strong>we shouldn't be sending callback in scanDocument()</strong>

            fDocumentHandler.endElement(endElementName, null);
        }
        if(dtdGrammarUtil != null)
            dtdGrammarUtil.endElement(endElementName);

        return fMarkupDepth;

    } // scanEndElement():int

    /**
     * Scans a character reference.
     * <p>
     * <pre>
     * [66] CharRef ::= '&#' [0-9]+ ';' | '&#x' [0-9a-fA-F]+ ';'
     * </pre>
     */
    protected void scanCharReference()
    throws IOException, XNIException {

        fStringBuffer2.clear();
        int ch = scanCharReferenceValue(fStringBuffer2, null);
        fMarkupDepth--;
        if (ch != -1) {
            // call handler

            if (fDocumentHandler != null) {
                if (fNotifyCharRefs) {
                    fDocumentHandler.startGeneralEntity(fCharRefLiteral, null, null, null);
                }
                Augmentations augs = null;
                if (fValidation && ch <= 0x20) {
                    if (fTempAugmentations != null) {
                        fTempAugmentations.removeAllItems();
                    }
                    else {
                        fTempAugmentations = new AugmentationsImpl();
                    }
                    augs = fTempAugmentations;
                    augs.putItem(Constants.CHAR_REF_PROBABLE_WS, Boolean.TRUE);
                }
                //xxx: How do we deal with this - how to return charReferenceValues
                //now this is being commented because this is taken care in scanDocument()
                //fDocumentHandler.characters(fStringBuffer2, null);
                if (fNotifyCharRefs) {
                    fDocumentHandler.endGeneralEntity(fCharRefLiteral, null);
                }
            }
        }

    } // scanCharReference()


    /**
     * Scans an entity reference.
     *
     * @return returns true if the new entity is started. If it was built-in entity
     *         'false' is returned.
     * @throws IOException  Thrown if i/o error occurs.
     * @throws XNIException Thrown if handler throws exception upon
     *                      notification.
     */
    protected void scanEntityReference(XMLStringBuffer content) throws IOException, XNIException {
        String name = fEntityScanner.scanName(NameType.REFERENCE);
        if (name == null) {
            reportFatalError("NameRequiredInReference", null);
            return;
        }
        if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
            reportFatalError("SemicolonRequiredInReference", new Object []{name});
        }
        if (fEntityStore.isUnparsedEntity(name)) {
            reportFatalError("ReferenceToUnparsedEntity", new Object[]{name});
        }
        fMarkupDepth--;
        fCurrentEntityName = name;

        // handle built-in entities
        if (name == fAmpSymbol) {
            handleCharacter('&', fAmpSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fLtSymbol) {
            handleCharacter('<', fLtSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fGtSymbol) {
            handleCharacter('>', fGtSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fQuotSymbol) {
            handleCharacter('"', fQuotSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        } else if (name == fAposSymbol) {
            handleCharacter('\'', fAposSymbol, content);
            fScannerState = SCANNER_STATE_BUILT_IN_REFS;
            return ;
        }

        //1. if the entity is external and support to external entities is not required
        // 2. or entities should not be replaced
        //3. or if it is built in entity reference.
        boolean isEE = fEntityStore.isExternalEntity(name);
        if((isEE && !fSupportExternalEntities) || (!isEE && !fReplaceEntityReferences) || foundBuiltInRefs){
            fScannerState = SCANNER_STATE_REFERENCE;
            return ;
        }
        // start general entity
        if (!fEntityStore.isDeclaredEntity(name)) {
            //SUPPORT_DTD=false && ReplaceEntityReferences should throw exception
            if (!fSupportDTD && fReplaceEntityReferences) {
                reportFatalError("EntityNotDeclared", new Object[]{name});
                return;
            }
            //REVISIT: one more case needs to be included: external PE and standalone is no
            if ( fHasExternalDTD && !fStandalone) {
                if (fValidation)
                    fErrorReporter.reportError(fEntityScanner, XMLMessageFormatter.XML_DOMAIN,"EntityNotDeclared",
                            new Object[]{name}, XMLErrorReporter.SEVERITY_ERROR);
            } else
                reportFatalError("EntityNotDeclared", new Object[]{name});
        }

        // create EntityReference only
        if (fCreateEntityRefNodes) {
            fDocumentHandler.startGeneralEntity(name, null, null, null);
        } else {
            //we are starting the entity even if the entity was not declared
            //if that was the case it its taken care in XMLEntityManager.startEntity()
            //we immediately call the endEntity. Application gets to know if there was
            //any entity that was not declared.
            fEntityManager.startEntity(true, name, false);
            //set the scaner state to content.. parser will automatically revive itself at any point of time.
            //setScannerState(SCANNER_STATE_CONTENT);
            //return true ;
        }
    } // scanEntityReference()

    // utility methods

    /**
     * Check if the depth exceeds the maxElementDepth limit
     * @param elementName name of the current element
     */
    void checkDepth(String elementName) {
        fLimitAnalyzer.addValue(Limit.MAX_ELEMENT_DEPTH_LIMIT, elementName, fElementStack.fDepth);
        if (fSecurityManager.isOverLimit(Limit.MAX_ELEMENT_DEPTH_LIMIT,fLimitAnalyzer)) {
            fSecurityManager.debugPrint(fLimitAnalyzer);
            reportFatalError("MaxElementDepthLimit", new Object[]{elementName,
                fLimitAnalyzer.getTotalValue(Limit.MAX_ELEMENT_DEPTH_LIMIT),
                fSecurityManager.getLimit(Limit.MAX_ELEMENT_DEPTH_LIMIT),
                "maxElementDepth"});
        }
    }

    /**
     * Calls document handler with a single character resulting from
     * built-in entity resolution.
     *
     * @param c
     * @param entity built-in name
     * @param XMLStringBuffer append the character to buffer
     *
     * we really dont need to call this function -- this function is only required when
     * we integrate with rest of Xerces2. SO maintaining the current behavior and still
     * calling this function to hanlde built-in entity reference.
     *
     */
    private void handleCharacter(char c, String entity, XMLStringBuffer content) throws XNIException {
        foundBuiltInRefs = true;
        checkEntityLimit(false, fEntityScanner.fCurrentEntity.name, 1);
        content.append(c);
        if (fDocumentHandler != null) {
            fSingleChar[0] = c;
            if (fNotifyBuiltInRefs) {
                fDocumentHandler.startGeneralEntity(entity, null, null, null);
            }
            fTempString.setValues(fSingleChar, 0, 1);
            if(!fIsCoalesce){
                fDocumentHandler.characters(fTempString, null);
                builtInRefCharacterHandled = true;
            }

            if (fNotifyBuiltInRefs) {
                fDocumentHandler.endGeneralEntity(entity, null);
            }
        }
    } // handleCharacter(char)

    // helper methods

    /**
     * Sets the scanner state.
     *
     * @param state The new scanner state.
     */
    protected final void setScannerState(int state) {

        fScannerState = state;
        if (DEBUG_SCANNER_STATE) {
            System.out.print("### setScannerState: ");
            //System.out.print(fScannerState);
            System.out.print(getScannerStateName(state));
            System.out.println();
        }

    } // setScannerState(int)


    /**
     * Sets the Driver.
     *
     * @param Driver The new Driver.
     */
    protected final void setDriver(Driver driver) {
        fDriver = driver;
        if (DEBUG_DISPATCHER) {
            System.out.print("%%% setDriver: ");
            System.out.print(getDriverName(driver));
            System.out.println();
        }
    }

    //
    // Private methods
    //

    /** Returns the scanner state name. */
    protected String getScannerStateName(int state) {

        switch (state) {
            case SCANNER_STATE_DOCTYPE: return "SCANNER_STATE_DOCTYPE";
            case SCANNER_STATE_ROOT_ELEMENT: return "SCANNER_STATE_ROOT_ELEMENT";
            case SCANNER_STATE_START_OF_MARKUP: return "SCANNER_STATE_START_OF_MARKUP";
            case SCANNER_STATE_COMMENT: return "SCANNER_STATE_COMMENT";
            case SCANNER_STATE_PI: return "SCANNER_STATE_PI";
            case SCANNER_STATE_CONTENT: return "SCANNER_STATE_CONTENT";
            case SCANNER_STATE_REFERENCE: return "SCANNER_STATE_REFERENCE";
            case SCANNER_STATE_END_OF_INPUT: return "SCANNER_STATE_END_OF_INPUT";
            case SCANNER_STATE_TERMINATED: return "SCANNER_STATE_TERMINATED";
            case SCANNER_STATE_CDATA: return "SCANNER_STATE_CDATA";
            case SCANNER_STATE_TEXT_DECL: return "SCANNER_STATE_TEXT_DECL";
            case SCANNER_STATE_ATTRIBUTE: return "SCANNER_STATE_ATTRIBUTE";
            case SCANNER_STATE_ATTRIBUTE_VALUE: return "SCANNER_STATE_ATTRIBUTE_VALUE";
            case SCANNER_STATE_START_ELEMENT_TAG: return "SCANNER_STATE_START_ELEMENT_TAG";
            case SCANNER_STATE_END_ELEMENT_TAG: return "SCANNER_STATE_END_ELEMENT_TAG";
            case SCANNER_STATE_CHARACTER_DATA: return "SCANNER_STATE_CHARACTER_DATA" ;
        }

        return "??? ("+state+')';

    } // getScannerStateName(int):String
    public String getEntityName(){
        //return the cached name
        return fCurrentEntityName;
    }

    /** Returns the driver name. */
    public String getDriverName(Driver driver) {

        if (DEBUG_DISPATCHER) {
            if (driver != null) {
                String name = driver.getClass().getName();
                int index = name.lastIndexOf('.');
                if (index != -1) {
                    name = name.substring(index + 1);
                    index = name.lastIndexOf('$');
                    if (index != -1) {
                        name = name.substring(index + 1);
                    }
                }
                return name;
            }
        }
        return "null";

    } // getDriverName():String

    /**
     * Check the protocol used in the systemId against allowed protocols
     *
     * @param systemId the Id of the URI
     * @param allowedProtocols a list of allowed protocols separated by comma
     * @return the name of the protocol if rejected, null otherwise
     */
    String checkAccess(String systemId, String allowedProtocols) throws IOException {
        String baseSystemId = fEntityScanner.getBaseSystemId();
        String expandedSystemId = XMLEntityManager.expandSystemId(systemId, baseSystemId, fStrictURI);
        return SecuritySupport.checkAccess(expandedSystemId, allowedProtocols, JdkConstants.ACCESS_EXTERNAL_ALL);
    }

    //
    // Classes
    //

    /**
     * @author Neeraj Bajaj, Sun Microsystems.
     */
    protected static final class Element {

        //
        // Data
        //

        /** Symbol. */
        public QName qname;

        //raw name stored as characters
        public char[] fRawname;

        /** The next Element entry. */
        public Element next;

        //
        // Constructors
        //

        /**
         * Constructs a new Element from the given QName and next Element
         * reference.
         */
        public Element(QName qname, Element next) {
            this.qname.setValues(qname);
            this.fRawname = qname.rawname.toCharArray();
            this.next = next;
        }

    } // class Element

    /**
     * Element stack.
     *
     * @author Neeraj Bajaj, Sun Microsystems.
     */
    protected class ElementStack2 {

        //
        // Data
        //

        /** The stack data. */
        protected QName [] fQName = new QName[20];

        //Element depth
        protected int fDepth;
        //total number of elements
        protected int fCount;
        //current position
        protected int fPosition;
        //Mark refers to the position
        protected int fMark;

        protected int fLastDepth ;

        //
        // Constructors
        //

        /** Default constructor. */
        public ElementStack2() {
            for (int i = 0; i < fQName.length; i++) {
                fQName[i] = new QName();
            }
            fMark = fPosition = 1;
        } // <init>()

        public void resize(){
            /**
             * int length = fElements.length;
             * Element [] temp = new Element[length * 2];
             * System.arraycopy(fElements, 0, temp, 0, length);
             * fElements = temp;
             */
            //resize QNames
            int oldLength = fQName.length;
            QName [] tmp = new QName[oldLength * 2];
            System.arraycopy(fQName, 0, tmp, 0, oldLength);
            fQName = tmp;

            for (int i = oldLength; i < fQName.length; i++) {
                fQName[i] = new QName();
            }

        }


        //
        // Public methods
        //

        /** Check if the element scanned during the start element
         *matches the stored element.
         *
         *@return true if the match suceeds.
         */
        public boolean matchElement(QName element) {
            //last depth is the depth when last elemnt was pushed
            //if last depth is greater than current depth
            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("fLastDepth = " + fLastDepth);
                System.out.println("fDepth = " + fDepth);
            }
            boolean match = false;
            if(fLastDepth > fDepth && fDepth <= 2){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Checking if the elements match " + element.rawname + " , " + fQName[fDepth].rawname);
                }
                if(element.rawname == fQName[fDepth].rawname){
                    fAdd = false;
                    //mark this position
                    //decrease the depth by 1 as arrays are 0 based
                    fMark = fDepth - 1;
                    //we found the match and from next element skipping will start, add 1
                    fPosition = fMark + 1 ;
                    match = true;
                    //Once we get match decrease the count -- this was increased by nextElement()
                    --fCount;
                    if(DEBUG_SKIP_ALGORITHM){
                        System.out.println("fAdd FALSE -- NOW ELEMENT SHOULD NOT BE ADDED");
                        System.out.println("fMark = " + fMark);
                        System.out.println("fPosition = " + fPosition);
                        System.out.println("fDepth = " + fDepth);
                        System.out.println("fCount = " + fCount);
                    }
                }else{
                    fAdd = true;
                    if(DEBUG_SKIP_ALGORITHM)System.out.println("fAdd is " + fAdd);
                }
            }
            //store the last depth
            fLastDepth = fDepth++;
            return match;
        } // pushElement(QName):QName

        /**
         * This function doesn't increase depth. The function in this function is
         *broken down into two functions for efficiency. <@see>matchElement</see>.
         * This function just returns the pointer to the object and its values are set.
         *
         *@return QName reference to the next element in the list
         */
        public QName nextElement() {

            //if number of elements becomes equal to the length of array -- stop the skipping
            if (fCount == fQName.length) {
                fShouldSkip = false;
                fAdd = false;
                if(DEBUG_SKIP_ALGORITHM)System.out.println("SKIPPING STOPPED, fShouldSkip = " + fShouldSkip);
                //xxx: this is not correct, we are returning the last element
                //this wont make any difference since flag has been set to 'false'
                return fQName[--fCount];
            }
            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("fCount = " + fCount);
            }
            return fQName[fCount++];

        }

        /** Note that this function is considerably different than nextElement()
         * This function just returns the previously stored elements
         */
        public QName getNext(){
            //when position reaches number of elements in the list..
            //set the position back to mark,  making it a circular linked list.
            if(fPosition == fCount){
                fPosition = fMark;
            }
            return fQName[fPosition++];
        }

        /** returns the current depth
         */
        public int popElement(){
            return fDepth--;
        }


        /** Clears the stack without throwing away existing QName objects. */
        public void clear() {
            fLastDepth = 0;
            fDepth = 0;
            fCount = 0 ;
            fPosition = fMark = 1;
        } // clear()

    } // class ElementStack

    /**
     * Element stack. This stack operates without synchronization, error
     * checking, and it re-uses objects instead of throwing popped items
     * away.
     *
     * @author Andy Clark, IBM
     */
    protected class ElementStack {

        //
        // Data
        //

        /** The stack data. */
        protected QName[] fElements;
        protected int []  fInt = new int[20];


        //Element depth
        protected int fDepth;
        //total number of elements
        protected int fCount;
        //current position
        protected int fPosition;
        //Mark refers to the position
        protected int fMark;

        protected int fLastDepth ;

        //
        // Constructors
        //

        /** Default constructor. */
        public ElementStack() {
            fElements = new QName[20];
            for (int i = 0; i < fElements.length; i++) {
                fElements[i] = new QName();
            }
        } // <init>()

        //
        // Public methods
        //

        /**
         * Pushes an element on the stack.
         * <p>
         * <strong>Note:</strong> The QName values are copied into the
         * stack. In other words, the caller does <em>not</em> orphan
         * the element to the stack. Also, the QName object returned
         * is <em>not</em> orphaned to the caller. It should be
         * considered read-only.
         *
         * @param element The element to push onto the stack.
         *
         * @return Returns the actual QName object that stores the
         */
        //XXX: THIS FUNCTION IS NOT USED
        public QName pushElement(QName element) {
            if (fDepth == fElements.length) {
                QName[] array = new QName[fElements.length * 2];
                System.arraycopy(fElements, 0, array, 0, fDepth);
                fElements = array;
                for (int i = fDepth; i < fElements.length; i++) {
                    fElements[i] = new QName();
                }
            }
            fElements[fDepth].setValues(element);
            return fElements[fDepth++];
        } // pushElement(QName):QName


        /** Note that this function is considerably different than nextElement()
         * This function just returns the previously stored elements
         */
        public QName getNext(){
            //when position reaches number of elements in the list..
            //set the position back to mark,  making it a circular linked list.
            if(fPosition == fCount){
                fPosition = fMark;
            }
            //store the position of last opened tag at particular depth
            //fInt[++fDepth] = fPosition;
            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("Element at fPosition = " + fPosition + " is " + fElements[fPosition].rawname);
            }
            //return fElements[fPosition++];
            return fElements[fPosition];
        }

        /** This function should be called only when element was skipped sucessfully.
         * 1. Increase the depth - because element was sucessfully skipped.
         *2. Store the position of the element token in array  "last opened tag" at depth.
         *3. increase the position counter so as to point to the next element in the array
         */
        public void push(){

            fInt[++fDepth] = fPosition++;
        }

        /** Check if the element scanned during the start element
         *matches the stored element.
         *
         *@return true if the match suceeds.
         */
        public boolean matchElement(QName element) {
            //last depth is the depth when last elemnt was pushed
            //if last depth is greater than current depth
            //if(DEBUG_SKIP_ALGORITHM){
            //   System.out.println("Check if the element " + element.rawname + " matches");
            //  System.out.println("fLastDepth = " + fLastDepth);
            // System.out.println("fDepth = " + fDepth);
            //}
            boolean match = false;
            if(fLastDepth > fDepth && fDepth <= 3){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("----------ENTERED THE LOOP WHERE WE CHECK FOR MATCHING OF ELMENT-----");
                    System.out.println("Depth = " + fDepth + " Checking if INCOMING element " + element.rawname + " match STORED ELEMENT " + fElements[fDepth - 1].rawname);
                }
                if(element.rawname == fElements[fDepth - 1].rawname){
                    fAdd = false;
                    //mark this position
                    //decrease the depth by 1 as arrays are 0 based
                    fMark = fDepth - 1;
                    //we found the match
                    fPosition = fMark;
                    match = true;
                    //Once we get match decrease the count -- this was increased by nextElement()
                    --fCount;
                    if(DEBUG_SKIP_ALGORITHM){
                        System.out.println("NOW ELEMENT SHOULD NOT BE ADDED, fAdd is set to false");
                        System.out.println("fMark = " + fMark);
                        System.out.println("fPosition = " + fPosition);
                        System.out.println("fDepth = " + fDepth);
                        System.out.println("fCount = " + fCount);
                        System.out.println("---------MATCH SUCEEDED-----------------");
                        System.out.println("");
                    }
                }else{
                    fAdd = true;
                    if(DEBUG_SKIP_ALGORITHM)System.out.println("fAdd is " + fAdd);
                }
            }
            //store the position for the current depth
            //when we are adding the elements, when skipping
            //starts even then this should be tracked ie. when
            //calling getNext()
            if(match){
                //from next element skipping will start, add 1
                fInt[fDepth] = fPosition++;
            } else{
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("At depth = " + fDepth + "array position is = " + (fCount - 1));
                }
                //sicne fInt[fDepth] contains pointer to the element array which are 0 based.
                fInt[fDepth] = fCount - 1;
            }

            //if number of elements becomes equal to the length of array -- stop the skipping
            //xxx: should we do "fCount == fInt.length"
            if (fCount == fElements.length) {
                fSkip = false;
                fAdd = false;
                //reposition the stack -- it seems to be too complex document and there is no symmerty in structure
                reposition();
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("ALL THE ELMENTS IN ARRAY HAVE BEEN FILLED");
                    System.out.println("REPOSITIONING THE STACK");
                    System.out.println("-----------SKIPPING STOPPED----------");
                    System.out.println("");
                }
                return false;
            }
            if(DEBUG_SKIP_ALGORITHM){
                if(match){
                    System.out.println("Storing fPosition = " + fInt[fDepth] + " at fDepth = " + fDepth);
                }else{
                    System.out.println("Storing fCount = " + fInt[fDepth] + " at fDepth = " + fDepth);
                }
            }
            //store the last depth
            fLastDepth = fDepth;
            return match;
        } // matchElement(QName):QName


        /**
         * Returns the next element on the stack.
         *
         * @return Returns the actual QName object. Callee should
         * use this object to store the details of next element encountered.
         */
        public QName nextElement() {
            if(fSkip){
                fDepth++;
                //boundary checks are done in matchElement()
                return fElements[fCount++];
            } else if (fDepth == fElements.length) {
                QName[] array = new QName[fElements.length * 2];
                System.arraycopy(fElements, 0, array, 0, fDepth);
                fElements = array;
                for (int i = fDepth; i < fElements.length; i++) {
                    fElements[i] = new QName();
                }
            }

            return fElements[fDepth++];

        } // pushElement(QName):QName


        /**
         * Pops an element off of the stack by setting the values of
         * the specified QName.
         * <p>
         * <strong>Note:</strong> The object returned is <em>not</em>
         * orphaned to the caller. Therefore, the caller should consider
         * the object to be read-only.
         */
        public QName popElement() {
            //return the same object that was pushed -- this would avoid
            //setting the values for every end element.
            //STRONG: this object is read only -- this object reference shouldn't be stored.
            if(fSkip || fAdd ){
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("POPPING Element, at position " + fInt[fDepth] + " element at that count is = " + fElements[fInt[fDepth]].rawname);
                    System.out.println("");
                }
                return fElements[fInt[fDepth--]];
            } else{
                if(DEBUG_SKIP_ALGORITHM){
                    System.out.println("Retrieveing element at depth = " + fDepth + " is " + fElements[fDepth].rawname );
                }
                return fElements[--fDepth] ;
            }
            //element.setValues(fElements[--fDepth]);
        } // popElement(QName)

        /** Reposition the stack. fInt [] contains all the opened tags at particular depth.
         * Transfer all the opened tags starting from depth '2' to the current depth and reposition them
         *as per the depth.
         */
        public void reposition(){
            for( int i = 2 ; i <= fDepth ; i++){
                fElements[i-1] = fElements[fInt[i]];
            }
            if(DEBUG_SKIP_ALGORITHM){
                for( int i = 0 ; i < fDepth ; i++){
                    System.out.println("fElements[" + i + "]" + " = " + fElements[i].rawname);
                }
            }
        }

        /** Clears the stack without throwing away existing QName objects. */
        public void clear() {
            fDepth = 0;
            fLastDepth = 0;
            fCount = 0 ;
            fPosition = fMark = 1;

        } // clear()

        /**
         * This function is as a result of optimization done for endElement --
         * we dont need to set the value for every end element encouterd.
         * For Well formedness checks we can have the same QName object that was pushed.
         * the values will be set only if application need to know about the endElement
         */

        public QName getLastPoppedElement(){
            return fElements[fDepth];
        }
    } // class ElementStack

    /**
     * Drives the parser to the next state/event on the input. Parser is guaranteed
     * to stop at the next state/event.
     *
     * Internally XML document is divided into several states. Each state represents
     * a sections of XML document. When this functions returns normally, it has read
     * the section of XML document and returns the state corresponding to section of
     * document which has been read. For optimizations, a particular driver
     * can read ahead of the section of document (state returned) just read and
     * can maintain a different internal state.
     *
     *
     * @author Neeraj Bajaj, Sun Microsystems
     */
    protected interface Driver {


        /**
         * Drives the parser to the next state/event on the input. Parser is guaranteed
         * to stop at the next state/event.
         *
         * Internally XML document is divided into several states. Each state represents
         * a sections of XML document. When this functions returns normally, it has read
         * the section of XML document and returns the state corresponding to section of
         * document which has been read. For optimizations, a particular driver
         * can read ahead of the section of document (state returned) just read and
         * can maintain a different internal state.
         *
         * @return state representing the section of document just read.
         *
         * @throws IOException  Thrown on i/o error.
         * @throws XNIException Thrown on parse error.
         */

        public int next() throws IOException, XNIException;

    } // interface Driver

    /**
     * Driver to handle content scanning. This driver is capable of reading
     * the fragment of XML document. When it has finished reading fragment
     * of XML documents, it can pass the job of reading to another driver.
     *
     * This class has been modified as per the new design which is more suited to
     * efficiently build pull parser. Lot of performance improvements have been done and
     * the code has been added to support stax functionality/features.
     *
     * @author Neeraj Bajaj, Sun Microsystems
     *
     *
     * @author Andy Clark, IBM
     * @author Eric Ye, IBM
     */
    protected class FragmentContentDriver
            implements Driver {

        //
        // Driver methods
        //

        /**
         *  decides the appropriate state of the parser
         */
        private void startOfMarkup() throws IOException {
            fMarkupDepth++;
            final int ch = fEntityScanner.peekChar();
            if (isValidNameStartChar(ch) || isValidNameStartHighSurrogate(ch)) {
                setScannerState(SCANNER_STATE_START_ELEMENT_TAG);
            } else {
                switch(ch){
                    case '?' :{
                        setScannerState(SCANNER_STATE_PI);
                        fEntityScanner.skipChar(ch, null);
                        break;
                    }
                    case '!' :{
                        fEntityScanner.skipChar(ch, null);
                        if (fEntityScanner.skipChar('-', null)) {
                            if (!fEntityScanner.skipChar('-', NameType.COMMENT)) {
                                reportFatalError("InvalidCommentStart",
                                        null);
                            }
                            setScannerState(SCANNER_STATE_COMMENT);
                        } else if (fEntityScanner.skipString(CDATA)) {
                            fCDataStart = true;
                            setScannerState(SCANNER_STATE_CDATA );
                        } else if (!scanForDoctypeHook()) {
                            reportFatalError("MarkupNotRecognizedInContent",
                                    null);
                        }
                        break;
                    }
                    case '/' :{
                        setScannerState(SCANNER_STATE_END_ELEMENT_TAG);
                        fEntityScanner.skipChar(ch, NameType.ELEMENTEND);
                        break;
                    }
                    default :{
                        reportFatalError("MarkupNotRecognizedInContent", null);
                    }
                }
            }

        }//startOfMarkup

        private void startOfContent() throws IOException {
            if (fEntityScanner.skipChar('<', null)) {
                setScannerState(SCANNER_STATE_START_OF_MARKUP);
            } else if (fEntityScanner.skipChar('&', NameType.REFERENCE)) {
                setScannerState(SCANNER_STATE_REFERENCE) ; //XMLEvent.ENTITY_REFERENCE ); //SCANNER_STATE_REFERENCE
            } else {
                //element content is there..
                setScannerState(SCANNER_STATE_CHARACTER_DATA);
            }
        }//startOfContent


        /**
         *
         * SCANNER_STATE_CONTENT and SCANNER_STATE_START_OF_MARKUP are two super states of the parser.
         * At any point of time when in doubt over the current state of the parser, the state should be
         * set to SCANNER_STATE_CONTENT. Parser will automatically revive itself and will set state of
         * the parser to one of its sub state.
         * sub states are defined in the parser on the basis of different XML component like
         * SCANNER_STATE_ENTITY_REFERENCE , SCANNER_STATE_START_ELEMENT, SCANNER_STATE_CDATA etc..
         * These sub states help the parser to have fine control over the parsing. These are the
         * different milepost, parser stops at each sub state (milepost). Based on this state it is
         * decided if paresr needs to stop at next milepost ??
         *
         */
        public void decideSubState() throws IOException {
            while( fScannerState == SCANNER_STATE_CONTENT || fScannerState == SCANNER_STATE_START_OF_MARKUP){

                switch (fScannerState) {

                    case SCANNER_STATE_CONTENT: {
                        startOfContent() ;
                        break;
                    }

                    case SCANNER_STATE_START_OF_MARKUP: {
                        startOfMarkup() ;
                        break;
                    }
                }
            }
        }//decideSubState

        /**
         * Drives the parser to the next state/event on the input. Parser is guaranteed
         * to stop at the next state/event. Internally XML document
         * is divided into several states. Each state represents a sections of XML
         * document. When this functions returns normally, it has read the section
         * of XML document and returns the state corresponding to section of
         * document which has been read. For optimizations, a particular driver
         * can read ahead of the section of document (state returned) just read and
         * can maintain a different internal state.
         *
         * State returned corresponds to Stax states.
         *
         * @return state representing the section of document just read.
         *
         * @throws IOException  Thrown on i/o error.
         * @throws XNIException Thrown on parse error.
         */

        public int next() throws IOException, XNIException {
            while (true) {
            try {

                //decide the actual sub state of the scanner.For more information refer to the javadoc of
                //decideSubState.

                if (fScannerState == SCANNER_STATE_CONTENT) {
                    final int ch = fEntityScanner.peekChar();
                    if (ch == '<') {
                        fEntityScanner.scanChar(null);
                        setScannerState(SCANNER_STATE_START_OF_MARKUP);
                    } else if (ch == '&') {
                        fEntityScanner.scanChar(NameType.REFERENCE);
                        setScannerState(SCANNER_STATE_REFERENCE) ;
                    } else {
                        //element content is there..
                        setScannerState(SCANNER_STATE_CHARACTER_DATA);
                    }
                }

                if (fScannerState == SCANNER_STATE_START_OF_MARKUP) {
                    startOfMarkup();
                }

                //decideSubState() ;

                //do some special handling if isCoalesce is set to true.
                if (fIsCoalesce) {
                    fUsebuffer = true ;
                    //if the last section was character data
                    if (fLastSectionWasCharacterData) {

                        //if we dont encounter any CDATA or ENTITY REFERENCE and
                        //current state is also not SCANNER_STATE_CHARACTER_DATA
                        //return the last scanned charactrer data.
                        if ((fScannerState != SCANNER_STATE_CDATA)
                                && (fScannerState != SCANNER_STATE_REFERENCE)
                                && (fScannerState != SCANNER_STATE_CHARACTER_DATA)) {
                            fLastSectionWasCharacterData = false;
                            return XMLEvent.CHARACTERS;
                        }
                    }//if last section was CDATA or ENTITY REFERENCE
                    //xxx: there might be another entity reference or CDATA after this
                    //<foo>blah blah &amp;&lt;<![CDATA[[aa]]>blah blah</foo>
                    else if ((fLastSectionWasCData || fLastSectionWasEntityReference)) {
                        //and current state is not SCANNER_STATE_CHARACTER_DATA
                        //or SCANNER_STATE_CDATA or SCANNER_STATE_REFERENCE
                        //this means there is nothing more to be coalesced.
                        //return the CHARACTERS event.
                        if ((fScannerState != SCANNER_STATE_CDATA)
                                && (fScannerState != SCANNER_STATE_REFERENCE)
                                && (fScannerState != SCANNER_STATE_CHARACTER_DATA)){

                            fLastSectionWasCData = false;
                            fLastSectionWasEntityReference = false;
                            return XMLEvent.CHARACTERS;
                        }
                    }
                }

                switch(fScannerState){

                    case XMLEvent.START_DOCUMENT :
                        return XMLEvent.START_DOCUMENT;

                    case SCANNER_STATE_START_ELEMENT_TAG :{

                        //returns true if the element is empty
                        fEmptyElement = scanStartElement() ;
                        //if the element is empty the next event is "end element"
                        if(fEmptyElement){
                            setScannerState(SCANNER_STATE_END_ELEMENT_TAG);
                        }else{
                            //set the next possible state
                            setScannerState(SCANNER_STATE_CONTENT);
                        }
                        return XMLEvent.START_ELEMENT ;
                    }

                    case SCANNER_STATE_CHARACTER_DATA: {

                        //if last section was either entity reference or cdata or
                        //character data we should be using buffer
                        fUsebuffer = fLastSectionWasEntityReference || fLastSectionWasCData
                                || fLastSectionWasCharacterData ;

                        //When coalesce is set to true and last state was REFERENCE or
                        //CDATA or CHARACTER_DATA, buffer should not be cleared.
                        if( fIsCoalesce && (fLastSectionWasEntityReference ||
                                fLastSectionWasCData || fLastSectionWasCharacterData) ){
                            fLastSectionWasEntityReference = false;
                            fLastSectionWasCData = false;
                            fLastSectionWasCharacterData = true ;
                            fUsebuffer = true;
                        }else{
                            //clear the buffer
                            fContentBuffer.clear();
                        }

                        //set the fTempString length to 0 before passing it on to scanContent
                        //scanContent sets the correct co-ordinates as per the content read
                        fTempString.length = 0;
                        int c = fEntityScanner.scanContent(fTempString);

                        if(fEntityScanner.skipChar('<', null)){
                            //check if we have reached end of element
                            if(fEntityScanner.skipChar('/', NameType.ELEMENTEND)){
                                //increase the mark up depth
                                fMarkupDepth++;
                                fLastSectionWasCharacterData = false;
                                setScannerState(SCANNER_STATE_END_ELEMENT_TAG);
                                //check if its start of new element
                            }else if(XMLChar.isNameStart(fEntityScanner.peekChar())){
                                fMarkupDepth++;
                                fLastSectionWasCharacterData = false;
                                setScannerState(SCANNER_STATE_START_ELEMENT_TAG);
                            }else{
                                setScannerState(SCANNER_STATE_START_OF_MARKUP);
                                //there can be cdata ahead if coalesce is true we should call again
                                if(fIsCoalesce){
                                    fLastSectionWasCharacterData = true;
                                    bufferContent();
                                    continue;
                                }
                            }
                            //in case last section was either entity reference or
                            //cdata or character data -- we should be using buffer
                            if(fUsebuffer){
                                bufferContent();
                            }

                            if(dtdGrammarUtil!= null && dtdGrammarUtil.isIgnorableWhiteSpace(fContentBuffer)){
                                if(DEBUG)System.out.println("Return SPACE EVENT");
                                return XMLEvent.SPACE;
                            }else
                                return XMLEvent.CHARACTERS;

                        } else{
                            bufferContent();
                        }
                        if (c == '\r') {
                            if(DEBUG){
                                System.out.println("'\r' character found");
                            }
                            // happens when there is the character reference &#13;
                            //xxx: We know the next chracter.. we should just skip it and add ']' directlry
                            fEntityScanner.scanChar(null);
                            fUsebuffer = true;
                            fContentBuffer.append((char)c);
                            c = -1 ;
                        } else if (c == ']') {
                            //fStringBuffer.clear();
                            //xxx: We know the next chracter.. we should just skip it and add ']' directlry
                            fUsebuffer = true;
                            fContentBuffer.append((char)fEntityScanner.scanChar(null));
                            // remember where we are in case we get an endEntity before we
                            // could flush the buffer out - this happens when we're parsing an
                            // entity which ends with a ]
                            fInScanContent = true;

                            // We work on a single character basis to handle cases such as:
                            // ']]]>' which we might otherwise miss.
                            //
                            if (fEntityScanner.skipChar(']', null)) {
                                fContentBuffer.append(']');
                                while (fEntityScanner.skipChar(']', null)) {
                                    fContentBuffer.append(']');
                                }
                                if (fEntityScanner.skipChar('>', null)) {
                                    reportFatalError("CDEndInContent", null);
                                }
                            }
                            c = -1 ;
                            fInScanContent = false;
                        }

                        do{
                            //xxx: we should be using only one buffer..
                            // we need not to grow the buffer only when isCoalesce() is not true;

                            if (c == '<') {
                                fEntityScanner.scanChar(null);
                                setScannerState(SCANNER_STATE_START_OF_MARKUP);
                                break;
                            }//xxx what should be the behavior if entity reference is present in the content ?
                            else if (c == '&') {
                                fEntityScanner.scanChar(NameType.REFERENCE);
                                setScannerState(SCANNER_STATE_REFERENCE);
                                break;
                            }///xxx since this part is also characters, it should be merged...
                            else if (c != -1 && isInvalidLiteral(c)) {
                                if (XMLChar.isHighSurrogate(c)) {
                                    // special case: surrogates
                                    scanSurrogates(fContentBuffer) ;
                                    setScannerState(SCANNER_STATE_CONTENT);
                                } else {
                                    reportFatalError("InvalidCharInContent",
                                            new Object[] {
                                        Integer.toString(c, 16)});
                                        fEntityScanner.scanChar(null);
                                }
                                break;
                            }
                            //xxx: scanContent also gives character callback.
                            c = scanContent(fContentBuffer) ;
                            //we should not be iterating again if fIsCoalesce is not set to true

                            if(!fIsCoalesce){
                                setScannerState(SCANNER_STATE_CONTENT);
                                break;
                            }

                        }while(true);

                        //if (fDocumentHandler != null) {
                        //  fDocumentHandler.characters(fContentBuffer, null);
                        //}
                        if(DEBUG)System.out.println("USING THE BUFFER, STRING START=" + fContentBuffer.toString() +"=END");
                        //if fIsCoalesce is true there might be more data so call fDriver.next()
                        if(fIsCoalesce){
                            fLastSectionWasCharacterData = true ;
                            continue;
                        }else{
                            if(dtdGrammarUtil!= null && dtdGrammarUtil.isIgnorableWhiteSpace(fContentBuffer)){
                                if(DEBUG)System.out.println("Return SPACE EVENT");
                                return XMLEvent.SPACE;
                            } else
                                return XMLEvent.CHARACTERS ;
                        }
                    }

                    case SCANNER_STATE_END_ELEMENT_TAG :{
                        if(fEmptyElement){
                            //set it back to false.
                            fEmptyElement = false;
                            setScannerState(SCANNER_STATE_CONTENT);
                            //check the case when there is comment after single element document
                            //<foo/> and some comment after this
                            return (fMarkupDepth == 0 && elementDepthIsZeroHook() ) ?
                                    XMLEvent.END_ELEMENT : XMLEvent.END_ELEMENT ;

                        } else if(scanEndElement() == 0) {
                            //It is last element of the document
                            if (elementDepthIsZeroHook()) {
                                //if element depth is zero , it indicates the end of the document
                                //the state shouldn't be set, because it is set by elementDepthIsZeroHook() function
                                //xxx understand this point once again..
                                return XMLEvent.END_ELEMENT ;
                            }

                        }
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.END_ELEMENT ;
                    }

                    case SCANNER_STATE_COMMENT: { //SCANNER_STATE_COMMENT:
                        scanComment();
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.COMMENT;
                        //break;
                    }
                    case SCANNER_STATE_PI:{ //SCANNER_STATE_PI: {
                        //clear the buffer first
                        fContentBuffer.clear() ;
                        //xxx: which buffer should be passed. Ideally we shouldn't have
                        //more than two buffers --
                        //xxx: where should we add the switch for buffering.
                        scanPI(fContentBuffer);
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.PROCESSING_INSTRUCTION;
                        //break;
                    }
                    case SCANNER_STATE_CDATA :{ //SCANNER_STATE_CDATA: {
                        //xxx: What if CDATA is the first event
                        //<foo><![CDATA[hello<><>]]>append</foo>

                        //we should not clear the buffer only when the last state was
                        //either SCANNER_STATE_REFERENCE or
                        //SCANNER_STATE_CHARACTER_DATA or SCANNER_STATE_REFERENCE
                        if(fIsCoalesce && ( fLastSectionWasEntityReference ||
                                fLastSectionWasCData || fLastSectionWasCharacterData)){
                            fLastSectionWasCData = true ;
                            fLastSectionWasEntityReference = false;
                            fLastSectionWasCharacterData = false;
                        }//if we dont need to coalesce clear the buffer
                        else{
                            fContentBuffer.clear();
                        }
                        fUsebuffer = true;
                        //CDATA section is read up to the chunk size limit
                        scanCDATASection(fContentBuffer , true);
                        if (!fCDataEnd) {
                            setScannerState(SCANNER_STATE_CDATA);
                        } else {
                            setScannerState(SCANNER_STATE_CONTENT);
                        }
                        //1. if fIsCoalesce is set to true we set the variable fLastSectionWasCData to true
                        //and just call fDispatche.next(). Since we have set the scanner state to
                        //SCANNER_STATE_CONTENT (super state) parser will automatically recover and
                        //behave appropriately. When isCoalesce is set to true we dont need to reportCDATA event
                        //2. Check if application has set for reporting CDATA event
                        //3. if the application has neither set the fIsCoalesce to true nor fReportCdataEvent
                        //return the cdata event as characters.
                        if (fIsCoalesce) {
                            fLastSectionWasCData = true ;
                            //there might be more data to coalesce.
                            continue;
                        } else if(fReportCdataEvent) {
                            return XMLEvent.CDATA;
                        } else {
                            return XMLEvent.CHARACTERS;
                        }
                    }

                    case SCANNER_STATE_REFERENCE :{
                        fMarkupDepth++;
                        foundBuiltInRefs = false;

                        //we should not clear the buffer only when the last state was
                        //either CDATA or
                        //SCANNER_STATE_CHARACTER_DATA or SCANNER_STATE_REFERENCE
                        if(fIsCoalesce && ( fLastSectionWasEntityReference ||
                                fLastSectionWasCData || fLastSectionWasCharacterData)){
                            //fLastSectionWasEntityReference or fLastSectionWasCData are only
                            //used when fIsCoalesce is set to true.
                            fLastSectionWasEntityReference = true ;
                            fLastSectionWasCData = false;
                            fLastSectionWasCharacterData = false;
                        }//if we dont need to coalesce clear the buffer
                        else{
                            fContentBuffer.clear();
                        }
                        fUsebuffer = true ;
                        //take care of character reference
                        if (fEntityScanner.skipChar('#', NameType.REFERENCE)) {
                            scanCharReferenceValue(fContentBuffer, null);
                            fMarkupDepth--;
                            if(!fIsCoalesce){
                                setScannerState(SCANNER_STATE_CONTENT);
                                return XMLEvent.CHARACTERS;
                            }
                        } else {
                            // this function also starts new entity
                            scanEntityReference(fContentBuffer);
                            //if there was built-in entity reference & coalesce is not true
                            //return CHARACTERS
                            if(fScannerState == SCANNER_STATE_BUILT_IN_REFS && !fIsCoalesce){
                                setScannerState(SCANNER_STATE_CONTENT);
                                if (builtInRefCharacterHandled) {
                                    builtInRefCharacterHandled = false;
                                    return XMLEvent.ENTITY_REFERENCE;
                                } else {
                                    return XMLEvent.CHARACTERS;
                                }
                            }

                            //if there was a text declaration, call next() it will be taken care.
                            if(fScannerState == SCANNER_STATE_TEXT_DECL){
                                fLastSectionWasEntityReference = true ;
                                continue;
                            }

                            if(fScannerState == SCANNER_STATE_REFERENCE){
                                setScannerState(SCANNER_STATE_CONTENT);
                                if (fReplaceEntityReferences &&
                                        fEntityStore.isDeclaredEntity(fCurrentEntityName)) {
                                    // Skip the entity reference, we don't care
                                    continue;
                                }
                                return XMLEvent.ENTITY_REFERENCE;
                            }
                        }
                        //Whether it was character reference, entity reference or built-in entity
                        //set the next possible state to SCANNER_STATE_CONTENT
                        setScannerState(SCANNER_STATE_CONTENT);
                        fLastSectionWasEntityReference = true ;
                        continue;
                    }

                    case SCANNER_STATE_TEXT_DECL: {
                        // scan text decl
                        if (fEntityScanner.skipString("<?xml")) {
                            fMarkupDepth++;
                            // NOTE: special case where entity starts with a PI
                            //       whose name starts with "xml" (e.g. "xmlfoo")
                            if (isValidNameChar(fEntityScanner.peekChar())) {
                                fStringBuffer.clear();
                                fStringBuffer.append("xml");

                                if (fNamespaces) {
                                    while (isValidNCName(fEntityScanner.peekChar())) {
                                        fStringBuffer.append((char)fEntityScanner.scanChar(null));
                                    }
                                } else {
                                    while (isValidNameChar(fEntityScanner.peekChar())) {
                                        fStringBuffer.append((char)fEntityScanner.scanChar(null));
                                    }
                                }
                                String target = fSymbolTable.addSymbol(fStringBuffer.ch,
                                        fStringBuffer.offset, fStringBuffer.length);
                                fContentBuffer.clear();
                                scanPIData(target, fContentBuffer);
                            }

                            // standard text declaration
                            else {
                                //xxx: this function gives callback
                                scanXMLDeclOrTextDecl(true);
                            }
                        }
                        // now that we've straightened out the readers, we can read in chunks:
                        fEntityManager.fCurrentEntity.mayReadChunks = true;
                        setScannerState(SCANNER_STATE_CONTENT);
                        //xxx: we don't return any state, so how do we get to know about TEXT declarations.
                        //it seems we have to careful when to allow function issue a callback
                        //and when to allow adapter issue a callback.
                        continue;
                    }


                    case SCANNER_STATE_ROOT_ELEMENT: {
                        if (scanRootElementHook()) {
                            fEmptyElement = true;
                            //rest would be taken care by fTrailingMiscDriver set by scanRootElementHook
                            return XMLEvent.START_ELEMENT;
                        }
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.START_ELEMENT ;
                    }
                    case SCANNER_STATE_CHAR_REFERENCE : {
                        fContentBuffer.clear();
                        scanCharReferenceValue(fContentBuffer, null);
                        fMarkupDepth--;
                        setScannerState(SCANNER_STATE_CONTENT);
                        return XMLEvent.CHARACTERS;
                    }
                    default:
                        throw new XNIException("Scanner State " + fScannerState + " not Recognized ");

                }//switch
            }
             // encoding errors
             catch (MalformedByteSequenceException e) {
                 fErrorReporter.reportError(e.getDomain(), e.getKey(),
                    e.getArguments(), XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                 return -1;
             }
             catch (CharConversionException e) {
                fErrorReporter.reportError(
                        XMLMessageFormatter.XML_DOMAIN,
                        "CharConversionFailure",
                        null,
                        XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                 return -1;
             }
            // premature end of file
            catch (EOFException e) {
                endOfFileHook(e);
                return -1;
            }
            } //while loop
        }//next

        //
        // Protected methods
        //

        // hooks

        // NOTE: These hook methods are added so that the full document
        //       scanner can share the majority of code with this class.

        /**
         * Scan for DOCTYPE hook. This method is a hook for subclasses
         * to add code to handle scanning for a the "DOCTYPE" string
         * after the string "<!" has been scanned.
         *
         * @return True if the "DOCTYPE" was scanned; false if "DOCTYPE"
         *          was not scanned.
         */
        protected boolean scanForDoctypeHook()
        throws IOException, XNIException {
            return false;
        } // scanForDoctypeHook():boolean

        /**
         * Element depth iz zero. This methos is a hook for subclasses
         * to add code to handle when the element depth hits zero. When
         * scanning a document fragment, an element depth of zero is
         * normal. However, when scanning a full XML document, the
         * scanner must handle the trailing miscellanous section of
         * the document after the end of the document's root element.
         *
         * @return True if the caller should stop and return true which
         *          allows the scanner to switch to a new scanning
         *          driver. A return value of false indicates that
         *          the content driver should continue as normal.
         */
        protected boolean elementDepthIsZeroHook()
        throws IOException, XNIException {
            return false;
        } // elementDepthIsZeroHook():boolean

        /**
         * Scan for root element hook. This method is a hook for
         * subclasses to add code that handles scanning for the root
         * element. When scanning a document fragment, there is no
         * "root" element. However, when scanning a full XML document,
         * the scanner must handle the root element specially.
         *
         * @return True if the caller should stop and return true which
         *          allows the scanner to switch to a new scanning
         *          driver. A return value of false indicates that
         *          the content driver should continue as normal.
         */
        protected boolean scanRootElementHook()
        throws IOException, XNIException {
            return false;
        } // scanRootElementHook():boolean

        /**
         * End of file hook. This method is a hook for subclasses to
         * add code that handles the end of file. The end of file in
         * a document fragment is OK if the markup depth is zero.
         * However, when scanning a full XML document, an end of file
         * is always premature.
         */
        protected void endOfFileHook(EOFException e)
        throws IOException, XNIException {

            // NOTE: An end of file is only only an error if we were
            //       in the middle of scanning some markup. -Ac
            if (fMarkupDepth != 0) {
                reportFatalError("PrematureEOF", null);
            }

        } // endOfFileHook()

    } // class FragmentContentDriver

    static void pr(String str) {
        System.out.println(str) ;
    }

    protected boolean fUsebuffer ;

    /** this function gets an XMLString (which is used to store the attribute value) from the special pool
     *  maintained for attributes.
     *  fAttributeCacheUsedCount tracks the number of attributes that has been consumed from the pool.
     *  if all the attributes has been consumed, it adds a new XMLString inthe pool and returns the same
     *  XMLString.
     *
     * @return XMLString XMLString used to store an attribute value.
     */

    protected XMLString getString(){
        if(fAttributeCacheUsedCount < initialCacheCount ||
                fAttributeCacheUsedCount < attributeValueCache.size()){
            return attributeValueCache.get(fAttributeCacheUsedCount++);
        } else{
            XMLString str = new XMLString();
            fAttributeCacheUsedCount++;
            attributeValueCache.add(str);
            return str;
        }
    }

    /**
     * Implements XMLBufferListener interface.
     */

    public void refresh(){
        refresh(0);
    }

    /**
     * receives callbacks from {@link XMLEntityReader } when buffer
     * is being changed.
     * @param refreshPosition
     */
    public void refresh(int refreshPosition){
        //If you are reading attributes and you got a callback
        //cache available attributes.
        if(fReadingAttributes){
            fAttributes.refresh();
        }
        if(fScannerState == SCANNER_STATE_CHARACTER_DATA){
            bufferContent();
        }
    }

    /**
     * Since 'TempString' shares the buffer (a char array) with the CurrentEntity,
     * when the cursor position reaches the end, that is, before the buffer is
     * being loaded with new data, the content in the TempString needs to be
     * copied into the ContentBuffer.
     */
    private void bufferContent() {
        fContentBuffer.append(fTempString);
        //clear the XMLString so that data can't be added again.
        fTempString.length = 0;
        fUsebuffer = true;
    }
} // class XMLDocumentFragmentScannerImpl
