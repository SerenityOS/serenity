/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.xsltc.dom;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.StripFilter;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.DTMAxisTraverser;
import com.sun.org.apache.xml.internal.dtm.DTMWSFilter;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.utils.XMLString;
import java.util.Map;
import javax.xml.transform.SourceLocator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * AdaptiveResultTreeImpl is a adaptive DOM model for result tree fragments (RTF). It is
 * used in the case where the RTF is likely to be pure text yet it can still be a DOM tree.
 * It is designed for RTFs which have &lt;xsl:call-template&gt; or &lt;xsl:apply-templates&gt; in
 * the contents. Example:
 * <pre>
 *    &lt;xsl:variable name = "x"&gt;
 *      &lt;xsl:call-template name = "test"&gt;
 *         &lt;xsl:with-param name="a" select="."/&gt;
 *      &lt;/xsl:call-template&gt;
 *    &lt;/xsl:variable>
 * </pre>
 * <p>In this example the result produced by <xsl:call-template> is likely to be a single
 * Text node. But it can also be a DOM tree. This kind of RTF cannot be modelled by
 * SimpleResultTreeImpl.
 * <p>
 * AdaptiveResultTreeImpl can be considered as a smart switcher between SimpleResultTreeImpl
 * and SAXImpl. It treats the RTF as simple Text and uses the SimpleResultTreeImpl model
 * at the beginning. However, if it receives a call which indicates that this is a DOM tree
 * (e.g. startElement), it will automatically transform itself into a wrapper around a
 * SAXImpl. In this way we can have a light-weight model when the result only contains
 * simple text, while at the same time it still works when the RTF is a DOM tree.
 * <p>
 * All methods in this class are overridden to delegate the action to the wrapped SAXImpl object
 * if it is non-null, or delegate the action to the SimpleResultTreeImpl if there is no
 * wrapped SAXImpl.
 * <p>
 * %REVISIT% Can we combine this class with SimpleResultTreeImpl? I think it is possible, but
 * it will make SimpleResultTreeImpl more expensive. I will use two separate classes at
 * this time.
 */
public class AdaptiveResultTreeImpl extends SimpleResultTreeImpl
{

    // Document URI index, which increases by 1 at each getDocumentURI() call.
    private static int _documentURIIndex = 0;

    private static final String EMPTY_STRING = "".intern();

    // The SAXImpl object wrapped by this class, if the RTF is a tree.
    private SAXImpl _dom;

    /** The following fields are only used for the nested SAXImpl **/

    // The whitespace filter
    private DTMWSFilter _wsfilter;

    // The size of the RTF
    private int _initSize;

    // True if we want to build the ID index table
    private boolean _buildIdIndex;

    // The AttributeList
    private final AttributesImpl _attributes = new AttributesImpl();

    // The element name
    private String _openElementName;


    // Create a AdaptiveResultTreeImpl
    public AdaptiveResultTreeImpl(XSLTCDTMManager dtmManager, int documentID,
                                  DTMWSFilter wsfilter, int initSize,
                                  boolean buildIdIndex)
    {
        super(dtmManager, documentID);

        _wsfilter = wsfilter;
        _initSize = initSize;
        _buildIdIndex = buildIdIndex;
    }

    // Return the DOM object wrapped in this object.
    public DOM getNestedDOM()
    {
        return _dom;
    }

    // Return the document ID
    public int getDocument()
    {
        if (_dom != null) {
            return _dom.getDocument();
        }
        else {
            return super.getDocument();
        }
    }

    // Return the String value of the RTF
    public String getStringValue()
    {
        if (_dom != null) {
            return _dom.getStringValue();
        }
        else {
            return super.getStringValue();
        }
    }

    public DTMAxisIterator getIterator()
    {
        if (_dom != null) {
            return _dom.getIterator();
        }
        else {
            return super.getIterator();
        }
    }

    public DTMAxisIterator getChildren(final int node)
    {
        if (_dom != null) {
            return _dom.getChildren(node);
        }
        else {
            return super.getChildren(node);
        }
    }

    public DTMAxisIterator getTypedChildren(final int type)
    {
        if (_dom != null) {
            return _dom.getTypedChildren(type);
        }
        else {
            return super.getTypedChildren(type);
        }
    }

