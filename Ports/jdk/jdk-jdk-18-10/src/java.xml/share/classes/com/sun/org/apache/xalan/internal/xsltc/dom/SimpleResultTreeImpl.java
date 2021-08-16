/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.xsltc.dom;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.StripFilter;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xml.internal.dtm.Axis;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.DTMAxisTraverser;
import com.sun.org.apache.xml.internal.dtm.DTMManager;
import com.sun.org.apache.xml.internal.dtm.ref.DTMAxisIteratorBase;
import com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault;
import com.sun.org.apache.xml.internal.serializer.EmptySerializer;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.utils.XMLString;
import com.sun.org.apache.xml.internal.utils.XMLStringDefault;
import java.util.Map;
import javax.xml.transform.SourceLocator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class represents a light-weight DOM model for simple result tree fragment(RTF).
 * A simple RTF is an RTF that has only one Text node. The Text node can be produced by a
 * combination of Text, xsl:value-of and xsl:number instructions. It can also be produced
 * by a control structure (xsl:if or xsl:choose) whose body is pure Text.
 * <p>
 * A SimpleResultTreeImpl has only two nodes, i.e. the ROOT node and its Text child. All DOM
 * interfaces are overridden with this in mind. For example, the getStringValue() interface
 * returns the value of the Text node. This class receives the character data from the
 * characters() interface.
 * <p>
 * This class implements DOM and SerializationHandler. It also implements the DTM interface
 * for support in MultiDOM. The nested iterators (SimpleIterator and SingletonIterator) are
 * used to support the nodeset() extension function.
 */
public class SimpleResultTreeImpl extends EmptySerializer implements DOM, DTM
{

    /**
     * The SimpleIterator is designed to support the nodeset() extension function. It has
     * a traversal direction parameter. The DOWN direction is used for child and descendant
     * axes, while the UP direction is used for parent and ancestor axes.
     *
     * This iterator only handles two nodes (RTF_ROOT and RTF_TEXT). If the type is set,
     * it will also match the node type with the given type.
     */
    public final class SimpleIterator extends DTMAxisIteratorBase
    {
        static final int DIRECTION_UP = 0;
        static final int DIRECTION_DOWN = 1;
        static final int NO_TYPE = -1;

        // The direction of traversal (default to DOWN).
        // DOWN is for child and descendant. UP is for parent and ancestor.
        int _direction = DIRECTION_DOWN;

        int _type = NO_TYPE;
        int _currentNode;

        public SimpleIterator()
        {
        }

        public SimpleIterator(int direction)
        {
            _direction = direction;
        }

        public SimpleIterator(int direction, int type)
        {
             _direction = direction;
             _type = type;
        }

        public int next()
        {
            // Increase the node ID for down traversal. Also match the node type
            // if the type is given.
            if (_direction == DIRECTION_DOWN) {
                while (_currentNode < NUMBER_OF_NODES) {
                    if (_type != NO_TYPE) {
                        if ((_currentNode == RTF_ROOT && _type == DTM.ROOT_NODE)
                            || (_currentNode == RTF_TEXT && _type == DTM.TEXT_NODE))
                            return returnNode(getNodeHandle(_currentNode++));
                        else
                            _currentNode++;
                    }
                    else
                        return returnNode(getNodeHandle(_currentNode++));
                }

                return END;
            }
            // Decrease the node ID for up traversal.
            else {
                while (_currentNode >= 0) {
                    if (_type != NO_TYPE) {
                        if ((_currentNode == RTF_ROOT && _type == DTM.ROOT_NODE)
                            || (_currentNode == RTF_TEXT && _type == DTM.TEXT_NODE))
                            return returnNode(getNodeHandle(_currentNode--));
                        else
                            _currentNode--;
                    }
                    else
                        return returnNode(getNodeHandle(_currentNode--));
                }

                return END;
            }
        }

