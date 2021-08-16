/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.org.apache.xalan.internal.xsltc.trax;

import com.sun.org.apache.xalan.internal.xsltc.runtime.Constants;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.org.apache.xalan.internal.xsltc.runtime.Constants;
import jdk.xml.internal.JdkXmlUtils;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2;

/**
 * @author G. Todd Miller
 * @author Sunitha Reddy
 * @author Huizhe Wang
 * @LastModified: Nov 2017
 */
public class SAX2DOM implements ContentHandler, LexicalHandler, Constants {

    private Node _root = null;
    private Document _document = null;
    private Node _nextSibling = null;
    private Stack<Node> _nodeStk = new Stack<>();
    private List<String> _namespaceDecls = null;
    private Node _lastSibling = null;
    private Locator locator = null;
    private boolean needToSetDocumentInfo = true;

    //Replace StringBuffer with StringBuilder now that we no long support jdk1.4
    private StringBuilder _textBuffer = new StringBuilder();
    private Node _nextSiblingCache = null;
    /**
     * JAXP document builder factory. Create a single instance and use
     * synchronization because the Javadoc is not explicit about
     * thread safety.
     */
    private DocumentBuilderFactory _factory;
    private boolean _internal = true;

    public SAX2DOM(boolean overrideDefaultParser) throws ParserConfigurationException {
        _document = createDocument(overrideDefaultParser);
        _root = _document;
    }

    public SAX2DOM(Node root, Node nextSibling, boolean overrideDefaultParser)
            throws ParserConfigurationException {
        _root = root;
        if (root instanceof Document) {
          _document = (Document)root;
        }
        else if (root != null) {
          _document = root.getOwnerDocument();
        }
        else {
          _document = createDocument(overrideDefaultParser);
          _root = _document;
        }

        _nextSibling = nextSibling;
    }

    public SAX2DOM(Node root, boolean overrideDefaultParser)
            throws ParserConfigurationException {
        this(root, null, overrideDefaultParser);
    }

    public Node getDOM() {
        return _root;
    }

    public void characters(char[] ch, int start, int length) {
        // Ignore text nodes of length 0
        if (length == 0) {
            return;
        }

        final Node last = _nodeStk.peek();

        // No text nodes can be children of root (DOM006 exception)
        if (last != _document) {
            _nextSiblingCache = _nextSibling;
            _textBuffer.append(ch, start, length);
        }
    }
    private void appendTextNode() {
        if (_textBuffer.length() > 0) {
            final Node last = _nodeStk.peek();
            if (last == _root && _nextSiblingCache != null) {
                _lastSibling = last.insertBefore(_document.createTextNode(_textBuffer.toString()), _nextSiblingCache);
            }
            else {
                _lastSibling = last.appendChild(_document.createTextNode(_textBuffer.toString()));
            }
            _textBuffer.setLength(0);
        }
    }
    public void startDocument() {
        _nodeStk.push(_root);
    }

    public void endDocument() {
        _nodeStk.pop();
    }

    private void setDocumentInfo() {
        //try to set document version
        if (locator == null) return;
        try{
            _document.setXmlVersion(((Locator2)locator).getXMLVersion());
        }catch(ClassCastException e){}

    }