    public DTMAxisIterator getAxisIterator(final int axis)
    {
        if (_dom != null) {
            return _dom.getAxisIterator(axis);
        }
        else {
            return super.getAxisIterator(axis);
        }
    }

    public DTMAxisIterator getTypedAxisIterator(final int axis, final int type)
    {
        if (_dom != null) {
            return _dom.getTypedAxisIterator(axis, type);
        }
        else {
            return super.getTypedAxisIterator(axis, type);
        }
    }

    public DTMAxisIterator getNthDescendant(int node, int n, boolean includeself)
    {
        if (_dom != null) {
            return _dom.getNthDescendant(node, n, includeself);
        }
        else {
            return super.getNthDescendant(node, n, includeself);
        }
    }

    public DTMAxisIterator getNamespaceAxisIterator(final int axis, final int ns)
    {
        if (_dom != null) {
            return _dom.getNamespaceAxisIterator(axis, ns);
        }
        else {
            return super.getNamespaceAxisIterator(axis, ns);
        }
    }

    public DTMAxisIterator getNodeValueIterator(DTMAxisIterator iter, int returnType,
                                             String value, boolean op)
    {
        if (_dom != null) {
            return _dom.getNodeValueIterator(iter, returnType, value, op);
        }
        else {
            return super.getNodeValueIterator(iter, returnType, value, op);
        }
    }

    public DTMAxisIterator orderNodes(DTMAxisIterator source, int node)
    {
        if (_dom != null) {
            return _dom.orderNodes(source, node);
        }
        else {
            return super.orderNodes(source, node);
        }
    }

    public String getNodeName(final int node)
    {
        if (_dom != null) {
            return _dom.getNodeName(node);
        }
        else {
            return super.getNodeName(node);
        }
    }

    public String getNodeNameX(final int node)
    {
        if (_dom != null) {
            return _dom.getNodeNameX(node);
        }
        else {
            return super.getNodeNameX(node);
        }
    }

    public String getNamespaceName(final int node)
    {
        if (_dom != null) {
            return _dom.getNamespaceName(node);
        }
        else {
            return super.getNamespaceName(node);
        }
    }

