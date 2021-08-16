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

package com.sun.org.apache.xml.internal.dtm.ref;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.utils.IntVector;

import org.w3c.dom.Node;

/**
 * <code>DTMAxisNodeList</code> gives us an implementation of the DOM's
 * NodeList interface wrapped around a DTM Iterator. The author
 * considers this something of an abominations, since NodeList was not
 * intended to be a general purpose "list of nodes" API and is
 * generally considered by the DOM WG to have be a mistake... but I'm
 * told that some of the XPath/XSLT folks say they must have this
 * solution.
 *
 * Please note that this is not necessarily equivlaent to a DOM
 * NodeList operating over the same document. In particular:
 * <ul>
 *
 * <li>If there are several Text nodes in logical succession (ie,
 * across CDATASection and EntityReference boundaries), we will return
 * only the first; the caller is responsible for stepping through
 * them.
 * (%REVIEW% Provide a convenience routine here to assist, pending
 * proposed DOM Level 3 getAdjacentText() operation?) </li>
 *
 * <li>Since the whole XPath/XSLT architecture assumes that the source
 * document is not altered while we're working with it, we do not
 * promise to implement the DOM NodeList's "live view" response to
 * document mutation. </li>
 *
 * </ul>
 *
 * <p>State: In progress!!</p>
 * */
public class DTMAxisIterNodeList extends DTMNodeListBase {
    private DTM m_dtm;
    private DTMAxisIterator m_iter;
    private IntVector m_cachedNodes;
    private int m_last = -1;
    //================================================================
    // Methods unique to this class
    private DTMAxisIterNodeList() {
    }

    /**
     * Public constructor: Wrap a DTMNodeList around an existing
     * and preconfigured DTMAxisIterator
     */
    public DTMAxisIterNodeList(DTM dtm, DTMAxisIterator dtmAxisIterator) {
        if (dtmAxisIterator == null) {
            m_last = 0;
        } else {
            m_cachedNodes = new IntVector();
            m_dtm = dtm;
        }
        m_iter = dtmAxisIterator;
    }

    /**
     * Access the wrapped DTMIterator. I'm not sure whether anyone will
     * need this or not, but let's write it and think about it.
     *
     */
    public DTMAxisIterator getDTMAxisIterator() {
        return m_iter;
    }


    //================================================================
    // org.w3c.dom.NodeList API follows

    /**
     * Returns the <code>index</code>th item in the collection. If
     * <code>index</code> is greater than or equal to the number of nodes in
     * the list, this returns <code>null</code>.
     * @param index Index into the collection.
     * @return The node at the <code>index</code>th position in the
     *   <code>NodeList</code>, or <code>null</code> if that is not a valid
     *   index.
     */
    public Node item(int index) {
        if (m_iter != null) {
            int node = 0;
            int count = m_cachedNodes.size();

            if (count > index) {
                node = m_cachedNodes.elementAt(index);
                return m_dtm.getNode(node);
            } else if (m_last == -1) {
                while (count <= index
                        && ((node = m_iter.next()) != DTMAxisIterator.END)) {
                    m_cachedNodes.addElement(node);
                    count++;
                }
                if (node == DTMAxisIterator.END) {
                    m_last = count;
                } else {
                    return m_dtm.getNode(node);
                }
            }
        }
        return null;
    }

    /**
     * The number of nodes in the list. The range of valid child node indices
     * is 0 to <code>length-1</code> inclusive.
     */
    public int getLength() {
        if (m_last == -1) {
            int node;
            while ((node = m_iter.next()) != DTMAxisIterator.END) {
                m_cachedNodes.addElement(node);
            }
            m_last = m_cachedNodes.size();
        }
        return m_last;
    }
}