        public DTMAxisIterator setStartNode(int nodeHandle)
        {
            int nodeID = getNodeIdent(nodeHandle);
            _startNode = nodeID;

            // Increase the node ID by 1 if self is not included.
            if (!_includeSelf && nodeID != DTM.NULL) {
                if (_direction == DIRECTION_DOWN)
                    nodeID++;
                else if (_direction == DIRECTION_UP)
                    nodeID--;
            }

            _currentNode = nodeID;
            return this;
        }

        public void setMark()
        {
            _markedNode = _currentNode;
        }

        public void gotoMark()
        {
            _currentNode = _markedNode;
        }

    } // END of SimpleIterator

    /**
     * The SingletonIterator is used for the self axis.
     */
    public final class SingletonIterator extends DTMAxisIteratorBase
    {
        static final int NO_TYPE = -1;
        int _type = NO_TYPE;
        int _currentNode;

        public SingletonIterator()
        {
        }

        public SingletonIterator(int type)
        {
            _type = type;
        }

        public void setMark()
        {
            _markedNode = _currentNode;
        }

        public void gotoMark()
        {
            _currentNode = _markedNode;
        }

        public DTMAxisIterator setStartNode(int nodeHandle)
        {
            _currentNode = _startNode = getNodeIdent(nodeHandle);
            return this;
        }

        public int next()
        {
            if (_currentNode == END)
                return END;

            _currentNode = END;

            if (_type != NO_TYPE) {
                if ((_currentNode == RTF_ROOT && _type == DTM.ROOT_NODE)
                    || (_currentNode == RTF_TEXT && _type == DTM.TEXT_NODE))
                    return getNodeHandle(_currentNode);
            }
            else
                return getNodeHandle(_currentNode);

            return END;
        }

    }  // END of SingletonIterator

    // empty iterator to be returned when there are no children
    private final static DTMAxisIterator EMPTY_ITERATOR =
        new DTMAxisIteratorBase() {
            public DTMAxisIterator reset() { return this; }
            public DTMAxisIterator setStartNode(int node) { return this; }
            public int next() { return DTM.NULL; }
            public void setMark() {}
            public void gotoMark() {}
            public int getLast() { return 0; }
            public int getPosition() { return 0; }
            public DTMAxisIterator cloneIterator() { return this; }
            public void setRestartable(boolean isRestartable) { }
        };


    // The root node id of the simple RTF
    public static final int RTF_ROOT = 0;

    // The Text node id of the simple RTF (simple RTF has only one Text node).
    public static final int RTF_TEXT = 1;

    // The number of nodes.
    public static final int NUMBER_OF_NODES = 2;

    // Document URI index, which increases by 1 at each getDocumentURI() call.
    private static int _documentURIIndex = 0;

    // Constant for empty String
    private static final String EMPTY_STR = "";

    // The String value of the Text node.
    // This is set at the endDocument() call.
    private String _text;

    // The array of Text items, which is built by the characters() call.
    // The characters() interface can be called multiple times. Each character item
    // can have different escape settings.
    protected String[] _textArray;

    // The DTMManager
    protected XSLTCDTMManager _dtmManager;

    // Number of character items
    protected int _size = 0;

    // The document ID
    private int _documentID;

    // A BitArray, each bit holding the escape setting for a character item.
    private BitArray _dontEscape = null;

    // The current escape setting
    private boolean _escaping = true;

    // Create a SimpleResultTreeImpl from a DTMManager and a document ID.
    public SimpleResultTreeImpl(XSLTCDTMManager dtmManager, int documentID)
    {
        _dtmManager = dtmManager;
        _documentID = documentID;
        _textArray = new String[4];
    }

    public DTMManagerDefault getDTMManager()
    {
        return _dtmManager;
    }

    // Return the document ID
    public int getDocument()
    {
        return _documentID;
    }

    // Return the String value of the RTF
    public String getStringValue()
    {
        return _text;
    }

    public DTMAxisIterator getIterator()
    {
        return new SingletonIterator(getDocument());
    }

    public DTMAxisIterator getChildren(final int node)
    {
        return new SimpleIterator().setStartNode(node);
    }

