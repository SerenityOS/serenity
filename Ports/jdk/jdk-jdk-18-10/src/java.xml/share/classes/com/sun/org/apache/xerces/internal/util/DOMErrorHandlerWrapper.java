/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.util;

import com.sun.org.apache.xerces.internal.dom.DOMErrorImpl;
import com.sun.org.apache.xerces.internal.dom.DOMLocatorImpl;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;
import org.w3c.dom.Node;

/**
 * This class handles DOM errors .
 *
 * @see DOMErrorHandler
 *
 * @author Gopal Sharma, SUN Microsystems Inc.
 */

// REVISIT: current implementations wraps error several times:
//          XMLErrorReport.reportError creates XMLParserException (by wrapping all info)
//          and goes via switch to send errors.
//          DOMErrorHandlerWrapper catches calls, copies info from XMLParserException and
//          sends one call back to the application
//          I think we can avoid this indirection if we modify XMLErrorReporter. --el

public class DOMErrorHandlerWrapper
    implements XMLErrorHandler, DOMErrorHandler {



    // It keeps the reference of DOMErrorHandler of application
    protected DOMErrorHandler fDomErrorHandler;

    // Error Status
    boolean eStatus = true ;

    // Print writer
    protected PrintWriter fOut;

    // some components may set error node
    // @see DOMNormalizer.
    public Node fCurrentNode;

    /** Error code for comparisons. **/
    protected final XMLErrorCode fErrorCode = new XMLErrorCode(null, null);

    protected final DOMErrorImpl fDOMError = new DOMErrorImpl();



    //
    // Constructors
    //

    // Default constructor /

    public DOMErrorHandlerWrapper() {
        fOut = new PrintWriter(System.err);
    }


    public DOMErrorHandlerWrapper(DOMErrorHandler domErrorHandler) {
        fDomErrorHandler = domErrorHandler;
    } // DOMErrorHandlerWrapper(DOMErrorHandler domErrorHandler)


    //
    // Public methods
    //

    /** Sets the DOM error handler. */
    public void setErrorHandler(DOMErrorHandler errorHandler) {
        fDomErrorHandler = errorHandler;
    } // setErrorHandler(ErrorHandler)


    public DOMErrorHandler getErrorHandler(){
        return fDomErrorHandler;
    } //getErrorHandler()

    //
    // XMLErrorHandler methods
    //

    /**
     * Reports a warning. Warnings are non-fatal and can be safely ignored
     * by most applications.
     *
     * @param domain    The domain of the warning. The domain can be any
     *                  string but is suggested to be a valid URI. The
     *                  domain can be used to conveniently specify a web
     *                  site location of the relevent specification or
     *                  document pertaining to this warning.
     * @param key       The warning key. This key can be any string and
     *                  is implementation dependent.
     * @param exception Exception.
     *
     * @throws XNIException Thrown to signal that the parser should stop
     *                      parsing the document.
     */

    public void warning(String domain, String key,
                        XMLParseException exception) throws XNIException {
        fDOMError.fSeverity = DOMError.SEVERITY_WARNING;
        fDOMError.fException = exception;
        // REVISIT: May need to lookup from DOMErrorTypeMap in the future.
        fDOMError.fType = key;
        fDOMError.fRelatedData = fDOMError.fMessage = exception.getMessage();
        DOMLocatorImpl locator = fDOMError.fLocator;
        if (locator != null) {
            locator.fColumnNumber = exception.getColumnNumber();
            locator.fLineNumber = exception.getLineNumber();
            locator.fUtf16Offset = exception.getCharacterOffset();
            locator.fUri = exception.getExpandedSystemId();
            locator.fRelatedNode = fCurrentNode;
        }
        if (fDomErrorHandler != null) {
            fDomErrorHandler.handleError(fDOMError);
        }
    } // warning(String,String,XMLParseException)

    /**
     * Reports an error. Errors are non-fatal and usually signify that the
     * document is invalid with respect to its grammar(s).
     *
     * @param domain    The domain of the error. The domain can be any
     *                  string but is suggested to be a valid URI. The
     *                  domain can be used to conveniently specify a web
     *                  site location of the relevent specification or
     *                  document pertaining to this error.
     * @param key       The error key. This key can be any string and
     *                  is implementation dependent.
     * @param exception Exception.
     *
     * @throws XNIException Thrown to signal that the parser should stop
     *                      parsing the document.
     */
    public void error(String domain, String key,
                      XMLParseException exception) throws XNIException {
        fDOMError.fSeverity = DOMError.SEVERITY_ERROR;
        fDOMError.fException = exception;
        // REVISIT: May need to lookup from DOMErrorTypeMap in the future.
        fDOMError.fType = key;
        fDOMError.fRelatedData = fDOMError.fMessage = exception.getMessage();
        DOMLocatorImpl locator = fDOMError.fLocator;
        if (locator != null) {
            locator.fColumnNumber = exception.getColumnNumber();
            locator.fLineNumber = exception.getLineNumber();
            locator.fUtf16Offset = exception.getCharacterOffset();
            locator.fUri = exception.getExpandedSystemId();
            locator.fRelatedNode= fCurrentNode;
        }
        if (fDomErrorHandler != null) {
            fDomErrorHandler.handleError(fDOMError);
        }
    } // error(String,String,XMLParseException)

    /**
     * Report a fatal error. Fatal errors usually occur when the document
     * is not well-formed and signifies that the parser cannot continue
     * normal operation.
     * <p>
     * <strong>Note:</strong> The error handler should <em>always</em>
     * throw an <code>XNIException</code> from this method. This exception
     * can either be the same exception that is passed as a parameter to
     * the method or a new XNI exception object. If the registered error
     * handler fails to throw an exception, the continuing operation of
     * the parser is undetermined.
     *
     * @param domain    The domain of the fatal error. The domain can be
     *                  any string but is suggested to be a valid URI. The
     *                  domain can be used to conveniently specify a web
     *                  site location of the relevent specification or
     *                  document pertaining to this fatal error.
     * @param key       The fatal error key. This key can be any string
     *                  and is implementation dependent.
     * @param exception Exception.
     *
     * @throws XNIException Thrown to signal that the parser should stop
     *                      parsing the document.
     */
    public void fatalError(String domain, String key,
                           XMLParseException exception) throws XNIException {
        fDOMError.fSeverity = DOMError.SEVERITY_FATAL_ERROR;
        fDOMError.fException = exception;
        fErrorCode.setValues(domain, key);
        String domErrorType = DOMErrorTypeMap.getDOMErrorType(fErrorCode);
        fDOMError.fType = (domErrorType != null) ? domErrorType : key;
        fDOMError.fRelatedData = fDOMError.fMessage = exception.getMessage();
        DOMLocatorImpl locator = fDOMError.fLocator;
        if (locator != null) {
            locator.fColumnNumber = exception.getColumnNumber();
            locator.fLineNumber = exception.getLineNumber();
            locator.fUtf16Offset = exception.getCharacterOffset();
            locator.fUri = exception.getExpandedSystemId();
            locator.fRelatedNode = fCurrentNode;
        }
        if (fDomErrorHandler != null) {
            fDomErrorHandler.handleError(fDOMError);
        }
    } // fatalError(String,String,XMLParseException)


    public boolean handleError(DOMError error) {
        printError(error);
        return eStatus;
    }

    /** Prints the error message. */

    private void printError(DOMError error) {
        int severity = error.getSeverity();
        fOut.print("[");
        if ( severity == DOMError.SEVERITY_WARNING) {
            fOut.print("Warning");
        } else if ( severity == DOMError.SEVERITY_ERROR) {
            fOut.print("Error");
        } else {
            fOut.print("FatalError");
            eStatus = false ; //REVISIT: Abort processing if fatal error, do we need to??
        }
        fOut.print("] ");
        DOMLocator locator = error.getLocation();
        if (locator != null) {
            fOut.print(locator.getLineNumber());
            fOut.print(":");
            fOut.print(locator.getColumnNumber());
            fOut.print(":");
            fOut.print(locator.getByteOffset());
            fOut.print(",");
            fOut.print(locator.getUtf16Offset());
            Node node = locator.getRelatedNode();
            if (node != null) {
                fOut.print("[");
                fOut.print(node.getNodeName());
                fOut.print("]");
            }
            String systemId = locator.getUri();
            if (systemId != null) {
                int index = systemId.lastIndexOf('/');
                if (index != -1)
                    systemId = systemId.substring(index + 1);
                fOut.print(": ");
                fOut.print(systemId);
            }

        }

        fOut.print(":");
        fOut.print(error.getMessage());
        fOut.println();
        fOut.flush();

    } // printError(DOMError)

    /**
     * A convenience class for converting between internal
     * error codes and DOM error types.
     */
    private static class DOMErrorTypeMap {

        /** Map for converting internal error codes to DOM error types. **/
        private static final Map<XMLErrorCode, String> fgDOMErrorTypeTable;

        static {
            // initialize error type table: internal error codes (represented by domain and key) need to be mapped to a DOM error type.

            // REVISIT: do well-formedness issues involving XML declaration <?xml ... ?> need to be added to hash table (no XML declaration node in DOM, but Document includes xmlEncoding, xmlStandalone, xmlVersion, etc.

            Map<XMLErrorCode, String> aDOMErrorTypeTable = new HashMap<>();
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInCDSect"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInContent"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "TwoColonsInQName"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ColonNotLegalWithNS"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInProlog"), "wf-invalid-character");  // e.g. in Processing Instruction

            // InvalidCharInXMLDecl omitted because XML declaration is not a DOM Node
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "CDEndInContent"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "CDSectUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "DoctypeNotAllowed"), "doctype-not-allowed");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ETagRequired"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ElementUnterminated"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EqRequiredInAttribute"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "OpenQuoteExpected"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "CloseQuoteExpected"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ETagUnterminated"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MarkupNotRecognizedInContent"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "DoctypeIllegalInContent"), "doctype-not-allowed");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInAttValue"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInPI"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInInternalSubset"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "QuoteRequiredInAttValue"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "LessthanInAttValue"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "AttributeValueUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PITargetRequired"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "SpaceRequiredInPI"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PIUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ReservedPITarget"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PI_NOT_IN_ONE_ENTITY"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PINotInOneEntity"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EncodingDeclInvalid"), "unsupported-encoding");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EncodingByteOrderUnsupported"), "unsupported-encoding");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInEntityValue"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInExternalSubset"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInIgnoreSect"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInPublicID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "InvalidCharInSystemID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "SpaceRequiredAfterSYSTEM"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "QuoteRequiredInSystemID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "SystemIDUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "SpaceRequiredAfterPUBLIC"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "QuoteRequiredInPublicID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PublicIDUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PubidCharIllegal"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "SpaceRequiredBetweenPublicAndSystem"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_ROOT_ELEMENT_TYPE_IN_DOCTYPEDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node (which follows !DOCTYPE)
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_ROOT_ELEMENT_TYPE_REQUIRED"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "DoctypedeclUnterminated"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PEReferenceWithinMarkup"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_MARKUP_NOT_RECOGNIZED_IN_DTD"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ELEMENTDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_ELEMENT_TYPE_REQUIRED_IN_ELEMENTDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_CONTENTSPEC_IN_ELEMENTDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_CONTENTSPEC_REQUIRED_IN_ELEMENTDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ElementDeclUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_CLOSE_PAREN_REQUIRED_IN_CHILDREN"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_ELEMENT_TYPE_REQUIRED_IN_MIXED_CONTENT"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_CLOSE_PAREN_REQUIRED_IN_MIXED"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MixedContentUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_ELEMENT_TYPE_IN_ATTLISTDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_ELEMENT_TYPE_REQUIRED_IN_ATTLISTDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_ATTRIBUTE_NAME_IN_ATTDEF"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "AttNameRequiredInAttDef"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_ATTTYPE_IN_ATTDEF"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "AttTypeRequiredInAttDef"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_DEFAULTDECL_IN_ATTDEF"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_DUPLICATE_ATTRIBUTE_DEFINITION"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_AFTER_NOTATION_IN_NOTATIONTYPE"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_OPEN_PAREN_REQUIRED_IN_NOTATIONTYPE"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_NAME_REQUIRED_IN_NOTATIONTYPE"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "NotationTypeUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_NMTOKEN_REQUIRED_IN_ENUMERATION"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EnumerationUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_DISTINCT_TOKENS_IN_ENUMERATION"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_DISTINCT_NOTATION_IN_ENUMERATION"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_AFTER_FIXED_IN_DEFAULTDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "IncludeSectUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "IgnoreSectUnterminated"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "NameRequiredInPEReference"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "SemicolonRequiredInPEReference"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_ENTITYDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node (which follows !ENTITY)
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_PERCENT_IN_PEDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node (which follows !ENTITY %)
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_ENTITY_NAME_IN_PEDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node (which follows !ENTITY %)
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_ENTITY_NAME_REQUIRED_IN_ENTITYDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node (which follows !ENTITY)
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_AFTER_ENTITY_NAME_IN_ENTITYDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_UNPARSED_ENTITYDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_NDATA_IN_UNPARSED_ENTITYDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_NOTATION_NAME_REQUIRED_FOR_UNPARSED_ENTITYDECL"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EntityDeclUnterminated"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_DUPLICATE_ENTITY_DEFINITION"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ExternalIDRequired"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_PUBIDLITERAL_IN_EXTERNALID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_AFTER_PUBIDLITERAL_IN_EXTERNALID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_SYSTEMLITERAL_IN_EXTERNALID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_URI_FRAGMENT_IN_SYSTEMID"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_BEFORE_NOTATION_NAME_IN_NOTATIONDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node, which follows !NOTATION
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_NOTATION_NAME_REQUIRED_IN_NOTATIONDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node, which follows !NOTATION
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "MSG_SPACE_REQUIRED_AFTER_NOTATION_NAME_IN_NOTATIONDECL"), "wf-invalid-character-in-node-name");  // considered error in name of node, which follows !NOTATION
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ExternalIDorPublicIDRequired"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "NotationDeclUnterminated"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ReferenceToExternalEntity"), "wf-invalid-character");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ReferenceToUnparsedEntity"), "wf-invalid-character");

            // REVISIT: do EntityNotDeclared, RecursiveReference, RecursiveGeneralReference, RecursivePEReference belong here?
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EncodingNotSupported"), "unsupported-encoding");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EncodingRequired"), "unsupported-encoding");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "IllegalQName"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ElementXMLNSPrefix"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "ElementPrefixUnbound"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "AttributePrefixUnbound"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "EmptyPrefixedAttName"), "wf-invalid-character-in-node-name");
            aDOMErrorTypeTable.put(new XMLErrorCode(XMLMessageFormatter.XML_DOMAIN, "PrefixDeclared"), "wf-invalid-character-in-node-name");

            fgDOMErrorTypeTable = Collections.unmodifiableMap(aDOMErrorTypeTable);
        }

        public static String getDOMErrorType (XMLErrorCode error) {
            return fgDOMErrorTypeTable.get(error);
        }

        private DOMErrorTypeMap () {}
    }

} // class DOMErrorHandlerWrapper
