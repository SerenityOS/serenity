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
import com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary;
import com.sun.org.apache.xml.internal.dtm.Axis;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.DTMManager;
import com.sun.org.apache.xml.internal.dtm.ref.DTMAxisIteratorBase;
import com.sun.org.apache.xml.internal.dtm.ref.DTMAxisIterNodeList;
import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBase;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.utils.SuballocatedIntVector;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jacek Ambroziak
 * @author Morten Jorgensen
 * @author Erwin Bolwidt <ejb@klomp.org>
 */
public final class MultiDOM implements DOM {

    private static final int NO_TYPE = DOM.FIRST_TYPE - 2;
    private static final int INITIAL_SIZE = 4;

    private DOM[] _adapters;
    private DOMAdapter _main;
    private DTMManager _dtmManager;
    private int _free;
    private int _size;

    private Map<String, Integer> _documents = new HashMap<>();

    private final class AxisIterator extends DTMAxisIteratorBase {
        // constitutive data
        private final int _axis;
        private final int _type;
        // implementation mechanism
        private DTMAxisIterator _source;
        private int _dtmId = -1;

        public AxisIterator(final int axis, final int type) {
            _axis = axis;
            _type = type;
        }

        public int next() {
            if (_source == null) {
                return(END);
            }
            return _source.next();
        }


        public void setRestartable(boolean flag) {
            if (_source != null) {
                _source.setRestartable(flag);
            }
        }

        public DTMAxisIterator setStartNode(final int node) {
            if (node == DTM.NULL) {
                return this;
            }

            int dom = node >>> DTMManager.IDENT_DTM_NODE_BITS;

            // Get a new source first time and when mask changes
            if (_source == null || _dtmId != dom) {
                if (_type == NO_TYPE) {
                    _source = _adapters[dom].getAxisIterator(_axis);
                } else if (_axis == Axis.CHILD) {
                    _source = _adapters[dom].getTypedChildren(_type);
                } else {
                    _source = _adapters[dom].getTypedAxisIterator(_axis, _type);
                }
            }

            _dtmId = dom;
            _source.setStartNode(node);
            return this;
        }

        public DTMAxisIterator reset() {
            if (_source != null) {
                _source.reset();
            }
            return this;
        }

        public int getLast() {
            if (_source != null) {
                return _source.getLast();
            }
            else {
                return END;
            }
        }

        public int getPosition() {
            if (_source != null) {
                return _source.getPosition();
            }
            else {
                return END;
            }
        }

        public boolean isReverse() {
            return Axis.isReverse(_axis);
        }

        public void setMark() {
            if (_source != null) {
                _source.setMark();
            }
        }

        public void gotoMark() {
            if (_source != null) {
                _source.gotoMark();
            }
        }

        public DTMAxisIterator cloneIterator() {
            final AxisIterator clone = new AxisIterator(_axis, _type);
            if (_source != null) {
                clone._source = _source.cloneIterator();
            }
            clone._dtmId = _dtmId;
            return clone;
        }
    } // end of AxisIterator


    /**************************************************************
     * This is a specialised iterator for predicates comparing node or
     * attribute values to variable or parameter values.
     */
    private final class NodeValueIterator extends DTMAxisIteratorBase {

        private DTMAxisIterator _source;
        private String _value;
        private boolean _op;
        private final boolean _isReverse;
        private int _returnType = RETURN_PARENT;

        public NodeValueIterator(DTMAxisIterator source, int returnType,
                                 String value, boolean op) {
            _source = source;
            _returnType = returnType;
            _value = value;
            _op = op;
            _isReverse = source.isReverse();
        }

        public boolean isReverse() {
            return _isReverse;
        }

        public DTMAxisIterator cloneIterator() {
            try {
                NodeValueIterator clone = (NodeValueIterator)super.clone();
                clone._source = _source.cloneIterator();
                clone.setRestartable(false);
                return clone.reset();
            }
            catch (CloneNotSupportedException e) {
                BasisLibrary.runTimeError(BasisLibrary.ITERATOR_CLONE_ERR,
                                          e.toString());
                return null;
            }
        }


        public void setRestartable(boolean isRestartable) {
            _isRestartable = isRestartable;
            _source.setRestartable(isRestartable);
        }

        public DTMAxisIterator reset() {
            _source.reset();
            return resetPosition();
        }

        public int next() {

            int node;
            while ((node = _source.next()) != END) {
                String val = getStringValueX(node);
                if (_value.equals(val) == _op) {
                    if (_returnType == RETURN_CURRENT)
                        return returnNode(node);
                    else
                        return returnNode(getParent(node));
                }
            }
            return END;
        }

        public DTMAxisIterator setStartNode(int node) {
            if (_isRestartable) {
                _source.setStartNode(_startNode = node);
                return resetPosition();
            }
            return this;
        }