    // Return the expanded type id of a given node
    public int getExpandedTypeID(final int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getExpandedTypeID(nodeHandle);
        }
        else {
            return super.getExpandedTypeID(nodeHandle);
        }
    }

    public int getNamespaceType(final int node)
    {
        if (_dom != null) {
            return _dom.getNamespaceType(node);
        }
        else {
            return super.getNamespaceType(node);
        }
    }

    public int getParent(final int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getParent(nodeHandle);
        }
        else {
            return super.getParent(nodeHandle);
        }
    }

    public int getAttributeNode(final int gType, final int element)
    {
        if (_dom != null) {
            return _dom.getAttributeNode(gType, element);
        }
        else {
            return super.getAttributeNode(gType, element);
        }
    }

    public String getStringValueX(final int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getStringValueX(nodeHandle);
        }
        else {
            return super.getStringValueX(nodeHandle);
        }
    }

    public void copy(final int node, SerializationHandler handler)
        throws TransletException
    {
        if (_dom != null) {
            _dom.copy(node, handler);
        }
        else {
            super.copy(node, handler);
        }
    }

    public void copy(DTMAxisIterator nodes, SerializationHandler handler)
        throws TransletException
    {
        if (_dom != null) {
            _dom.copy(nodes, handler);
        }
        else {
            super.copy(nodes, handler);
        }
    }

    public String shallowCopy(final int node, SerializationHandler handler)
        throws TransletException
    {
        if (_dom != null) {
            return _dom.shallowCopy(node, handler);
        }
        else {
            return super.shallowCopy(node, handler);
        }
    }

    public boolean lessThan(final int node1, final int node2)
    {
        if (_dom != null) {
            return _dom.lessThan(node1, node2);
        }
        else {
            return super.lessThan(node1, node2);
        }
    }

    /**
     * Dispatch the character content of a node to an output handler.
     *
     * The escape setting should be taken care of when outputting to
     * a handler.
     */
    public void characters(final int node, SerializationHandler handler)
        throws TransletException
    {
        if (_dom != null) {
            _dom.characters(node, handler);
        }
        else {
            super.characters(node, handler);
        }
    }

    public Node makeNode(int index)
    {
        if (_dom != null) {
            return _dom.makeNode(index);
        }
        else {
            return super.makeNode(index);
        }
    }

    public Node makeNode(DTMAxisIterator iter)
    {
        if (_dom != null) {
            return _dom.makeNode(iter);
        }
        else {
            return super.makeNode(iter);
        }
    }

    public NodeList makeNodeList(int index)
    {
        if (_dom != null) {
            return _dom.makeNodeList(index);
        }
        else {
            return super.makeNodeList(index);
        }
    }

    public NodeList makeNodeList(DTMAxisIterator iter)
    {
        if (_dom != null) {
            return _dom.makeNodeList(iter);
        }
        else {
            return super.makeNodeList(iter);
        }
    }

    public String getLanguage(int node)
    {
        if (_dom != null) {
            return _dom.getLanguage(node);
        }
        else {
            return super.getLanguage(node);
        }
    }

    public int getSize()
    {
        if (_dom != null) {
            return _dom.getSize();
        }
        else {
            return super.getSize();
        }
    }

    public String getDocumentURI(int node)
    {
        if (_dom != null) {
            return _dom.getDocumentURI(node);
        }
        else {
            return "adaptive_rtf" + _documentURIIndex++;
        }
    }

    public void setFilter(StripFilter filter)
    {
        if (_dom != null) {
            _dom.setFilter(filter);
        }
        else {
            super.setFilter(filter);
        }
    }

    public void setupMapping(String[] names, String[] uris, int[] types, String[] namespaces)
    {
        if (_dom != null) {
            _dom.setupMapping(names, uris, types, namespaces);
        }
        else {
            super.setupMapping(names, uris, types, namespaces);
        }
    }

    public boolean isElement(final int node)
    {
        if (_dom != null) {
            return _dom.isElement(node);
        }
        else {
            return super.isElement(node);
        }
    }

    public boolean isAttribute(final int node)
    {
        if (_dom != null) {
            return _dom.isAttribute(node);
        }
        else {
            return super.isAttribute(node);
        }
    }

    public String lookupNamespace(int node, String prefix)
        throws TransletException
    {
        if (_dom != null) {
            return _dom.lookupNamespace(node, prefix);
        }
        else {
            return super.lookupNamespace(node, prefix);
        }
    }

    /**
     * Return the node identity from a node handle.
     */
    public final int getNodeIdent(final int nodehandle)
    {
        if (_dom != null) {
            return _dom.getNodeIdent(nodehandle);
        }
        else {
            return super.getNodeIdent(nodehandle);
        }
    }

    /**
     * Return the node handle from a node identity.
     */
    public final int getNodeHandle(final int nodeId)
    {
        if (_dom != null) {
            return _dom.getNodeHandle(nodeId);
        }
        else {
            return super.getNodeHandle(nodeId);
        }
    }

    public DOM getResultTreeFrag(int initialSize, int rtfType)
    {
        if (_dom != null) {
            return _dom.getResultTreeFrag(initialSize, rtfType);
        }
        else {
            return super.getResultTreeFrag(initialSize, rtfType);
        }
    }

    public SerializationHandler getOutputDomBuilder()
    {
        return this;
    }

    public int getNSType(int node)
    {
        if (_dom != null) {
            return _dom.getNSType(node);
        }
        else {
            return super.getNSType(node);
        }
    }

    public String getUnparsedEntityURI(String name)
    {
        if (_dom != null) {
            return _dom.getUnparsedEntityURI(name);
        }
        else {
            return super.getUnparsedEntityURI(name);
        }
    }

    public Map<String, Integer> getElementsWithIDs()
    {
        if (_dom != null) {
            return _dom.getElementsWithIDs();
        }
        else {
            return super.getElementsWithIDs();
        }
    }

    /** Implementation of the SerializationHandler interfaces **/

    /** The code in some of the following interfaces are copied from SAXAdapter. **/

    private void maybeEmitStartElement() throws SAXException
    {
        if (_openElementName != null) {

           int index;
           if ((index =_openElementName.indexOf(':')) < 0)
               _dom.startElement(null, _openElementName, _openElementName, _attributes);
           else {
                String uri =_dom.getNamespaceURI(_openElementName.substring(0,index));
                _dom.startElement(uri, _openElementName.substring(index+1), _openElementName, _attributes);
           }


            _openElementName = null;
        }

    }

    // Create and initialize the wrapped SAXImpl object
    private void prepareNewDOM() throws SAXException
    {
        _dom = (SAXImpl)_dtmManager.getDTM(null, true, _wsfilter,
                                  true, false, false,
                                  _initSize, _buildIdIndex);
        _dom.startDocument();
        // Flush pending Text nodes to SAXImpl
        for (int i = 0; i < _size; i++) {
            String str = _textArray[i];
            _dom.characters(str.toCharArray(), 0, str.length());
        }
        _size = 0;
    }

    public void startDocument() throws SAXException
    {
    }

    public void endDocument() throws SAXException
    {
        if (_dom != null) {
            _dom.endDocument();
        }
        else {
            super.endDocument();
        }
    }

    public void characters(String str) throws SAXException
    {
        if (_dom != null) {
            characters(str.toCharArray(), 0, str.length());
        }
        else {
            super.characters(str);
        }
    }

    public void characters(char[] ch, int offset, int length)
        throws SAXException
    {
        if (_dom != null) {
            maybeEmitStartElement();
            _dom.characters(ch, offset, length);
        }
        else {
            super.characters(ch, offset, length);
        }
    }

    public boolean setEscaping(boolean escape) throws SAXException
    {
        if (_dom != null) {
            return _dom.setEscaping(escape);
        }
        else {
            return super.setEscaping(escape);
        }
    }

    public void startElement(String elementName) throws SAXException
    {
        if (_dom == null) {
            prepareNewDOM();
        }

        maybeEmitStartElement();
        _openElementName = elementName;
        _attributes.clear();
    }

    public void startElement(String uri, String localName, String qName)
        throws SAXException
    {
        startElement(qName);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        startElement(qName);
    }

    public void endElement(String elementName) throws SAXException
    {
        maybeEmitStartElement();
        _dom.endElement(null, null, elementName);
    }

    public void endElement(String uri, String localName, String qName)
        throws SAXException
    {
        endElement(qName);
    }

    public void addAttribute(String qName, String value)
    {
        // "prefix:localpart" or "localpart"
        int colonpos = qName.indexOf(':');
        String uri = EMPTY_STRING;
        String localName = qName;
        if (colonpos >0)
        {
            String prefix = qName.substring(0, colonpos);
            localName = qName.substring(colonpos+1);
            uri = _dom.getNamespaceURI(prefix);
        }

        addAttribute(uri, localName, qName, "CDATA", value);
    }

    public void addUniqueAttribute(String qName, String value, int flags)
        throws SAXException
    {
        addAttribute(qName, value);
    }

    public void addAttribute(String uri, String localName, String qname,
            String type, String value)
    {
        if (_openElementName != null) {
            _attributes.addAttribute(uri, localName, qname, type, value);
        }
        else {
            BasisLibrary.runTimeError(BasisLibrary.STRAY_ATTRIBUTE_ERR, qname);
        }
    }

    public void namespaceAfterStartElement(String prefix, String uri)
        throws SAXException
    {
        if (_dom == null) {
           prepareNewDOM();
        }

        _dom.startPrefixMapping(prefix, uri);
    }

    public void comment(String comment) throws SAXException
    {
        if (_dom == null) {
           prepareNewDOM();
        }

        maybeEmitStartElement();
        char[] chars = comment.toCharArray();
        _dom.comment(chars, 0, chars.length);
    }

    public void comment(char[] chars, int offset, int length)
        throws SAXException
    {
        if (_dom == null) {
           prepareNewDOM();
        }

        maybeEmitStartElement();
        _dom.comment(chars, offset, length);
    }

    public void processingInstruction(String target, String data)
        throws SAXException
    {
        if (_dom == null) {
           prepareNewDOM();
        }

        maybeEmitStartElement();
        _dom.processingInstruction(target, data);
    }

    /** Implementation of the DTM interfaces **/

    public void setFeature(String featureId, boolean state)
    {
        if (_dom != null) {
            _dom.setFeature(featureId, state);
        }
    }

    public void setProperty(String property, Object value)
    {
        if (_dom != null) {
            _dom.setProperty(property, value);
        }
    }

    public DTMAxisTraverser getAxisTraverser(final int axis)
    {
        if (_dom != null) {
            return _dom.getAxisTraverser(axis);
        }
        else {
            return super.getAxisTraverser(axis);
        }
    }

    public boolean hasChildNodes(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.hasChildNodes(nodeHandle);
        }
        else {
            return super.hasChildNodes(nodeHandle);
        }
    }

    public int getFirstChild(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getFirstChild(nodeHandle);
        }
        else {
            return super.getFirstChild(nodeHandle);
        }
    }

    public int getLastChild(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getLastChild(nodeHandle);
        }
        else {
            return super.getLastChild(nodeHandle);
        }
    }

    public int getAttributeNode(int elementHandle, String namespaceURI, String name)
    {
        if (_dom != null) {
            return _dom.getAttributeNode(elementHandle, namespaceURI, name);
        }
        else {
            return super.getAttributeNode(elementHandle, namespaceURI, name);
        }
    }

    public int getFirstAttribute(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getFirstAttribute(nodeHandle);
        }
        else {
            return super.getFirstAttribute(nodeHandle);
        }
    }

    public int getFirstNamespaceNode(int nodeHandle, boolean inScope)
    {
        if (_dom != null) {
            return _dom.getFirstNamespaceNode(nodeHandle, inScope);
        }
        else {
            return super.getFirstNamespaceNode(nodeHandle, inScope);
        }
    }

    public int getNextSibling(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getNextSibling(nodeHandle);
        }
        else {
            return super.getNextSibling(nodeHandle);
        }
     }

    public int getPreviousSibling(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getPreviousSibling(nodeHandle);
        }
        else {
            return super.getPreviousSibling(nodeHandle);
        }
     }

    public int getNextAttribute(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getNextAttribute(nodeHandle);
        }
        else {
            return super.getNextAttribute(nodeHandle);
        }
    }

    public int getNextNamespaceNode(int baseHandle, int namespaceHandle,
                                  boolean inScope)
    {
        if (_dom != null) {
            return _dom.getNextNamespaceNode(baseHandle, namespaceHandle, inScope);
        }
        else {
            return super.getNextNamespaceNode(baseHandle, namespaceHandle, inScope);
        }
    }

    public int getOwnerDocument(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getOwnerDocument(nodeHandle);
        }
        else {
            return super.getOwnerDocument(nodeHandle);
        }
    }

    public int getDocumentRoot(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getDocumentRoot(nodeHandle);
        }
        else {
            return super.getDocumentRoot(nodeHandle);
        }
    }

    public XMLString getStringValue(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getStringValue(nodeHandle);
        }
        else {
            return super.getStringValue(nodeHandle);
        }
    }

    public int getStringValueChunkCount(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getStringValueChunkCount(nodeHandle);
        }
        else {
            return super.getStringValueChunkCount(nodeHandle);
        }
    }

    public char[] getStringValueChunk(int nodeHandle, int chunkIndex,
                                    int[] startAndLen)
    {
        if (_dom != null) {
            return _dom.getStringValueChunk(nodeHandle, chunkIndex, startAndLen);
        }
        else {
            return super.getStringValueChunk(nodeHandle, chunkIndex, startAndLen);
        }
    }

    public int getExpandedTypeID(String namespace, String localName, int type)
    {
        if (_dom != null) {
            return _dom.getExpandedTypeID(namespace, localName, type);
        }
        else {
            return super.getExpandedTypeID(namespace, localName, type);
        }
    }

    public String getLocalNameFromExpandedNameID(int ExpandedNameID)
    {
        if (_dom != null) {
            return _dom.getLocalNameFromExpandedNameID(ExpandedNameID);
        }
        else {
            return super.getLocalNameFromExpandedNameID(ExpandedNameID);
        }
    }

    public String getNamespaceFromExpandedNameID(int ExpandedNameID)
    {
        if (_dom != null) {
            return _dom.getNamespaceFromExpandedNameID(ExpandedNameID);
        }
        else {
            return super.getNamespaceFromExpandedNameID(ExpandedNameID);
        }
    }

    public String getLocalName(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getLocalName(nodeHandle);
        }
        else {
            return super.getLocalName(nodeHandle);
        }
    }

    public String getPrefix(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getPrefix(nodeHandle);
        }
        else {
            return super.getPrefix(nodeHandle);
        }
    }

    public String getNamespaceURI(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getNamespaceURI(nodeHandle);
        }
        else {
            return super.getNamespaceURI(nodeHandle);
        }
    }

    public String getNodeValue(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getNodeValue(nodeHandle);
        }
        else {
            return super.getNodeValue(nodeHandle);
        }
    }

    public short getNodeType(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getNodeType(nodeHandle);
        }
        else {
            return super.getNodeType(nodeHandle);
        }
    }

    public short getLevel(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getLevel(nodeHandle);
        }
        else {
            return super.getLevel(nodeHandle);
        }
    }

    public boolean isSupported(String feature, String version)
    {
        if (_dom != null) {
            return _dom.isSupported(feature, version);
        }
        else {
            return super.isSupported(feature, version);
        }
    }

    public String getDocumentBaseURI()
    {
        if (_dom != null) {
            return _dom.getDocumentBaseURI();
        }
        else {
            return super.getDocumentBaseURI();
        }
    }

    public void setDocumentBaseURI(String baseURI)
    {
        if (_dom != null) {
            _dom.setDocumentBaseURI(baseURI);
        }
        else {
            super.setDocumentBaseURI(baseURI);
        }
    }

    public String getDocumentSystemIdentifier(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getDocumentSystemIdentifier(nodeHandle);
        }
        else {
            return super.getDocumentSystemIdentifier(nodeHandle);
        }
    }

    public String getDocumentEncoding(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getDocumentEncoding(nodeHandle);
        }
        else {
            return super.getDocumentEncoding(nodeHandle);
        }
    }

    public String getDocumentStandalone(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getDocumentStandalone(nodeHandle);
        }
        else {
            return super.getDocumentStandalone(nodeHandle);
        }
    }

    public String getDocumentVersion(int documentHandle)
    {
        if (_dom != null) {
            return _dom.getDocumentVersion(documentHandle);
        }
        else {
            return super.getDocumentVersion(documentHandle);
        }
    }

    public boolean getDocumentAllDeclarationsProcessed()
    {
        if (_dom != null) {
            return _dom.getDocumentAllDeclarationsProcessed();
        }
        else {
            return super.getDocumentAllDeclarationsProcessed();
        }
    }

    public String getDocumentTypeDeclarationSystemIdentifier()
    {
        if (_dom != null) {
            return _dom.getDocumentTypeDeclarationSystemIdentifier();
        }
        else {
            return super.getDocumentTypeDeclarationSystemIdentifier();
        }
    }

    public String getDocumentTypeDeclarationPublicIdentifier()
    {
        if (_dom != null) {
            return _dom.getDocumentTypeDeclarationPublicIdentifier();
        }
        else {
            return super.getDocumentTypeDeclarationPublicIdentifier();
        }
    }

    public int getElementById(String elementId)
    {
        if (_dom != null) {
            return _dom.getElementById(elementId);
        }
        else {
            return super.getElementById(elementId);
        }
    }

    public boolean supportsPreStripping()
    {
        if (_dom != null) {
            return _dom.supportsPreStripping();
        }
        else {
            return super.supportsPreStripping();
        }
    }

    public boolean isNodeAfter(int firstNodeHandle, int secondNodeHandle)
    {
        if (_dom != null) {
            return _dom.isNodeAfter(firstNodeHandle, secondNodeHandle);
        }
        else {
            return super.isNodeAfter(firstNodeHandle, secondNodeHandle);
        }
    }

    public boolean isCharacterElementContentWhitespace(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.isCharacterElementContentWhitespace(nodeHandle);
        }
        else {
            return super.isCharacterElementContentWhitespace(nodeHandle);
        }
    }

    public boolean isDocumentAllDeclarationsProcessed(int documentHandle)
    {
        if (_dom != null) {
            return _dom.isDocumentAllDeclarationsProcessed(documentHandle);
        }
        else {
            return super.isDocumentAllDeclarationsProcessed(documentHandle);
        }
    }

    public boolean isAttributeSpecified(int attributeHandle)
    {
        if (_dom != null) {
            return _dom.isAttributeSpecified(attributeHandle);
        }
        else {
            return super.isAttributeSpecified(attributeHandle);
        }
    }

    public void dispatchCharactersEvents(int nodeHandle, org.xml.sax.ContentHandler ch,
                                         boolean normalize)
          throws org.xml.sax.SAXException
    {
        if (_dom != null) {
            _dom.dispatchCharactersEvents(nodeHandle,  ch, normalize);
        }
        else {
            super.dispatchCharactersEvents(nodeHandle, ch, normalize);
        }
    }

    public void dispatchToEvents(int nodeHandle, org.xml.sax.ContentHandler ch)
      throws org.xml.sax.SAXException
    {
        if (_dom != null) {
            _dom.dispatchToEvents(nodeHandle,  ch);
        }
        else {
            super.dispatchToEvents(nodeHandle, ch);
        }
    }

    public org.w3c.dom.Node getNode(int nodeHandle)
    {
        if (_dom != null) {
            return _dom.getNode(nodeHandle);
        }
        else {
            return super.getNode(nodeHandle);
        }
    }

    public boolean needsTwoThreads()
    {
        if (_dom != null) {
            return _dom.needsTwoThreads();
        }
        else {
            return super.needsTwoThreads();
        }
    }

    public org.xml.sax.ContentHandler getContentHandler()
    {
        if (_dom != null) {
            return _dom.getContentHandler();
        }
        else {
            return super.getContentHandler();
        }
    }

    public org.xml.sax.ext.LexicalHandler getLexicalHandler()
    {
        if (_dom != null) {
            return _dom.getLexicalHandler();
        }
        else {
            return super.getLexicalHandler();
        }
    }

    public org.xml.sax.EntityResolver getEntityResolver()
    {
        if (_dom != null) {
            return _dom.getEntityResolver();
        }
        else {
            return super.getEntityResolver();
        }
    }

    public org.xml.sax.DTDHandler getDTDHandler()
    {
        if (_dom != null) {
            return _dom.getDTDHandler();
        }
        else {
            return super.getDTDHandler();
        }
    }

    public org.xml.sax.ErrorHandler getErrorHandler()
    {
        if (_dom != null) {
            return _dom.getErrorHandler();
        }
        else {
            return super.getErrorHandler();
        }
    }

    public org.xml.sax.ext.DeclHandler getDeclHandler()
    {
        if (_dom != null) {
            return _dom.getDeclHandler();
        }
        else {
            return super.getDeclHandler();
        }
    }

    public void appendChild(int newChild, boolean clone, boolean cloneDepth)
    {
        if (_dom != null) {
            _dom.appendChild(newChild, clone, cloneDepth);
        }
        else {
            super.appendChild(newChild, clone, cloneDepth);
        }
    }

    public void appendTextChild(String str)
    {
        if (_dom != null) {
            _dom.appendTextChild(str);
        }
        else {
            super.appendTextChild(str);
        }
    }

    public SourceLocator getSourceLocatorFor(int node)
    {
        if (_dom != null) {
            return _dom.getSourceLocatorFor(node);
        }
        else {
            return super.getSourceLocatorFor(node);
        }
    }

    public void documentRegistration()
    {
        if (_dom != null) {
            _dom.documentRegistration();
        }
        else {
            super.documentRegistration();
        }
    }

    public void documentRelease()
    {
        if (_dom != null) {
            _dom.documentRelease();
        }
        else {
            super.documentRelease();
        }
    }

    public void release() {
        if (_dom != null) {
            _dom.release();
            _dom = null;
        }
        super.release();
    }
}
