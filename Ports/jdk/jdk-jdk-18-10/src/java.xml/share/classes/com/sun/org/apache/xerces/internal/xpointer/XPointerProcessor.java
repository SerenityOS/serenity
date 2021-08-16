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

package com.sun.org.apache.xerces.internal.xpointer;

import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XNIException;

/**
 * <p>
 * The XPointerProcessor is responsible for parsing an XPointer
 * expression and and providing scheme specific resolution of
 * the document fragment pointed to be the pointer.
 * </p>
 *
 * @xerces.internal
 *
 */
public interface XPointerProcessor {

    // The start element event
    public static final int EVENT_ELEMENT_START = 0;

    // The end element event
    public static final int EVENT_ELEMENT_END = 1;

    // The empty element event
    public static final int EVENT_ELEMENT_EMPTY = 2;

    /**
     * Parses an XPointer expression.  It performs scheme specific processing
     * depending on the pointer parts and sets up a Vector of XPointerParts
     * in the order (left-to-right) they appear in the XPointer expression.
     *
     * @param  xpointer A String representing the xpointer expression.
     * @throws XNIException Thrown if the xpointer string does not conform to
     *         the XPointer Framework syntax or the syntax of the pointer part does
     *         not conform to its definition for its scheme.
     */
    public void parseXPointer(String xpointer) throws XNIException;

    /**
     * Evaluates an XML resource with respect to an XPointer expressions
     * by checking if it's element and attributes parameters match the
     * criteria specified in the xpointer expression.
     *
     * @param element - The name of the element.
     * @param attributes - The element attributes.
     * @param augs - Additional information that may include infoset augmentations
     * @param event - An integer indicating
     *                0 - The start of an element
     *                1 - The end of an element
     *                2 - An empty element call
     * @return true if the element was resolved by the xpointer
     * @throws XNIException Thrown to signal an error
     */
    public boolean resolveXPointer(QName element, XMLAttributes attributes,
            Augmentations augs, int event) throws XNIException;

    /**
     * Returns true if the XPointer expression resolves to the current resource fragment
     * or Node which is part of the input resource being streamed else returns false.
     *
     * @return True if the xpointer expression matches a node/fragment in the resource
     *         else returns false.
     * @throws XNIException Thrown to signal an error
     */
    public boolean isFragmentResolved() throws XNIException;

    /**
     * Returns true if the XPointer expression resolves any subresource of the
     * input resource.
     *
     * @return True if the xpointer expression matches a fragment in the resource
     *         else returns false.
     * @throws XNIException Thrown to signal an error
     */
    public boolean isXPointerResolved() throws XNIException;

}