    public DTMAxisIterator getTypedChildren(final int type)
    {
        return new SimpleIterator(SimpleIterator.DIRECTION_DOWN, type);
    }

    // Return the axis iterator for a given axis.
    // The SimpleIterator is used for the child, descendant, parent and ancestor axes.
    public DTMAxisIterator getAxisIterator(final int axis)
    {
        switch (axis)
        {
            case Axis.CHILD:
            case Axis.DESCENDANT:
                return new SimpleIterator(SimpleIterator.DIRECTION_DOWN);
            case Axis.PARENT:
            case Axis.ANCESTOR:
                return new SimpleIterator(SimpleIterator.DIRECTION_UP);
            case Axis.ANCESTORORSELF:
                return (new SimpleIterator(SimpleIterator.DIRECTION_UP)).includeSelf();
            case Axis.DESCENDANTORSELF:
                return (new SimpleIterator(SimpleIterator.DIRECTION_DOWN)).includeSelf();
            case Axis.SELF:
                return new SingletonIterator();
            default:
                return EMPTY_ITERATOR;
        }
    }

    public DTMAxisIterator getTypedAxisIterator(final int axis, final int type)
    {
        switch (axis)
        {
            case Axis.CHILD:
            case Axis.DESCENDANT:
                return new SimpleIterator(SimpleIterator.DIRECTION_DOWN, type);
            case Axis.PARENT:
            case Axis.ANCESTOR:
                return new SimpleIterator(SimpleIterator.DIRECTION_UP, type);
            case Axis.ANCESTORORSELF:
                return (new SimpleIterator(SimpleIterator.DIRECTION_UP, type)).includeSelf();
            case Axis.DESCENDANTORSELF:
                return (new SimpleIterator(SimpleIterator.DIRECTION_DOWN, type)).includeSelf();
            case Axis.SELF:
                return new SingletonIterator(type);
            default:
                return EMPTY_ITERATOR;
        }
    }

    // %REVISIT% Can this one ever get used?
    public DTMAxisIterator getNthDescendant(int node, int n, boolean includeself)
    {
        return null;
    }

    public DTMAxisIterator getNamespaceAxisIterator(final int axis, final int ns)
    {
        return null;
    }

    // %REVISIT% Can this one ever get used?
    public DTMAxisIterator getNodeValueIterator(DTMAxisIterator iter, int returnType,
                                             String value, boolean op)
    {
        return null;
    }

    public DTMAxisIterator orderNodes(DTMAxisIterator source, int node)
    {
        return source;
    }

    public String getNodeName(final int node)
    {
        if (getNodeIdent(node) == RTF_TEXT)
            return "#text";
        else
            return EMPTY_STR;
    }

    public String getNodeNameX(final int node)
    {
        return EMPTY_STR;
    }

    public String getNamespaceName(final int node)
    {
        return EMPTY_STR;
    }

    // Return the expanded type id of a given node
    public int getExpandedTypeID(final int nodeHandle)
    {
        int nodeID = getNodeIdent(nodeHandle);
        if (nodeID == RTF_TEXT)
            return DTM.TEXT_NODE;
        else if (nodeID == RTF_ROOT)
            return DTM.ROOT_NODE;
        else
            return DTM.NULL;
    }

    public int getNamespaceType(final int node)
    {
        return 0;
    }

    public int getParent(final int nodeHandle)
    {
        int nodeID = getNodeIdent(nodeHandle);
        return (nodeID == RTF_TEXT) ? getNodeHandle(RTF_ROOT) : DTM.NULL;
    }

    public int getAttributeNode(final int gType, final int element)
    {
        return DTM.NULL;
    }

    public String getStringValueX(final int nodeHandle)
    {
        int nodeID = getNodeIdent(nodeHandle);
        if (nodeID == RTF_ROOT || nodeID == RTF_TEXT)
            return _text;
        else
            return EMPTY_STR;
    }

    public void copy(final int node, SerializationHandler handler)
        throws TransletException
    {
        characters(node, handler);
    }

