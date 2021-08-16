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

package com.sun.org.apache.xalan.internal.xsltc;

import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import java.util.Map;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public interface DOM {
    public final static int  FIRST_TYPE             = 0;

    public final static int  NO_TYPE                = -1;

    // 0 is reserved for NodeIterator.END
    public final static int NULL     = 0;

    // used by some node iterators to know which node to return
    public final static int RETURN_CURRENT = 0;
    public final static int RETURN_PARENT  = 1;

    // Constants used by getResultTreeFrag to indicate the types of the RTFs.
    public final static int SIMPLE_RTF   = 0;
    public final static int ADAPTIVE_RTF = 1;
    public final static int TREE_RTF     = 2;

    /** returns singleton iterator containg the document root */
    public DTMAxisIterator getIterator();
    public String getStringValue();

    public DTMAxisIterator getChildren(final int node);
    public DTMAxisIterator getTypedChildren(final int type);
    public DTMAxisIterator getAxisIterator(final int axis);
    public DTMAxisIterator getTypedAxisIterator(final int axis, final int type);
    public DTMAxisIterator getNthDescendant(int node, int n, boolean includeself);
    public DTMAxisIterator getNamespaceAxisIterator(final int axis, final int ns);
    public DTMAxisIterator getNodeValueIterator(DTMAxisIterator iter, int returnType,
                                             String value, boolean op);
    public DTMAxisIterator orderNodes(DTMAxisIterator source, int node);
    public String getNodeName(final int node);
    public String getNodeNameX(final int node);
    public String getNamespaceName(final int node);
    public int getExpandedTypeID(final int node);
    public int getNamespaceType(final int node);
    public int getParent(final int node);
    public int getAttributeNode(final int gType, final int element);
    public String getStringValueX(final int node);
    public void copy(final int node, SerializationHandler handler)
        throws TransletException;
    public void copy(DTMAxisIterator nodes, SerializationHandler handler)
        throws TransletException;
    public String shallowCopy(final int node, SerializationHandler handler)
        throws TransletException;
    public boolean lessThan(final int node1, final int node2);
    public void characters(final int textNode, SerializationHandler handler)
        throws TransletException;
    public Node makeNode(int index);
    public Node makeNode(DTMAxisIterator iter);
    public NodeList makeNodeList(int index);
    public NodeList makeNodeList(DTMAxisIterator iter);
    public String getLanguage(int node);
    public int getSize();
    public String getDocumentURI(int node);
    public void setFilter(StripFilter filter);
    public void setupMapping(String[] names, String[] urisArray, int[] typesArray, String[] namespaces);
    public boolean isElement(final int node);
    public boolean isAttribute(final int node);
    public String lookupNamespace(int node, String prefix)
        throws TransletException;
    public int getNodeIdent(final int nodehandle);
    public int getNodeHandle(final int nodeId);
    public DOM getResultTreeFrag(int initialSize, int rtfType);
    public DOM getResultTreeFrag(int initialSize, int rtfType, boolean addToDTMManager);
    public SerializationHandler getOutputDomBuilder();
    public int getNSType(int node);
    public int getDocument();
    public String getUnparsedEntityURI(String name);
    public Map<String, Integer> getElementsWithIDs();
    public void release();
}
