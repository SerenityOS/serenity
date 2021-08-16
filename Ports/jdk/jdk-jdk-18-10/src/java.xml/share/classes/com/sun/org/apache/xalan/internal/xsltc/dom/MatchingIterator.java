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
 * This is a special kind of iterator that takes a source iterator and a
 * node N. If initialized with a node M (the parent of N) it computes the
 * position of N amongst the children of M. This position can be obtained
 * by calling getPosition().
 * It is an iterator even though next() will never be called. It is used to
 * match patterns with a single predicate like:
 *
 *    BOOK[position() = last()]
 *
 * In this example, the source iterator will return elements of type BOOK,
 * a call to position() will return the position of N. Notice that because
 * of the way the pattern matching is implemented, N will always be a node
 * in the source since (i) it is a BOOK or the test sequence would not be
 * considered and (ii) the source iterator is initialized with M which is
 * the parent of N. Also, and still in this example, a call to last() will
 * return the number of elements in the source (i.e. the number of BOOKs).
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public final class MatchingIterator extends DTMAxisIteratorBase {

    /**
     * A reference to a source iterator.
     */
    private DTMAxisIterator _source;

    /**
     * The node to match.
     */
    private final int _match;

    public MatchingIterator(int match, DTMAxisIterator source) {
        _source = source;
        _match = match;
    }


    public void setRestartable(boolean isRestartable) {
        _isRestartable = isRestartable;
        _source.setRestartable(isRestartable);
    }

    public DTMAxisIterator cloneIterator() {

        try {
            final MatchingIterator clone = (MatchingIterator) super.clone();
            clone._source = _source.cloneIterator();
            clone._isRestartable = false;
            return clone.reset();
        }
        catch (CloneNotSupportedException e) {
            BasisLibrary.runTimeError(BasisLibrary.ITERATOR_CLONE_ERR,
                                      e.toString());
            return null;
        }
    }

    public DTMAxisIterator setStartNode(int node) {
        if (_isRestartable) {
            // iterator is not a clone
            _source.setStartNode(node);

            // Calculate the position of the node in the set
            _position = 1;
            while ((node = _source.next()) != END && node != _match) {
                _position++;
            }
        }
        return this;
    }

    public DTMAxisIterator reset() {
        _source.reset();
        return resetPosition();
    }

    public int next() {
        return _source.next();
    }

    public int getLast() {
        if (_last == -1) {
            _last = _source.getLast();
        }
        return _last;
    }

    public int getPosition() {
        return _position;
    }

    public void setMark() {
        _source.setMark();
    }

    public void gotoMark() {
        _source.gotoMark();
    }
}