    public void copy(DTMAxisIterator nodes, SerializationHandler handler)
        throws TransletException
    {
        int node;
        while ((node = nodes.next()) != DTM.NULL)
        {
            copy(node, handler);
        }
    }

    public String shallowCopy(final int node, SerializationHandler handler)
        throws TransletException
    {
        characters(node, handler);
        return null;
    }

    public boolean lessThan(final int node1, final int node2)
    {
        if (node1 == DTM.NULL) {
            return false;
        }
        else if (node2 == DTM.NULL) {
            return true;
        }
        else
            return (node1 < node2);
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
        int nodeID = getNodeIdent(node);
        if (nodeID == RTF_ROOT || nodeID == RTF_TEXT) {
            boolean escapeBit = false;
            boolean oldEscapeSetting = false;

            try {
                for (int i = 0; i < _size; i++) {

                    if (_dontEscape != null) {
                        escapeBit = _dontEscape.getBit(i);
                        if (escapeBit) {
                            oldEscapeSetting = handler.setEscaping(false);
                        }
                    }

                    handler.characters(_textArray[i]);

                    if (escapeBit) {
                        handler.setEscaping(oldEscapeSetting);
                    }
                }
            } catch (SAXException e) {
                throw new TransletException(e);
            }
        }
    }

    // %REVISIT% Can the makeNode() and makeNodeList() interfaces ever get used?
    public Node makeNode(int index)
    {
        return null;
    }

    public Node makeNode(DTMAxisIterator iter)
    {
        return null;
    }

    public NodeList makeNodeList(int index)
    {
        return null;
    }

    public NodeList makeNodeList(DTMAxisIterator iter)
    {
        return null;
    }

    public String getLanguage(int node)
    {
        return null;
    }

    public int getSize()
    {
        return 2;
    }

    public String getDocumentURI(int node)
    {
        return "simple_rtf" + _documentURIIndex++;
    }

    public void setFilter(StripFilter filter)
    {
    }

    public void setupMapping(String[] names, String[] uris, int[] types, String[] namespaces)
    {
    }

    public boolean isElement(final int node)
    {
        return false;
    }

    public boolean isAttribute(final int node)
    {
        return false;
    }

    public String lookupNamespace(int node, String prefix)
        throws TransletException
    {
        return null;
    }

    /**
     * Return the node identity from a node handle.
     */
    public int getNodeIdent(final int nodehandle)
    {
        return (nodehandle != DTM.NULL) ? (nodehandle - _documentID) : DTM.NULL;
    }

    /**
     * Return the node handle from a node identity.
     */
    public int getNodeHandle(final int nodeId)
    {
        return (nodeId != DTM.NULL) ? (nodeId + _documentID) : DTM.NULL;
    }

    public DOM getResultTreeFrag(int initialSize, int rtfType)
    {
        return null;
    }

    public DOM getResultTreeFrag(int initialSize, int rtfType, boolean addToManager)
    {
        return null;
    }

    public SerializationHandler getOutputDomBuilder()
    {
        return this;
    }

    public int getNSType(int node)
    {
        return 0;
    }

    public String getUnparsedEntityURI(String name)
    {
        return null;
    }

    public Map<String, Integer> getElementsWithIDs()
    {
        return null;
    }

    /** Implementation of the SerializationHandler interfaces **/

    /**
     * We only need to override the endDocument, characters, and
     * setEscaping interfaces. A simple RTF does not have element
     * nodes. We do not need to touch startElement and endElement.
     */

    public void startDocument() throws SAXException
    {

    }

