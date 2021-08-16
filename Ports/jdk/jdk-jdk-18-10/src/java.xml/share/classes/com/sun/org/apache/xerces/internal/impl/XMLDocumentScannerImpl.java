/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDDescription;
import com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.util.NamespaceSupport;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLResourceIdentifierImpl;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.xml.internal.stream.Entity;
import com.sun.xml.internal.stream.StaxXMLInputSource;
import com.sun.xml.internal.stream.dtd.DTDGrammarUtil;
import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import jdk.xml.internal.SecuritySupport;


/**
 * This class is responsible for scanning XML document structure
 * and content.
 *
 * This class has been modified as per the new design which is more suited to
 * efficiently build pull parser. Lot of improvements have been done and
 * the code has been added to support stax functionality/features.
 *
 * @author Neeraj Bajaj, Sun Microsystems
 * @author K.Venugopal, Sun Microsystems
 * @author Glenn Marcy, IBM
 * @author Andy Clark, IBM
 * @author Arnaud  Le Hors, IBM
 * @author Eric Ye, IBM
 * @author Sunitha Reddy, Sun Microsystems
 *
 * Refer to the table in unit-test javax.xml.stream.XMLStreamReaderTest.SupportDTD for changes
 * related to property SupportDTD.
 * @author Joe Wang, Sun Microsystems
 * @LastModified: Sep 2017
 */
