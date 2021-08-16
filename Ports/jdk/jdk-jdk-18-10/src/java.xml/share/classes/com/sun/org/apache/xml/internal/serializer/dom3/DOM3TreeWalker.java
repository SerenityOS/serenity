/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.serializer.dom3;

import com.sun.org.apache.xerces.internal.util.XML11Char;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.serializer.utils.MsgKey;
import com.sun.org.apache.xml.internal.serializer.utils.Utils;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSSerializerFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.LocatorImpl;

/**
 * Built on org.apache.xml.serializer.TreeWalker and adds functionality to
 * traverse and serialize a DOM Node (Level 2 or Level 3) as specified in
 * the DOM Level 3 LS Recommedation by evaluating and applying DOMConfiguration
 * parameters and filters if any during serialization.
 *
 * @xsl.usage internal
 * @LastModified: Apr 2021
 */
final class DOM3TreeWalker {

    /**
     * The SerializationHandler, it extends ContentHandler and when
     * this class is instantiated via the constructor provided, a
     * SerializationHandler object is passed to it.
     */
    private SerializationHandler fSerializer = null;

    /** We do not need DOM2Helper since DOM Level 3 LS applies to DOM Level 2 or newer */

    /** Locator object for this TreeWalker          */
    private LocatorImpl fLocator = new LocatorImpl();

    /** ErrorHandler */
    private DOMErrorHandler fErrorHandler = null;

    /** LSSerializerFilter */
    private LSSerializerFilter fFilter = null;

    /** If the serializer is an instance of a LexicalHandler */
    private LexicalHandler fLexicalHandler = null;

    private int fWhatToShowFilter;

    /** New Line character to use in serialization */
    private String fNewLine = null;

    /** DOMConfiguration Properties */
    private Properties fDOMConfigProperties = null;

    /** Keeps track if we are in an entity reference when entities=true */
    private boolean fInEntityRef = false;

    /** Stores the version of the XML document to be serialize */
    private String fXMLVersion = null;

    /** XML Version, default 1.0 */
    private boolean fIsXMLVersion11 = false;

    /** Is the Node a Level 3 DOM node */
    private boolean fIsLevel3DOM = false;

    /** DOM Configuration Parameters */
    private int fFeatures = 0;

    /** Flag indicating whether following text to be processed is raw text          */
    boolean fNextIsRaw = false;

    //
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    //
    private static final String XMLNS_PREFIX = "xmlns";

    //
    private static final String XML_URI = "http://www.w3.org/XML/1998/namespace";

    //
    private static final String XML_PREFIX = "xml";

    /** stores namespaces in scope */
    protected NamespaceSupport fNSBinder;

    /** stores all namespace bindings on the current element */
    protected NamespaceSupport fLocalNSBinder;

    /** stores the current element depth */
    private int fElementDepth = 0;

    // ***********************************************************************
    // DOMConfiguration paramter settings
    // ***********************************************************************
    // Parameter canonical-form, true [optional] - NOT SUPPORTED
    private final static int CANONICAL = 0x1 << 0;

    // Parameter cdata-sections, true [required] (default)
    private final static int CDATA = 0x1 << 1;

    // Parameter check-character-normalization, true [optional] - NOT SUPPORTED
    private final static int CHARNORMALIZE = 0x1 << 2;

    // Parameter comments, true [required] (default)
    private final static int COMMENTS = 0x1 << 3;

    // Parameter datatype-normalization, true [optional] - NOT SUPPORTED
    private final static int DTNORMALIZE = 0x1 << 4;

    // Parameter element-content-whitespace, true [required] (default) - value - false [optional] NOT SUPPORTED
    private final static int ELEM_CONTENT_WHITESPACE = 0x1 << 5;

    // Parameter entities, true [required] (default)
    private final static int ENTITIES = 0x1 << 6;

    // Parameter infoset, true [required] (default), false has no effect --> True has no effect for the serializer
    private final static int INFOSET = 0x1 << 7;

    // Parameter namespaces, true [required] (default)
    private final static int NAMESPACES = 0x1 << 8;

    // Parameter namespace-declarations, true [required] (default)
    private final static int NAMESPACEDECLS = 0x1 << 9;

    // Parameter normalize-characters, true [optional] - NOT SUPPORTED
    private final static int NORMALIZECHARS = 0x1 << 10;

    // Parameter split-cdata-sections, true [required] (default)
    private final static int SPLITCDATA = 0x1 << 11;

    // Parameter validate, true [optional] - NOT SUPPORTED
    private final static int VALIDATE = 0x1 << 12;

    // Parameter validate-if-schema, true [optional] - NOT SUPPORTED
    private final static int SCHEMAVALIDATE = 0x1 << 13;

    // Parameter split-cdata-sections, true [required] (default)
    private final static int WELLFORMED = 0x1 << 14;

    // Parameter discard-default-content, true [required] (default)
    // Not sure how this will be used in level 2 Documents
    private final static int DISCARDDEFAULT = 0x1 << 15;

    // Parameter format-pretty-print, true [optional]
    private final static int PRETTY_PRINT = 0x1 << 16;

    // Parameter ignore-unknown-character-denormalizations, true [required] (default)
    // We currently do not support XML 1.1 character normalization
    private final static int IGNORE_CHAR_DENORMALIZE = 0x1 << 17;

    // Parameter discard-default-content, true [required] (default)
    private final static int XMLDECL = 0x1 << 18;

    /**
     * Constructor.
     * @param   contentHandler serialHandler The implemention of the SerializationHandler interface
     */
    DOM3TreeWalker(
        SerializationHandler serialHandler,
        DOMErrorHandler errHandler,
        LSSerializerFilter filter,
        String newLine) {
        fSerializer = serialHandler;
        //fErrorHandler = errHandler == null ? new DOMErrorHandlerImpl() : errHandler; // Should we be using the default?
        fErrorHandler = errHandler;
        fFilter = filter;
        fLexicalHandler = null;
        fNewLine = newLine;

        fNSBinder = new NamespaceSupport();
        fLocalNSBinder = new NamespaceSupport();

        fDOMConfigProperties = fSerializer.getOutputFormat();
        fSerializer.setDocumentLocator(fLocator);
        initProperties(fDOMConfigProperties);
    }