        public void setMark() {
            _source.setMark();
        }

        public void gotoMark() {
            _source.gotoMark();
        }
    }

    public MultiDOM(DOM main) {
        _size = INITIAL_SIZE;
        _free = 1;
        _adapters = new DOM[INITIAL_SIZE];
        DOMAdapter adapter = (DOMAdapter)main;
        _adapters[0] = adapter;
        _main = adapter;
        DOM dom = adapter.getDOMImpl();
        if (dom instanceof DTMDefaultBase) {
            _dtmManager = ((DTMDefaultBase)dom).getManager();
        }

        // %HZ% %REVISIT% Is this the right thing to do here?  In the old
        // %HZ% %REVISIT% version, the main document did not get added through
        // %HZ% %REVISIT% a call to addDOMAdapter, which meant it couldn't be
        // %HZ% %REVISIT% found by a call to getDocumentMask.  The problem is
        // %HZ% %REVISIT% TransformerHandler is typically constructed with a
        // %HZ% %REVISIT% system ID equal to the stylesheet's URI; with SAX
        // %HZ% %REVISIT% input, it ends up giving that URI to the document.
        // %HZ% %REVISIT% Then, any references to document('') are resolved
        // %HZ% %REVISIT% using the stylesheet's URI.
        // %HZ% %REVISIT% MultiDOM.getDocumentMask is called to verify that
        // %HZ% %REVISIT% a document associated with that URI has not been
        // %HZ% %REVISIT% encountered, and that method ends up returning the
        // %HZ% %REVISIT% mask of the main document, when what we really what
        // %HZ% %REVISIT% is to read the stylesheet itself!
        addDOMAdapter(adapter, false);
    }

    public int nextMask() {
        return _free;
    }

    public void setupMapping(String[] names, String[] uris, int[] types, String[] namespaces) {
        // This method only has a function in DOM adapters
    }

    public int addDOMAdapter(DOMAdapter adapter) {
        return addDOMAdapter(adapter, true);
    }

    private int addDOMAdapter(DOMAdapter adapter, boolean indexByURI) {
        // Add the DOM adapter to the array of DOMs
        DOM dom = adapter.getDOMImpl();

        int domNo = 1;
        int dtmSize = 1;
        SuballocatedIntVector dtmIds = null;
        if (dom instanceof DTMDefaultBase) {
            DTMDefaultBase dtmdb = (DTMDefaultBase)dom;
            dtmIds = dtmdb.getDTMIDs();
            dtmSize = dtmIds.size();
            domNo = dtmIds.elementAt(dtmSize-1) >>> DTMManager.IDENT_DTM_NODE_BITS;
        }
        else if (dom instanceof SimpleResultTreeImpl) {
            SimpleResultTreeImpl simpleRTF = (SimpleResultTreeImpl)dom;
            domNo = simpleRTF.getDocument() >>> DTMManager.IDENT_DTM_NODE_BITS;
        }

        if (domNo >= _size) {
            int oldSize = _size;
            do {
                _size *= 2;
            } while (_size <= domNo);

            final DOMAdapter[] newArray = new DOMAdapter[_size];
            System.arraycopy(_adapters, 0, newArray, 0, oldSize);
            _adapters = newArray;
        }

        _free = domNo + 1;

        if (dtmSize == 1) {
            _adapters[domNo] = adapter;
        }
        else if (dtmIds != null) {
            int domPos = 0;
            for (int i = dtmSize - 1; i >= 0; i--) {
                domPos = dtmIds.elementAt(i) >>> DTMManager.IDENT_DTM_NODE_BITS;
                _adapters[domPos] = adapter;
            }
            domNo = domPos;
        }

        // Store reference to document (URI) in the Map
        if (indexByURI) {
            String uri = adapter.getDocumentURI(0);
            _documents.put(uri, domNo);
        }

        // If the dom is an AdaptiveResultTreeImpl, we need to create a
        // DOMAdapter around its nested dom object (if it is non-null) and
        // add the DOMAdapter to the list.
        if (dom instanceof AdaptiveResultTreeImpl) {
            AdaptiveResultTreeImpl adaptiveRTF = (AdaptiveResultTreeImpl)dom;
            DOM nestedDom = adaptiveRTF.getNestedDOM();
            if (nestedDom != null) {
                DOMAdapter newAdapter = new DOMAdapter(nestedDom,
                                                       adapter.getNamesArray(),
                                                       adapter.getUrisArray(),
                                                       adapter.getTypesArray(),
                                                       adapter.getNamespaceArray());
                addDOMAdapter(newAdapter);
            }
        }

        return domNo;
    }

    public int getDocumentMask(String uri) {
        Integer domIdx = _documents.get(uri);
        if (domIdx == null) {
            return(-1);
        } else {
            return domIdx.intValue();
        }
    }