public class XMLDocumentScannerImpl
        extends XMLDocumentFragmentScannerImpl{

    //
    // Constants
    //

    // scanner states

    /** Scanner state: XML declaration. */
    protected static final int SCANNER_STATE_XML_DECL = 42;

    /** Scanner state: prolog. */
    protected static final int SCANNER_STATE_PROLOG = 43;

    /** Scanner state: trailing misc. */
    protected static final int SCANNER_STATE_TRAILING_MISC = 44;

    /** Scanner state: DTD internal declarations. */
    protected static final int SCANNER_STATE_DTD_INTERNAL_DECLS = 45;

    /** Scanner state: open DTD external subset. */
    protected static final int SCANNER_STATE_DTD_EXTERNAL = 46;

    /** Scanner state: DTD external declarations. */
    protected static final int SCANNER_STATE_DTD_EXTERNAL_DECLS = 47;

    /** Scanner state: NO MORE ELEMENTS. */
    protected static final int SCANNER_STATE_NO_SUCH_ELEMENT_EXCEPTION = 48;

    // feature identifiers

    /** Property identifier document scanner: */
    protected static final String DOCUMENT_SCANNER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.DOCUMENT_SCANNER_PROPERTY;

    /** Feature identifier: load external DTD. */
    protected static final String LOAD_EXTERNAL_DTD =
            Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE;

    /** Feature identifier: load external DTD. */
    protected static final String DISALLOW_DOCTYPE_DECL_FEATURE =
            Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

    // property identifiers

    /** Property identifier: DTD scanner. */
    protected static final String DTD_SCANNER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.DTD_SCANNER_PROPERTY;

    // property identifier:  ValidationManager
    protected static final String VALIDATION_MANAGER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    /** property identifier:  NamespaceContext */
    protected static final String NAMESPACE_CONTEXT =
        Constants.XERCES_PROPERTY_PREFIX + Constants.NAMESPACE_CONTEXT_PROPERTY;

    // recognized features and properties

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        LOAD_EXTERNAL_DTD,
                DISALLOW_DOCTYPE_DECL_FEATURE,
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
        Boolean.TRUE,
                Boolean.FALSE,
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        DTD_SCANNER,
                VALIDATION_MANAGER
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
            null,
                null
    };

    //
    // Data((Boolean)propertyManager.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE)).booleanValue();
    //

    // properties

    /** DTD scanner. */
    protected XMLDTDScanner fDTDScanner = null;

    /** Validation manager . */
    //xxx: fValidationManager code needs to be added yet!
    protected ValidationManager fValidationManager;

    protected XMLStringBuffer fDTDDecl = null;
    protected boolean fReadingDTD = false;
    protected boolean fAddedListener = false;

    // protected data

    // other info

    /** Doctype name. */
    protected String fDoctypeName;

    /** Doctype declaration public identifier. */
    protected String fDoctypePublicId;

    /** Doctype declaration system identifier. */
    protected String fDoctypeSystemId;

    /** Namespace support. */
    protected NamespaceContext fNamespaceContext = new NamespaceSupport();

    // features

    /** Load external DTD. */
    protected boolean fLoadExternalDTD = true;

    // state

    /** Seen doctype declaration. */
    protected boolean fSeenDoctypeDecl;

    protected boolean fScanEndElement;

    //protected int fScannerLastState ;

    // drivers

    /** XML declaration driver. */
    protected Driver fXMLDeclDriver = new XMLDeclDriver();

    /** Prolog driver. */
    protected Driver fPrologDriver = new PrologDriver();

    /** DTD driver. */
    protected Driver fDTDDriver = null ;

    /** Trailing miscellaneous section driver. */
    protected Driver fTrailingMiscDriver = new TrailingMiscDriver();
    protected int fStartPos = 0;
    protected int fEndPos = 0;
    protected boolean fSeenInternalSubset= false;
    // temporary variables

    /** Array of 3 strings. */
    private String[] fStrings = new String[3];

    /** External subset source. */
    private XMLInputSource fExternalSubsetSource = null;

    /** A DTD Description. */
    private final XMLDTDDescription fDTDDescription = new XMLDTDDescription(null, null, null, null, null);

    private static final char [] DOCTYPE = {'D','O','C','T','Y','P','E'};
    private static final char [] COMMENTSTRING = {'-','-'};

    //
    // Constructors
    //

    /** Default constructor. */
    public XMLDocumentScannerImpl() {} // <init>()


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
        //this starts a new entity and sets the current entity to the document entity.
        fEntityManager.startDocumentEntity(inputSource);
        // fDocumentSystemId = fEntityManager.expandSystemId(inputSource.getSystemId());
        setScannerState(XMLEvent.START_DOCUMENT);
    } // setInputSource(XMLInputSource)



    /**return the state of the scanner */
    public int getScannetState(){
        return fScannerState ;
    }




    public void reset(PropertyManager propertyManager) {
        super.reset(propertyManager);
        // other settings
        fDoctypeName = null;
        fDoctypePublicId = null;
        fDoctypeSystemId = null;
        fSeenDoctypeDecl = false;
        fNamespaceContext.reset();
        fSupportDTD = ((Boolean)propertyManager.getProperty(XMLInputFactory.SUPPORT_DTD)).booleanValue();

        // xerces features
        fLoadExternalDTD = !((Boolean)propertyManager.getProperty(Constants.ZEPHYR_PROPERTY_PREFIX + Constants.IGNORE_EXTERNAL_DTD)).booleanValue();
        setScannerState(XMLEvent.START_DOCUMENT);
        setDriver(fXMLDeclDriver);
        fSeenInternalSubset = false;
        if(fDTDScanner != null){
            ((XMLDTDScannerImpl)fDTDScanner).reset(propertyManager);
        }
        fEndPos = 0;
        fStartPos = 0;
        if(fDTDDecl != null){
            fDTDDecl.clear();
        }

    }

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
        fDoctypeName = null;
        fDoctypePublicId = null;
        fDoctypeSystemId = null;
        fSeenDoctypeDecl = false;
        fExternalSubsetSource = null;

        // xerces features
        fLoadExternalDTD = componentManager.getFeature(LOAD_EXTERNAL_DTD, true);
        fDisallowDoctype = componentManager.getFeature(DISALLOW_DOCTYPE_DECL_FEATURE, false);

        fNamespaces = componentManager.getFeature(NAMESPACES, true);

        fSeenInternalSubset = false;
        // xerces properties
        fDTDScanner = (XMLDTDScanner)componentManager.getProperty(DTD_SCANNER);

        fValidationManager = (ValidationManager)componentManager.getProperty(VALIDATION_MANAGER, null);

        try {
            fNamespaceContext = (NamespaceContext)componentManager.getProperty(NAMESPACE_CONTEXT);
        }
        catch (XMLConfigurationException e) { }
        if (fNamespaceContext == null) {
            fNamespaceContext = new NamespaceSupport();
        }
        fNamespaceContext.reset();

        fEndPos = 0;
        fStartPos = 0;
        if(fDTDDecl != null)
            fDTDDecl.clear();


        //fEntityScanner.registerListener((XMLBufferListener)componentManager.getProperty(DOCUMENT_SCANNER));

        // setup driver
        setScannerState(SCANNER_STATE_XML_DECL);
        setDriver(fXMLDeclDriver);

    } // reset(XMLComponentManager)


    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    public String[] getRecognizedFeatures() {
        String[] featureIds = super.getRecognizedFeatures();
        int length = featureIds != null ? featureIds.length : 0;
        String[] combinedFeatureIds = new String[length + RECOGNIZED_FEATURES.length];
        if (featureIds != null) {
            System.arraycopy(featureIds, 0, combinedFeatureIds, 0, featureIds.length);
        }
        System.arraycopy(RECOGNIZED_FEATURES, 0, combinedFeatureIds, length, RECOGNIZED_FEATURES.length);
        return combinedFeatureIds;
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
            final int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();

            if (suffixLength == Constants.LOAD_EXTERNAL_DTD_FEATURE.length() &&
                featureId.endsWith(Constants.LOAD_EXTERNAL_DTD_FEATURE)) {
                fLoadExternalDTD = state;
                return;
            }
            else if (suffixLength == Constants.DISALLOW_DOCTYPE_DECL_FEATURE.length() &&
                featureId.endsWith(Constants.DISALLOW_DOCTYPE_DECL_FEATURE)) {
                fDisallowDoctype = state;
                return;
            }
        }

    } // setFeature(String,boolean)

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        String[] propertyIds = super.getRecognizedProperties();
        int length = propertyIds != null ? propertyIds.length : 0;
        String[] combinedPropertyIds = new String[length + RECOGNIZED_PROPERTIES.length];
        if (propertyIds != null) {
            System.arraycopy(propertyIds, 0, combinedPropertyIds, 0, propertyIds.length);
        }
        System.arraycopy(RECOGNIZED_PROPERTIES, 0, combinedPropertyIds, length, RECOGNIZED_PROPERTIES.length);
        return combinedPropertyIds;
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

            if (suffixLength == Constants.DTD_SCANNER_PROPERTY.length() &&
                propertyId.endsWith(Constants.DTD_SCANNER_PROPERTY)) {
                fDTDScanner = (XMLDTDScanner)value;
            }
            if (suffixLength == Constants.NAMESPACE_CONTEXT_PROPERTY.length() &&
                propertyId.endsWith(Constants.NAMESPACE_CONTEXT_PROPERTY)) {
                if (value != null) {
                    fNamespaceContext = (NamespaceContext)value;
                }
            }

            return;
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
        return super.getFeatureDefault(featureId);
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
        return super.getPropertyDefault(propertyId);
    } // getPropertyDefault(String):Object

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
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startEntity(String name,
            XMLResourceIdentifier identifier,
            String encoding, Augmentations augs) throws XNIException {

        super.startEntity(name, identifier, encoding,augs);

        //register current document scanner as a listener for XMLEntityScanner
        fEntityScanner.registerListener(this);

        // prepare to look for a TextDecl if external general entity
        if (!name.equals("[xml]") && fEntityScanner.isExternal()) {
            // Don't do this if we're skipping the entity!
            if (augs == null || !((Boolean) augs.getItem(Constants.ENTITY_SKIPPED)).booleanValue()) {
                setScannerState(SCANNER_STATE_TEXT_DECL);
            }
        }

        // call handler
        /** comment this part.. LOCATOR problem.. */
        if (fDocumentHandler != null && name.equals("[xml]")) {
            fDocumentHandler.startDocument(fEntityScanner, encoding, fNamespaceContext, null);
        }

    } // startEntity(String,identifier,String)


    /**
     * This method notifies the end of an entity. The DTD has the pseudo-name
     * of "[dtd]" parameter entity names start with '%'; and general entities
     * are just specified by their name.
     *
     * @param name The name of the entity.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endEntity(String name, Augmentations augs) throws IOException, XNIException {

        super.endEntity(name, augs);

        if(name.equals("[xml]")){
            //if fMarkupDepth has reached 0.
            //and driver is fTrailingMiscDriver (which
            //handles end of document in normal case)
            //set the scanner state of SCANNER_STATE_TERMINATED
            if(fMarkupDepth == 0 && fDriver == fTrailingMiscDriver){
                //set the scanner set to SCANNER_STATE_TERMINATED
                setScannerState(SCANNER_STATE_TERMINATED) ;
            } else{
                //else we have reached the end of document prematurely
                //so throw EOFException.
                throw new java.io.EOFException();
            }

            //this is taken care in wrapper which generates XNI callbacks, There are no next events

            //if (fDocumentHandler != null) {
                //fDocumentHandler.endDocument(null);
            //}
        }
    } // endEntity(String)


    public XMLStringBuffer getDTDDecl(){
        Entity entity = fEntityScanner.getCurrentEntity();
        fDTDDecl.append(((Entity.ScannedEntity)entity).ch,fStartPos , fEndPos-fStartPos);
        if(fSeenInternalSubset)
            fDTDDecl.append("]>");
        return fDTDDecl;
    }

    public String getCharacterEncodingScheme(){
        return fDeclaredEncoding;
    }

    /** return the next state on the input
     *
     * @return int
     */

    public int next() throws IOException, XNIException {
        return fDriver.next();
    }

    //getNamespaceContext
    public NamespaceContext getNamespaceContext(){
        return fNamespaceContext ;
    }



    //
    // Protected methods
    //

    // driver factory methods

    /** Creates a content driver. */
    protected Driver createContentDriver() {
        return new ContentDriver();
    } // createContentDriver():Driver

    // scanning methods

    /** Scans a doctype declaration. */
    protected boolean scanDoctypeDecl(boolean supportDTD) throws IOException, XNIException {

        // spaces
        if (!fEntityScanner.skipSpaces()) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ROOT_ELEMENT_TYPE_IN_DOCTYPEDECL",
                    null);
        }

        // root element name
        fDoctypeName = fEntityScanner.scanName(NameType.DOCTYPE);
        if (fDoctypeName == null) {
            reportFatalError("MSG_ROOT_ELEMENT_TYPE_REQUIRED", null);
        }

        // external id
        if (fEntityScanner.skipSpaces()) {
            scanExternalID(fStrings, false);
            fDoctypeSystemId = fStrings[0];
            fDoctypePublicId = fStrings[1];
            fEntityScanner.skipSpaces();
        }

        fHasExternalDTD = fDoctypeSystemId != null;

        // Attempt to locate an external subset with an external subset resolver.
        if (supportDTD && !fHasExternalDTD && fExternalSubsetResolver != null) {
            fDTDDescription.setValues(null, null, fEntityManager.getCurrentResourceIdentifier().getExpandedSystemId(), null);
            fDTDDescription.setRootName(fDoctypeName);
            fExternalSubsetSource = fExternalSubsetResolver.getExternalSubset(fDTDDescription);
            fHasExternalDTD = fExternalSubsetSource != null;
        }

        // call handler
        if (supportDTD && fDocumentHandler != null) {
            // NOTE: I don't like calling the doctypeDecl callback until
            //       end of the *full* doctype line (including internal
            //       subset) is parsed correctly but SAX2 requires that
            //       it knows the root element name and public and system
            //       identifier for the startDTD call. -Ac
            if (fExternalSubsetSource == null) {
                fDocumentHandler.doctypeDecl(fDoctypeName, fDoctypePublicId, fDoctypeSystemId, null);
            }
            else {
                fDocumentHandler.doctypeDecl(fDoctypeName, fExternalSubsetSource.getPublicId(), fExternalSubsetSource.getSystemId(), null);
            }
        }

        // is there an internal subset?
        boolean internalSubset = true;
        if (!fEntityScanner.skipChar('[', null)) {
            internalSubset = false;
            fEntityScanner.skipSpaces();
            if (!fEntityScanner.skipChar('>', null)) {
                reportFatalError("DoctypedeclUnterminated", new Object[]{fDoctypeName});
            }
            fMarkupDepth--;
        }
        return internalSubset;

    } // scanDoctypeDecl():boolean

    //
    // Private methods
    //
    /** Set the scanner state after scanning DTD */
    protected void setEndDTDScanState() {
        setScannerState(SCANNER_STATE_PROLOG);
        setDriver(fPrologDriver);
        fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this);
        fReadingDTD=false;
    }

    /** Returns the scanner state name. */
    protected String getScannerStateName(int state) {

        switch (state) {
            case SCANNER_STATE_XML_DECL: return "SCANNER_STATE_XML_DECL";
            case SCANNER_STATE_PROLOG: return "SCANNER_STATE_PROLOG";
            case SCANNER_STATE_TRAILING_MISC: return "SCANNER_STATE_TRAILING_MISC";
            case SCANNER_STATE_DTD_INTERNAL_DECLS: return "SCANNER_STATE_DTD_INTERNAL_DECLS";
            case SCANNER_STATE_DTD_EXTERNAL: return "SCANNER_STATE_DTD_EXTERNAL";
            case SCANNER_STATE_DTD_EXTERNAL_DECLS: return "SCANNER_STATE_DTD_EXTERNAL_DECLS";
        }
        return super.getScannerStateName(state);

    } // getScannerStateName(int):String

    //
    // Classes
    //

    /**
     * Driver to handle XMLDecl scanning.
     *
     * This class has been modified as per the new design which is more suited to
     * efficiently build pull parser. Lots of performance improvements have been done and
     * the code has been added to support stax functionality/features.
     *
     * @author Neeraj Bajaj, Sun Microsystems.
     *
     * @author Andy Clark, IBM
     */
    protected final class XMLDeclDriver
            implements Driver {

        //
        // Driver methods
        //


        public int next() throws IOException, XNIException {

            // next driver is prolog regardless of whether there
            // is an XMLDecl in this document
            setScannerState(SCANNER_STATE_PROLOG);
            setDriver(fPrologDriver);

            //System.out.println("fEntityScanner = " + fEntityScanner);
            // scan XMLDecl
            try {
                if (fEntityScanner.skipString(XMLDECL)) {
                    if (XMLChar.isSpace(fEntityScanner.peekChar())) {
                        fMarkupDepth++;
                        scanXMLDeclOrTextDecl(false);
                    } else {
                        // PI, reset position
                        fEntityManager.fCurrentEntity.position = 0;
                    }
                }

                //START_OF_THE_DOCUMENT
                fEntityManager.fCurrentEntity.mayReadChunks = true;
                return XMLEvent.START_DOCUMENT;

            }
            // encoding errors
            catch (MalformedByteSequenceException e) {
                fErrorReporter.reportError(e.getDomain(), e.getKey(),
                        e.getArguments(), XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return -1;
            } catch (CharConversionException e) {
                fErrorReporter.reportError(
                        XMLMessageFormatter.XML_DOMAIN,
                        "CharConversionFailure",
                        null,
                        XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return -1;
            }
            // premature end of file
            catch (EOFException e) {
                reportFatalError("PrematureEOF", null);
                return -1;
                //throw e;
            }

        }
    } // class XMLDeclDriver

    /**
     * Driver to handle prolog scanning.
     *
     * @author Andy Clark, IBM
     */
    protected final class PrologDriver
            implements Driver {

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

        public int next() throws IOException, XNIException {

            try {
                do {
                    switch (fScannerState) {
                        case SCANNER_STATE_PROLOG: {
                            fEntityScanner.skipSpaces();
                            if (fEntityScanner.skipChar('<', null)) {
                                setScannerState(SCANNER_STATE_START_OF_MARKUP);
                            } else if (fEntityScanner.skipChar('&', NameType.REFERENCE)) {
                                setScannerState(SCANNER_STATE_REFERENCE);
                            } else {
                                setScannerState(SCANNER_STATE_CONTENT);
                            }
                            break;
                        }

                        case SCANNER_STATE_START_OF_MARKUP: {
                            fMarkupDepth++;
                            if (isValidNameStartChar(fEntityScanner.peekChar()) ||
                                    isValidNameStartHighSurrogate(fEntityScanner.peekChar())) {
                                setScannerState(SCANNER_STATE_ROOT_ELEMENT);
                                setDriver(fContentDriver);
                                //from now onwards this would be handled by fContentDriver,in the same next() call
                                return fContentDriver.next();
                            } else if (fEntityScanner.skipChar('!', null)) {
                                if (fEntityScanner.skipChar('-', null)) {
                                    if (!fEntityScanner.skipChar('-', null)) {
                                        reportFatalError("InvalidCommentStart",
                                                null);
                                    }
                                    setScannerState(SCANNER_STATE_COMMENT);
                                } else if (fEntityScanner.skipString(DOCTYPE)) {
                                    setScannerState(SCANNER_STATE_DOCTYPE);
                                    Entity entity = fEntityScanner.getCurrentEntity();
                                    if(entity instanceof Entity.ScannedEntity){
                                        fStartPos=((Entity.ScannedEntity)entity).position;
                                    }
                                    fReadingDTD=true;
                                    if(fDTDDecl == null)
                                        fDTDDecl = new XMLStringBuffer();
                                    fDTDDecl.append("<!DOCTYPE");

                                } else {
                                    reportFatalError("MarkupNotRecognizedInProlog",
                                            null);
                                }
                            } else if (fEntityScanner.skipChar('?', null)) {
                                setScannerState(SCANNER_STATE_PI);
                            } else {
                                reportFatalError("MarkupNotRecognizedInProlog",
                                        null);
                            }
                            break;
                        }
                    }
                } while (fScannerState == SCANNER_STATE_PROLOG || fScannerState == SCANNER_STATE_START_OF_MARKUP );

                switch(fScannerState){
                    /**
                    //this part is handled by FragmentContentHandler
                    case SCANNER_STATE_ROOT_ELEMENT: {
                        //we have read '<' and beginning of reading the start element tag
                        setScannerState(SCANNER_STATE_START_ELEMENT_TAG);
                        setDriver(fContentDriver);
                        //from now onwards this would be handled by fContentDriver,in the same next() call
                        return fContentDriver.next();
                    }
                     */
                    case SCANNER_STATE_COMMENT: {
                        //this function fills the data..
                        scanComment();
                        setScannerState(SCANNER_STATE_PROLOG);
                        return XMLEvent.COMMENT;
                        //setScannerState(SCANNER_STATE_PROLOG);
                        //break;
                    }
                    case SCANNER_STATE_PI: {
                        fContentBuffer.clear() ;
                        scanPI(fContentBuffer);
                        setScannerState(SCANNER_STATE_PROLOG);
                        return XMLEvent.PROCESSING_INSTRUCTION;
                    }

                    case SCANNER_STATE_DOCTYPE: {
                        if (fDisallowDoctype) {
                            reportFatalError("DoctypeNotAllowed", null);
                        }

                        if (fSeenDoctypeDecl) {
                            reportFatalError("AlreadySeenDoctype", null);
                        }
                        fSeenDoctypeDecl = true;

                        // scanDoctypeDecl() sends XNI doctypeDecl event that
                        // in SAX is converted to startDTD() event.
                        if (scanDoctypeDecl(fSupportDTD)) {
                            //allow parsing of entity decls to continue in order to stay well-formed
                            setScannerState(SCANNER_STATE_DTD_INTERNAL_DECLS);
                            fSeenInternalSubset = true;
                            if(fDTDDriver == null){
                                fDTDDriver = new DTDDriver();
                            }
                            setDriver(fContentDriver);
                            //always return DTD event, the event however, will not contain any entities
                            return fDTDDriver.next();
                        }

                        if(fSeenDoctypeDecl){
                            Entity entity = fEntityScanner.getCurrentEntity();
                            if(entity instanceof Entity.ScannedEntity){
                                fEndPos = ((Entity.ScannedEntity)entity).position;
                            }
                            fReadingDTD = false;
                        }

                        // handle external subset
                        if (fDoctypeSystemId != null) {
                            if (((fValidation || fLoadExternalDTD)
                                && (fValidationManager == null || !fValidationManager.isCachedDTD()))) {
                                if (fSupportDTD) {
                                    setScannerState(SCANNER_STATE_DTD_EXTERNAL);
                                } else {
                                    setScannerState(SCANNER_STATE_PROLOG);
                                }

                                setDriver(fContentDriver);
                                if(fDTDDriver == null) {
                                    fDTDDriver = new DTDDriver();
                                }

                                return fDTDDriver.next();
                            }
                        }
                        else if (fExternalSubsetSource != null) {
                            if (((fValidation || fLoadExternalDTD)
                                && (fValidationManager == null || !fValidationManager.isCachedDTD()))) {
                                // This handles the case of a DOCTYPE that had neither an internal subset or an external subset.
                                fDTDScanner.setInputSource(fExternalSubsetSource);
                                fExternalSubsetSource = null;
                            if (fSupportDTD)
                                setScannerState(SCANNER_STATE_DTD_EXTERNAL_DECLS);
                            else
                                setScannerState(SCANNER_STATE_PROLOG);
                            setDriver(fContentDriver);
                            if(fDTDDriver == null)
                                fDTDDriver = new DTDDriver();
                            return fDTDDriver.next();
                            }
                        }

                        // Send endDTD() call if:
                        // a) systemId is null or if an external subset resolver could not locate an external subset.
                        // b) "load-external-dtd" and validation are false
                        // c) DTD grammar is cached

                        // in XNI this results in 3 events:  doctypeDecl, startDTD, endDTD
                        // in SAX this results in 2 events: startDTD, endDTD
                        if (fDTDScanner != null) {
                            fDTDScanner.setInputSource(null);
                        }
                        setScannerState(SCANNER_STATE_PROLOG);
                        return XMLEvent.DTD;
                    }

                    case SCANNER_STATE_CONTENT: {
                        reportFatalError("ContentIllegalInProlog", null);
                        fEntityScanner.scanChar(null);
                        return -1;
                    }
                    case SCANNER_STATE_REFERENCE: {
                        reportFatalError("ReferenceIllegalInProlog", null);
                        return -1;
                    }

                    /**
                     * if (complete) {
                     * if (fEntityScanner.scanChar() != '<') {
                     * reportFatalError("RootElementRequired", null);
                     * }
                     * setScannerState(SCANNER_STATE_ROOT_ELEMENT);
                     * setDriver(fContentDriver);
                     * }
                     */
                }
            }
            // encoding errors
            catch (MalformedByteSequenceException e) {
                fErrorReporter.reportError(e.getDomain(), e.getKey(),
                        e.getArguments(), XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return -1;
            } catch (CharConversionException e) {
                fErrorReporter.reportError(
                        XMLMessageFormatter.XML_DOMAIN,
                        "CharConversionFailure",
                        null,
                        XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return -1;
            }
            // premature end of file
            catch (EOFException e) {
                reportFatalError("PrematureEOF", null);
                //xxx  what should be returned here.... ???
                return -1 ;
                //throw e;
            }
            //xxx  what should be returned here.... ???
            return -1;

        }


    } // class PrologDriver

    /**
     * Driver to handle the internal and external DTD subsets.
     *
     * @author Andy Clark, IBM
     */
    protected final class DTDDriver
            implements Driver {

        //
        // Driver methods
        //

        public int next() throws IOException, XNIException{

            dispatch(true);

            //xxx: remove this hack and align this with reusing DTD components
            //currently this routine will only be executed from Stax
            if(fPropertyManager != null){
                dtdGrammarUtil =  new DTDGrammarUtil(((XMLDTDScannerImpl)fDTDScanner).getGrammar(),fSymbolTable, fNamespaceContext);
            }

            return XMLEvent.DTD ;
        }

        /**
         * Dispatch an XML "event".
         *
         * @param complete True if this driver is intended to scan
         *                 and dispatch as much as possible.
         *
         * @return True if there is more to dispatch either from this
         *          or a another driver.
         *
         * @throws IOException  Thrown on i/o error.
         * @throws XNIException Thrown on parse error.
         */
        public boolean dispatch(boolean complete)
        throws IOException, XNIException {
            fEntityManager.setEntityHandler(null);
            try {
                boolean again;
                XMLResourceIdentifierImpl resourceIdentifier = new XMLResourceIdentifierImpl();
                if( fDTDScanner == null){

                    if (fEntityManager.getEntityScanner() instanceof XML11EntityScanner){
                        fDTDScanner = new XML11DTDScannerImpl();
                    } else

                    fDTDScanner = new XMLDTDScannerImpl();

                    ((XMLDTDScannerImpl)fDTDScanner).reset(fPropertyManager);
                }

                fDTDScanner.setLimitAnalyzer(fLimitAnalyzer);
                do {
                    again = false;
                    switch (fScannerState) {
                        case SCANNER_STATE_DTD_INTERNAL_DECLS: {
                            boolean moreToScan = false;
                            if (!fDTDScanner.skipDTD(fSupportDTD)) {
                                // REVISIT: Should there be a feature for
                                //          the "complete" parameter?
                                boolean completeDTD = true;

                                moreToScan = fDTDScanner.scanDTDInternalSubset(completeDTD, fStandalone, fHasExternalDTD && fLoadExternalDTD);
                            }
                            Entity entity = fEntityScanner.getCurrentEntity();
                            if(entity instanceof Entity.ScannedEntity){
                                fEndPos=((Entity.ScannedEntity)entity).position;
                            }
                            fReadingDTD=false;
                            if (!moreToScan) {
                                // end doctype declaration
                                if (!fEntityScanner.skipChar(']', null)) {
                                    reportFatalError("DoctypedeclNotClosed", new Object[]{fDoctypeName});
                                }
                                fEntityScanner.skipSpaces();
                                if (!fEntityScanner.skipChar('>', null)) {
                                    reportFatalError("DoctypedeclUnterminated", new Object[]{fDoctypeName});
                                }
                                fMarkupDepth--;

                                if (!fSupportDTD) {
                                    //simply reset the entity store without having to mess around
                                    //with the DTD Scanner code
                                    fEntityStore = fEntityManager.getEntityStore();
                                    fEntityStore.reset();
                                } else {
                                    // scan external subset next unless we are ignoring DTDs
                                    if (fDoctypeSystemId != null && (fValidation || fLoadExternalDTD)) {
                                        setScannerState(SCANNER_STATE_DTD_EXTERNAL);
                                        break;
                                    }
                                }

                                setEndDTDScanState();
                                return true;

                            }
                            break;
                        }
                        case SCANNER_STATE_DTD_EXTERNAL: {
                            /**
                            fDTDDescription.setValues(fDoctypePublicId, fDoctypeSystemId, null, null);
                            fDTDDescription.setRootName(fDoctypeName);
                            XMLInputSource xmlInputSource =
                                fEntityManager.resolveEntity(fDTDDescription);
                            fDTDScanner.setInputSource(xmlInputSource);
                            setScannerState(SCANNER_STATE_DTD_EXTERNAL_DECLS);
                            again = true;
                            break;
                             */

                            resourceIdentifier.setValues(fDoctypePublicId, fDoctypeSystemId, null, null);
                            XMLInputSource xmlInputSource = null ;
                            StaxXMLInputSource staxInputSource =  fEntityManager.resolveEntityAsPerStax(resourceIdentifier);

                            // Check access permission. If the source is resolved by a resolver, the check is skipped.
                            if (!staxInputSource.isCreatedByResolver()) {
                                String accessError = checkAccess(fDoctypeSystemId, fAccessExternalDTD);
                                if (accessError != null) {
                                    reportFatalError("AccessExternalDTD", new Object[]{ SecuritySupport.sanitizePath(fDoctypeSystemId), accessError });
                                }
                            }
                            xmlInputSource = staxInputSource.getXMLInputSource();
                            fDTDScanner.setInputSource(xmlInputSource);
                            if (fEntityScanner.fCurrentEntity != null) {
                                setScannerState(SCANNER_STATE_DTD_EXTERNAL_DECLS);
                            } else {
                                setScannerState(SCANNER_STATE_PROLOG);
                            }
                            again = true;
                            break;
                        }
                        case SCANNER_STATE_DTD_EXTERNAL_DECLS: {
                            // REVISIT: Should there be a feature for
                            //          the "complete" parameter?
                            boolean completeDTD = true;
                            boolean moreToScan = fDTDScanner.scanDTDExternalSubset(completeDTD);
                            if (!moreToScan) {
                                setEndDTDScanState();
                                return true;
                            }
                            break;
                        }
                        case SCANNER_STATE_PROLOG : {
                            // skip entity decls
                            setEndDTDScanState();
                            return true;
                        }
                        default: {
                            throw new XNIException("DTDDriver#dispatch: scanner state="+fScannerState+" ("+getScannerStateName(fScannerState)+')');
                        }
                    }
                } while (complete || again);
            }
            // encoding errors
            catch (MalformedByteSequenceException e) {
                fErrorReporter.reportError(e.getDomain(), e.getKey(),
                        e.getArguments(), XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return false;
            } catch (CharConversionException e) {
                fErrorReporter.reportError(
                        XMLMessageFormatter.XML_DOMAIN,
                        "CharConversionFailure",
                        null,
                        XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return false;
            }
            // premature end of file
            catch (EOFException e) {
                e.printStackTrace();
                reportFatalError("PrematureEOF", null);
                return false;
                //throw e;
            }

            // cleanup
            finally {
                fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this);
            }

            return true;

        }

        // dispatch(boolean):boolean

    } // class DTDDriver

    /**
     * Driver to handle content scanning.
     *
     * @author Andy Clark, IBM
     * @author Eric Ye, IBM
     */
    protected class ContentDriver
            extends FragmentContentDriver {

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

            if (fEntityScanner.skipString(DOCTYPE)) {
                setScannerState(SCANNER_STATE_DOCTYPE);
                //                fEntityScanner.markStartOfDTD();
                return true;
            }
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

            setScannerState(SCANNER_STATE_TRAILING_MISC);
            setDriver(fTrailingMiscDriver);
            return true;

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

            if (scanStartElement()) {
                setScannerState(SCANNER_STATE_TRAILING_MISC);
                setDriver(fTrailingMiscDriver);
                return true;
            }
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

            reportFatalError("PrematureEOF", null);
            // in case continue-after-fatal-error set, should not do this...
            //throw e;

        } // endOfFileHook()

        protected void resolveExternalSubsetAndRead()
        throws IOException, XNIException {

            fDTDDescription.setValues(null, null, fEntityManager.getCurrentResourceIdentifier().getExpandedSystemId(), null);
            fDTDDescription.setRootName(fElementQName.rawname);
            XMLInputSource src = fExternalSubsetResolver.getExternalSubset(fDTDDescription);

            if (src != null) {
                fDoctypeName = fElementQName.rawname;
                fDoctypePublicId = src.getPublicId();
                fDoctypeSystemId = src.getSystemId();
                // call document handler
                if (fDocumentHandler != null) {
                    // This inserts a doctypeDecl event into the stream though no
                    // DOCTYPE existed in the instance document.
                    fDocumentHandler.doctypeDecl(fDoctypeName, fDoctypePublicId, fDoctypeSystemId, null);
                }
                try {
                    fDTDScanner.setInputSource(src);
                    while (fDTDScanner.scanDTDExternalSubset(true));
                } finally {
                    fEntityManager.setEntityHandler(XMLDocumentScannerImpl.this);
                }
            }
        } // resolveExternalSubsetAndRead()



    } // class ContentDriver

    /**
     * Driver to handle trailing miscellaneous section scanning.
     *
     * @author Andy Clark, IBM
     * @author Eric Ye, IBM
     */
    protected final class TrailingMiscDriver
            implements Driver {

        //
        // Driver methods
        //
        public int next() throws IOException, XNIException{
            //this could for cases like <foo/>
            //look at scanRootElementHook
            if(fEmptyElement){
                fEmptyElement = false;
                return XMLEvent.END_ELEMENT;
            }

            try {
                if(fScannerState == SCANNER_STATE_TERMINATED){
                    return XMLEvent.END_DOCUMENT ;}
                do {
                    switch (fScannerState) {
                        case SCANNER_STATE_TRAILING_MISC: {

                            fEntityScanner.skipSpaces();
                            //we should have reached the end of the document in
                            //most cases.
                            if(fScannerState == SCANNER_STATE_TERMINATED ){
                                return XMLEvent.END_DOCUMENT ;
                            }
                            if (fEntityScanner.skipChar('<', null)) {
                                setScannerState(SCANNER_STATE_START_OF_MARKUP);
                            } else {
                                setScannerState(SCANNER_STATE_CONTENT);
                            }
                            break;
                        }
                        case SCANNER_STATE_START_OF_MARKUP: {
                            fMarkupDepth++;
                            if (fEntityScanner.skipChar('?', null)) {
                                setScannerState(SCANNER_STATE_PI);
                            } else if (fEntityScanner.skipChar('!', null)) {
                                setScannerState(SCANNER_STATE_COMMENT);
                            } else if (fEntityScanner.skipChar('/', null)) {
                                reportFatalError("MarkupNotRecognizedInMisc",
                                        null);
                            } else if (isValidNameStartChar(fEntityScanner.peekChar()) ||
                                    isValidNameStartHighSurrogate(fEntityScanner.peekChar())) {
                                reportFatalError("MarkupNotRecognizedInMisc",
                                        null);
                                scanStartElement();
                                setScannerState(SCANNER_STATE_CONTENT);
                            } else {
                                reportFatalError("MarkupNotRecognizedInMisc",
                                        null);
                            }
                            break;
                        }
                    }
                } while(fScannerState == SCANNER_STATE_START_OF_MARKUP ||
                        fScannerState == SCANNER_STATE_TRAILING_MISC);

                switch (fScannerState){
                    case SCANNER_STATE_PI: {
                        fContentBuffer.clear();
                        scanPI(fContentBuffer);
                        setScannerState(SCANNER_STATE_TRAILING_MISC);
                        return XMLEvent.PROCESSING_INSTRUCTION ;
                    }
                    case SCANNER_STATE_COMMENT: {
                        if (!fEntityScanner.skipString(COMMENTSTRING)) {
                            reportFatalError("InvalidCommentStart", null);
                        }
                        scanComment();
                        setScannerState(SCANNER_STATE_TRAILING_MISC);
                        return XMLEvent.COMMENT;
                    }
                    case SCANNER_STATE_CONTENT: {
                        int ch = fEntityScanner.peekChar();
                        if (ch == -1) {
                            setScannerState(SCANNER_STATE_TERMINATED);
                            return XMLEvent.END_DOCUMENT ;
                        } else{
                            reportFatalError("ContentIllegalInTrailingMisc",
                                    null);
                            fEntityScanner.scanChar(null);
                            setScannerState(SCANNER_STATE_TRAILING_MISC);
                            return XMLEvent.CHARACTERS;
                        }

                    }
                    case SCANNER_STATE_REFERENCE: {
                        reportFatalError("ReferenceIllegalInTrailingMisc",
                                null);
                        setScannerState(SCANNER_STATE_TRAILING_MISC);
                        return XMLEvent.ENTITY_REFERENCE ;
                    }
                    case SCANNER_STATE_TERMINATED: {
                        //there can't be any element after SCANNER_STATE_TERMINATED or when the parser
                        //has reached the end of document
                        setScannerState(SCANNER_STATE_NO_SUCH_ELEMENT_EXCEPTION);
                        //xxx what to do when the scanner has reached the terminating state.
                        return XMLEvent.END_DOCUMENT ;
                    }
                    case SCANNER_STATE_NO_SUCH_ELEMENT_EXCEPTION:{
                        throw new java.util.NoSuchElementException("No more events to be parsed");
                    }
                    default: throw new XNIException("Scanner State " + fScannerState + " not Recognized ");
                }//switch
            // encoding errors
            } catch (MalformedByteSequenceException e) {
                fErrorReporter.reportError(e.getDomain(), e.getKey(),
                        e.getArguments(), XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return -1;
            } catch (CharConversionException e) {
                fErrorReporter.reportError(
                        XMLMessageFormatter.XML_DOMAIN,
                        "CharConversionFailure",
                        null,
                        XMLErrorReporter.SEVERITY_FATAL_ERROR, e);
                return -1;
            } catch (EOFException e) {
                // NOTE: This is the only place we're allowed to reach
                //       the real end of the document stream. Unless the
                //       end of file was reached prematurely.
                if (fMarkupDepth != 0) {
                    reportFatalError("PrematureEOF", null);
                    return -1;
                    //throw e;
                }
                //System.out.println("EOFException thrown") ;
                setScannerState(SCANNER_STATE_TERMINATED);
            }

            return XMLEvent.END_DOCUMENT;

        }//next

    } // class TrailingMiscDriver

    /**
     * Implements XMLBufferListener interface.
     */


    /**
     * receives callbacks from {@link XMLEntityReader } when buffer
     * is being changed.
     * @param refreshPosition
     */
    public void refresh(int refreshPosition){
        super.refresh(refreshPosition);
        if(fReadingDTD){
            Entity entity = fEntityScanner.getCurrentEntity();
            if(entity instanceof Entity.ScannedEntity){
                fEndPos=((Entity.ScannedEntity)entity).position;
            }
            fDTDDecl.append(((Entity.ScannedEntity)entity).ch,fStartPos , fEndPos-fStartPos);
            fStartPos = refreshPosition;
        }
    }

} // class XMLDocumentScannerImpl