    public void endDocument() throws SAXException
    {
        // Set the String value when the document is built.
        if (_size == 1)
            _text = _textArray[0];
        else {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < _size; i++) {
                buffer.append(_textArray[i]);
            }
            _text = buffer.toString();
        }
    }

    public void characters(String str) throws SAXException
    {
        // Resize the text array if necessary
        if (_size >= _textArray.length) {
            String[] newTextArray = new String[_textArray.length * 2];
            System.arraycopy(_textArray, 0, newTextArray, 0, _textArray.length);
            _textArray = newTextArray;
        }

        // If the escape setting is false, set the corresponding bit in
        // the _dontEscape BitArray.
        if (!_escaping) {
            // The _dontEscape array is only created when needed.
            if (_dontEscape == null) {
                _dontEscape = new BitArray(8);
            }

            // Resize the _dontEscape array if necessary
            if (_size >= _dontEscape.size())
                _dontEscape.resize(_dontEscape.size() * 2);

            _dontEscape.setBit(_size);
        }

        _textArray[_size++] = str;
    }

    public void characters(char[] ch, int offset, int length)
        throws SAXException
    {
        if (_size >= _textArray.length) {
            String[] newTextArray = new String[_textArray.length * 2];
            System.arraycopy(_textArray, 0, newTextArray, 0, _textArray.length);
            _textArray = newTextArray;
        }

        if (!_escaping) {
            if (_dontEscape == null) {
                _dontEscape = new BitArray(8);
            }

            if (_size >= _dontEscape.size())
                _dontEscape.resize(_dontEscape.size() * 2);

            _dontEscape.setBit(_size);
        }

        _textArray[_size++] = new String(ch, offset, length);

    }

    public boolean setEscaping(boolean escape) throws SAXException
    {
        final boolean temp = _escaping;
        _escaping = escape;
        return temp;
    }

    /** Implementation of the DTM interfaces **/

    /**
     * The DTM interfaces are not used in this class. Implementing the DTM
     * interface is a requirement from MultiDOM. If we have a better way
     * of handling multiple documents, we can get rid of the DTM dependency.
     *
     * The following interfaces are just placeholders. The implementation
     * does not have an impact because they will not be used.
     */

    public void setFeature(String featureId, boolean state)
    {
    }

    public void setProperty(String property, Object value)
    {
    }

    public DTMAxisTraverser getAxisTraverser(final int axis)
    {
        return null;
    }

    public boolean hasChildNodes(int nodeHandle)
    {
        return (getNodeIdent(nodeHandle) == RTF_ROOT);
    }

    public int getFirstChild(int nodeHandle)
    {
        int nodeID = getNodeIdent(nodeHandle);
        if (nodeID == RTF_ROOT)
            return getNodeHandle(RTF_TEXT);
        else
            return DTM.NULL;
    }

    public int getLastChild(int nodeHandle)
    {
        return getFirstChild(nodeHandle);
    }

    public int getAttributeNode(int elementHandle, String namespaceURI, String name)
    {
        return DTM.NULL;
    }

    public int getFirstAttribute(int nodeHandle)
    {
        return DTM.NULL;
    }

    public int getFirstNamespaceNode(int nodeHandle, boolean inScope)
    {
        return DTM.NULL;
    }

    public int getNextSibling(int nodeHandle)
    {
        return DTM.NULL;
    }

    public int getPreviousSibling(int nodeHandle)
    {
        return DTM.NULL;
    }

    public int getNextAttribute(int nodeHandle)
    {
        return DTM.NULL;
    }

    public int getNextNamespaceNode(int baseHandle, int namespaceHandle,
                                  boolean inScope)
    {
        return DTM.NULL;
    }

    public int getOwnerDocument(int nodeHandle)
    {
        return getDocument();
    }

    public int getDocumentRoot(int nodeHandle)
    {
        return getDocument();
    }

    public XMLString getStringValue(int nodeHandle)
    {
        return new XMLStringDefault(getStringValueX(nodeHandle));
    }

    public int getStringValueChunkCount(int nodeHandle)
    {
        return 0;
    }

    public char[] getStringValueChunk(int nodeHandle, int chunkIndex,
                                    int[] startAndLen)
    {
        return null;
    }

    public int getExpandedTypeID(String namespace, String localName, int type)
    {
        return DTM.NULL;
    }

    public String getLocalNameFromExpandedNameID(int ExpandedNameID)
    {
        return EMPTY_STR;
    }

    public String getNamespaceFromExpandedNameID(int ExpandedNameID)
    {
        return EMPTY_STR;
    }

    public String getLocalName(int nodeHandle)
    {
        return EMPTY_STR;
    }

    public String getPrefix(int nodeHandle)
    {
        return null;
    }

    public String getNamespaceURI(int nodeHandle)
    {
        return EMPTY_STR;
    }

    public String getNodeValue(int nodeHandle)
    {
        return (getNodeIdent(nodeHandle) == RTF_TEXT) ? _text : null;
    }

    public short getNodeType(int nodeHandle)
    {
        int nodeID = getNodeIdent(nodeHandle);
        if (nodeID == RTF_TEXT)
            return DTM.TEXT_NODE;
        else if (nodeID == RTF_ROOT)
            return DTM.ROOT_NODE;
        else
            return DTM.NULL;

    }

    public short getLevel(int nodeHandle)
    {
        int nodeID = getNodeIdent(nodeHandle);
        if (nodeID == RTF_TEXT)
            return 2;
        else if (nodeID == RTF_ROOT)
            return 1;
        else
            return DTM.NULL;
    }

    public boolean isSupported(String feature, String version)
    {
        return false;
    }

    public String getDocumentBaseURI()
    {
        return EMPTY_STR;
    }

    public void setDocumentBaseURI(String baseURI)
    {
    }

    public String getDocumentSystemIdentifier(int nodeHandle)
    {
        return null;
    }

    public String getDocumentEncoding(int nodeHandle)
    {
        return null;
    }

    public String getDocumentStandalone(int nodeHandle)
    {
        return null;
    }

    public String getDocumentVersion(int documentHandle)
    {
        return null;
    }

    public boolean getDocumentAllDeclarationsProcessed()
    {
        return false;
    }

    public String getDocumentTypeDeclarationSystemIdentifier()
    {
        return null;
    }

    public String getDocumentTypeDeclarationPublicIdentifier()
    {
        return null;
    }

    public int getElementById(String elementId)
    {
        return DTM.NULL;
    }

    public boolean supportsPreStripping()
    {
        return false;
    }

    public boolean isNodeAfter(int firstNodeHandle, int secondNodeHandle)
    {
        return lessThan(firstNodeHandle, secondNodeHandle);
    }

    public boolean isCharacterElementContentWhitespace(int nodeHandle)
    {
        return false;
    }

    public boolean isDocumentAllDeclarationsProcessed(int documentHandle)
    {
        return false;
    }

    public boolean isAttributeSpecified(int attributeHandle)
    {
        return false;
    }

    public void dispatchCharactersEvents(
        int nodeHandle,
        org.xml.sax.ContentHandler ch,
        boolean normalize)
          throws org.xml.sax.SAXException
    {
    }

    public void dispatchToEvents(int nodeHandle, org.xml.sax.ContentHandler ch)
      throws org.xml.sax.SAXException
    {
    }

    public org.w3c.dom.Node getNode(int nodeHandle)
    {
        return makeNode(nodeHandle);
    }

    public boolean needsTwoThreads()
    {
        return false;
    }

    public org.xml.sax.ContentHandler getContentHandler()
    {
        return null;
    }

    public org.xml.sax.ext.LexicalHandler getLexicalHandler()
    {
        return null;
    }

    public org.xml.sax.EntityResolver getEntityResolver()
    {
        return null;
    }

    public org.xml.sax.DTDHandler getDTDHandler()
    {
        return null;
    }

    public org.xml.sax.ErrorHandler getErrorHandler()
    {
        return null;
    }

    public org.xml.sax.ext.DeclHandler getDeclHandler()
    {
        return null;
    }

    public void appendChild(int newChild, boolean clone, boolean cloneDepth)
    {
    }

    public void appendTextChild(String str)
    {
    }

    public SourceLocator getSourceLocatorFor(int node)
    {
        return null;
    }

    public void documentRegistration()
    {
    }

    public void documentRelease()
    {
    }

    public void migrateTo(DTMManager manager)
    {
    }

    public void release()
    {
        if (_documentID != 0) {
            _dtmManager.release(this, true);
            _documentID = 0;
        }
    }
}
