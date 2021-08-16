/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.utils.XMLLimitAnalyzer;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.xni.XMLDTDContentModelHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDTDHandler;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDScanner;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.xml.internal.stream.dtd.nonvalidating.DTDGrammar;
import java.io.EOFException;
import java.io.IOException;

/**
 * This class is responsible for scanning the declarations found
 * in the internal and external subsets of a DTD in an XML document.
 * The scanner acts as the sources for the DTD information which is
 * communicated to the DTD handlers.
 * <p>
 * This component requires the following features and properties from the
 * component manager that uses it:
 * <ul>
 *  <li>http://xml.org/sax/features/validation</li>
 *  <li>http://apache.org/xml/features/scanner/notify-char-refs</li>
 *  <li>http://apache.org/xml/properties/internal/symbol-table</li>
 *  <li>http://apache.org/xml/properties/internal/error-reporter</li>
 *  <li>http://apache.org/xml/properties/internal/entity-manager</li>
 * </ul>
 *
 * @author Arnaud  Le Hors, IBM
 * @author Andy Clark, IBM
 * @author Glenn Marcy, IBM
 * @author Eric Ye, IBM
 *
 * @LastModified: Feb 2020
 */
public class XMLDTDScannerImpl
extends XMLScanner
implements XMLDTDScanner, XMLComponent, XMLEntityHandler {

    //
    // Constants
    //

    // scanner states

    /** Scanner state: end of input. */
    protected static final int SCANNER_STATE_END_OF_INPUT = 0;

    /** Scanner state: text declaration. */
    protected static final int SCANNER_STATE_TEXT_DECL = 1;

    /** Scanner state: markup declaration. */
    protected static final int SCANNER_STATE_MARKUP_DECL = 2;

    // recognized features and properties

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        VALIDATION,
        NOTIFY_CHAR_REFS,
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
        null,
        Boolean.FALSE,
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        SYMBOL_TABLE,
        ERROR_REPORTER,
        ENTITY_MANAGER,
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
        null,
        null,
        null,
    };

    // debugging

    /** Debug scanner state. */
    private static final boolean DEBUG_SCANNER_STATE = false;

    //
    // Data
    //

    // handlers

    /** DTD handler. */
    public XMLDTDHandler fDTDHandler = null;

    /** DTD content model handler. */
    protected XMLDTDContentModelHandler fDTDContentModelHandler;

    // state

    /** Scanner state. */
    protected int fScannerState;

    /** Standalone. */
    protected boolean fStandalone;

    /** Seen external DTD. */
    protected boolean fSeenExternalDTD;

    /** Seen external parameter entity. */
    protected boolean fSeenExternalPE;

    // private data

    /** Start DTD called. */
    private boolean fStartDTDCalled;

    /** Default attribute */
    private XMLAttributesImpl fAttributes = new XMLAttributesImpl();

    /**
     * Stack of content operators (either '|' or ',') in children
     * content.
     */
    private int[] fContentStack = new int[5];

    /** Size of content stack. */
    private int fContentDepth;

    /** Parameter entity stack to check well-formedness. */
    private int[] fPEStack = new int[5];


    /** Parameter entity stack to report start/end entity calls. */
    private boolean[] fPEReport = new boolean[5];

    /** Number of opened parameter entities. */
    private int fPEDepth;

    /** Markup depth. */
    private int fMarkUpDepth;

    /** Number of opened external entities. */
    private int fExtEntityDepth;

    /** Number of opened include sections. */
    private int fIncludeSectDepth;

    // temporary variables

    /** Array of 3 strings. */
    private String[] fStrings = new String[3];

    /** String. */
    private XMLString fString = new XMLString();

    /** String buffer. */
    private XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    /** String buffer. */
    private XMLStringBuffer fStringBuffer2 = new XMLStringBuffer();

    /** Literal text. */
    private XMLString fLiteral = new XMLString();

    /** Literal text. */
    private XMLString fLiteral2 = new XMLString();

    /** Enumeration values. */
    private String[] fEnumeration = new String[5];

    /** Enumeration values count. */
    private int fEnumerationCount;

    /** Ignore conditional section buffer. */
    private XMLStringBuffer fIgnoreConditionalBuffer = new XMLStringBuffer(128);

    /** Object contains grammar information for a non-validaing parser. */
    DTDGrammar nvGrammarInfo = null;

    boolean nonValidatingMode = false;
    //
    // Constructors
    //

    /** Default constructor. */
    public XMLDTDScannerImpl() {
    } // <init>()

    /** Constructor for he use of non-XMLComponentManagers. */
    public XMLDTDScannerImpl(SymbolTable symbolTable,
            XMLErrorReporter errorReporter, XMLEntityManager entityManager) {
        fSymbolTable = symbolTable;
        fErrorReporter = errorReporter;
        fEntityManager = entityManager;
        entityManager.setProperty(SYMBOL_TABLE, fSymbolTable);
    }

    //
    // XMLDTDScanner methods
    //

    /**
     * Sets the input source.
     *
     * @param inputSource The input source or null.
     *
     * @throws IOException Thrown on i/o error.
     */
    public void setInputSource(XMLInputSource inputSource) throws IOException {
        if (inputSource == null) {
            // no system id was available
            if (fDTDHandler != null) {
                fDTDHandler.startDTD(null, null);
                fDTDHandler.endDTD(null);
            }
            if (nonValidatingMode){
                nvGrammarInfo.startDTD(null,null);
                nvGrammarInfo.endDTD(null);
            }
            return;
        }
        fEntityManager.setEntityHandler(this);
        fEntityManager.startDTDEntity(inputSource);
    } // setInputSource(XMLInputSource)


    public void setLimitAnalyzer(XMLLimitAnalyzer limitAnalyzer) {
        fLimitAnalyzer = limitAnalyzer;
    }

    /**
     * Scans the external subset of the document.
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
    public boolean scanDTDExternalSubset(boolean complete)
    throws IOException, XNIException {

        fEntityManager.setEntityHandler(this);
        if (fScannerState == SCANNER_STATE_TEXT_DECL) {
            fSeenExternalDTD = true;
            boolean textDecl = scanTextDecl();
            if (fScannerState == SCANNER_STATE_END_OF_INPUT) {
                return false;
            }
            else {
                // next state is markup decls regardless of whether there
                // is a TextDecl or not
                setScannerState(SCANNER_STATE_MARKUP_DECL);
                if (textDecl && !complete) {
                    return true;
                }
            }
        }
        // keep dispatching "events"
        do {
            if (!scanDecls(complete)) {
                return false;
            }
        } while (complete);

        // return that there is more to scan
        return true;

    } // scanDTDExternalSubset(boolean):boolean

    /**
     * Scans the internal subset of the document.
     *
     * @param complete True if the scanner should scan the document
     *                 completely, pushing all events to the registered
     *                 document handler. A value of false indicates that
     *                 that the scanner should only scan the next portion
     *                 of the document and return. A scanner instance is
     *                 permitted to completely scan a document if it does
     *                 not support this "pull" scanning model.
     * @param standalone True if the document was specified as standalone.
     *                   This value is important for verifying certain
     *                   well-formedness constraints.
     * @param hasExternalDTD True if the document has an external DTD.
     *                       This allows the scanner to properly notify
     *                       the handler of the end of the DTD in the
     *                       absence of an external subset.
     *
     * @return True if there is more to scan, false otherwise.
     */
    public boolean scanDTDInternalSubset(boolean complete, boolean standalone,
    boolean hasExternalSubset)
    throws IOException, XNIException {
        // reset entity scanner
        //xxx:stax getText() is supposed to return only DTD internal subset
        //shouldn't we record position here before we go ahead ??

        fEntityScanner = fEntityManager.getEntityScanner();
        fEntityManager.setEntityHandler(this);
        fStandalone = standalone;
        //System.out.println("state"+fScannerState);
        if (fScannerState == SCANNER_STATE_TEXT_DECL) {
            // call handler
            if (fDTDHandler != null) {
                fDTDHandler.startDTD(fEntityScanner, null);
                fStartDTDCalled = true;
            }

            if (nonValidatingMode){
                fStartDTDCalled = true;
                nvGrammarInfo.startDTD(fEntityScanner,null);
            }
            // set starting state for internal subset
            setScannerState(SCANNER_STATE_MARKUP_DECL);
        }
        // keep dispatching "events"
        do {
            if (!scanDecls(complete)) {
                // call handler
                if (fDTDHandler != null && hasExternalSubset == false) {
                    fDTDHandler.endDTD(null);
                }
                if (nonValidatingMode && hasExternalSubset == false ){
                    nvGrammarInfo.endDTD(null);
                }
                // we're done, set starting state for external subset
                setScannerState(SCANNER_STATE_TEXT_DECL);
                // we're done scanning DTD.
                fLimitAnalyzer.reset(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT);
                fLimitAnalyzer.reset(XMLSecurityManager.Limit.TOTAL_ENTITY_SIZE_LIMIT);
                return false;
            }
        } while (complete);

        // return that there is more to scan
        return true;

    } // scanDTDInternalSubset(boolean,boolean,boolean):boolean

    /**
     * Skip the DTD if javax.xml.stream.supportDTD is false.
     *
     * @param supportDTD The value of the property javax.xml.stream.supportDTD.
     * @return true if DTD is skipped, false otherwise.
     * @throws java.io.IOException if i/o error occurs
     */
    @Override
    public boolean skipDTD(boolean supportDTD) throws IOException {
        if (supportDTD)
            return false;

        fStringBuffer.clear();
        while (fEntityScanner.scanData("]", fStringBuffer, 0)) {
            int c = fEntityScanner.peekChar();
            if (c != -1) {
                if (XMLChar.isHighSurrogate(c)) {
                    scanSurrogates(fStringBuffer);
                }
                if (isInvalidLiteral(c)) {
                    reportFatalError("InvalidCharInDTD",
                        new Object[] { Integer.toHexString(c) });
                    fEntityScanner.scanChar(null);
                }
            }
        }
        fEntityScanner.fCurrentEntity.position--;
        return true;
    }

    //
    // XMLComponent methods
    //

    /**
     * reset
     *
     * @param componentManager
     */
    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

        super.reset(componentManager);
        init();

    } // reset(XMLComponentManager)

    // this is made for something like XMLDTDLoader--XMLComponentManager-free operation...
    public void reset() {
        super.reset();
        init();

    }

    public void reset(PropertyManager props) {
        setPropertyManager(props);
        super.reset(props);
        init() ;
        nonValidatingMode = true;
        //Revisit : Create new grammar until we implement GrammarPool.
        nvGrammarInfo = new DTDGrammar(fSymbolTable);
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
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES.clone();
    } // getRecognizedProperties():String[]

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
    // XMLDTDSource methods
    //

    /**
     * setDTDHandler
     *
     * @param dtdHandler
     */
    public void setDTDHandler(XMLDTDHandler dtdHandler) {
        fDTDHandler = dtdHandler;
    } // setDTDHandler(XMLDTDHandler)

    /**
     * getDTDHandler
     *
     * @return the XMLDTDHandler
     */
    public XMLDTDHandler getDTDHandler() {
        return fDTDHandler;
    } // getDTDHandler():  XMLDTDHandler

    //
    // XMLDTDContentModelSource methods
    //

    /**
     * setDTDContentModelHandler
     *
     * @param dtdContentModelHandler
     */
    public void setDTDContentModelHandler(XMLDTDContentModelHandler
    dtdContentModelHandler) {
        fDTDContentModelHandler = dtdContentModelHandler;
    } // setDTDContentModelHandler

    /**
     * getDTDContentModelHandler
     *
     * @return XMLDTDContentModelHandler
     */
    public XMLDTDContentModelHandler getDTDContentModelHandler() {
        return fDTDContentModelHandler ;
    } // setDTDContentModelHandler

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

        super.startEntity(name, identifier, encoding, augs);

        boolean dtdEntity = name.equals("[dtd]");
        if (dtdEntity) {
            // call handler
            if (fDTDHandler != null && !fStartDTDCalled ) {
                fDTDHandler.startDTD(fEntityScanner, null);
            }
            if (fDTDHandler != null) {
                fDTDHandler.startExternalSubset(identifier,null);
            }
            fEntityManager.startExternalSubset();
            fEntityStore.startExternalSubset();
            fExtEntityDepth++;
        }
        else if (name.charAt(0) == '%') {
            pushPEStack(fMarkUpDepth, fReportEntity);
            if (fEntityScanner.isExternal()) {
                fExtEntityDepth++;
            }
        }

        // call handler
        if (fDTDHandler != null && !dtdEntity && fReportEntity) {
            fDTDHandler.startParameterEntity(name, identifier, encoding, null);
        }

    } // startEntity(String,XMLResourceIdentifier,String)

    /**
     * This method notifies the end of an entity. The DTD has the pseudo-name
     * of "[dtd]" parameter entity names start with '%'; and general entities
     * are just specified by their name.
     *
     * @param name The name of the entity.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endEntity(String name, Augmentations augs)
    throws XNIException, IOException {

        super.endEntity(name, augs);

        // if there is no data after the doctype
        //
        if (fScannerState == SCANNER_STATE_END_OF_INPUT)
            return;

        boolean dtdEntity = name.equals("[dtd]");
        // Handle end of PE
        boolean reportEntity = fReportEntity;
        if (name.startsWith("%")) {
            reportEntity = peekReportEntity();
            // check well-formedness of the entity
            int startMarkUpDepth = popPEStack();
            // throw fatalError if this entity was incomplete and
            // was a freestanding decl
            if (startMarkUpDepth == 0 && startMarkUpDepth < fMarkUpDepth) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                "ILL_FORMED_PARAMETER_ENTITY_WHEN_USED_IN_DECL",
                new Object[]{ fEntityManager.fCurrentEntity.name},
                XMLErrorReporter.SEVERITY_FATAL_ERROR);
            }
            if (startMarkUpDepth != fMarkUpDepth) {
                reportEntity = false;
                if (fValidation) {
                    // Proper nesting of parameter entities is a Validity Constraint
                    // and must not be enforced when validation is off
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                    "ImproperDeclarationNesting",
                    new Object[]{ name },
                    XMLErrorReporter.SEVERITY_ERROR);
                }
            }
            if (fEntityScanner.isExternal()) {
                fExtEntityDepth--;
            }
            // call handler
            if (fDTDHandler != null && reportEntity) {
                fDTDHandler.endParameterEntity(name, null);
            }
        }

        // end DTD
        if (dtdEntity) {
            if (fIncludeSectDepth != 0) {
                reportFatalError("IncludeSectUnterminated", null);
            }
            fScannerState = SCANNER_STATE_END_OF_INPUT;
            // call handler
            fEntityManager.endExternalSubset();
            fEntityStore.endExternalSubset();

            if (fDTDHandler != null) {
                fDTDHandler.endExternalSubset(null);
                fDTDHandler.endDTD(null);
            }
            fExtEntityDepth--;
        }

        //XML (Document Entity) is the last opened entity, however
        //if for some reason DTD Scanner receives this callback
        //there is something wrong (probably invalid XML), throw exception.
        //or
        //For standalone DTD loader, it might be the last opened entity
        //and if this is the last opened entity and fMarkUpDepth != 0 or
        //fIncludeSectDepth != 0 or fExtEntityDepth != 0 throw Exception
        if (augs != null && Boolean.TRUE.equals(augs.getItem(Constants.LAST_ENTITY))
            && ( fMarkUpDepth != 0 || fExtEntityDepth !=0 || fIncludeSectDepth != 0)){
            throw new EOFException();
        }

    } // endEntity(String)

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
            System.out.print(getScannerStateName(state));
            //System.out.println();
        }

    } // setScannerState(int)

    //
    // Private methods
    //

    /** Returns the scanner state name. */
    private static String getScannerStateName(int state) {

        if (DEBUG_SCANNER_STATE) {
            switch (state) {
                case SCANNER_STATE_END_OF_INPUT: return "SCANNER_STATE_END_OF_INPUT";
                case SCANNER_STATE_TEXT_DECL: return "SCANNER_STATE_TEXT_DECL";
                case SCANNER_STATE_MARKUP_DECL: return "SCANNER_STATE_MARKUP_DECL";
            }
        }

        return "??? ("+state+')';

    } // getScannerStateName(int):String

    protected final boolean scanningInternalSubset() {
        return fExtEntityDepth == 0;
    }

    /**
     * start a parameter entity dealing with the textdecl if there is any
     *
     * @param name The name of the parameter entity to start (without the '%')
     * @param literal Whether this is happening within a literal
     */
    protected void startPE(String name, boolean literal)
    throws IOException, XNIException {
        int depth = fPEDepth;
        String pName = "%"+name;
        if (fValidation && !fEntityStore.isDeclaredEntity(pName)) {
            fErrorReporter.reportError( XMLMessageFormatter.XML_DOMAIN,"EntityNotDeclared",
            new Object[]{name}, XMLErrorReporter.SEVERITY_ERROR);
        }
        fEntityManager.startEntity(false, fSymbolTable.addSymbol(pName),
        literal);
        // if we actually got a new entity and it's external
        // parse text decl if there is any
        if (depth != fPEDepth && fEntityScanner.isExternal()) {
            scanTextDecl();
        }
    }

    /**
     * Dispatch an XML "event".
     *
     * @param complete True if this method is intended to scan
     *                 and dispatch as much as possible.
     *
     * @return True if a TextDecl was scanned.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown on parse error.
     *
     */
    protected final boolean scanTextDecl()
    throws IOException, XNIException {

        // scan XMLDecl
        boolean textDecl = false;
        if (fEntityScanner.skipString("<?xml")) {
            fMarkUpDepth++;
            // NOTE: special case where document starts with a PI
            //       whose name starts with "xml" (e.g. "xmlfoo")
            if (isValidNameChar(fEntityScanner.peekChar())) {
                fStringBuffer.clear();
                fStringBuffer.append("xml");
                while (isValidNameChar(fEntityScanner.peekChar())) {
                    fStringBuffer.append((char)fEntityScanner.scanChar(null));
                }
                String target =
                fSymbolTable.addSymbol(fStringBuffer.ch,
                fStringBuffer.offset,
                fStringBuffer.length);
                scanPIData(target, fString);
            }

            // standard Text declaration
            else {
                // pseudo-attribute values
                String version = null;
                String encoding = null;

                scanXMLDeclOrTextDecl(true, fStrings);
                textDecl = true;
                fMarkUpDepth--;

                version = fStrings[0];
                encoding = fStrings[1];

                fEntityScanner.setEncoding(encoding);

                // call handler
                if (fDTDHandler != null) {
                    fDTDHandler.textDecl(version, encoding, null);
                }
            }
        }
        fEntityManager.fCurrentEntity.mayReadChunks = true;

        return textDecl;

    } // scanTextDecl(boolean):boolean

    /**
     * Scans a processing data. This is needed to handle the situation
     * where a document starts with a processing instruction whose
     * target name <em>starts with</em> "xml". (e.g. xmlfoo)
     *
     * @param target The PI target
     * @param data The string to fill in with the data
     */
    protected final void scanPIData(String target, XMLString data)
    throws IOException, XNIException {
        //Venu REVISIT
        //      super.scanPIData(target, data);
        fMarkUpDepth--;

        // call handler
        if (fDTDHandler != null) {
            fDTDHandler.processingInstruction(target, data, null);
        }

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
    protected final void scanComment() throws IOException, XNIException {

        fReportEntity = false;
        scanComment(fStringBuffer);
        fMarkUpDepth--;

        // call handler
        if (fDTDHandler != null) {
            fDTDHandler.comment(fStringBuffer, null);
        }
        fReportEntity = true;

    } // scanComment()

    /**
     * Scans an element declaration
     * <p>
     * <pre>
     * [45]    elementdecl    ::=    '&lt;!ELEMENT' S Name S contentspec S? '>'
     * [46]    contentspec    ::=    'EMPTY' | 'ANY' | Mixed | children
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!ELEMENT'
     */
    protected final void scanElementDecl() throws IOException, XNIException {

        // spaces
        fReportEntity = false;
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL",
            null);
        }

        // element name
        String name = fEntityScanner.scanName(NameType.ELEMENTSTART);
        if (name == null) {
            reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL",
            null);
        }

        // spaces
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL",
            new Object[]{name});
        }

        // content model
        if (fDTDContentModelHandler != null) {
            fDTDContentModelHandler.startContentModel(name, null);
        }
        String contentModel = null;
        fReportEntity = true;
        if (fEntityScanner.skipString("EMPTY")) {
            contentModel = "EMPTY";
            // call handler
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.empty(null);
            }
        }
        else if (fEntityScanner.skipString("ANY")) {
            contentModel = "ANY";
            // call handler
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.any(null);
            }
        }
        else {
            if (!fEntityScanner.skipChar('(', null)) {
                reportFatalError("MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN",
                new Object[]{name});
            }
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.startGroup(null);
            }
            fStringBuffer.clear();
            fStringBuffer.append('(');
            fMarkUpDepth++;
            skipSeparator(false, !scanningInternalSubset());

            // Mixed content model
            if (fEntityScanner.skipString("#PCDATA")) {
                scanMixed(name);
            }
            else {              // children content
                scanChildren(name);
            }
            contentModel = fStringBuffer.toString();
        }

        // call handler
        if (fDTDContentModelHandler != null) {
            fDTDContentModelHandler.endContentModel(null);
        }

        fReportEntity = false;
        skipSeparator(false, !scanningInternalSubset());
        // end
        if (!fEntityScanner.skipChar('>', null)) {
            reportFatalError("ElementDeclUnterminated", new Object[]{name});
        }
        fReportEntity = true;
        fMarkUpDepth--;

        // call handler
        if (fDTDHandler != null) {
            fDTDHandler.elementDecl(name, contentModel, null);
        }
        if (nonValidatingMode) nvGrammarInfo.elementDecl(name, contentModel, null);
    } // scanElementDecl()

    /**
     * scan Mixed content model
     * This assumes the content model has been parsed up to #PCDATA and
     * can simply append to fStringBuffer.
     * <pre>
     * [51]    Mixed    ::=    '(' S? '#PCDATA' (S? '|' S? Name)* S? ')*'
     *                       | '(' S? '#PCDATA' S? ')'
     * </pre>
     *
     * @param elName The element type name this declaration is about.
     *
     * <strong>Note:</strong> Called after scanning past '(#PCDATA'.
     */
    private final void scanMixed(String elName)
    throws IOException, XNIException {

        String childName = null;

        fStringBuffer.append("#PCDATA");
        // call handler
        if (fDTDContentModelHandler != null) {
            fDTDContentModelHandler.pcdata(null);
        }
        skipSeparator(false, !scanningInternalSubset());
        while (fEntityScanner.skipChar('|', null)) {
            fStringBuffer.append('|');
            // call handler
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.separator(XMLDTDContentModelHandler.SEPARATOR_CHOICE,
                null);
            }
            skipSeparator(false, !scanningInternalSubset());

            childName = fEntityScanner.scanName(NameType.ENTITY);
            if (childName == null) {
                reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT",
                new Object[]{elName});
            }
            fStringBuffer.append(childName);
            // call handler
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.element(childName, null);
            }
            skipSeparator(false, !scanningInternalSubset());
        }
        // The following check must be done in a single call (as opposed to one
        // for ')' and then one for '*') to guarantee that callbacks are
        // properly nested. We do not want to trigger endEntity too early in
        // case we cross the boundary of an entity between the two characters.
        if (fEntityScanner.skipString(")*")) {
            fStringBuffer.append(")*");
            // call handler
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.endGroup(null);
                fDTDContentModelHandler.occurrence(XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE,
                null);
            }
        }
        else if (childName != null) {
            reportFatalError("MixedContentUnterminated",
            new Object[]{elName});
        }
        else if (fEntityScanner.skipChar(')', null)){
            fStringBuffer.append(')');
            // call handler
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.endGroup(null);
            }
        }
        else {
            reportFatalError("MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN",
            new Object[]{elName});
        }
        fMarkUpDepth--;
        // we are done
    }

    /**
     * scan children content model
     * This assumes it can simply append to fStringBuffer.
     * <pre>
     * [47]    children  ::=    (choice | seq) ('?' | '*' | '+')?
     * [48]    cp        ::=    (Name | choice | seq) ('?' | '*' | '+')?
     * [49]    choice    ::=    '(' S? cp ( S? '|' S? cp )+ S? ')'
     * [50]    seq       ::=    '(' S? cp ( S? ',' S? cp )* S? ')'
     * </pre>
     *
     * @param elName The element type name this declaration is about.
     *
     * <strong>Note:</strong> Called after scanning past the first open
     * paranthesis.
     */
    private final void scanChildren(String elName)
    throws IOException, XNIException {

        fContentDepth = 0;
        pushContentStack(0);
        int currentOp = 0;
        int c;
        while (true) {
            if (fEntityScanner.skipChar('(', null)) {
                fMarkUpDepth++;
                fStringBuffer.append('(');
                // call handler
                if (fDTDContentModelHandler != null) {
                    fDTDContentModelHandler.startGroup(null);
                }
                // push current op on stack and reset it
                pushContentStack(currentOp);
                currentOp = 0;
                skipSeparator(false, !scanningInternalSubset());
                continue;
            }
            skipSeparator(false, !scanningInternalSubset());
            String childName = fEntityScanner.scanName(NameType.ELEMENTSTART);
            if (childName == null) {
                reportFatalError("MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN",
                new Object[]{elName});
                return;
            }
            // call handler
            if (fDTDContentModelHandler != null) {
                fDTDContentModelHandler.element(childName, null);
            }
            fStringBuffer.append(childName);
            c = fEntityScanner.peekChar();
            if (c == '?' || c == '*' || c == '+') {
                // call handler
                if (fDTDContentModelHandler != null) {
                    short oc;
                    if (c == '?') {
                        oc = XMLDTDContentModelHandler.OCCURS_ZERO_OR_ONE;
                    }
                    else if (c == '*') {
                        oc = XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE;
                    }
                    else {
                        oc = XMLDTDContentModelHandler.OCCURS_ONE_OR_MORE;
                    }
                    fDTDContentModelHandler.occurrence(oc, null);
                }
                fEntityScanner.scanChar(null);
                fStringBuffer.append((char)c);
            }
            while (true) {
                skipSeparator(false, !scanningInternalSubset());
                c = fEntityScanner.peekChar();
                if (c == ',' && currentOp != '|') {
                    currentOp = c;
                    // call handler
                    if (fDTDContentModelHandler != null) {
                        fDTDContentModelHandler.separator(XMLDTDContentModelHandler.SEPARATOR_SEQUENCE,
                        null);
                    }
                    fEntityScanner.scanChar(null);
                    fStringBuffer.append(',');
                    break;
                }
                else if (c == '|' && currentOp != ',') {
                    currentOp = c;
                    // call handler
                    if (fDTDContentModelHandler != null) {
                        fDTDContentModelHandler.separator(XMLDTDContentModelHandler.SEPARATOR_CHOICE,
                        null);
                    }
                    fEntityScanner.scanChar(null);
                    fStringBuffer.append('|');
                    break;
                }
                else if (c != ')') {
                    reportFatalError("MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN",
                    new Object[]{elName});
                }
                // call handler
                if (fDTDContentModelHandler != null) {
                    fDTDContentModelHandler.endGroup(null);
                }
                // restore previous op
                currentOp = popContentStack();
                short oc;
                // The following checks must be done in a single call (as
                // opposed to one for ')' and then one for '?', '*', and '+')
                // to guarantee that callbacks are properly nested. We do not
                // want to trigger endEntity too early in case we cross the
                // boundary of an entity between the two characters.
                if (fEntityScanner.skipString(")?")) {
                    fStringBuffer.append(")?");
                    // call handler
                    if (fDTDContentModelHandler != null) {
                        oc = XMLDTDContentModelHandler.OCCURS_ZERO_OR_ONE;
                        fDTDContentModelHandler.occurrence(oc, null);
                    }
                }
                else if (fEntityScanner.skipString(")+")) {
                    fStringBuffer.append(")+");
                    // call handler
                    if (fDTDContentModelHandler != null) {
                        oc = XMLDTDContentModelHandler.OCCURS_ONE_OR_MORE;
                        fDTDContentModelHandler.occurrence(oc, null);
                    }
                }
                else if (fEntityScanner.skipString(")*")) {
                    fStringBuffer.append(")*");
                    // call handler
                    if (fDTDContentModelHandler != null) {
                        oc = XMLDTDContentModelHandler.OCCURS_ZERO_OR_MORE;
                        fDTDContentModelHandler.occurrence(oc, null);
                    }
                }
                else {
                    // no occurrence specified
                    fEntityScanner.scanChar(null);
                    fStringBuffer.append(')');
                }
                fMarkUpDepth--;
                if (fContentDepth == 0) {
                    return;
                }
            }
            skipSeparator(false, !scanningInternalSubset());
        }
    }

    /**
     * Scans an attlist declaration
     * <p>
     * <pre>
     * [52]  AttlistDecl    ::=   '&lt;!ATTLIST' S Name AttDef* S? '>'
     * [53]  AttDef         ::=   S Name S AttType S DefaultDecl
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!ATTLIST'
     */
    protected final void scanAttlistDecl() throws IOException, XNIException {

        // spaces
        fReportEntity = false;
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL",
            null);
        }

        // element name
        String elName = fEntityScanner.scanName(NameType.ELEMENTSTART);
        if (elName == null) {
            reportFatalError("MSG_ELEMENT_TYPE_REQUIRED_IN_ATTLISTDECL",
            null);
        }

        // call handler
        if (fDTDHandler != null) {
            fDTDHandler.startAttlist(elName, null);
        }

        // spaces
        if (!skipSeparator(true, !scanningInternalSubset())) {
            // no space, is it the end yet?
            if (fEntityScanner.skipChar('>', null)) {
                // yes, stop here
                // call handler
                if (fDTDHandler != null) {
                    fDTDHandler.endAttlist(null);
                }
                fMarkUpDepth--;
                return;
            }
            else {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ATTRIBUTE_NAME_IN_ATTDEF",
                new Object[]{elName});
            }
        }

        // definitions
        while (!fEntityScanner.skipChar('>', null)) {
            String name = fEntityScanner.scanName(NameType.ATTRIBUTENAME);
            if (name == null) {
                reportFatalError("AttNameRequiredInAttDef",
                new Object[]{elName});
            }
            // spaces
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ATTTYPE_IN_ATTDEF",
                new Object[]{elName, name});
            }
            // type
            String type = scanAttType(elName, name);

            // spaces
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_DEFAULTDECL_IN_ATTDEF",
                new Object[]{elName, name});
            }

            // default decl
            String defaultType = scanAttDefaultDecl(elName, name,
            type,
            fLiteral, fLiteral2);
            // REVISIT: Should we do anything with the non-normalized
            //          default attribute value? -Ac
            // yes--according to bug 5073.  - neilg
            String[] enumr = null;
            if( fDTDHandler != null || nonValidatingMode){
                if (fEnumerationCount != 0) {
                    enumr = new String[fEnumerationCount];
                    System.arraycopy(fEnumeration, 0, enumr,
                    0, fEnumerationCount);
                }
            }
            // call handler
            // Determine whether the default value to be passed should be null.
            // REVISIT: should probably check whether fLiteral.ch is null instead. LM.
            if (defaultType!=null && (defaultType.equals("#REQUIRED") ||
            defaultType.equals("#IMPLIED"))) {
                if (fDTDHandler != null){
                    fDTDHandler.attributeDecl(elName, name, type, enumr,
                    defaultType, null, null, null);
                }
                if(nonValidatingMode){
                    nvGrammarInfo.attributeDecl(elName, name, type, enumr,
                    defaultType, null, null, null);

                }
            }
            else {
                if (fDTDHandler != null){
                    fDTDHandler.attributeDecl(elName, name, type, enumr,
                    defaultType, fLiteral, fLiteral2, null);
                }
                if(nonValidatingMode){
                    nvGrammarInfo.attributeDecl(elName, name, type, enumr,
                    defaultType, fLiteral, fLiteral2, null);
                }
            }
            skipSeparator(false, !scanningInternalSubset());
        }

        // call handler
        if (fDTDHandler != null) {
            fDTDHandler.endAttlist(null);
        }
        fMarkUpDepth--;
        fReportEntity = true;

    } // scanAttlistDecl()

    /**
     * Scans an attribute type definition
     * <p>
     * <pre>
     * [54]  AttType        ::=   StringType | TokenizedType | EnumeratedType
     * [55]  StringType     ::=   'CDATA'
     * [56]  TokenizedType  ::=   'ID'
     *                          | 'IDREF'
     *                          | 'IDREFS'
     *                          | 'ENTITY'
     *                          | 'ENTITIES'
     *                          | 'NMTOKEN'
     *                          | 'NMTOKENS'
     * [57]  EnumeratedType ::=    NotationType | Enumeration
     * [58]  NotationType ::= 'NOTATION' S '(' S? Name (S? '|' S? Name)* S? ')'
     * [59]  Enumeration    ::=    '(' S? Nmtoken (S? '|' S? Nmtoken)* S? ')'
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!ATTLIST'
     *
     * @param elName The element type name this declaration is about.
     * @param atName The attribute name this declaration is about.
     */
    private final String scanAttType(String elName, String atName)
    throws IOException, XNIException {

        String type = null;
        fEnumerationCount = 0;
        /*
         * Watchout: the order here is important: when a string happens to
         * be a substring of another string, the longer one needs to be
         * looked for first!!
         */
        if (fEntityScanner.skipString("CDATA")) {
            type = "CDATA";
        }
        else if (fEntityScanner.skipString("IDREFS")) {
            type = "IDREFS";
        }
        else if (fEntityScanner.skipString("IDREF")) {
            type = "IDREF";
        }
        else if (fEntityScanner.skipString("ID")) {
            type = "ID";
        }
        else if (fEntityScanner.skipString("ENTITY")) {
            type = "ENTITY";
        }
        else if (fEntityScanner.skipString("ENTITIES")) {
            type = "ENTITIES";
        }
        else if (fEntityScanner.skipString("NMTOKENS")) {
            type = "NMTOKENS";
        }
        else if (fEntityScanner.skipString("NMTOKEN")) {
            type = "NMTOKEN";
        }
        else if (fEntityScanner.skipString("NOTATION")) {
            type = "NOTATION";
            // spaces
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_AFTER_NOTATION_IN_NOTATIONTYPE",
                new Object[]{elName, atName});
            }
            // open paren
            int c = fEntityScanner.scanChar(null);
            if (c != '(') {
                reportFatalError("MSG_OPEN_PAREN_REQUIRED_IN_NOTATIONTYPE",
                new Object[]{elName, atName});
            }
            fMarkUpDepth++;
            do {
                skipSeparator(false, !scanningInternalSubset());
                String aName = fEntityScanner.scanName(NameType.ATTRIBUTENAME);
                if (aName == null) {
                    reportFatalError("MSG_NAME_REQUIRED_IN_NOTATIONTYPE",
                    new Object[]{elName, atName});
                }
                ensureEnumerationSize(fEnumerationCount + 1);
                fEnumeration[fEnumerationCount++] = aName;
                skipSeparator(false, !scanningInternalSubset());
                c = fEntityScanner.scanChar(null);
            } while (c == '|');
            if (c != ')') {
                reportFatalError("NotationTypeUnterminated",
                new Object[]{elName, atName});
            }
            fMarkUpDepth--;
        }
        else {              // Enumeration
            type = "ENUMERATION";
            // open paren
            int c = fEntityScanner.scanChar(null);
            if (c != '(') {
                //                       "OPEN_PAREN_REQUIRED_BEFORE_ENUMERATION_IN_ATTRDECL",
                reportFatalError("AttTypeRequiredInAttDef",
                new Object[]{elName, atName});
            }
            fMarkUpDepth++;
            do {
                skipSeparator(false, !scanningInternalSubset());
                String token = fEntityScanner.scanNmtoken();
                if (token == null) {
                    reportFatalError("MSG_NMTOKEN_REQUIRED_IN_ENUMERATION",
                    new Object[]{elName, atName});
                }
                ensureEnumerationSize(fEnumerationCount + 1);
                fEnumeration[fEnumerationCount++] = token;
                skipSeparator(false, !scanningInternalSubset());
                c = fEntityScanner.scanChar(null);
            } while (c == '|');
            if (c != ')') {
                reportFatalError("EnumerationUnterminated",
                new Object[]{elName, atName});
            }
            fMarkUpDepth--;
        }
        return type;

    } // scanAttType():String


    /**
     * Scans an attribute default declaration
     * <p>
     * <pre>
     * [60] DefaultDecl ::= '#REQUIRED' | '#IMPLIED' | (('#FIXED' S)? AttValue)
     * </pre>
     *
     * @param name The name of the attribute being scanned.
     * @param defaultVal The string to fill in with the default value.
     */
    protected final String scanAttDefaultDecl(String elName, String atName,
    String type,
    XMLString defaultVal,
    XMLString nonNormalizedDefaultVal)
    throws IOException, XNIException {

        String defaultType = null;
        fString.clear();
        defaultVal.clear();
        if (fEntityScanner.skipString("#REQUIRED")) {
            defaultType = "#REQUIRED";
        }
        else if (fEntityScanner.skipString("#IMPLIED")) {
            defaultType = "#IMPLIED";
        }
        else {
            if (fEntityScanner.skipString("#FIXED")) {
                defaultType = "#FIXED";
                // spaces
                if (!skipSeparator(true, !scanningInternalSubset())) {
                    reportFatalError("MSG_SPACE_REQUIRED_AFTER_FIXED_IN_DEFAULTDECL",
                    new Object[]{elName, atName});
                }
            }
            // AttValue
            boolean isVC = !fStandalone  &&  (fSeenExternalDTD || fSeenExternalPE) ;
            scanAttributeValue(defaultVal, nonNormalizedDefaultVal, atName,
            fAttributes, 0, isVC, elName, false);
        }
        return defaultType;

    } // ScanAttDefaultDecl

    /**
     * Scans an entity declaration
     * <p>
     * <pre>
     * [70]    EntityDecl  ::=    GEDecl | PEDecl
     * [71]    GEDecl      ::=    '&lt;!ENTITY' S Name S EntityDef S? '>'
     * [72]    PEDecl      ::=    '&lt;!ENTITY' S '%' S Name S PEDef S? '>'
     * [73]    EntityDef   ::=    EntityValue | (ExternalID NDataDecl?)
     * [74]    PEDef       ::=    EntityValue | ExternalID
     * [75]    ExternalID  ::=    'SYSTEM' S SystemLiteral
     *                          | 'PUBLIC' S PubidLiteral S SystemLiteral
     * [76]    NDataDecl   ::=    S 'NDATA' S Name
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!ENTITY'
     */
    private final void scanEntityDecl() throws IOException, XNIException {

        boolean isPEDecl = false;
        boolean sawPERef = false;
        fReportEntity = false;
        if (fEntityScanner.skipSpaces()) {
            if (!fEntityScanner.skipChar('%', NameType.REFERENCE)) {
                isPEDecl = false; // <!ENTITY x "x">
            }
            else if (skipSeparator(true, !scanningInternalSubset())) {
                // <!ENTITY % x "x">
                isPEDecl = true;
            }
            else if (scanningInternalSubset()) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL",
                null);
                isPEDecl = true;
            }
            else if (fEntityScanner.peekChar() == '%') {
                // <!ENTITY %%x; "x"> is legal
                skipSeparator(false, !scanningInternalSubset());
                isPEDecl = true;
            }
            else {
                sawPERef = true;
            }
        }
        else if (scanningInternalSubset() || !fEntityScanner.skipChar('%', NameType.REFERENCE)) {
            // <!ENTITY[^ ]...> or <!ENTITY[^ %]...>
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL",
            null);
            isPEDecl = false;
        }
        else if (fEntityScanner.skipSpaces()) {
            // <!ENTITY% ...>
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_PERCENT_IN_PEDECL",
            null);
            isPEDecl = false;
        }
        else {
            sawPERef = true;
        }
        if (sawPERef) {
            while (true) {
                String peName = fEntityScanner.scanName(NameType.REFERENCE);
                if (peName == null) {
                    reportFatalError("NameRequiredInPEReference", null);
                }
                else if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
                    reportFatalError("SemicolonRequiredInPEReference",
                    new Object[]{peName});
                }
                else {
                    startPE(peName, false);
                }
                fEntityScanner.skipSpaces();
                if (!fEntityScanner.skipChar('%', NameType.REFERENCE))
                    break;
                if (!isPEDecl) {
                    if (skipSeparator(true, !scanningInternalSubset())) {
                        isPEDecl = true;
                        break;
                    }
                    isPEDecl = fEntityScanner.skipChar('%', NameType.REFERENCE);
                }
            }
        }

        // name
        String name = fEntityScanner.scanName(NameType.ENTITY);
        if (name == null) {
            reportFatalError("MSG_ENTITY_NAME_REQUIRED_IN_ENTITYDECL", null);
        }

        // spaces
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL",
            new Object[]{name});
        }

        // external id
        scanExternalID(fStrings, false);
        String systemId = fStrings[0];
        String publicId = fStrings[1];

        if (isPEDecl && systemId != null) {
            fSeenExternalPE = true;
        }

        String notation = null;
        // NDATA
        boolean sawSpace = skipSeparator(true, !scanningInternalSubset());
        if (!isPEDecl && fEntityScanner.skipString("NDATA")) {
            // check whether there was space before NDATA
            if (!sawSpace) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NDATA_IN_UNPARSED_ENTITYDECL",
                new Object[]{name});
            }

            // spaces
            if (!skipSeparator(true, !scanningInternalSubset())) {
                reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_UNPARSED_ENTITYDECL",
                new Object[]{name});
            }
            notation = fEntityScanner.scanName(NameType.NOTATION);
            if (notation == null) {
                reportFatalError("MSG_NOTATION_NAME_REQUIRED_FOR_UNPARSED_ENTITYDECL",
                new Object[]{name});
            }
        }

        // internal entity
        if (systemId == null) {
            scanEntityValue(name, isPEDecl, fLiteral, fLiteral2);
            // since we need it's value anyway, let's snag it so it doesn't get corrupted
            // if a new load takes place before we store the entity values
            fStringBuffer.clear();
            fStringBuffer2.clear();
            fStringBuffer.append(fLiteral.ch, fLiteral.offset, fLiteral.length);
            fStringBuffer2.append(fLiteral2.ch, fLiteral2.offset, fLiteral2.length);
        }

        // skip possible trailing space
        skipSeparator(false, !scanningInternalSubset());

        // end
        if (!fEntityScanner.skipChar('>', null)) {
            reportFatalError("EntityDeclUnterminated", new Object[]{name});
        }
        fMarkUpDepth--;

        // register entity and make callback
        if (isPEDecl) {
            name = "%" + name;
        }
        if (systemId != null) {
            String baseSystemId = fEntityScanner.getBaseSystemId();
            if (notation != null) {
                fEntityStore.addUnparsedEntity(name, publicId, systemId, baseSystemId, notation);
            }
            else {
                fEntityStore.addExternalEntity(name, publicId, systemId,
                baseSystemId);
            }
            if (fDTDHandler != null) {
                //Venu Revisit : why false has been removed in expandSYstem
                fResourceIdentifier.setValues(publicId, systemId, baseSystemId, XMLEntityManager.expandSystemId(systemId, baseSystemId ));

                if (notation != null) {
                    fDTDHandler.unparsedEntityDecl(name, fResourceIdentifier,
                    notation, null);
                }
                else {
                    fDTDHandler.externalEntityDecl(name, fResourceIdentifier, null);
                }
            }
        }
        else {
            fEntityStore.addInternalEntity(name, fStringBuffer.toString());
            if (fDTDHandler != null) {
                fDTDHandler.internalEntityDecl(name, fStringBuffer, fStringBuffer2, null);
            }
        }
        fReportEntity = true;

    } // scanEntityDecl()

    /**
     * Scans an entity value.
     *
     * @param value The string to fill in with the value.
     * @param nonNormalizedValue The string to fill in with the
     *                           non-normalized value.
     *
     * <strong>Note:</strong> This method uses fString, fStringBuffer (through
     * the use of scanCharReferenceValue), and fStringBuffer2, anything in them
     * at the time of calling is lost.
     */
    protected final void scanEntityValue(String entityName, boolean isPEDecl, XMLString value,
    XMLString nonNormalizedValue)
    throws IOException, XNIException {
        int quote = fEntityScanner.scanChar(null);
        if (quote != '\'' && quote != '"') {
            reportFatalError("OpenQuoteMissingInDecl", null);
        }
        // store at which depth of entities we start
        int entityDepth = fEntityDepth;

        XMLString literal = fString;
        XMLString literal2 = fString;
        int countChar = 0;
        if (fLimitAnalyzer == null ) {
            fLimitAnalyzer = fEntityManager.fLimitAnalyzer;
         }
        fLimitAnalyzer.startEntity(entityName);

        if (fEntityScanner.scanLiteral(quote, fString, false) != quote) {
            fStringBuffer.clear();
            fStringBuffer2.clear();
            int offset;
            do {
                countChar = 0;
                offset = fStringBuffer.length;
                fStringBuffer.append(fString);
                fStringBuffer2.append(fString);
                if (fEntityScanner.skipChar('&', NameType.REFERENCE)) {
                    if (fEntityScanner.skipChar('#', NameType.REFERENCE)) {
                        fStringBuffer2.append("&#");
                        scanCharReferenceValue(fStringBuffer, fStringBuffer2);
                    }
                    else {
                        fStringBuffer.append('&');
                        fStringBuffer2.append('&');
                        String eName = fEntityScanner.scanName(NameType.REFERENCE);
                        if (eName == null) {
                            reportFatalError("NameRequiredInReference",
                            null);
                        }
                        else {
                            fStringBuffer.append(eName);
                            fStringBuffer2.append(eName);
                        }
                        if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
                            reportFatalError("SemicolonRequiredInReference",
                            new Object[]{eName});
                        }
                        else {
                            fStringBuffer.append(';');
                            fStringBuffer2.append(';');
                        }
                    }
                }
                else if (fEntityScanner.skipChar('%', NameType.REFERENCE)) {
                    while (true) {
                        fStringBuffer2.append('%');
                        String peName = fEntityScanner.scanName(NameType.REFERENCE);
                        if (peName == null) {
                            reportFatalError("NameRequiredInPEReference",
                            null);
                        }
                        else if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
                            reportFatalError("SemicolonRequiredInPEReference",
                            new Object[]{peName});
                        }
                        else {
                            if (scanningInternalSubset()) {
                                reportFatalError("PEReferenceWithinMarkup",
                                new Object[]{peName});
                            }
                            fStringBuffer2.append(peName);
                            fStringBuffer2.append(';');
                        }
                        startPE(peName, true);
                        // REVISIT: [Q] Why do we skip spaces here? -Ac
                        // REVISIT: This will make returning the non-
                        //          normalized value harder. -Ac
                        fEntityScanner.skipSpaces();
                        if (!fEntityScanner.skipChar('%', NameType.REFERENCE))
                            break;
                    }
                }
                else {
                    int c = fEntityScanner.peekChar();
                    if (XMLChar.isHighSurrogate(c)) {
                        countChar++;
                        scanSurrogates(fStringBuffer2);
                    }
                    else if (isInvalidLiteral(c)) {
                        reportFatalError("InvalidCharInLiteral",
                        new Object[]{Integer.toHexString(c)});
                        fEntityScanner.scanChar(null);
                    }
                    // if it's not the delimiting quote or if it is but from a
                    // different entity than the one this literal started from,
                    // simply append the character to our buffer
                    else if (c != quote || entityDepth != fEntityDepth) {
                        fStringBuffer.append((char)c);
                        fStringBuffer2.append((char)c);
                        fEntityScanner.scanChar(null);
                    }
                }
                checkEntityLimit(isPEDecl, entityName, fStringBuffer.length - offset + countChar);
            } while (fEntityScanner.scanLiteral(quote, fString, false) != quote);
            checkEntityLimit(isPEDecl, entityName, fString.length);
            fStringBuffer.append(fString);
            fStringBuffer2.append(fString);
            literal = fStringBuffer;
            literal2 = fStringBuffer2;
        } else {
            checkEntityLimit(isPEDecl, entityName, literal);
        }
        value.setValues(literal);
        nonNormalizedValue.setValues(literal2);
        if (fLimitAnalyzer != null) {
            if (isPEDecl) {
                fLimitAnalyzer.endEntity(XMLSecurityManager.Limit.PARAMETER_ENTITY_SIZE_LIMIT, entityName);
            } else {
                fLimitAnalyzer.endEntity(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT, entityName);
            }
        }

        if (!fEntityScanner.skipChar(quote, null)) {
            reportFatalError("CloseQuoteMissingInDecl", null);
        }
    } // scanEntityValue(XMLString,XMLString):void

    /**
     * Scans a notation declaration
     * <p>
     * <pre>
     * [82] NotationDecl ::= '&lt;!NOTATION' S Name S (ExternalID|PublicID) S? '>'
     * [83]  PublicID    ::= 'PUBLIC' S PubidLiteral
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;!NOTATION'
     */
    private final void scanNotationDecl() throws IOException, XNIException {

        // spaces
        fReportEntity = false;
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_NOTATIONDECL",
            null);
        }

        // notation name
        String name = fEntityScanner.scanName(NameType.NOTATION);
        if (name == null) {
            reportFatalError("MSG_NOTATION_NAME_REQUIRED_IN_NOTATIONDECL",
            null);
        }

        // spaces
        if (!skipSeparator(true, !scanningInternalSubset())) {
            reportFatalError("MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL",
            new Object[]{name});
        }

        // external id
        scanExternalID(fStrings, true);
        String systemId = fStrings[0];
        String publicId = fStrings[1];
        String baseSystemId = fEntityScanner.getBaseSystemId();

        if (systemId == null && publicId == null) {
            reportFatalError("ExternalIDorPublicIDRequired",
            new Object[]{name});
        }

        // skip possible trailing space
        skipSeparator(false, !scanningInternalSubset());

        // end
        if (!fEntityScanner.skipChar('>', null)) {
            reportFatalError("NotationDeclUnterminated", new Object[]{name});
        }
        fMarkUpDepth--;

        fResourceIdentifier.setValues(publicId, systemId, baseSystemId, XMLEntityManager.expandSystemId(systemId, baseSystemId ));
        if (nonValidatingMode) nvGrammarInfo.notationDecl(name, fResourceIdentifier, null);
        // call handler
        if (fDTDHandler != null) {
            //Venu Revisit wby false has been removed.
            //fResourceIdentifier.setValues(publicId, systemId, baseSystemId, XMLEntityManager.expandSystemId(systemId, baseSystemId, false));
            fDTDHandler.notationDecl(name, fResourceIdentifier, null);
        }
        fReportEntity = true;

    } // scanNotationDecl()

    /**
     * Scans a conditional section. If it's a section to ignore the whole
     * section gets scanned through and this method only returns after the
     * closing bracket has been found. When it's an include section though, it
     * returns to let the main loop take care of scanning it. In that case the
     * end of the section if handled by the main loop (scanDecls).
     * <p>
     * <pre>
     * [61] conditionalSect   ::= includeSect | ignoreSect
     * [62] includeSect       ::= '&lt;![' S? 'INCLUDE' S? '[' extSubsetDecl ']]>'
     * [63] ignoreSect   ::= '&lt;![' S? 'IGNORE' S? '[' ignoreSectContents* ']]>'
     * [64] ignoreSectContents ::= Ignore ('&lt;![' ignoreSectContents ']]>' Ignore)*
     * [65] Ignore            ::=    Char* - (Char* ('&lt;![' | ']]>') Char*)
     * </pre>
     * <p>
     * <strong>Note:</strong> Called after scanning past '&lt;![' */
    private final void scanConditionalSect(int currPEDepth)
    throws IOException, XNIException {

        fReportEntity = false;
        skipSeparator(false, !scanningInternalSubset());

        if (fEntityScanner.skipString("INCLUDE")) {
            skipSeparator(false, !scanningInternalSubset());
            if(currPEDepth != fPEDepth && fValidation) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                "INVALID_PE_IN_CONDITIONAL",
                new Object[]{ fEntityManager.fCurrentEntity.name},
                XMLErrorReporter.SEVERITY_ERROR);
            }
            // call handler
            if (!fEntityScanner.skipChar('[', null)) {
                reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
            }

            if (fDTDHandler != null) {
                fDTDHandler.startConditional(XMLDTDHandler.CONDITIONAL_INCLUDE,
                null);
            }
            fIncludeSectDepth++;
            // just stop there and go back to the main loop
            fReportEntity = true;
        }
        else if (fEntityScanner.skipString("IGNORE")) {
            skipSeparator(false, !scanningInternalSubset());
            if(currPEDepth != fPEDepth && fValidation) {
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                "INVALID_PE_IN_CONDITIONAL",
                new Object[]{ fEntityManager.fCurrentEntity.name},
                XMLErrorReporter.SEVERITY_ERROR);
            }
            // call handler
            if (fDTDHandler != null) {
                fDTDHandler.startConditional(XMLDTDHandler.CONDITIONAL_IGNORE,
                null);
            }
            if (!fEntityScanner.skipChar('[', null)) {
                reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
            }
            fReportEntity = true;
            int initialDepth = ++fIncludeSectDepth;
            if (fDTDHandler != null) {
                fIgnoreConditionalBuffer.clear();
            }
            while (true) {
                if (fEntityScanner.skipChar('<', null)) {
                    if (fDTDHandler != null) {
                        fIgnoreConditionalBuffer.append('<');
                    }
                    //
                    // These tests are split so that we handle cases like
                    // '<<![' and '<!<![' which we might otherwise miss.
                    //
                    if (fEntityScanner.skipChar('!', null)) {
                        if(fEntityScanner.skipChar('[', null)) {
                            if (fDTDHandler != null) {
                                fIgnoreConditionalBuffer.append("![");
                            }
                            fIncludeSectDepth++;
                        } else {
                            if (fDTDHandler != null) {
                                fIgnoreConditionalBuffer.append("!");
                            }
                        }
                    }
                }
                else if (fEntityScanner.skipChar(']', null)) {
                    if (fDTDHandler != null) {
                        fIgnoreConditionalBuffer.append(']');
                    }
                    //
                    // The same thing goes for ']<![' and '<]]>', etc.
                    //
                    if (fEntityScanner.skipChar(']', null)) {
                        if (fDTDHandler != null) {
                            fIgnoreConditionalBuffer.append(']');
                        }
                        while (fEntityScanner.skipChar(']', null)) {
                            /* empty loop body */
                            if (fDTDHandler != null) {
                                fIgnoreConditionalBuffer.append(']');
                            }
                        }
                        if (fEntityScanner.skipChar('>', null)) {
                            if (fIncludeSectDepth-- == initialDepth) {
                                fMarkUpDepth--;
                                // call handler
                                if (fDTDHandler != null) {
                                    fLiteral.setValues(fIgnoreConditionalBuffer.ch, 0,
                                    fIgnoreConditionalBuffer.length - 2);
                                    fDTDHandler.ignoredCharacters(fLiteral, null);
                                    fDTDHandler.endConditional(null);
                                }
                                return;
                            } else if(fDTDHandler != null) {
                                fIgnoreConditionalBuffer.append('>');
                            }
                        }
                    }
                }
                else {
                    int c = fEntityScanner.scanChar(null);
                    if (fScannerState == SCANNER_STATE_END_OF_INPUT) {
                        reportFatalError("IgnoreSectUnterminated", null);
                        return;
                    }
                    if (fDTDHandler != null) {
                        fIgnoreConditionalBuffer.append((char)c);
                    }
                }
            }
        }
        else {
            reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
        }

    } // scanConditionalSect()

    /**
     * Dispatch an XML "event".
     *
     * @param complete True if this method is intended to scan
     *                 and dispatch as much as possible.
     *
     * @return True if there is more to scan.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown on parse error.
     *
     */
    protected final boolean scanDecls(boolean complete)
    throws IOException, XNIException {

        skipSeparator(false, true);
        boolean again = true;
        //System.out.println("scanDecls"+fScannerState);
        while (again && fScannerState == SCANNER_STATE_MARKUP_DECL) {
            again = complete;
            if (fEntityScanner.skipChar('<', null)) {
                fMarkUpDepth++;
                if (fEntityScanner.skipChar('?', null)) {
                    fStringBuffer.clear();
                    scanPI(fStringBuffer);
                    fMarkUpDepth--; // we're done with this decl
                }
                else if (fEntityScanner.skipChar('!', null)) {
                    if (fEntityScanner.skipChar('-', null)) {
                        if (!fEntityScanner.skipChar('-', null)) {
                            reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD",
                            null);
                        } else {
                            scanComment();
                        }
                    }
                    else if (fEntityScanner.skipString("ELEMENT")) {
                        scanElementDecl();
                    }
                    else if (fEntityScanner.skipString("ATTLIST")) {
                        scanAttlistDecl();
                    }
                    else if (fEntityScanner.skipString("ENTITY")) {
                        scanEntityDecl();
                    }
                    else if (fEntityScanner.skipString("NOTATION")) {
                        scanNotationDecl();
                    }
                    else if (fEntityScanner.skipChar('[', null) &&
                    !scanningInternalSubset()) {
                        scanConditionalSect(fPEDepth);
                    }
                    else {
                        fMarkUpDepth--;
                        reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD",
                        null);
                    }
                }
                else {
                    fMarkUpDepth--;
                    reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
                }
            }
            else if (fIncludeSectDepth > 0 && fEntityScanner.skipChar(']', null)) {
                // end of conditional section?
                if (!fEntityScanner.skipChar(']', null)
                || !fEntityScanner.skipChar('>', null)) {
                    reportFatalError("IncludeSectUnterminated", null);
                }
                // call handler
                if (fDTDHandler != null) {
                    fDTDHandler.endConditional(null);
                }
                // decreaseMarkupDepth();
                fIncludeSectDepth--;
                fMarkUpDepth--;
            }
            else if (scanningInternalSubset() &&
            fEntityScanner.peekChar() == ']') {
                // this is the end of the internal subset, let's stop here
                return false;
            }
            else if (fEntityScanner.skipSpaces()) {
                // simply skip
            }
            else {
                reportFatalError("MSG_MARKUP_NOT_RECOGNIZED_IN_DTD", null);
            }
            skipSeparator(false, true);
        }
        return fScannerState != SCANNER_STATE_END_OF_INPUT;
    }

    /**
     * Skip separator. This is typically just whitespace but it can also be one
     * or more parameter entity references.
     * <p>
     * If there are some it "expands them" by calling the corresponding entity
     * from the entity manager.
     * <p>
     * This is recursive and will process has many refs as possible.
     *
     * @param spaceRequired Specify whether some leading whitespace should be
     *                      found
     * @param lookForPERefs Specify whether parameter entity references should
     *                      be looked for
     * @return True if any leading whitespace was found or the end of a
     *         parameter entity was crossed.
     */
    private boolean skipSeparator(boolean spaceRequired, boolean lookForPERefs)
    throws IOException, XNIException {
        int depth = fPEDepth;
        boolean sawSpace = fEntityScanner.skipSpaces();
        if (!lookForPERefs || !fEntityScanner.skipChar('%', NameType.REFERENCE)) {
            return !spaceRequired || sawSpace || (depth != fPEDepth);
        }
        while (true) {
            String name = fEntityScanner.scanName(NameType.ENTITY);
            if (name == null) {
                reportFatalError("NameRequiredInPEReference", null);
            }
            else if (!fEntityScanner.skipChar(';', NameType.REFERENCE)) {
                reportFatalError("SemicolonRequiredInPEReference",
                new Object[]{name});
            }
            startPE(name, false);
            fEntityScanner.skipSpaces();
            if (!fEntityScanner.skipChar('%', NameType.REFERENCE))
                return true;
        }
    }


    /*
     * Element Children Content Stack
     */
    private final void pushContentStack(int c) {
        if (fContentStack.length == fContentDepth) {
            int[] newStack = new int[fContentDepth * 2];
            System.arraycopy(fContentStack, 0, newStack, 0, fContentDepth);
            fContentStack = newStack;
        }
        fContentStack[fContentDepth++] = c;
    }

    private final int popContentStack() {
        return fContentStack[--fContentDepth];
    }


    /*
     * Parameter Entity Stack
     */
    private final void pushPEStack(int depth, boolean report) {
        if (fPEStack.length == fPEDepth) {
            int[] newIntStack = new int[fPEDepth * 2];
            System.arraycopy(fPEStack, 0, newIntStack, 0, fPEDepth);
            fPEStack = newIntStack;
            // report end/start calls
            boolean[] newBooleanStack = new boolean[fPEDepth * 2];
            System.arraycopy(fPEReport, 0, newBooleanStack, 0, fPEDepth);
            fPEReport = newBooleanStack;

        }
        fPEReport[fPEDepth] = report;
        fPEStack[fPEDepth++] = depth;
    }

    /** pop the stack */
    private final int popPEStack() {
        return fPEStack[--fPEDepth];
    }

    /** look at the top of the stack */
    private final boolean peekReportEntity() {
        return fPEReport[fPEDepth-1];
    }


    /*
     * Utility method
     */
    private final void ensureEnumerationSize(int size) {
        if (fEnumeration.length == size) {
            String[] newEnum = new String[size * 2];
            System.arraycopy(fEnumeration, 0, newEnum, 0, size);
            fEnumeration = newEnum;
        }
    }

    // private methods
    private void init() {
        // reset state related data
        fStartDTDCalled = false;
        fExtEntityDepth = 0;
        fIncludeSectDepth = 0;
        fMarkUpDepth = 0;
        fPEDepth = 0;

        fStandalone = false;
        fSeenExternalDTD = false;
        fSeenExternalPE = false;

        // set starting state
        setScannerState(SCANNER_STATE_TEXT_DECL);
        //new SymbolTable());

        fLimitAnalyzer = fEntityManager.fLimitAnalyzer;
        fSecurityManager = fEntityManager.fSecurityManager;
    }

    public DTDGrammar getGrammar(){
        return nvGrammarInfo;
    }

} // class XMLDTDScannerImpl
