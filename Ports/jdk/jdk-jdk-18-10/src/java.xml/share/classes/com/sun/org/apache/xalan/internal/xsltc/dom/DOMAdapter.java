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
import com.sun.org.apache.xalan.internal.xsltc.DOMEnhancedForDTM;
import com.sun.org.apache.xalan.internal.xsltc.StripFilter;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import java.util.Map;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jacek Ambroziak
 * @author Morten Jorgensen
 */
public final class DOMAdapter implements DOM {

    // Mutually exclusive casting of DOM interface to known implementations
    private DOMEnhancedForDTM _enhancedDOM;

    private DOM _dom;

    private String[] _namesArray;
    private String[] _urisArray;
    private int[]    _typesArray;
    private String[] _namespaceArray;

    // Cached mappings
    private short[] _mapping = null;
    private int[]   _reverse = null;
    private short[] _NSmapping = null;
    private short[] _NSreverse = null;

    private int _multiDOMMask;

    public DOMAdapter(DOM dom,
                      String[] namesArray,
                      String[] urisArray,
                      int[] typesArray,
                      String[] namespaceArray) {
        if (dom instanceof DOMEnhancedForDTM){
            _enhancedDOM = (DOMEnhancedForDTM) dom;
        }

        _dom = dom;
        _namesArray = namesArray;
        _urisArray = urisArray;
        _typesArray = typesArray;
        _namespaceArray = namespaceArray;
    }

    public void setupMapping(String[] names, String[] urisArray,
                             int[] typesArray, String[] namespaces) {
        _namesArray = names;
        _urisArray = urisArray;
        _typesArray = typesArray;
        _namespaceArray = namespaces;
    }

    public String[] getNamesArray() {
        return _namesArray;
    }

    public String[] getUrisArray() {
        return _urisArray;
    }

    public int[] getTypesArray() {
        return _typesArray;
    }

    public String[] getNamespaceArray() {
        return _namespaceArray;
    }

    public DOM getDOMImpl() {
        return _dom;
    }

    private short[] getMapping() {
        if (_mapping == null) {
            if (_enhancedDOM != null) {
                _mapping = _enhancedDOM.getMapping(_namesArray, _urisArray,
                                                   _typesArray);
            }
        }
        return _mapping;
    }

    private int[] getReverse() {
        if (_reverse == null) {
            if (_enhancedDOM != null) {
                _reverse = _enhancedDOM.getReverseMapping(_namesArray,
                                                          _urisArray,
                                                          _typesArray);
            }
        }
        return _reverse;
    }

    private short[] getNSMapping() {
        if (_NSmapping == null) {
            if (_enhancedDOM != null) {
                _NSmapping = _enhancedDOM.getNamespaceMapping(_namespaceArray);
            }
        }
        return _NSmapping;
    }

    private short[] getNSReverse() {
        if (_NSreverse == null) {
            if (_enhancedDOM != null) {
                _NSreverse = _enhancedDOM
                                  .getReverseNamespaceMapping(_namespaceArray);
            }
        }
        return _NSreverse;
    }

    /**
      * Returns singleton iterator containg the document root
      */
    public DTMAxisIterator getIterator() {
        return _dom.getIterator();
    }

    public String getStringValue() {
        return _dom.getStringValue();
    }

    public DTMAxisIterator getChildren(final int node) {
        if (_enhancedDOM != null) {
            return _enhancedDOM.getChildren(node);
        }
        else {
            DTMAxisIterator iterator = _dom.getChildren(node);
            return iterator.setStartNode(node);
        }
    }

    public void setFilter(StripFilter filter) {}

    public DTMAxisIterator getTypedChildren(final int type) {
        final int[] reverse = getReverse();

        if (_enhancedDOM != null) {
            return _enhancedDOM.getTypedChildren(reverse[type]);
        }
        else {
            return _dom.getTypedChildren(type);
        }
    }

    public DTMAxisIterator getNamespaceAxisIterator(final int axis,
                                                    final int ns) {
        return _dom.getNamespaceAxisIterator(axis, getNSReverse()[ns]);
    }

    public DTMAxisIterator getAxisIterator(final int axis) {
        if (_enhancedDOM != null) {
            return _enhancedDOM.getAxisIterator(axis);
        }
        else {
            return _dom.getAxisIterator(axis);
        }
    }

    public DTMAxisIterator getTypedAxisIterator(final int axis,
                                                final int type) {
        final int[] reverse = getReverse();
        if (_enhancedDOM != null) {
            return _enhancedDOM.getTypedAxisIterator(axis, reverse[type]);
        } else {
            return _dom.getTypedAxisIterator(axis, type);
        }
    }

    public int getMultiDOMMask() {
        return _multiDOMMask;
    }

    public void setMultiDOMMask(int mask) {
        _multiDOMMask = mask;
    }

    public DTMAxisIterator getNthDescendant(int type, int n,
                                            boolean includeself) {
        return _dom.getNthDescendant(getReverse()[type], n, includeself);
    }

    public DTMAxisIterator getNodeValueIterator(DTMAxisIterator iterator,
                                                int type, String value,
                                                boolean op) {
        return _dom.getNodeValueIterator(iterator, type, value, op);
    }

    public DTMAxisIterator orderNodes(DTMAxisIterator source, int node) {
        return _dom.orderNodes(source, node);
    }

