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

package com.sun.org.apache.xalan.internal.xsltc;

import com.sun.org.apache.xml.internal.dtm.DTM;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public interface NodeIterator extends Cloneable {
    public static final int END = DTM.NULL;

    /**
     * Callers should not call next() after it returns END.
     */
    public int next();

    /**
     * Resets the iterator to the last start node.
     */
    public NodeIterator reset();

    /**
     * Returns the number of elements in this iterator.
     */
    public int getLast();

    /**
     * Returns the position of the current node in the set.
     */
    public int getPosition();

    /**
     * Remembers the current node for the next call to gotoMark().
     */
    public void setMark();

    /**
     * Restores the current node remembered by setMark().
     */
    public void gotoMark();

    /**
     * Set start to END should 'close' the iterator,
     * i.e. subsequent call to next() should return END.
     */
    public NodeIterator setStartNode(int node);

    /**
     * True if this iterator has a reversed axis.
     */
    public boolean isReverse();

    /**
     * Returns a deep copy of this iterator.
     */
    public NodeIterator cloneIterator();

    /**
     * Prevents or allows iterator restarts.
     */
    public void setRestartable(boolean isRestartable);

}