    /**
     * Perform a pre-order traversal non-recursive style.
     *
     * Note that TreeWalker assumes that the subtree is intended to represent
     * a complete (though not necessarily well-formed) document and, during a
     * traversal, startDocument and endDocument will always be issued to the
     * SAX listener.
     *
     * @param pos Node in the tree where to start traversal
     *
     * @throws TransformerException
     */
    public void traverse(Node pos) throws org.xml.sax.SAXException {
        this.fSerializer.startDocument();

        // Determine if the Node is a DOM Level 3 Core Node.
        if (pos.getNodeType() != Node.DOCUMENT_NODE) {
            Document ownerDoc = pos.getOwnerDocument();
            if (ownerDoc != null
                && ownerDoc.getImplementation().hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        } else {
            if (((Document) pos)
                .getImplementation()
                .hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        }

        if (fSerializer instanceof LexicalHandler) {
            fLexicalHandler = ((LexicalHandler) this.fSerializer);
        }

        if (fFilter != null)
            fWhatToShowFilter = fFilter.getWhatToShow();

        Node top = pos;

        while (null != pos) {
            startNode(pos);

            Node nextNode = null;

            nextNode = pos.getFirstChild();

            while (null == nextNode) {
                endNode(pos);

                if (top.equals(pos))
                    break;

                nextNode = pos.getNextSibling();

                if (null == nextNode) {
                    pos = pos.getParentNode();

                    if ((null == pos) || (top.equals(pos))) {
                        if (null != pos)
                            endNode(pos);

                        nextNode = null;

                        break;
                    }
                }
            }

            pos = nextNode;
        }
        this.fSerializer.endDocument();
    }

    /**
     * Perform a pre-order traversal non-recursive style.

     * Note that TreeWalker assumes that the subtree is intended to represent
     * a complete (though not necessarily well-formed) document and, during a
     * traversal, startDocument and endDocument will always be issued to the
     * SAX listener.
     *
     * @param pos Node in the tree where to start traversal
     * @param top Node in the tree where to end traversal
     *
     * @throws TransformerException
     */
    public void traverse(Node pos, Node top) throws org.xml.sax.SAXException {

        this.fSerializer.startDocument();

        // Determine if the Node is a DOM Level 3 Core Node.
        if (pos.getNodeType() != Node.DOCUMENT_NODE) {
            Document ownerDoc = pos.getOwnerDocument();
            if (ownerDoc != null
                && ownerDoc.getImplementation().hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        } else {
            if (((Document) pos)
                .getImplementation()
                .hasFeature("Core", "3.0")) {
                fIsLevel3DOM = true;
            }
        }

        if (fSerializer instanceof LexicalHandler) {
            fLexicalHandler = ((LexicalHandler) this.fSerializer);
        }

        if (fFilter != null)
            fWhatToShowFilter = fFilter.getWhatToShow();

        while (null != pos) {
            startNode(pos);

            Node nextNode = null;

            nextNode = pos.getFirstChild();

            while (null == nextNode) {
                endNode(pos);

                if ((null != top) && top.equals(pos))
                    break;

                nextNode = pos.getNextSibling();

                if (null == nextNode) {
                    pos = pos.getParentNode();

                    if ((null == pos) || ((null != top) && top.equals(pos))) {
                        nextNode = null;

                        break;
                    }
                }
            }

            pos = nextNode;
        }
        this.fSerializer.endDocument();
    }

    /**
     * Optimized dispatch of characters.
     */
    private final void dispatachChars(Node node)
        throws org.xml.sax.SAXException {
        if (fSerializer != null) {
            String data = ((Text) node).getData();
            this.fSerializer.characters(data.toCharArray(), 0, data.length());
        }
    }

    /**
     * Start processing given node
     *
     * @param node Node to process
     *
     * @throws org.xml.sax.SAXException
     */
    protected void startNode(Node node) throws org.xml.sax.SAXException {
        if (node instanceof Locator) {
            Locator loc = (Locator) node;
            fLocator.setColumnNumber(loc.getColumnNumber());
            fLocator.setLineNumber(loc.getLineNumber());
            fLocator.setPublicId(loc.getPublicId());
            fLocator.setSystemId(loc.getSystemId());
        } else {
            fLocator.setColumnNumber(0);
            fLocator.setLineNumber(0);
        }

        switch (node.getNodeType()) {
            case Node.DOCUMENT_TYPE_NODE :
                serializeDocType((DocumentType) node, true);
                break;
            case Node.COMMENT_NODE :
                serializeComment((Comment) node);
                break;
            case Node.DOCUMENT_FRAGMENT_NODE :
                // Children are traversed
                break;
            case Node.DOCUMENT_NODE :
                break;
            case Node.ELEMENT_NODE :
                serializeElement((Element) node, true);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE :
                serializePI((ProcessingInstruction) node);
                break;
            case Node.CDATA_SECTION_NODE :
                serializeCDATASection((CDATASection) node);
                break;
            case Node.TEXT_NODE :
                serializeText((Text) node);
                break;
            case Node.ENTITY_REFERENCE_NODE :
                serializeEntityReference((EntityReference) node, true);
                break;
            default :
                }
    }

    /**
     * End processing of given node
     *
     *
     * @param node Node we just finished processing
     *
     * @throws org.xml.sax.SAXException
     */
    protected void endNode(Node node) throws org.xml.sax.SAXException {

        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE :
                break;
            case Node.DOCUMENT_TYPE_NODE :
                serializeDocType((DocumentType) node, false);
                break;
            case Node.ELEMENT_NODE :
                serializeElement((Element) node, false);
                break;
            case Node.CDATA_SECTION_NODE :
                break;
            case Node.ENTITY_REFERENCE_NODE :
                serializeEntityReference((EntityReference) node, false);
                break;
            default :
                }
    }

    // ***********************************************************************
    // Node serialization methods
    // ***********************************************************************
    /**
     * Applies a filter on the node to serialize
     *
     * @param node The Node to serialize
     * @return True if the node is to be serialized else false if the node
     *         is to be rejected or skipped.
     */
    protected boolean applyFilter(Node node, int nodeType) {
        if (fFilter != null && (fWhatToShowFilter & nodeType) != 0) {

            short code = fFilter.acceptNode(node);
            switch (code) {
                case NodeFilter.FILTER_REJECT :
                case NodeFilter.FILTER_SKIP :
                    return false; // skip the node
                default : // fall through..
            }
        }
        return true;
    }

    /**
     * Serializes a Document Type Node.
     *
     * @param node The Docuemnt Type Node to serialize
     * @param bStart Invoked at the start or end of node.  Default true.
     */
    protected void serializeDocType(DocumentType node, boolean bStart)
        throws SAXException {
        // The DocType and internalSubset can not be modified in DOM and is
        // considered to be well-formed as the outcome of successful parsing.
        String docTypeName = node.getNodeName();
        String publicId = node.getPublicId();
        String systemId = node.getSystemId();
        String internalSubset = node.getInternalSubset();

        //DocumentType nodes are never passed to the filter

        if (internalSubset != null && !"".equals(internalSubset)) {

            if (bStart) {
                try {
                    // The Serializer does not provide a way to write out the
                    // DOCTYPE internal subset via an event call, so we write it
                    // out here.
                    Writer writer = fSerializer.getWriter();
                    StringBuilder dtd = new StringBuilder();

                    dtd.append("<!DOCTYPE ");
                    dtd.append(docTypeName);
                    if (null != publicId) {
                        dtd.append(" PUBLIC \"");
                        dtd.append(publicId);
                        dtd.append('\"');
                    }

                    if (null != systemId) {
                        char quote = JdkXmlUtils.getQuoteChar(systemId);
                        if (null == publicId) {
                            dtd.append(" SYSTEM ").append(quote);
                        } else {
                            dtd.append(" ").append(quote);
                        }
                        dtd.append(systemId);
                        dtd.append(quote);
                    }

                    dtd.append(" [ ");

                    dtd.append(fNewLine);
                    dtd.append(internalSubset);
                    dtd.append("]>");
                    dtd.append(fNewLine);

                    writer.write(dtd.toString());
                    writer.flush();

                } catch (IOException e) {
                    throw new SAXException(Utils.messages.createMessage(
                            MsgKey.ER_WRITING_INTERNAL_SUBSET, null), e);
                }
            } // else if !bStart do nothing

        } else {

            if (bStart) {
                if (fLexicalHandler != null) {
                    fLexicalHandler.startDTD(docTypeName, publicId, systemId);
                }
            } else {
                if (fLexicalHandler != null) {
                    fLexicalHandler.endDTD();
                }
            }
        }
    }

    /**
     * Serializes a Comment Node.
     *
     * @param node The Comment Node to serialize
     */
    protected void serializeComment(Comment node) throws SAXException {
        // comments=true
        if ((fFeatures & COMMENTS) != 0) {
            String data = node.getData();

            // well-formed=true
            if ((fFeatures & WELLFORMED) != 0) {
                isCommentWellFormed(data);
            }

            if (fLexicalHandler != null) {
                // apply the LSSerializer filter after the operations requested by the
                // DOMConfiguration parameters have been applied
                if (!applyFilter(node, NodeFilter.SHOW_COMMENT)) {
                    return;
                }

                fLexicalHandler.comment(data.toCharArray(), 0, data.length());
            }
        }
    }

    /**
     * Serializes an Element Node.
     *
     * @param node The Element Node to serialize
     * @param bStart Invoked at the start or end of node.
     */
    protected void serializeElement(Element node, boolean bStart)
        throws SAXException {
        if (bStart) {
            fElementDepth++;

            // We use the Xalan specific startElement and starPrefixMapping calls
            // (and addAttribute and namespaceAfterStartElement) as opposed to
            // SAX specific, for performance reasons as they reduce the overhead
            // of creating an AttList object upfront.

            // well-formed=true
            if ((fFeatures & WELLFORMED) != 0) {
                isElementWellFormed(node);
            }

            // REVISIT: We apply the LSSerializer filter for elements before
            // namesapce fixup
            if (!applyFilter(node, NodeFilter.SHOW_ELEMENT)) {
                return;
            }

            // namespaces=true, record and fixup namspaced element
            if ((fFeatures & NAMESPACES) != 0) {
                fNSBinder.pushContext();
                fLocalNSBinder.reset();

                recordLocalNSDecl(node);
                fixupElementNS(node);
            }

            // Namespace normalization
            fSerializer.startElement(
                        node.getNamespaceURI(),
                    node.getLocalName(),
                    node.getNodeName());

            serializeAttList(node);

        } else {
                fElementDepth--;

            // apply the LSSerializer filter
            if (!applyFilter(node, NodeFilter.SHOW_ELEMENT)) {
                return;
            }

            this.fSerializer.endElement(
                node.getNamespaceURI(),
                node.getLocalName(),
                node.getNodeName());
            // since endPrefixMapping was not used by SerializationHandler it was removed
            // for performance reasons.

            if ((fFeatures & NAMESPACES) != 0 ) {
                    fNSBinder.popContext();
            }

        }
    }

    /**
     * Serializes the Attr Nodes of an Element.
     *
     * @param node The OwnerElement whose Attr Nodes are to be serialized.
     */
    protected void serializeAttList(Element node) throws SAXException {
        NamedNodeMap atts = node.getAttributes();
        int nAttrs = atts.getLength();

        for (int i = 0; i < nAttrs; i++) {
            Node attr = atts.item(i);

            String localName = attr.getLocalName();
            String attrName = attr.getNodeName();
            String attrPrefix = attr.getPrefix() == null ? "" : attr.getPrefix();
            String attrValue = attr.getNodeValue();

            // Determine the Attr's type.
            String type = null;
            if (fIsLevel3DOM) {
                type = ((Attr) attr).getSchemaTypeInfo().getTypeName();
            }
            type = type == null ? "CDATA" : type;

            String attrNS = attr.getNamespaceURI();
            if (attrNS !=null && attrNS.length() == 0) {
                attrNS=null;
                // we must remove prefix for this attribute
                attrName=attr.getLocalName();
            }

            boolean isSpecified = ((Attr) attr).getSpecified();
            boolean addAttr = true;
            boolean applyFilter = false;
            boolean xmlnsAttr =
                attrName.equals("xmlns") || attrName.startsWith("xmlns:");

            // well-formed=true
            if ((fFeatures & WELLFORMED) != 0) {
                isAttributeWellFormed(attr);
            }

            //-----------------------------------------------------------------
            // start Attribute namespace fixup
            //-----------------------------------------------------------------
            // namespaces=true, normalize all non-namespace attributes
            // Step 3. Attribute
            if ((fFeatures & NAMESPACES) != 0 && !xmlnsAttr) {

                        // If the Attr has a namespace URI
                        if (attrNS != null) {
                                attrPrefix = attrPrefix == null ? "" : attrPrefix;

                                String declAttrPrefix = fNSBinder.getPrefix(attrNS);
                                String declAttrNS = fNSBinder.getURI(attrPrefix);

                                // attribute has no prefix (default namespace decl does not apply to
                                // attributes)
                                // OR
                                // attribute prefix is not declared
                                // OR
                                // conflict: attribute has a prefix that conflicts with a binding
                                if ("".equals(attrPrefix) || "".equals(declAttrPrefix)
                                                || !attrPrefix.equals(declAttrPrefix)) {

                                        // namespaceURI matches an in scope declaration of one or
                                        // more prefixes
                                        if (declAttrPrefix != null && !"".equals(declAttrPrefix)) {
                                                // pick the prefix that was found and change attribute's
                                                // prefix and nodeName.
                                                attrPrefix = declAttrPrefix;

                                                if (declAttrPrefix.length() > 0 ) {
                                                        attrName = declAttrPrefix + ":" + localName;
                                                } else {
                                                        attrName = localName;
                                                }
                                        } else {
                                                // The current prefix is not null and it has no in scope
                                                // declaration
                                                if (attrPrefix != null && !"".equals(attrPrefix)
                                                                && declAttrNS == null) {
                                                        // declare this prefix
                                                        if ((fFeatures & NAMESPACEDECLS) != 0) {
                                                                fSerializer.addAttribute(XMLNS_URI, attrPrefix,
                                                                                XMLNS_PREFIX + ":" + attrPrefix, "CDATA",
                                                                                attrNS);
                                                                fNSBinder.declarePrefix(attrPrefix, attrNS);
                                                                fLocalNSBinder.declarePrefix(attrPrefix, attrNS);
                                                        }
                                                } else {
                                                        // find a prefix following the pattern "NS" +index
                                                        // (starting at 1)
                                                        // make sure this prefix is not declared in the current
                                                        // scope.
                                                        int counter = 1;
                                                        attrPrefix = "NS" + counter++;

                                                        while (fLocalNSBinder.getURI(attrPrefix) != null) {
                                                                attrPrefix = "NS" + counter++;
                                                        }
                                                        // change attribute's prefix and Name
                                                        attrName = attrPrefix + ":" + localName;

                                                        // create a local namespace declaration attribute
                                                        // Add the xmlns declaration attribute
                                                        if ((fFeatures & NAMESPACEDECLS) != 0) {

                                                                fSerializer.addAttribute(XMLNS_URI, attrPrefix,
                                                                                XMLNS_PREFIX + ":" + attrPrefix, "CDATA",
                                                                                attrNS);
                                                        fNSBinder.declarePrefix(attrPrefix, attrNS);
                                                        fLocalNSBinder.declarePrefix(attrPrefix, attrNS);
                                                        }
                                                }
                                        }
                                }

                        } else { // if the Attr has no namespace URI
                                // Attr has no localName
                                if (localName == null) {
                                        // DOM Level 1 node!
                                        String msg = Utils.messages.createMessage(
                                                        MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                                                        new Object[] { attrName });

                                        if (fErrorHandler != null) {
                                                fErrorHandler
                                                                .handleError(new DOMErrorImpl(
                                                                                DOMError.SEVERITY_ERROR, msg,
                                                                                MsgKey.ER_NULL_LOCAL_ELEMENT_NAME, null,
                                                                                null, null));
                                        }

                                } else { // uri=null and no colon
                                        // attr has no namespace URI and no prefix
                                        // no action is required, since attrs don't use default
                                }
                        }

            }


            // discard-default-content=true
            // Default attr's are not passed to the filter and this contraint
            // is applied only when discard-default-content=true
            // What about default xmlns attributes???? check for xmlnsAttr
            if ((((fFeatures & DISCARDDEFAULT) != 0) && isSpecified)
                || ((fFeatures & DISCARDDEFAULT) == 0)) {
                applyFilter = true;
            } else {
                addAttr = false;
            }

            if (applyFilter) {
                // apply the filter for Attributes that are not default attributes
                // or namespace decl attributes
                if (fFilter != null
                    && (fFilter.getWhatToShow() & NodeFilter.SHOW_ATTRIBUTE)
                        != 0) {

                    if (!xmlnsAttr) {
                        short code = fFilter.acceptNode(attr);
                        switch (code) {
                            case NodeFilter.FILTER_REJECT :
                            case NodeFilter.FILTER_SKIP :
                                addAttr = false;
                                break;
                            default : //fall through..
                        }
                    }
                }
            }

            // if the node is a namespace node
            if (addAttr && xmlnsAttr) {
                // If namespace-declarations=true, add the node , else don't add it
                if ((fFeatures & NAMESPACEDECLS) != 0) {
                        // The namespace may have been fixed up, in that case don't add it.
                        if (localName != null && !"".equals(localName)) {
                                fSerializer.addAttribute(attrNS, localName, attrName, type, attrValue);
                        }
                }
            } else if (
                addAttr && !xmlnsAttr) { // if the node is not a namespace node
                // If namespace-declarations=true, add the node with the Attr nodes namespaceURI
                // else add the node setting it's namespace to null or else the serializer will later
                // attempt to add a xmlns attr for the prefixed attribute
                if (((fFeatures & NAMESPACEDECLS) != 0) && (attrNS != null)) {
                    fSerializer.addAttribute(
                        attrNS,
                        localName,
                        attrName,
                        type,
                        attrValue);
                } else {
                    fSerializer.addAttribute(
                        "",
                        localName,
                        attrName,
                        type,
                        attrValue);
                }
            }

            //
            if (xmlnsAttr && ((fFeatures & NAMESPACEDECLS) != 0)) {
                int index;
                // Use "" instead of null, as Xerces likes "" for the
                // name of the default namespace.  Fix attributed
                // to "Steven Murray" <smurray@ebt.com>.
                String prefix =
                    (index = attrName.indexOf(":")) < 0
                        ? ""
                        : attrName.substring(index + 1);

                if (!"".equals(prefix)) {
                    fSerializer.namespaceAfterStartElement(prefix, attrValue);
                }
            }
        }

    }

    /**
     * Serializes an ProcessingInstruction Node.
     *
     * @param node The ProcessingInstruction Node to serialize
     */
    protected void serializePI(ProcessingInstruction node)
        throws SAXException {
        ProcessingInstruction pi = node;
        String name = pi.getNodeName();

        // well-formed=true
        if ((fFeatures & WELLFORMED) != 0) {
            isPIWellFormed(node);
        }

        // apply the LSSerializer filter
        if (!applyFilter(node, NodeFilter.SHOW_PROCESSING_INSTRUCTION)) {
            return;
        }

        // String data = pi.getData();
        if (name.equals("xslt-next-is-raw")) {
            fNextIsRaw = true;
        } else {
            this.fSerializer.processingInstruction(name, pi.getData());
        }
    }

    /**
     * Serializes an CDATASection Node.
     *
     * @param node The CDATASection Node to serialize
     */
    protected void serializeCDATASection(CDATASection node)
        throws SAXException {
        // well-formed=true
        if ((fFeatures & WELLFORMED) != 0) {
            isCDATASectionWellFormed(node);
        }

        // cdata-sections = true
        if ((fFeatures & CDATA) != 0) {

            // split-cdata-sections = true
            // Assumption: This parameter has an effect only when
                        // cdata-sections=true
            // ToStream, by default splits cdata-sections. Hence the check
                        // below.
            String nodeValue = node.getNodeValue();
            int endIndex = nodeValue.indexOf("]]>");
            if ((fFeatures & SPLITCDATA) != 0) {
                if (endIndex >= 0) {
                    // The first node split will contain the ]] markers
                    String relatedData = nodeValue.substring(0, endIndex + 2);

                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_CDATA_SECTIONS_SPLIT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_WARNING,
                                msg,
                                MsgKey.ER_CDATA_SECTIONS_SPLIT,
                                null,
                                relatedData,
                                null));
                    }
                }
            } else {
                if (endIndex >= 0) {
                    // The first node split will contain the ]] markers
                    String relatedData = nodeValue.substring(0, endIndex + 2);

                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_CDATA_SECTIONS_SPLIT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_ERROR,
                                msg,
                                MsgKey.ER_CDATA_SECTIONS_SPLIT));
                    }
                    // Report an error and return.  What error???
                    return;
                }
            }

            // apply the LSSerializer filter
            if (!applyFilter(node, NodeFilter.SHOW_CDATA_SECTION)) {
                return;
            }

            // splits the cdata-section
            if (fLexicalHandler != null) {
                fLexicalHandler.startCDATA();
            }
            dispatachChars(node);
            if (fLexicalHandler != null) {
                fLexicalHandler.endCDATA();
            }
        } else {
            dispatachChars(node);
        }
    }

    /**
     * Serializes an Text Node.
     *
     * @param node The Text Node to serialize
     */
    protected void serializeText(Text node) throws SAXException {
        if (fNextIsRaw) {
            fNextIsRaw = false;
            fSerializer.processingInstruction(
                javax.xml.transform.Result.PI_DISABLE_OUTPUT_ESCAPING,
                "");
            dispatachChars(node);
            fSerializer.processingInstruction(
                javax.xml.transform.Result.PI_ENABLE_OUTPUT_ESCAPING,
                "");
        } else {
            // keep track of dispatch or not to avoid duplicaiton of filter code
            boolean bDispatch = false;

            // well-formed=true
            if ((fFeatures & WELLFORMED) != 0) {
                isTextWellFormed(node);
            }

            // if the node is whitespace
            // Determine the Attr's type.
            boolean isElementContentWhitespace = false;
            if (fIsLevel3DOM) {
                isElementContentWhitespace =
                       node.isElementContentWhitespace();
            }

            if (isElementContentWhitespace) {
                // element-content-whitespace=true
                if ((fFeatures & ELEM_CONTENT_WHITESPACE) != 0) {
                    bDispatch = true;
                }
            } else {
                bDispatch = true;
            }

            // apply the LSSerializer filter
            if (!applyFilter(node, NodeFilter.SHOW_TEXT)) {
                return;
            }

            if (bDispatch
                    && (!fSerializer.getIndent() || !node.getData().replace('\n', ' ').trim().isEmpty())) {
                dispatachChars(node);
            }
        }
    }

    /**
     * Serializes an EntityReference Node.
     *
     * @param node The EntityReference Node to serialize
     * @param bStart Inicates if called from start or endNode
     */
    protected void serializeEntityReference(
        EntityReference node,
        boolean bStart)
        throws SAXException {
        if (bStart) {
            EntityReference eref = node;
            // entities=true
            if ((fFeatures & ENTITIES) != 0) {

                // perform well-formedness and other checking only if
                // entities = true

                // well-formed=true
                if ((fFeatures & WELLFORMED) != 0) {
                    isEntityReferneceWellFormed(node);
                }

                // check "unbound-prefix-in-entity-reference" [fatal]
                // Raised if the configuration parameter "namespaces" is set to true
                if ((fFeatures & NAMESPACES) != 0) {
                    checkUnboundPrefixInEntRef(node);
                }

                // The filter should not apply in this case, since the
                // EntityReference is not being expanded.
                // should we pass entity reference nodes to the filter???
            }

            // if "entities" is true, or EntityReference node has no children,
            // it will be serialized as the form "&entityName;" in the output.
            if (fLexicalHandler != null && ((fFeatures & ENTITIES) != 0 || !node.hasChildNodes())) {

                // startEntity outputs only Text but not Element, Attr, Comment
                // and PI child nodes.  It does so by setting the m_inEntityRef
                // in ToStream and using this to decide if a node is to be
                // serialized or not.
                fLexicalHandler.startEntity(eref.getNodeName());
            }

        } else {
            EntityReference eref = node;
            // entities=true or false,
            if (fLexicalHandler != null) {
                fLexicalHandler.endEntity(eref.getNodeName());
            }
        }
    }


    // ***********************************************************************
    // Methods to check well-formedness
    // ***********************************************************************
    /**
     * Taken from org.apache.xerces.dom.CoreDocumentImpl
     *
     * Check the string against XML's definition of acceptable names for
     * elements and attributes and so on using the XMLCharacterProperties
     * utility class
     */
    protected boolean isXMLName(String s, boolean xml11Version) {

        if (s == null) {
            return false;
        }
        if (!xml11Version)
            return XMLChar.isValidName(s);
        else
            return XML11Char.isXML11ValidName(s);
    }

    /**
     * Taken from org.apache.xerces.dom.CoreDocumentImpl
     *
     * Checks if the given qualified name is legal with respect
     * to the version of XML to which this document must conform.
     *
     * @param prefix prefix of qualified name
     * @param local local part of qualified name
     */
    protected boolean isValidQName(
        String prefix,
        String local,
        boolean xml11Version) {

        // check that both prefix and local part match NCName
        if (local == null)
            return false;
        boolean validNCName = false;

        if (!xml11Version) {
            validNCName =
                (prefix == null || XMLChar.isValidNCName(prefix))
                    && XMLChar.isValidNCName(local);
        } else {
            validNCName =
                (prefix == null || XML11Char.isXML11ValidNCName(prefix))
                    && XML11Char.isXML11ValidNCName(local);
        }

        return validNCName;
    }

    /**
     * Checks if a XML character is well-formed
     *
     * @param characters A String of characters to be checked for Well-Formedness
     * @param refInvalidChar A reference to the character to be returned that was determined invalid.
     */
    protected boolean isWFXMLChar(String chardata, Character refInvalidChar) {
        if (chardata == null || (chardata.length() == 0)) {
            return true;
        }

        char[] dataarray = chardata.toCharArray();
        int datalength = dataarray.length;

        // version of the document is XML 1.1
        if (fIsXMLVersion11) {
            //we need to check all characters as per production rules of XML11
            int i = 0;
            while (i < datalength) {
                if (XML11Char.isXML11Invalid(dataarray[i++])) {
                    // check if this is a supplemental character
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    // Reference to invalid character which is returned
                    refInvalidChar = ch;
                    return false;
                }
            }
        } // version of the document is XML 1.0
        else {
            // we need to check all characters as per production rules of XML 1.0
            int i = 0;
            while (i < datalength) {
                if (XMLChar.isInvalid(dataarray[i++])) {
                    // check if this is a supplemental character
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    // Reference to invalid character which is returned
                    refInvalidChar = ch;
                    return false;
                }
            }
        } // end-else fDocument.isXMLVersion()

        return true;
    } // isXMLCharWF

    /**
     * Checks if a XML character is well-formed.  If there is a problem with
     * the character a non-null Character is returned else null is returned.
     *
     * @param characters A String of characters to be checked for Well-Formedness
     * @return Character A reference to the character to be returned that was determined invalid.
     */
    protected Character isWFXMLChar(String chardata) {
        Character refInvalidChar;
        if (chardata == null || (chardata.length() == 0)) {
            return null;
        }

        char[] dataarray = chardata.toCharArray();
        int datalength = dataarray.length;

        // version of the document is XML 1.1
        if (fIsXMLVersion11) {
            //we need to check all characters as per production rules of XML11
            int i = 0;
            while (i < datalength) {
                if (XML11Char.isXML11Invalid(dataarray[i++])) {
                    // check if this is a supplemental character
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    // Reference to invalid character which is returned
                    refInvalidChar = ch;
                    return refInvalidChar;
                }
            }
        } // version of the document is XML 1.0
        else {
            // we need to check all characters as per production rules of XML 1.0
            int i = 0;
            while (i < datalength) {
                if (XMLChar.isInvalid(dataarray[i++])) {
                    // check if this is a supplemental character
                    char ch = dataarray[i - 1];
                    if (XMLChar.isHighSurrogate(ch) && i < datalength) {
                        char ch2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(ch2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(ch, ch2))) {
                            continue;
                        }
                    }
                    // Reference to invalid character which is returned
                    refInvalidChar = ch;
                    return refInvalidChar;
                }
            }
        } // end-else fDocument.isXMLVersion()

        return null;
    } // isXMLCharWF

    /**
     * Checks if a comment node is well-formed
     *
     * @param data The contents of the comment node
     * @return a boolean indiacating if the comment is well-formed or not.
     */
    protected void isCommentWellFormed(String data) {
        if (data == null || (data.length() == 0)) {
            return;
        }

        char[] dataarray = data.toCharArray();
        int datalength = dataarray.length;

        // version of the document is XML 1.1
        if (fIsXMLVersion11) {
            // we need to check all chracters as per production rules of XML11
            int i = 0;
            while (i < datalength) {
                char c = dataarray[i++];
                if (XML11Char.isXML11Invalid(c)) {
                    // check if this is a supplemental character
                    if (XMLChar.isHighSurrogate(c) && i < datalength) {
                        char c2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(c2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(c, c2))) {
                            continue;
                        }
                    }
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                            new Object[] { c});

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                } else if (c == '-' && i < datalength && dataarray[i] == '-') {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_DASH_IN_COMMENT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                }
            }
        } // version of the document is XML 1.0
        else {
            // we need to check all chracters as per production rules of XML 1.0
            int i = 0;
            while (i < datalength) {
                char c = dataarray[i++];
                if (XMLChar.isInvalid(c)) {
                    // check if this is a supplemental character
                    if (XMLChar.isHighSurrogate(c) && i < datalength) {
                        char c2 = dataarray[i++];
                        if (XMLChar.isLowSurrogate(c2)
                            && XMLChar.isSupplemental(
                                XMLChar.supplemental(c, c2))) {
                            continue;
                        }
                    }
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                            new Object[] { c});

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                } else if (c == '-' && i < datalength && dataarray[i] == '-') {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_WF_DASH_IN_COMMENT,
                            null);

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_WF_INVALID_CHARACTER,
                                null,
                                null,
                                null));
                    }
                }
            }
        }
        return;
    }

    /**
     * Checks if an element node is well-formed, by checking its Name for well-formedness.
     *
     * @param data The contents of the comment node
     * @return a boolean indiacating if the comment is well-formed or not.
     */
    protected void isElementWellFormed(Node node) {
        boolean isNameWF = false;
        if ((fFeatures & NAMESPACES) != 0) {
            isNameWF =
                isValidQName(
                    node.getPrefix(),
                    node.getLocalName(),
                    fIsXMLVersion11);
        } else {
            isNameWF = isXMLName(node.getNodeName(), fIsXMLVersion11);
        }

        if (!isNameWF) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "Element", node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an attr node is well-formed, by checking it's Name and value
     * for well-formedness.
     *
     * @param data The contents of the comment node
     * @return a boolean indiacating if the comment is well-formed or not.
     */
    protected void isAttributeWellFormed(Node node) {
        boolean isNameWF = false;
        if ((fFeatures & NAMESPACES) != 0) {
            isNameWF =
                isValidQName(
                    node.getPrefix(),
                    node.getLocalName(),
                    fIsXMLVersion11);
        } else {
            isNameWF = isXMLName(node.getNodeName(), fIsXMLVersion11);
        }

        if (!isNameWF) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "Attr", node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }

        // Check the Attr's node value
        // WFC: No < in Attribute Values
        String value = node.getNodeValue();
        if (value.indexOf('<') >= 0) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_LT_IN_ATTVAL,
                    new Object[] {
                        ((Attr) node).getOwnerElement().getNodeName(),
                        node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_LT_IN_ATTVAL,
                        null,
                        null,
                        null));
            }
        }

        // we need to loop through the children of attr nodes and check their values for
        // well-formedness
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            // An attribute node with no text or entity ref child for example
            // doc.createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ns");
            // followes by
            // element.setAttributeNodeNS(attribute);
            // can potentially lead to this situation.  If the attribute
            // was a prefix Namespace attribute declaration then then DOM Core
            // should have some exception defined for this.
            if (child == null) {
                // we should probably report an error
                continue;
            }
            switch (child.getNodeType()) {
                case Node.TEXT_NODE :
                    isTextWellFormed((Text) child);
                    break;
                case Node.ENTITY_REFERENCE_NODE :
                    isEntityReferneceWellFormed((EntityReference) child);
                    break;
                default :
            }
        }

        // TODO:
        // WFC: Check if the attribute prefix is bound to
        // http://www.w3.org/2000/xmlns/

        // WFC: Unique Att Spec
        // Perhaps pass a seen boolean value to this method.  serializeAttList will determine
        // if the attr was seen before.
    }

    /**
     * Checks if a PI node is well-formed, by checking it's Name and data
     * for well-formedness.
     *
     * @param data The contents of the comment node
     */
    protected void isPIWellFormed(ProcessingInstruction node) {
        // Is the PI Target a valid XML name
        if (!isXMLName(node.getNodeName(), fIsXMLVersion11)) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "ProcessingInstruction", node.getTarget()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }

        // Does the PI Data carry valid XML characters

        // REVISIT: Should we check if the PI DATA contains a ?> ???
        Character invalidChar = isWFXMLChar(node.getData());
        if (invalidChar != null) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                    new Object[] { Integer.toHexString(Character.getNumericValue(invalidChar.charValue())) });

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an CDATASection node is well-formed, by checking it's data
     * for well-formedness.  Note that the presence of a CDATA termination mark
     * in the contents of a CDATASection is handled by the parameter
     * spli-cdata-sections
     *
     * @param data The contents of the comment node
     */
    protected void isCDATASectionWellFormed(CDATASection node) {
        // Does the data valid XML character data
        Character invalidChar = isWFXMLChar(node.getData());
        //if (!isWFXMLChar(node.getData(), invalidChar)) {
        if (invalidChar != null) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                    new Object[] { Integer.toHexString(Character.getNumericValue(invalidChar.charValue())) });

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an Text node is well-formed, by checking if it contains invalid
     * XML characters.
     *
     * @param data The contents of the comment node
     */
    protected void isTextWellFormed(Text node) {
        // Does the data valid XML character data
        Character invalidChar = isWFXMLChar(node.getData());
        if (invalidChar != null) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                    new Object[] { Integer.toHexString(Character.getNumericValue(invalidChar.charValue())) });

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER,
                        null,
                        null,
                        null));
            }
        }
    }

    /**
     * Checks if an EntityRefernece node is well-formed, by checking it's node name.  Then depending
     * on whether it is referenced in Element content or in an Attr Node, checks if the EntityReference
     * references an unparsed entity or a external entity and if so throws raises the
     * appropriate well-formedness error.
     *
     * @param data The contents of the comment node
     * @parent The parent of the EntityReference Node
     */
    protected void isEntityReferneceWellFormed(EntityReference node) {
        // Is the EntityReference name a valid XML name
        if (!isXMLName(node.getNodeName(), fIsXMLVersion11)) {
            String msg =
                Utils.messages.createMessage(
                    MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                    new Object[] { "EntityReference", node.getNodeName()});

            if (fErrorHandler != null) {
                fErrorHandler.handleError(
                    new DOMErrorImpl(
                        DOMError.SEVERITY_FATAL_ERROR,
                        msg,
                        MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                        null,
                        null,
                        null));
            }
        }

        // determine the parent node
        Node parent = node.getParentNode();

        // Traverse the declared entities and check if the nodeName and namespaceURI
        // of the EntityReference matches an Entity.  If so, check the if the notationName
        // is not null, if so, report an error.
        DocumentType docType = node.getOwnerDocument().getDoctype();
        if (docType != null) {
            NamedNodeMap entities = docType.getEntities();
            for (int i = 0; i < entities.getLength(); i++) {
                Entity ent = (Entity) entities.item(i);

                String nodeName =
                    node.getNodeName() == null ? "" : node.getNodeName();
                String nodeNamespaceURI =
                    node.getNamespaceURI() == null
                        ? ""
                        : node.getNamespaceURI();
                String entName =
                    ent.getNodeName() == null ? "" : ent.getNodeName();
                String entNamespaceURI =
                    ent.getNamespaceURI() == null ? "" : ent.getNamespaceURI();
                // If referenced in Element content
                // WFC: Parsed Entity
                if (parent.getNodeType() == Node.ELEMENT_NODE) {
                    if (entNamespaceURI.equals(nodeNamespaceURI)
                        && entName.equals(nodeName)) {

                        if (ent.getNotationName() != null) {
                            String msg =
                                Utils.messages.createMessage(
                                    MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                                    new Object[] { node.getNodeName()});

                            if (fErrorHandler != null) {
                                fErrorHandler.handleError(
                                    new DOMErrorImpl(
                                        DOMError.SEVERITY_FATAL_ERROR,
                                        msg,
                                        MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                                        null,
                                        null,
                                        null));
                            }
                        }
                    }
                } // end if WFC: Parsed Entity

                // If referenced in an Attr value
                // WFC: No External Entity References
                if (parent.getNodeType() == Node.ATTRIBUTE_NODE) {
                    if (entNamespaceURI.equals(nodeNamespaceURI)
                        && entName.equals(nodeName)) {

                        if (ent.getPublicId() != null
                            || ent.getSystemId() != null
                            || ent.getNotationName() != null) {
                            String msg =
                                Utils.messages.createMessage(
                                    MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                                    new Object[] { node.getNodeName()});

                            if (fErrorHandler != null) {
                                fErrorHandler.handleError(
                                    new DOMErrorImpl(
                                        DOMError.SEVERITY_FATAL_ERROR,
                                        msg,
                                        MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                                        null,
                                        null,
                                        null));
                            }
                        }
                    }
                } //end if WFC: No External Entity References
            }
        }
    } // isEntityReferneceWellFormed

    /**
     * If the configuration parameter "namespaces" is set to true, this methods
     * checks if an entity whose replacement text contains unbound namespace
     * prefixes is referenced in a location where there are no bindings for
     * the namespace prefixes and if so raises a LSException with the error-type
     * "unbound-prefix-in-entity-reference"
     *
     * @param Node, The EntityReference nodes whose children are to be checked
     */
    protected void checkUnboundPrefixInEntRef(Node node) {
        Node child, next;
        for (child = node.getFirstChild(); child != null; child = next) {
            next = child.getNextSibling();

            if (child.getNodeType() == Node.ELEMENT_NODE) {

                //If a NamespaceURI is not declared for the current
                //node's prefix, raise a fatal error.
                String prefix = child.getPrefix();
                if (prefix != null
                                && fNSBinder.getURI(prefix) == null) {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                            new Object[] {
                                node.getNodeName(),
                                child.getNodeName(),
                                prefix });

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_FATAL_ERROR,
                                msg,
                                MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                                null,
                                null,
                                null));
                    }
                }

                NamedNodeMap attrs = child.getAttributes();

                for (int i = 0; i < attrs.getLength(); i++) {
                    String attrPrefix = attrs.item(i).getPrefix();
                    if (attrPrefix != null
                                && fNSBinder.getURI(attrPrefix) == null) {
                        String msg =
                            Utils.messages.createMessage(
                                MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                                new Object[] {
                                    node.getNodeName(),
                                    child.getNodeName(),
                                    attrs.item(i)});

                        if (fErrorHandler != null) {
                            fErrorHandler.handleError(
                                new DOMErrorImpl(
                                    DOMError.SEVERITY_FATAL_ERROR,
                                    msg,
                                    MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                                    null,
                                    null,
                                    null));
                        }
                    }
                }
            }

            if (child.hasChildNodes()) {
                checkUnboundPrefixInEntRef(child);
            }
        }
    }

    // ***********************************************************************
    // Namespace normalization
    // ***********************************************************************
    /**
     * Records local namespace declarations, to be used for normalization later
     *
     * @param Node, The element node, whose namespace declarations are to be recorded
     */
    protected void recordLocalNSDecl(Node node) {
        NamedNodeMap atts = ((Element) node).getAttributes();
        int length = atts.getLength();

        for (int i = 0; i < length; i++) {
            Node attr = atts.item(i);

            String localName = attr.getLocalName();
            String attrPrefix = attr.getPrefix();
            String attrValue = attr.getNodeValue();
            String attrNS = attr.getNamespaceURI();

            localName =
                localName == null
                    || XMLNS_PREFIX.equals(localName) ? "" : localName;
            attrPrefix = attrPrefix == null ? "" : attrPrefix;
            attrValue = attrValue == null ? "" : attrValue;
            attrNS = attrNS == null ? "" : attrNS;

            // check if attribute is a namespace decl
            if (XMLNS_URI.equals(attrNS)) {

                // No prefix may be bound to http://www.w3.org/2000/xmlns/.
                if (XMLNS_URI.equals(attrValue)) {
                    String msg =
                        Utils.messages.createMessage(
                            MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                            new Object[] { attrPrefix, XMLNS_URI });

                    if (fErrorHandler != null) {
                        fErrorHandler.handleError(
                            new DOMErrorImpl(
                                DOMError.SEVERITY_ERROR,
                                msg,
                                MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                                null,
                                null,
                                null));
                    }
                } else {
                    // store the namespace-declaration
                        if (XMLNS_PREFIX.equals(attrPrefix) ) {
                        // record valid decl
                        if (attrValue.length() != 0) {
                            fNSBinder.declarePrefix(localName, attrValue);
                        } else {
                            // Error; xmlns:prefix=""
                        }
                    } else { // xmlns
                        // empty prefix is always bound ("" or some string)
                        fNSBinder.declarePrefix("", attrValue);
                    }
                }

            }
        }
    }

    /**
     * Fixes an element's namespace
     *
     * @param Node, The element node, whose namespace is to be fixed
     */
    protected void fixupElementNS(Node node) throws SAXException {
        String namespaceURI = ((Element) node).getNamespaceURI();
        String prefix = ((Element) node).getPrefix();
        String localName = ((Element) node).getLocalName();

        if (namespaceURI != null) {
            //if ( Element's prefix/namespace pair (or default namespace,
            // if no prefix) are within the scope of a binding )
            prefix = prefix == null ? "" : prefix;
            String inScopeNamespaceURI = fNSBinder.getURI(prefix);

            if ((inScopeNamespaceURI != null
                && inScopeNamespaceURI.equals(namespaceURI))) {
                // do nothing, declaration in scope is inherited

            } else {
                // Create a local namespace declaration attr for this namespace,
                // with Element's current prefix (or a default namespace, if
                // no prefix). If there's a conflicting local declaration
                // already present, change its value to use this namespace.

                // Add the xmlns declaration attribute
                //fNSBinder.pushNamespace(prefix, namespaceURI, fElementDepth);
                if ((fFeatures & NAMESPACEDECLS) != 0) {
                    if ("".equals(prefix) || "".equals(namespaceURI)) {
                        ((Element)node).setAttributeNS(XMLNS_URI, XMLNS_PREFIX, namespaceURI);
                    } else {
                        ((Element)node).setAttributeNS(XMLNS_URI, XMLNS_PREFIX + ":" + prefix, namespaceURI);
                    }
                }
                fLocalNSBinder.declarePrefix(prefix, namespaceURI);
                fNSBinder.declarePrefix(prefix, namespaceURI);

            }
        } else {
            // Element has no namespace
            // DOM Level 1
            if (localName == null || "".equals(localName)) {
                //  DOM Level 1 node!
                String msg =
                    Utils.messages.createMessage(
                        MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                        new Object[] { node.getNodeName()});

                if (fErrorHandler != null) {
                    fErrorHandler.handleError(
                        new DOMErrorImpl(
                            DOMError.SEVERITY_ERROR,
                            msg,
                            MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                            null,
                            null,
                            null));
                }
            } else {
                namespaceURI = fNSBinder.getURI("");
                if (namespaceURI !=null && namespaceURI.length() > 0) {
                    ((Element)node).setAttributeNS(XMLNS_URI, XMLNS_PREFIX, "");
                        fLocalNSBinder.declarePrefix("", "");
                    fNSBinder.declarePrefix("", "");
                }
            }
        }
    }
    /**
     * This table is a quick lookup of a property key (String) to the integer that
     * is the bit to flip in the fFeatures field, so the integers should have
     * values 1,2,4,8,16...
     *
     */
    private static final Map<String, Integer> fFeatureMap;
    static {

        // Initialize the mappings of property keys to bit values (Integer objects)
        // or mappings to a String object "", which indicates we are interested
        // in the property, but it does not have a simple bit value to flip

        Map<String, Integer> featureMap = new HashMap<>();
        // cdata-sections
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CDATA_SECTIONS,
            CDATA);

        // comments
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_COMMENTS,
            COMMENTS);

        // element-content-whitespace
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS
                + DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE,
            ELEM_CONTENT_WHITESPACE);

        // entities
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ENTITIES,
            ENTITIES);

        // namespaces
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACES,
            NAMESPACES);

        // namespace-declarations
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS
                + DOMConstants.DOM_NAMESPACE_DECLARATIONS,
            NAMESPACEDECLS);

        // split-cdata-sections
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_SPLIT_CDATA,
            SPLITCDATA);

        // discard-default-content
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_WELLFORMED,
            WELLFORMED);

        // discard-default-content
        featureMap.put(
            DOMConstants.S_DOM3_PROPERTIES_NS
                + DOMConstants.DOM_DISCARD_DEFAULT_CONTENT,
            DISCARDDEFAULT);

        fFeatureMap = Collections.unmodifiableMap(featureMap);
    }

    /**
     * Initializes fFeatures based on the DOMConfiguration Parameters set.
     *
     * @param properties DOMConfiguraiton properties that were set and which are
     * to be used while serializing the DOM.
     */
    protected void initProperties(Properties properties) {
        for(String key : properties.stringPropertyNames()) {

            // caonical-form
            // Other features will be enabled or disabled when this is set to true or false.

            // error-handler; set via the constructor

            // infoset
            // Other features will be enabled or disabled when this is set to true

            // A quick lookup for the given set of properties (cdata-sections ...)
            final Integer bitFlag = fFeatureMap.get(key);
            if (bitFlag != null) {
                // Dealing with a property that has a simple bit value that
                // we need to set

                // cdata-sections
                // comments
                // element-content-whitespace
                // entities
                // namespaces
                // namespace-declarations
                // split-cdata-sections
                // well-formed
                // discard-default-content
                if ((properties.getProperty(key).endsWith("yes"))) {
                    fFeatures = fFeatures | bitFlag;
                } else {
                    fFeatures = fFeatures & ~bitFlag;
                }
            } else {
                /**
                 * Other properties that have a bit more complex value
                 * than the features in the above map.
                 */
                if ((DOMConstants.S_DOM3_PROPERTIES_NS
                    + DOMConstants.DOM_FORMAT_PRETTY_PRINT)
                    .equals(key)) {
                    // format-pretty-print; set internally on the serializers via xsl:output properties in LSSerializer
                    if ((properties.getProperty(key).endsWith("yes"))) {
                        fSerializer.setIndent(true);
                        fSerializer.setIndentAmount(4);
                    } else {
                        fSerializer.setIndent(false);
                    }
                } else if ((DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL).equals(key)) {
                    // omit-xml-declaration; set internally on the serializers via xsl:output properties in LSSerializer
                    if ((properties.getProperty(key).endsWith("yes"))) {
                        fSerializer.setOmitXMLDeclaration(true);
                    } else {
                        fSerializer.setOmitXMLDeclaration(false);
                    }
                } else if ((DOMConstants.S_XERCES_PROPERTIES_NS
                            + DOMConstants.S_XML_VERSION).equals(key)) {
                    // Retreive the value of the XML Version attribute via the xml-version
                    String version = properties.getProperty(key);
                    if ("1.1".equals(version)) {
                        fIsXMLVersion11 = true;
                        fSerializer.setVersion(version);
                    } else {
                        fSerializer.setVersion("1.0");
                    }
                } else if ((DOMConstants.S_XSL_OUTPUT_ENCODING).equals(key)) {
                    // Retreive the value of the XML Encoding attribute
                    String encoding = properties.getProperty(key);
                    if (encoding != null) {
                        fSerializer.setEncoding(encoding);
                    }
                } else if ((OutputPropertiesFactory.S_KEY_ENTITIES).equals(key)) {
                    // Retreive the value of the XML Encoding attribute
                    String entities = properties.getProperty(key);
                    if (DOMConstants.S_XSL_VALUE_ENTITIES.equals(entities)) {
                        fSerializer.setDTDEntityExpansion(false);
                    }
                }
            }
        }
        // Set the newLine character to use
        if (fNewLine != null) {
            fSerializer.setOutputProperty(OutputPropertiesFactory.S_KEY_LINE_SEPARATOR, fNewLine);
        }
    }

} //TreeWalker
