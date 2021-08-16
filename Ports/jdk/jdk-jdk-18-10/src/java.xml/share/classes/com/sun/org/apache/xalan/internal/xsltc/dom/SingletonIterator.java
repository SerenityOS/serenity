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

import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.ref.DTMAxisIteratorBase;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public class SingletonIterator extends DTMAxisIteratorBase {
    private int _node;
    private final boolean _isConstant;

    public SingletonIterator() {
        this(Integer.MIN_VALUE, false);
    }

    public SingletonIterator(int node) {
        this(node, false);
    }

    public SingletonIterator(int node, boolean constant) {
        _node = _startNode = node;
        _isConstant = constant;
    }

    /**
     * Override the value of <tt>_node</tt> only when this
     * object was constructed using the empty constructor.
     */
    public DTMAxisIterator setStartNode(int node) {
        if (_isConstant) {
            _node = _startNode;
            return resetPosition();
        }
        else if (_isRestartable) {
            if (_node <= 0)
                _node = _startNode = node;
            return resetPosition();
        }
        return this;
    }

    public DTMAxisIterator reset() {
        if (_isConstant) {
            _node = _startNode;
            return resetPosition();
        }
        else {
            final boolean temp = _isRestartable;
            _isRestartable = true;
            setStartNode(_startNode);
            _isRestartable = temp;
        }
        return this;
    }

    public int next() {
        final int result = _node;
        _node = DTMAxisIterator.END;
        return returnNode(result);
    }

    public void setMark() {
        _markedNode = _node;
    }

    public void gotoMark() {
        _node = _markedNode;
    }
}