    public void startElement(String namespace, String localName, String qName,
        Attributes attrs)
    {
        appendTextNode();
        if (needToSetDocumentInfo) {
            setDocumentInfo();
            needToSetDocumentInfo = false;
        }

        final Element tmp = _document.createElementNS(namespace, qName);

        // Add namespace declarations first
        if (_namespaceDecls != null) {
            final int nDecls = _namespaceDecls.size();
            for (int i = 0; i < nDecls; i++) {
                final String prefix = _namespaceDecls.get(i++);

                if (prefix == null || prefix.equals(EMPTYSTRING)) {
                    tmp.setAttributeNS(XMLNS_URI, XMLNS_PREFIX,
                        _namespaceDecls.get(i));
                }
                else {
                    tmp.setAttributeNS(XMLNS_URI, XMLNS_STRING + prefix,
                        _namespaceDecls.get(i));
                }
            }
            _namespaceDecls.clear();
        }

        // Add attributes to element
/*      final int nattrs = attrs.getLength();
        for (int i = 0; i < nattrs; i++) {
            if (attrs.getLocalName(i) == null) {
                tmp.setAttribute(attrs.getQName(i), attrs.getValue(i));
            }
            else {
                tmp.setAttributeNS(attrs.getURI(i), attrs.getQName(i),
                    attrs.getValue(i));
            }
        } */


        // Add attributes to element
        final int nattrs = attrs.getLength();
        for (int i = 0; i < nattrs; i++) {
            // checking if Namespace processing is being done
            String attQName = attrs.getQName(i);
            String attURI = attrs.getURI(i);
            String type = (attrs.getType(i) == null) ?
                    XMLSymbols.fCDATASymbol : attrs.getType(i);
            if (attrs.getLocalName(i).equals("")) {
                tmp.setAttribute(attQName, attrs.getValue(i));
                if (type.equals("ID")) {
                    tmp.setIdAttribute(attQName, true);
                }
            } else {
                tmp.setAttributeNS(attURI, attQName, attrs.getValue(i));
                if (type.equals("ID")) {
                    tmp.setIdAttributeNS(attURI, attrs.getLocalName(i), true);
                }
            }
        }


        // Append this new node onto current stack node
        Node last = _nodeStk.peek();

        // If the SAX2DOM is created with a non-null next sibling node,
        // insert the result nodes before the next sibling under the root.
        if (last == _root && _nextSibling != null)
            last.insertBefore(tmp, _nextSibling);
        else
            last.appendChild(tmp);

        // Push this node onto stack
        _nodeStk.push(tmp);
        _lastSibling = null;
    }

    public void endElement(String namespace, String localName, String qName) {
        appendTextNode();
        _nodeStk.pop();
        _lastSibling = null;
    }

    public void startPrefixMapping(String prefix, String uri) {
        if (_namespaceDecls == null) {
            _namespaceDecls = new ArrayList<>(2);
        }
        _namespaceDecls.add(prefix);
        _namespaceDecls.add(uri);
    }

    public void endPrefixMapping(String prefix) {
        // do nothing
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void ignorableWhitespace(char[] ch, int start, int length) {
    }

    /**
     * adds processing instruction node to DOM.
     */
    public void processingInstruction(String target, String data) {
        appendTextNode();
        final Node last = _nodeStk.peek();
        ProcessingInstruction pi = _document.createProcessingInstruction(
                target, data);
        if (pi != null){
          if (last == _root && _nextSibling != null)
              last.insertBefore(pi, _nextSibling);
          else
              last.appendChild(pi);

          _lastSibling = pi;
        }
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void skippedEntity(String name) {
    }


    /**
     * Lexical Handler method to create comment node in DOM tree.
     */
    public void comment(char[] ch, int start, int length) {
        appendTextNode();
        final Node last = _nodeStk.peek();
        Comment comment = _document.createComment(new String(ch,start,length));
        if (comment != null){
          if (last == _root && _nextSibling != null)
              last.insertBefore(comment, _nextSibling);
          else
              last.appendChild(comment);

          _lastSibling = comment;
        }
    }

    // Lexical Handler methods- not implemented
    public void startCDATA() { }
    public void endCDATA() { }
    public void startEntity(java.lang.String name) { }
    public void endDTD() { }
    public void endEntity(String name) { }
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException {}

    private Document createDocument(boolean overrideDefaultParser)
            throws ParserConfigurationException {
        if (_factory == null) {
            _factory = JdkXmlUtils.getDOMFactory(overrideDefaultParser);
            _internal = true;
            if (!(_factory instanceof com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl)) {
                _internal = false;
            }
        }
        Document doc;
        if (_internal) {
            //default implementation is thread safe
            doc = _factory.newDocumentBuilder().newDocument();
        } else {
            synchronized(SAX2DOM.class) {
                doc = _factory.newDocumentBuilder().newDocument();
            }
        }
        return doc;
    }

}