    public DOM getDOMAdapter(String uri) {
        Integer domIdx = _documents.get(uri);
        if (domIdx == null) {
            return(null);
        } else {
            return(_adapters[domIdx.intValue()]);
        }
    }

    public int getDocument()
    {
        return _main.getDocument();
    }

    public DTMManager getDTMManager() {
        return _dtmManager;
    }

    /**
      * Returns singleton iterator containing the document root
      */
    public DTMAxisIterator getIterator() {
        // main source document @ 0
        return _main.getIterator();
    }

    public String getStringValue() {
        return _main.getStringValue();
    }

    public DTMAxisIterator getChildren(final int node) {
        return _adapters[getDTMId(node)].getChildren(node);
    }

    public DTMAxisIterator getTypedChildren(final int type) {
        return new AxisIterator(Axis.CHILD, type);
    }

    public DTMAxisIterator getAxisIterator(final int axis) {
        return new AxisIterator(axis, NO_TYPE);
    }

    public DTMAxisIterator getTypedAxisIterator(final int axis, final int type)
    {
        return new AxisIterator(axis, type);
    }

    public DTMAxisIterator getNthDescendant(int node, int n,
                                            boolean includeself)
    {
        return _adapters[getDTMId(node)].getNthDescendant(node, n, includeself);
    }

    public DTMAxisIterator getNodeValueIterator(DTMAxisIterator iterator,
                                                int type, String value,
                                                boolean op)
    {
        return(new NodeValueIterator(iterator, type, value, op));
    }

    public DTMAxisIterator getNamespaceAxisIterator(final int axis,
                                                    final int ns)
    {
        DTMAxisIterator iterator = _main.getNamespaceAxisIterator(axis, ns);
        return(iterator);
    }

    public DTMAxisIterator orderNodes(DTMAxisIterator source, int node) {
        return _adapters[getDTMId(node)].orderNodes(source, node);
    }

