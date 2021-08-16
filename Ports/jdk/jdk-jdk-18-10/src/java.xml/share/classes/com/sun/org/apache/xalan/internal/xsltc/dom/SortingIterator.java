/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

import com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.ref.DTMAxisIteratorBase;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 */
public final class SortingIterator extends DTMAxisIteratorBase {
    private final static int INIT_DATA_SIZE = 16;

    private DTMAxisIterator _source;
    private NodeSortRecordFactory _factory;

    private NodeSortRecord[] _data;
    private int _free = 0;
    private int _current;       // index in _nodes of the next node to try

    public SortingIterator(DTMAxisIterator source,
                           NodeSortRecordFactory factory) {
        _source = source;
        _factory = factory;
    }

    public int next() {
        return _current < _free ? _data[_current++].getNode() : END;
    }

    public DTMAxisIterator setStartNode(int node) {
        try {
            _source.setStartNode(_startNode = node);
            _data = new NodeSortRecord[INIT_DATA_SIZE];
            _free = 0;

            // gather all nodes from the source iterator
            while ((node = _source.next()) != END) {
                addRecord(_factory.makeNodeSortRecord(node,_free));
            }
            // now sort the records
            quicksort(0, _free - 1);

            _current = 0;
            return this;
        }
        catch (Exception e) {
            return this;
        }
    }

    public int getPosition() {
        return _current == 0 ? 1 : _current;
    }

    public int getLast() {
        return _free;
    }

    public void setMark() {
        _source.setMark();
        _markedNode = _current;
    }

    public void gotoMark() {
        _source.gotoMark();
        _current = _markedNode;
    }

    /**
     * Clone a <code>SortingIterator</code> by cloning its source
     * iterator and then sharing the factory and the array of
     * <code>NodeSortRecords</code>.
     */
    public DTMAxisIterator cloneIterator() {
        try {
            final SortingIterator clone = (SortingIterator) super.clone();
            clone._source = _source.cloneIterator();
            clone._factory = _factory;          // shared between clones
            clone._data = _data;                // shared between clones
            clone._free = _free;
            clone._current = _current;
            clone.setRestartable(false);
            return clone.reset();
        }
        catch (CloneNotSupportedException e) {
            BasisLibrary.runTimeError(BasisLibrary.ITERATOR_CLONE_ERR,
                                      e.toString());
            return null;
        }
    }

    private void addRecord(NodeSortRecord record) {
        if (_free == _data.length) {
            NodeSortRecord[] newArray = new NodeSortRecord[_data.length * 2];
            System.arraycopy(_data, 0, newArray, 0, _free);
            _data = newArray;
        }
        _data[_free++] = record;
    }

    private void quicksort(int p, int r) {
        while (p < r) {
            final int q = partition(p, r);
            quicksort(p, q);
            p = q + 1;
        }
    }

    private int partition(int p, int r) {
        final NodeSortRecord x = _data[(p + r) >>> 1];
        int i = p - 1;
        int j = r + 1;
        while (true) {
            while (x.compareTo(_data[--j]) < 0);
            while (x.compareTo(_data[++i]) > 0);
            if (i < j) {
                final NodeSortRecord t = _data[i];
                _data[i] = _data[j];
                _data[j] = t;
            }
            else {
                return(j);
            }
        }
    }
}
