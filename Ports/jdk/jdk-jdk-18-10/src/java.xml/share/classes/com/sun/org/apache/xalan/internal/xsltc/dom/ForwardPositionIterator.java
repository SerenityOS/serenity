/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * This iterator is a wrapper that always returns the position of
 * a node in document order. It is needed for the case where
 * a call to position() occurs in the context of an XSLT element
 * such as xsl:for-each, xsl:apply-templates, etc.
 *
 * The getPosition() methods in DTMAxisIterators defined
 * in DTMDefaultBaseIterators always return the position
 * in document order, which is backwards for XPath in the
 * case of the ancestor, ancestor-or-self, previous and
 * previous-sibling.
 *
 * XSLTC implements position() with the
 * BasisLibrary.positionF() method, and uses the
 * DTMAxisIterator.isReverse() method to determine
 * whether the result of getPosition() should be
 * interpreted as being equal to position().
 * But when the expression appears in apply-templates of
 * for-each, the position() function operates in document
 * order.
 *
 * The only effect of the ForwardPositionIterator is to force
 * the result of isReverse() to false, so that
 * BasisLibrary.positionF() calculates position() in a way
 * that's consistent with the context in which the
 * iterator is being used."
 *
 * (Apparently the correction of isReverse() occurs
 * implicitly, by inheritance. This class also appears
 * to maintain its own position counter, which seems
 * redundant.)
 *
 * @deprecated This class exists only for backwards compatibility with old
 *             translets.  New code should not reference it.
 */
@Deprecated
public final class ForwardPositionIterator extends DTMAxisIteratorBase {

    private DTMAxisIterator _source;

    public ForwardPositionIterator(DTMAxisIterator source) {
        _source = source;
    }

    public DTMAxisIterator cloneIterator() {
        try {
            final ForwardPositionIterator clone =
                (ForwardPositionIterator) super.clone();
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

    public int next() {
        return returnNode(_source.next());
    }

    public DTMAxisIterator setStartNode(int node) {
        _source.setStartNode(node);
        return this;
    }

    public DTMAxisIterator reset() {
        _source.reset();
        return resetPosition();
    }

    public void setMark() {
        _source.setMark();
    }

    public void gotoMark() {
        _source.gotoMark();
    }
}
