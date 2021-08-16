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

package com.sun.org.apache.xerces.internal.impl.dtd.models;

import com.sun.org.apache.xerces.internal.xni.QName;

import com.sun.org.apache.xerces.internal.impl.dtd.XMLContentSpec;

/**
 * MixedContentModel is a derivative of the abstract content model base
 * class that handles the special case of mixed model elements. If an element
 * is mixed model, it has PCDATA as its first possible content, followed
 * by an alternation of the possible children. The children cannot have any
 * numeration or order, so it must look like this:
 * <pre>
 *   &lt;!ELEMENT Foo ((#PCDATA|a|b|c|)*)&gt;
 * </pre>
 * So, all we have to do is to keep an array of the possible children and
 * validate by just looking up each child being validated by looking it up
 * in the list.
 *
 * @xerces.internal
 *
 */
public class MixedContentModel
    implements ContentModelValidator {

    //
    // Data
    //

    /** The count of possible children that we have to deal with. */
    private int fCount;

    /** The list of possible children that we have to accept. */
    private QName fChildren[];

    /** The type of the children to support ANY. */
    private int fChildrenType[];

    /* this is the EquivClassComparator object */
    //private EquivClassComparator comparator = null;

    /**
     * True if mixed content model is ordered. DTD mixed content models
     * are <em>always</em> unordered.
     */
    private boolean fOrdered;

    //
    // Constructors
    //

    /**
     * Constructs a mixed content model.
     *
     * @param children The list of allowed children.
     * @param type The list of the types of the children.
     * @param offset The start offset position in the children.
     * @param length The child count.
     * @param ordered True if content must be ordered.
     */
    public MixedContentModel(QName[] children, int[] type, int offset, int length , boolean ordered) {
        // Make our own copy now, which is exactly the right size
        fCount = length;
        fChildren = new QName[fCount];
        fChildrenType = new int[fCount];
        for (int i = 0; i < fCount; i++) {
            fChildren[i] = new QName(children[offset + i]);
            fChildrenType[i] = type[offset + i];
        }
        fOrdered = ordered;

    }

    //
    // ContentModelValidator methods
    //


    /**
     * Check that the specified content is valid according to this
     * content model. This method can also be called to do 'what if'
     * testing of content models just to see if they would be valid.
     * <p>
     * A value of -1 in the children array indicates a PCDATA node. All other
     * indexes will be positive and represent child elements. The count can be
     * zero, since some elements have the EMPTY content model and that must be
     * confirmed.
     *
     * @param children The children of this element.  Each integer is an index within
     *                 the <code>StringPool</code> of the child element name.  An index
     *                 of -1 is used to indicate an occurrence of non-whitespace character
     *                 data.
     * @param offset Offset into the array where the children starts.
     * @param length The number of entries in the <code>children</code> array.
     *
     * @return The value -1 if fully valid, else the 0 based index of the child
     *         that first failed. If the value returned is equal to the number
     *         of children, then the specified children are valid but additional
     *         content is required to reach a valid ending state.
     *
     */
    public int validate(QName[] children, int offset, int length) {

        // must match order
        if (fOrdered) {
            int inIndex = 0;
            for (int outIndex = 0; outIndex < length; outIndex++) {

                // ignore mixed text
                final QName curChild = children[offset + outIndex];
                if (curChild.localpart == null) {
                    continue;
                }

                // element must match
                int type = fChildrenType[inIndex];
                if (type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                    if (fChildren[inIndex].rawname != children[offset + outIndex].rawname) {
                        return outIndex;
                    }
                }
                else if (type == XMLContentSpec.CONTENTSPECNODE_ANY) {
                    String uri = fChildren[inIndex].uri;
                    if (uri != null && uri != children[outIndex].uri) {
                        return outIndex;
                    }
                }
                else if (type == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL) {
                    if (children[outIndex].uri != null) {
                        return outIndex;
                    }
                }
                else if (type == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {
                    if (fChildren[inIndex].uri == children[outIndex].uri) {
                        return outIndex;
                    }
                }

                // advance index
                inIndex++;
            }
        }

        // can appear in any order
        else {
            for (int outIndex = 0; outIndex < length; outIndex++)
            {
                // Get the current child out of the source index
                final QName curChild = children[offset + outIndex];

                // If its PCDATA, then we just accept that
                if (curChild.localpart == null)
                    continue;

                // And try to find it in our list
                int inIndex = 0;
                for (; inIndex < fCount; inIndex++)
                {
                    int type = fChildrenType[inIndex];
                    if (type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                        if (curChild.rawname == fChildren[inIndex].rawname) {
                            break;
                        }
                    }
                    else if (type == XMLContentSpec.CONTENTSPECNODE_ANY) {
                        String uri = fChildren[inIndex].uri;
                        if (uri == null || uri == children[outIndex].uri) {
                            break;
                        }
                    }
                    else if (type == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL) {
                        if (children[outIndex].uri == null) {
                            break;
                        }
                    }
                    else if (type == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {
                        if (fChildren[inIndex].uri != children[outIndex].uri) {
                            break;
                        }
                    }
                    // REVISIT: What about checking for multiple ANY matches?
                    //          The content model ambiguity *could* be checked
                    //          by the caller before constructing the mixed
                    //          content model.
                }

                // We did not find this one, so the validation failed
                if (inIndex == fCount)
                    return outIndex;
            }
        }

        // Everything seems to be in order, so return success
        return -1;
    } // validate

} // class MixedContentModel