    public int getExpandedTypeID(final int node) {
        final short[] mapping = getMapping();
        final int type;
        if (_enhancedDOM != null) {
            type = mapping[_enhancedDOM.getExpandedTypeID2(node)];
        }
        else {
                if(null != mapping)
                {
                type = mapping[_dom.getExpandedTypeID(node)];
                }
                else
                {
                        type = _dom.getExpandedTypeID(node);
                }
        }
        return type;
    }

    public int getNamespaceType(final int node) {
        return getNSMapping()[_dom.getNSType(node)];
    }

    public int getNSType(int node) {
        return _dom.getNSType(node);
    }

    public int getParent(final int node) {
        return _dom.getParent(node);
    }

    public int getAttributeNode(final int type, final int element) {
        return _dom.getAttributeNode(getReverse()[type], element);
    }

    public String getNodeName(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _dom.getNodeName(node);
    }

    public String getNodeNameX(final int node)
    {
        if (node == DTM.NULL) {
            return "";
        }
        return _dom.getNodeNameX(node);
    }

    public String getNamespaceName(final int node)
    {
        if (node == DTM.NULL) {
            return "";
        }
        return _dom.getNamespaceName(node);
    }

    public String getStringValueX(final int node)
    {
        if (_enhancedDOM != null) {
            return _enhancedDOM.getStringValueX(node);
        }
        else {
            if (node == DTM.NULL) {
                return "";
            }
            return _dom.getStringValueX(node);
        }
    }

    public void copy(final int node, SerializationHandler handler)
        throws TransletException
    {
        _dom.copy(node, handler);
    }

    public void copy(DTMAxisIterator nodes,SerializationHandler handler)
        throws TransletException
    {
        _dom.copy(nodes, handler);
    }

    public String shallowCopy(final int node, SerializationHandler handler)
        throws TransletException
    {
        if (_enhancedDOM != null) {
            return _enhancedDOM.shallowCopy(node, handler);
        }
        else {
            return _dom.shallowCopy(node, handler);
        }
    }

    public boolean lessThan(final int node1, final int node2)
    {
        return _dom.lessThan(node1, node2);
    }

    public void characters(final int textNode, SerializationHandler handler)
      throws TransletException
    {
        if (_enhancedDOM != null) {
            _enhancedDOM.characters(textNode, handler);
        }
        else {
            _dom.characters(textNode, handler);
        }
    }

    public Node makeNode(int index)
    {
        return _dom.makeNode(index);
    }

    public Node makeNode(DTMAxisIterator iter)
    {
        return _dom.makeNode(iter);
    }

    public NodeList makeNodeList(int index)
    {
        return _dom.makeNodeList(index);
    }

    public NodeList makeNodeList(DTMAxisIterator iter)
    {
        return _dom.makeNodeList(iter);
    }

    public String getLanguage(int node)
    {
        return _dom.getLanguage(node);
    }

    public int getSize()
    {
        return _dom.getSize();
    }

    public void setDocumentURI(String uri)
    {
        if (_enhancedDOM != null) {
            _enhancedDOM.setDocumentURI(uri);
        }
    }

    public String getDocumentURI()
    {
        if (_enhancedDOM != null) {
            return _enhancedDOM.getDocumentURI();
        }
        else {
            return "";
        }
    }

    public String getDocumentURI(int node)
    {
        return _dom.getDocumentURI(node);
    }

    public int getDocument()
    {
        return _dom.getDocument();
    }

    public boolean isElement(final int node)
    {
        return(_dom.isElement(node));
    }

    public boolean isAttribute(final int node)
    {
        return(_dom.isAttribute(node));
    }

    public int getNodeIdent(int nodeHandle)
    {
        return _dom.getNodeIdent(nodeHandle);
    }

    public int getNodeHandle(int nodeId)
    {
        return _dom.getNodeHandle(nodeId);
    }

    /**
     * Return a instance of a DOM class to be used as an RTF
     */
    public DOM getResultTreeFrag(int initSize, int rtfType)
    {
        if (_enhancedDOM != null) {
            return _enhancedDOM.getResultTreeFrag(initSize, rtfType);
        }
        else {
            return _dom.getResultTreeFrag(initSize, rtfType);
        }
    }

    /**
     * Return a instance of a DOM class to be used as an RTF
     */
    public DOM getResultTreeFrag(int initSize, int rtfType,
                                 boolean addToManager)
    {
        if (_enhancedDOM != null) {
            return _enhancedDOM.getResultTreeFrag(initSize, rtfType,
                                                  addToManager);
        }
        else {
            return _dom.getResultTreeFrag(initSize, rtfType, addToManager);
        }
    }


    /**
     * Returns a SerializationHandler class wrapped in a SAX adapter.
     */
    public SerializationHandler getOutputDomBuilder()
    {
        return _dom.getOutputDomBuilder();
    }

    public String lookupNamespace(int node, String prefix)
        throws TransletException
    {
        return _dom.lookupNamespace(node, prefix);
    }

    public String getUnparsedEntityURI(String entity) {
        return _dom.getUnparsedEntityURI(entity);
    }

    public Map<String, Integer> getElementsWithIDs() {
        return _dom.getElementsWithIDs();
    }

    public void release() {
        _dom.release();
    }
}
