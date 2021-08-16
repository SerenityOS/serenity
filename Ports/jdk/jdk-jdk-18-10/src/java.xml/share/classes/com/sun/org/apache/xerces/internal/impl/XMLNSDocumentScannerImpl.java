/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.org.apache.xerces.internal.impl;

import java.io.IOException;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.impl.dtd.XMLDTDValidatorFilter;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;

import javax.xml.stream.events.XMLEvent;

/**
 * This class adds the functionality of namespace processing.
 *
 * This class has been modified as per the new design which is more suited to
 * efficiently build pull parser. Lot of improvements have been done and
 * the code has been added to support stax functionality/features.
 *
 *
 * This class scans an XML document, checks if document has a DTD, and if
 * DTD is not found the scanner will remove the DTD Validator from the pipeline and perform
 * namespace binding.
 *
 *
 * @author Neeraj Bajaj, Sun Microsystems
 * @author Venugopal Rao K, Sun Microsystems
 * @author Elena Litani, IBM
 */
public class XMLNSDocumentScannerImpl
        extends XMLDocumentScannerImpl {

    /**
     * If is true, the dtd validator is no longer in the pipeline
     * and the scanner should bind namespaces
     */
    protected boolean fBindNamespaces;

    /** If validating parser, make sure we report an error in the
     *   scanner if DTD grammar is missing.*/
    protected boolean fPerformValidation;


    /** Default value of this feature is false, when in Stax mode this should be true */
    protected boolean fNotAddNSDeclAsAttribute = false;

    /** DTD validator */
     private XMLDTDValidatorFilter fDTDValidator;

     /** xmlns, default Namespace, declared */
     private boolean fXmlnsDeclared = false;

    /** Resets the fields of this scanner.
     */
    public void reset(PropertyManager propertyManager) {
        setPropertyManager(propertyManager);
        super.reset(propertyManager);
        fBindNamespaces = false;
        fNotAddNSDeclAsAttribute = !((Boolean)propertyManager.getProperty(Constants.ADD_NAMESPACE_DECL_AS_ATTRIBUTE)).booleanValue();
    }

    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {
        super.reset(componentManager);
        fNotAddNSDeclAsAttribute = false ;
        fPerformValidation = false;
        fBindNamespaces = false;
    }

    /** return the next state on the input
     *
     * @return int
     */

    public int next() throws IOException, XNIException {
        //since namespace context should still be valid when the parser is at the end element state therefore
        //we pop the context only when next() has been called after the end element state was encountered. - nb.

        if((fScannerLastState == XMLEvent.END_ELEMENT) && fBindNamespaces){
            fScannerLastState = -1;
            fNamespaceContext.popContext();
        }

        return fScannerLastState = super.next();
    }

    /**
     * The scanner is responsible for removing DTD validator
     * from the pipeline if it is not needed.
     *
     * @param previous The filter component before DTDValidator
     * @param dtdValidator
     *                 The DTDValidator
     * @param next     The documentHandler after the DTDValidator
     */
    public void setDTDValidator(XMLDTDValidatorFilter dtd){
        fDTDValidator = dtd;
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
     *
     * @return True if element is empty. (i.e. It matches
     *          production [44].
     */
    protected boolean scanStartElement()
    throws IOException, XNIException {

        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanStartElement()");
        //when skipping is true and no more elements should be added
        if(fSkip && !fAdd){
            //get the stored element -- if everything goes right this should match the
            //token in the buffer

            QName name = fElementStack.getNext();

            if(DEBUG_SKIP_ALGORITHM){
                System.out.println("Trying to skip String = " + name.rawname);
            }

            //Be conservative -- if skipping fails -- stop.
            fSkip = fEntityScanner.skipString(name.rawname); // skipQElement(name);

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
            // There are two variables,fNamespaces and fBindNamespaces
            //StAX uses XMLNSDocumentScannerImpl so this distinction needs to be maintained
            if (fNamespaces) {
                fEntityScanner.scanQName(fElementQName, NameType.ELEMENTSTART);
            } else {
                String name = fEntityScanner.scanName(NameType.ELEMENTSTART);
                fElementQName.setValues(null, name, name, null);
            }

            if(DEBUG)System.out.println("Element scanned in start element is " + fElementQName.toString());
            if(DEBUG_SKIP_ALGORITHM){
                if(fAdd){
                    System.out.println("Elements are being ADDED -- elemet added is = " + fElementQName.rawname + " at count = " + fElementStack.fCount);
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
        checkDepth(rawname);
        if (fBindNamespaces) {
            fNamespaceContext.pushContext();
            if (fScannerState == SCANNER_STATE_ROOT_ELEMENT) {
                if (fPerformValidation) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                            "MSG_GRAMMAR_NOT_FOUND",
                            new Object[]{ rawname},
                            XMLErrorReporter.SEVERITY_ERROR);

                            if (fDoctypeName == null || !fDoctypeName.equals(rawname)) {
                                fErrorReporter.reportError( XMLMessageFormatter.XML_DOMAIN,
                                        "RootElementTypeMustMatchDoctypedecl",
                                        new Object[]{fDoctypeName, rawname},
                                        XMLErrorReporter.SEVERITY_ERROR);
                            }
                }
            }
        }


        fEmptyElement = false;
        fAttributes.removeAllAttributes();

        if(!seekCloseOfStartTag()){
            fReadingAttributes = true;
            fAttributeCacheUsedCount =0;
            fStringBufferIndex =0;
            fAddDefaultAttr = true;
            fXmlnsDeclared = false;

            do {
                scanAttribute(fAttributes);
                if (fSecurityManager != null && (!fSecurityManager.isNoLimit(fElementAttributeLimit)) &&
                        fAttributes.getLength() > fElementAttributeLimit){
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                 "ElementAttributeLimit",
                                                 new Object[]{rawname, fElementAttributeLimit },
                                                 XMLErrorReporter.SEVERITY_FATAL_ERROR );
                }

            } while (!seekCloseOfStartTag());
            fReadingAttributes=false;
        }

        if (fBindNamespaces) {
            // REVISIT: is it required? forbit xmlns prefix for element
            if (fElementQName.prefix == XMLSymbols.PREFIX_XMLNS) {
                fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                        "ElementXMLNSPrefix",
                        new Object[]{fElementQName.rawname},
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);
            }

            // bind the element
            String prefix = fElementQName.prefix != null
                    ? fElementQName.prefix : XMLSymbols.EMPTY_STRING;
            // assign uri to the element
            fElementQName.uri = fNamespaceContext.getURI(prefix);
            // make sure that object in the element stack is updated as well
            fCurrentElement.uri = fElementQName.uri;

            if (fElementQName.prefix == null && fElementQName.uri != null) {
                fElementQName.prefix = XMLSymbols.EMPTY_STRING;
            }
            if (fElementQName.prefix != null && fElementQName.uri == null) {
                fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                        "ElementPrefixUnbound",
                        new Object[]{fElementQName.prefix, fElementQName.rawname},
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);
            }

            // bind attributes (xmlns are already bound bellow)
            int length = fAttributes.getLength();
            // fLength = 0; //initialize structure
            for (int i = 0; i < length; i++) {
                fAttributes.getName(i, fAttributeQName);

                String aprefix = fAttributeQName.prefix != null
                        ? fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
                String uri = fNamespaceContext.getURI(aprefix);
                // REVISIT: try removing the first "if" and see if it is faster.
                //
                if (fAttributeQName.uri != null && fAttributeQName.uri == uri) {
                    // checkDuplicates(fAttributeQName, fAttributes);
                    continue;
                }
                if (aprefix != XMLSymbols.EMPTY_STRING) {
                    fAttributeQName.uri = uri;
                    if (uri == null) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "AttributePrefixUnbound",
                                new Object[]{fElementQName.rawname,fAttributeQName.rawname,aprefix},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                    fAttributes.setURI(i, uri);
                    // checkDuplicates(fAttributeQName, fAttributes);
                }
            }

            if (length > 1) {
                QName name = fAttributes.checkDuplicatesNS();
                if (name != null) {
                    if (name.uri != null) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "AttributeNSNotUnique",
                                new Object[]{fElementQName.rawname, name.localpart, name.uri},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    } else {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "AttributeNotUnique",
                                new Object[]{fElementQName.rawname, name.rawname},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                }
            }
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
                if(DEBUG)
                    System.out.println("emptyElement = " + fElementQName);

                fDocumentHandler.emptyElement(fElementQName, fAttributes, null);
            }

            //We should not be popping out the context here in endELement becaause the namespace context is still
            //valid when parser is at the endElement state.
            fScanEndElement = true;
            //if (fBindNamespaces) {
            //  fNamespaceContext.popContext();
            //}

            //pop the element off the stack..
            fElementStack.popElement();

        } else {

            if(dtdGrammarUtil != null)
                dtdGrammarUtil.startElement(fElementQName,fAttributes);
            if(fDocumentHandler != null){
                //complete element and attributes are traversed in this function so we can send a callback
                //here.
                //<strong>we shouldn't be sending callback in scanDocument()</strong>
                if(DEBUG)
                    System.out.println("startElement = " + fElementQName);
                fDocumentHandler.startElement(fElementQName, fAttributes, null);
            }
        }


        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +"<<< scanStartElement(): "+fEmptyElement);
        return fEmptyElement;

    } // scanStartElement():boolean



    /**
     * Scans an attribute.
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
    protected void scanAttribute(XMLAttributesImpl attributes)
    throws IOException, XNIException {
        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +">>> scanAttribute()");

        // name
        fEntityScanner.scanQName(fAttributeQName, NameType.ATTRIBUTENAME);

        // equals
        fEntityScanner.skipSpaces();
        if (!fEntityScanner.skipChar('=', NameType.ATTRIBUTE)) {
            reportFatalError("EqRequiredInAttribute",
                    new Object[]{fCurrentElement.rawname,fAttributeQName.rawname});
        }
        fEntityScanner.skipSpaces();

        // content
        int attrIndex = 0 ;


        //REVISIT: one more case needs to be included: external PE and standalone is no
        boolean isVC =  fHasExternalDTD && !fStandalone;

        // REVISIT: it seems that this function should not take attributes, and length
        //fTempString would store attribute value
        ///fTempString2 would store attribute non-normalized value

        //this function doesn't use 'attIndex'. We are adding the attribute later
        //after we have figured out that current attribute is not namespace declaration
        //since scanAttributeValue doesn't use attIndex parameter therefore we
        //can safely add the attribute later..
        XMLString tmpStr = getString();

        /**
         * Determine whether this is a namespace declaration that will be subject
         * to the name limit check in the scanAttributeValue operation.
         * Namespace declaration format: xmlns="..." or xmlns:prefix="..."
         * Note that prefix:xmlns="..." isn't a namespace.
         */
        String localpart = fAttributeQName.localpart;
        String prefix = fAttributeQName.prefix != null
                ? fAttributeQName.prefix : XMLSymbols.EMPTY_STRING;
        boolean isNSDecl = fBindNamespaces & (prefix == XMLSymbols.PREFIX_XMLNS ||
                    prefix == XMLSymbols.EMPTY_STRING && localpart == XMLSymbols.PREFIX_XMLNS);

        scanAttributeValue(tmpStr, fTempString2, fAttributeQName.rawname, attributes,
                attrIndex, isVC, fCurrentElement.rawname, isNSDecl);

        String value = null;
        //fTempString.toString();

        // record namespace declarations if any.
        if (fBindNamespaces) {
            if (isNSDecl) {
                //check the length of URI
                if (tmpStr.length > fXMLNameLimit) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                            "MaxXMLNameLimit",
                            new Object[]{new String(tmpStr.ch,tmpStr.offset,tmpStr.length),
                            tmpStr.length, fXMLNameLimit,
                            fSecurityManager.getStateLiteral(XMLSecurityManager.Limit.MAX_NAME_LIMIT)},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
                // get the internalized value of this attribute
                String uri = fSymbolTable.addSymbol(tmpStr.ch,tmpStr.offset,tmpStr.length);
                value = uri;
                // 1. "xmlns" can't be bound to any namespace
                if (prefix == XMLSymbols.PREFIX_XMLNS && localpart == XMLSymbols.PREFIX_XMLNS) {
                    fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                            "CantBindXMLNS",
                            new Object[]{fAttributeQName},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }

                // 2. the namespace for "xmlns" can't be bound to any prefix
                if (uri == NamespaceContext.XMLNS_URI) {
                    fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                            "CantBindXMLNS",
                            new Object[]{fAttributeQName},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }

                // 3. "xml" can't be bound to any other namespace than it's own
                if (localpart == XMLSymbols.PREFIX_XML) {
                    if (uri != NamespaceContext.XML_URI) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "CantBindXML",
                                new Object[]{fAttributeQName},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                }
                // 4. the namespace for "xml" can't be bound to any other prefix
                else {
                    if (uri ==NamespaceContext.XML_URI) {
                        fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                                "CantBindXML",
                                new Object[]{fAttributeQName},
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                }
                prefix = localpart != XMLSymbols.PREFIX_XMLNS ? localpart : XMLSymbols.EMPTY_STRING;
                //set it equal to XMLSymbols.PREFIX_XMLNS when namespace declaration
                // is of type xmlns = "..", in this case prefix = "" and localname = XMLSymbols.PREFIX_XMLNS
                //this special behavior is because of dependency on this behavior in DOM components
                if(prefix == XMLSymbols.EMPTY_STRING && localpart == XMLSymbols.PREFIX_XMLNS){
                    fAttributeQName.prefix = XMLSymbols.PREFIX_XMLNS;
                }
                // http://www.w3.org/TR/1999/REC-xml-names-19990114/#dt-prefix
                // We should only report an error if there is a prefix,
                // that is, the local part is not "xmlns". -SG
                if (uri == XMLSymbols.EMPTY_STRING && localpart != XMLSymbols.PREFIX_XMLNS) {
                    fErrorReporter.reportError(XMLMessageFormatter.XMLNS_DOMAIN,
                            "EmptyPrefixedAttName",
                            new Object[]{fAttributeQName},
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }

                // check for duplicate prefix bindings
                if (((com.sun.org.apache.xerces.internal.util.NamespaceSupport) fNamespaceContext).containsPrefixInCurrentContext(prefix)) {
                    reportFatalError("AttributeNotUnique",
                            new Object[]{fCurrentElement.rawname,
                            fAttributeQName.rawname});
                }

                // declare prefix in context
                boolean declared = fNamespaceContext.declarePrefix(prefix, uri.length() != 0 ? uri : null);

                // check for duplicate xmlns declarations
                if (!declared) { // by convention, prefix == "xmlns" | "xml"
                    // error if duplicate declaration
                    if (fXmlnsDeclared) {
                        reportFatalError("AttributeNotUnique",
                                new Object[]{fCurrentElement.rawname,
                                fAttributeQName.rawname});
                    }

                    // xmlns declared
                    fXmlnsDeclared = true;
                }

                //xerces internals (XSAttributeChecker) has dependency on namespace declaration returned
                //as part of XMLAttributes.
                //addition of namespace declaration to the attribute list is controlled by fNotAddNSDeclAsAttribute
                //feature. This is required in Stax where namespace declarations are not considered as attribute

                if(fNotAddNSDeclAsAttribute){
                    return ;
                }
            }
        }

        //add the attributes to the list of attributes
        if (fBindNamespaces) {
            attrIndex = attributes.getLength();
            attributes.addAttributeNS(fAttributeQName, XMLSymbols.fCDATASymbol, null);
        } else {
            int oldLen = attributes.getLength();
            attrIndex = attributes.addAttribute(fAttributeQName, XMLSymbols.fCDATASymbol, null);

            // WFC: Unique Att Spec
            if (oldLen == attributes.getLength()) {
                reportFatalError("AttributeNotUnique",
                        new Object[]{fCurrentElement.rawname,
                                fAttributeQName.rawname});
            }
        }

        attributes.setValue(attrIndex, value,tmpStr);
        //attributes.setNonNormalizedValue(attrIndex, fTempString2.toString());
        //removing  as we are not using non-normalized values . -Venu
        attributes.setSpecified(attrIndex, true);

        // attempt to bind attribute
        if (fAttributeQName.prefix != null) {
            attributes.setURI(attrIndex, fNamespaceContext.getURI(fAttributeQName.prefix));
        }

        if (DEBUG_START_END_ELEMENT) System.out.println(this.getClass().toString() +"<<< scanAttribute()");
    } // scanAttribute(XMLAttributes)





    /** Creates a content driver. */
    protected Driver createContentDriver() {
        return new NSContentDriver();
    } // createContentDriver():Driver

    /**
     * Driver to handle content scanning.
     */
    protected final class NSContentDriver
            extends ContentDriver {
        /**
         * Scan for root element hook. This method is a hook for
         * subclasses to add code that handles scanning for the root
         * element. This method will also attempt to remove DTD validator
         * from the pipeline, if there is no DTD grammar. If DTD validator
         * is no longer in the pipeline bind namespaces in the scanner.
         *
         *
         * @return True if the caller should stop and return true which
         *          allows the scanner to switch to a new scanning
         *          driver. A return value of false indicates that
         *          the content driver should continue as normal.
         */
        protected boolean scanRootElementHook()
        throws IOException, XNIException {

            reconfigurePipeline();
            if (scanStartElement()) {
                setScannerState(SCANNER_STATE_TRAILING_MISC);
                setDriver(fTrailingMiscDriver);
                return true;
            }
            return false;

        } // scanRootElementHook():boolean

        /**
         * Re-configures pipeline by removing the DTD validator
         * if no DTD grammar exists. If no validator exists in the
         * pipeline or there is no DTD grammar, namespace binding
         * is performed by the scanner in the enclosing class.
         */
        private void reconfigurePipeline() {
            //fDTDValidator will be null in Stax mode
            if (fNamespaces && fDTDValidator == null) {
                fBindNamespaces = true;
            }
            else if (fNamespaces && !fDTDValidator.hasGrammar() ) {
                fBindNamespaces = true;
                fPerformValidation = fDTDValidator.validate();
                // re-configure pipeline by removing DTDValidator
                XMLDocumentSource source = fDTDValidator.getDocumentSource();
                XMLDocumentHandler handler = fDTDValidator.getDocumentHandler();
                source.setDocumentHandler(handler);
                if (handler != null)
                    handler.setDocumentSource(source);
                fDTDValidator.setDocumentSource(null);
                fDTDValidator.setDocumentHandler(null);
            }
        } // reconfigurePipeline()
    }

} // class XMLNSDocumentScannerImpl
