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
 * SimpleContentModel is a derivative of the abstract content model base
 * class that handles a small set of simple content models that are just
 * way overkill to give the DFA treatment.
 * <p>
 * This class handles the following scenarios:
 * <ul>
 * <li> a
 * <li> a?
 * <li> a*
 * <li> a+
 * <li> a,b
 * <li> a|b
 * </ul>
 * <p>
 * These all involve a unary operation with one element type, or a binary
 * operation with two elements. These are very simple and can be checked
 * in a simple way without a DFA and without the overhead of setting up a
 * DFA for such a simple check.
 *
 * @xerces.internal
 *
 */
public class SimpleContentModel
    implements ContentModelValidator {

    //
    // Constants
    //

    /** CHOICE */
    public static final short CHOICE = -1;

    /** SEQUENCE */
    public static final short SEQUENCE = -1;

    //
    // Data
    //


    /**
     * The element decl pool indices of the first (and optional second)
     * child node. The operation code tells us whether the second child
     * is used or not.
     */
    private QName fFirstChild = new QName();

    /**
     * The element decl pool indices of the first (and optional second)
     * child node. The operation code tells us whether the second child
     * is used or not.
     */
    private QName fSecondChild = new QName();

    /**
     * The operation that this object represents. Since this class only
     * does simple contents, there is only ever a single operation
     * involved (i.e. the children of the operation are always one or
     * two leafs.) This is one of the XMLDTDParams.CONTENTSPECNODE_XXX values.
     */
    private int fOperator;

    /* this is the EquivClassComparator object */
    //private EquivClassComparator comparator = null;


    //
    // Constructors
    //

    /**
     * Constructs a simple content model.
     *
     * @param operator The content model operator.
     * @param firstChild qualified name of the first child
     * @param secondChild qualified name of the second child
     *
     */
    public SimpleContentModel(short operator, QName firstChild, QName secondChild) {
        //
        //  Store away the children and operation. This is all we need to
        //  do the content model check.
        //
        //  The operation is one of the ContentSpecNode.NODE_XXX values!
        //
        fFirstChild.setValues(firstChild);
        if (secondChild != null) {
            fSecondChild.setValues(secondChild);
        }
        else {
            fSecondChild.clear();
        }
        fOperator = operator;
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

        //
        //  According to the type of operation, we do the correct type of
        //  content check.
        //
        switch(fOperator)
        {
            case XMLContentSpec.CONTENTSPECNODE_LEAF :
                // If there is not a child, then report an error at index 0
                if (length == 0)
                    return 0;

                // If the 0th child is not the right kind, report an error at 0
                if (children[offset].rawname != fFirstChild.rawname) {
                    return 0;
                }

                // If more than one child, report an error at index 1
                if (length > 1)
                    return 1;
                break;

            case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE :
                //
                //  If there is one child, make sure its the right type. If not,
                //  then its an error at index 0.
                //
                if (length == 1) {
                    if (children[offset].rawname != fFirstChild.rawname) {
                        return 0;
                    }
                }

                //
                //  If the child count is greater than one, then obviously
                //  bad, so report an error at index 1.
                //
                if (length > 1)
                    return 1;
                break;

            case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE :
                //
                //  If the child count is zero, that's fine. If its more than
                //  zero, then make sure that all children are of the element
                //  type that we stored. If not, report the index of the first
                //  failed one.
                //
                if (length > 0)
                {
                    for (int index = 0; index < length; index++) {
                        if (children[offset + index].rawname != fFirstChild.rawname) {
                            return index;
                        }
                    }
                }
                break;

            case XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE :
                //
                //  If the child count is zero, that's an error so report
                //  an error at index 0.
                //
                if (length == 0)
                    return 0;

                //
                //  Otherwise we have to check them all to make sure that they
                //  are of the correct child type. If not, then report the index
                //  of the first one that is not.
                //
                for (int index = 0; index < length; index++) {
                    if (children[offset + index].rawname != fFirstChild.rawname) {
                        return index;
                    }
                }
                break;

            case XMLContentSpec.CONTENTSPECNODE_CHOICE :
                //
                //  There must be one and only one child, so if the element count
                //  is zero, return an error at index 0.
                //
                if (length == 0)
                    return 0;

                // If the zeroth element isn't one of our choices, error at 0
                if ((children[offset].rawname != fFirstChild.rawname) &&
                    (children[offset].rawname != fSecondChild.rawname)) {
                    return 0;
                }

                // If there is more than one element, then an error at 1
                if (length > 1)
                    return 1;
                break;

            case XMLContentSpec.CONTENTSPECNODE_SEQ :
                //
                //  There must be two children and they must be the two values
                //  we stored, in the stored order.
                //
                if (length == 2) {
                    if (children[offset].rawname != fFirstChild.rawname) {
                        return 0;
                    }
                    if (children[offset + 1].rawname != fSecondChild.rawname) {
                        return 1;
                    }
                }
                else {
                    if (length > 2) {
                        return 2;
                    }

                    return length;
                }

                break;

            default :
                throw new RuntimeException("ImplementationMessages.VAL_CST");
        }

        // We survived, so return success status
        return -1;
    } // validate

} // class SimpleContentModel
