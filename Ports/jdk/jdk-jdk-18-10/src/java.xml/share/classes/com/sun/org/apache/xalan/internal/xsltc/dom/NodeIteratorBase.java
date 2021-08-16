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

import com.sun.org.apache.xalan.internal.xsltc.NodeIterator;
import com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 */
public abstract class NodeIteratorBase implements NodeIterator {

    /**
     * Cached computed value of last().
     */
    protected int _last = -1;

    /**
     * Value of position() in this iterator. Incremented in
     * returnNode().
     */
    protected int _position = 0;

    /**
     * Store node in call to setMark().
     */
    protected int _markedNode;

    /**
     * Store node in call to setStartNode().
     */
    protected int _startNode = NodeIterator.END;

    /**
     * Flag indicating if "self" should be returned.
     */
    protected boolean _includeSelf = false;

    /**
     * Flag indicating if iterator can be restarted.
     */
    protected boolean _isRestartable = true;

    /**
     * Setter for _isRestartable flag.
     */
    public void setRestartable(boolean isRestartable) {
        _isRestartable = isRestartable;
    }

    /**
     * Initialize iterator using a node. If iterator is not
     * restartable, then do nothing. If node is equal to END then
     * subsequent calls to next() must return END.
     */
    abstract public NodeIterator setStartNode(int node);

    /**
     * Reset this iterator using state from last call to
     * setStartNode().
     */
    public NodeIterator reset() {
        final boolean temp = _isRestartable;
        _isRestartable = true;
        // Must adjust _startNode if self is included
        setStartNode(_includeSelf ? _startNode + 1 : _startNode);
        _isRestartable = temp;
        return this;
    }

    /**
     * Setter for _includeSelf flag.
     */
    public NodeIterator includeSelf() {
        _includeSelf = true;
        return this;
    }

    /**
     * Default implementation of getLast(). Stores current position
     * and current node, resets the iterator, counts all nodes and
     * restores iterator to original state.
     */
    public int getLast() {
        if (_last == -1) {
            final int temp = _position;
            setMark();
            reset();
            do {
                _last++;
            } while (next() != END);
            gotoMark();
            _position = temp;
        }
        return _last;
    }

    /**
     * Returns the position() in this iterator.
     */
    public int getPosition() {
        return _position == 0 ? 1 : _position;
    }

    /**
     * Indicates if position in this iterator is computed in reverse
     * document order. Note that nodes are always returned in document
     * order.
     */
    public boolean isReverse() {
        return false;
    }

    /**
     * Clones and resets this iterator. Note that the cloned iterator is
     * not restartable. This is because cloning is needed for variable
     * references, and the context node of the original variable
     * declaration must be preserved.
     */
    public NodeIterator cloneIterator() {
        try {
            final NodeIteratorBase clone = (NodeIteratorBase)super.clone();
            clone._isRestartable = false;
            return clone.reset();
        }
        catch (CloneNotSupportedException e) {
            BasisLibrary.runTimeError(BasisLibrary.ITERATOR_CLONE_ERR,
                                      e.toString());
            return null;
        }
    }

    /**
     * Utility method that increments position and returns its
     * argument.
     */
    protected final int returnNode(final int node) {
        _position++;
        return node;
    }

    /**
     * Reset the position in this iterator.
     */
    protected final NodeIterator resetPosition() {
        _position = 0;
        return this;
    }
}