    public int getExpandedTypeID(final int node) {
        if (node != DTM.NULL) {
            return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].getExpandedTypeID(node);
        }
        else {
            return DTM.NULL;
        }
    }

    public int getNamespaceType(final int node) {
        return _adapters[getDTMId(node)].getNamespaceType(node);
    }

    public int getNSType(int node)
   {
        return _adapters[getDTMId(node)].getNSType(node);
   }

    public int getParent(final int node) {
        if (node == DTM.NULL) {
            return DTM.NULL;
        }
        return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].getParent(node);
    }

    public int getAttributeNode(final int type, final int el) {
        if (el == DTM.NULL) {
            return DTM.NULL;
        }
        return _adapters[el >>> DTMManager.IDENT_DTM_NODE_BITS].getAttributeNode(type, el);
    }

    public String getNodeName(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].getNodeName(node);
    }

    public String getNodeNameX(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].getNodeNameX(node);
    }

    public String getNamespaceName(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].getNamespaceName(node);
    }

    public String getStringValueX(final int node) {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].getStringValueX(node);
    }

    public void copy(final int node, SerializationHandler handler)
        throws TransletException
    {
        if (node != DTM.NULL) {
            _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].copy(node, handler);
        }
    }

    public void copy(DTMAxisIterator nodes, SerializationHandler handler)
            throws TransletException
    {
        int node;
        while ((node = nodes.next()) != DTM.NULL) {
            _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].copy(node, handler);
        }
    }


    public String shallowCopy(final int node, SerializationHandler handler)
            throws TransletException
    {
        if (node == DTM.NULL) {
            return "";
        }
        return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].shallowCopy(node, handler);
    }

    public boolean lessThan(final int node1, final int node2) {
        if (node1 == DTM.NULL) {
            return true;
        }
        if (node2 == DTM.NULL) {
            return false;
        }
        final int dom1 = getDTMId(node1);
        final int dom2 = getDTMId(node2);
        return dom1 == dom2 ? _adapters[dom1].lessThan(node1, node2)
                            : dom1 < dom2;
    }

    public void characters(final int textNode, SerializationHandler handler)
                 throws TransletException
    {
        if (textNode != DTM.NULL) {
            _adapters[textNode >>> DTMManager.IDENT_DTM_NODE_BITS].characters(textNode, handler);
        }
    }

    public void setFilter(StripFilter filter) {
        for (int dom=0; dom<_free; dom++) {
            if (_adapters[dom] != null) {
                _adapters[dom].setFilter(filter);
            }
        }
    }

    public Node makeNode(int index) {
        if (index == DTM.NULL) {
            return null;
        }
        return _adapters[getDTMId(index)].makeNode(index);
    }

    public Node makeNode(DTMAxisIterator iter) {
        // TODO: gather nodes from all DOMs ?
        return _main.makeNode(iter);
    }

    public NodeList makeNodeList(int index) {
        if (index == DTM.NULL) {
            return null;
        }
        return _adapters[getDTMId(index)].makeNodeList(index);
    }

    public NodeList makeNodeList(DTMAxisIterator iter) {
        int index = iter.next();
        if (index == DTM.NULL) {
            return new DTMAxisIterNodeList(null, null);
        }
        iter.reset();
        return _adapters[getDTMId(index)].makeNodeList(iter);
    }

    public String getLanguage(int node) {
        return _adapters[getDTMId(node)].getLanguage(node);
    }

    public int getSize() {
        int size = 0;
        for (int i=0; i<_size; i++) {
            size += _adapters[i].getSize();
        }
        return(size);
    }

    public String getDocumentURI(int node) {
        if (node == DTM.NULL) {
            node = DOM.NULL;
        }
        return _adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].getDocumentURI(0);
    }

    public boolean isElement(final int node) {
        if (node == DTM.NULL) {
            return false;
        }
        return(_adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].isElement(node));
    }

    public boolean isAttribute(final int node) {
        if (node == DTM.NULL) {
            return false;
        }
        return(_adapters[node >>> DTMManager.IDENT_DTM_NODE_BITS].isAttribute(node));
    }

    public int getDTMId(int nodeHandle)
    {
        if (nodeHandle == DTM.NULL)
            return 0;

        int id = nodeHandle >>> DTMManager.IDENT_DTM_NODE_BITS;
        while (id >= 2 && _adapters[id] == _adapters[id-1]) {
            id--;
        }
        return id;
    }

    public DOM getDTM(int nodeHandle) {
        return _adapters[getDTMId(nodeHandle)];
    }

    public int getNodeIdent(int nodeHandle)
    {
        return _adapters[nodeHandle >>> DTMManager.IDENT_DTM_NODE_BITS].getNodeIdent(nodeHandle);
    }

    public int getNodeHandle(int nodeId)
    {
        return _main.getNodeHandle(nodeId);
    }

    public DOM getResultTreeFrag(int initSize, int rtfType)
    {
        return _main.getResultTreeFrag(initSize, rtfType);
    }

    public DOM getResultTreeFrag(int initSize, int rtfType, boolean addToManager)
    {
        return _main.getResultTreeFrag(initSize, rtfType, addToManager);
    }

    public DOM getMain()
    {
        return _main;
    }

    /**
     * Returns a DOMBuilder class wrapped in a SAX adapter.
     */
    public SerializationHandler getOutputDomBuilder()
    {
        return _main.getOutputDomBuilder();
    }

    public String lookupNamespace(int node, String prefix)
        throws TransletException
    {
        return _main.lookupNamespace(node, prefix);
    }

    // %HZ% Does this method make any sense here???
    public String getUnparsedEntityURI(String entity) {
        return _main.getUnparsedEntityURI(entity);
    }

    // %HZ% Does this method make any sense here???
    public Map<String, Integer> getElementsWithIDs() {
        return _main.getElementsWithIDs();
    }

    public void release() {
        _main.release();
    }

    private boolean isMatchingAdapterEntry(DOM entry, DOMAdapter adapter) {
        DOM dom = adapter.getDOMImpl();

        return (entry == adapter) || (
            /*
             * Method addDOMAdapter overwrites for AdaptiveResultTreeImpl
             * objects the usual entry with an adapter to the nested
             * DOM, so we must check this here. See last 'if' statement
             * of addDOMAdapter.
             */
            (dom instanceof AdaptiveResultTreeImpl) &&
            (entry instanceof DOMAdapter) &&
            (((AdaptiveResultTreeImpl)dom).getNestedDOM() == ((DOMAdapter)entry).getDOMImpl())
        );
    }

    public void removeDOMAdapter(DOMAdapter adapter) {
        _documents.remove(adapter.getDocumentURI(0));
        DOM dom = adapter.getDOMImpl();

        if (dom instanceof DTMDefaultBase) {
            SuballocatedIntVector ids = ((DTMDefaultBase) dom).getDTMIDs();
            int idsSize = ids.size();
            for (int i = 0; i < idsSize; i++) {
                _adapters[ids.elementAt(i) >>> DTMManager.IDENT_DTM_NODE_BITS] = null;
            }
        } else {
            int id = dom.getDocument() >>> DTMManager.IDENT_DTM_NODE_BITS;
            if ((id > 0) && (id < _adapters.length) && isMatchingAdapterEntry(_adapters[id], adapter)) {
                _adapters[id] = null;
            } else {
                boolean found = false;
                for (int i = 0; i < _adapters.length; i++) {
                    if (isMatchingAdapterEntry(_adapters[id], adapter)) {
                        _adapters[i] = null;
                        found = true;
                        break;
                    }
                }
            }
        }
    }
}
