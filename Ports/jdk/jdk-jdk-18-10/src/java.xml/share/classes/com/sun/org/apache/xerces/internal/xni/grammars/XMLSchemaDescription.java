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

package com.sun.org.apache.xerces.internal.xni.grammars;

import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;

/**
 * All information specific to XML Schema grammars.
 *
 * @author Sandy Gao, IBM
 *
 */
public interface XMLSchemaDescription extends XMLGrammarDescription {

    // used to indicate what triggered the call
    /**
     * Indicate that the current schema document is &lt;include&gt;d by another
     * schema document.
     */
    public final static short CONTEXT_INCLUDE   = 0;
    /**
     * Indicate that the current schema document is &lt;redefine&gt;d by another
     * schema document.
     */
    public final static short CONTEXT_REDEFINE  = 1;
    /**
     * Indicate that the current schema document is &lt;import&gt;ed by another
     * schema document.
     */
    public final static short CONTEXT_IMPORT    = 2;
    /**
     * Indicate that the current schema document is being preparsed.
     */
    public final static short CONTEXT_PREPARSE  = 3;
    /**
     * Indicate that the parse of the current schema document is triggered
     * by xsi:schemaLocation/noNamespaceSchemaLocation attribute(s) in the
     * instance document. This value is only used if we don't defer the loading
     * of schema documents.
     */
    public final static short CONTEXT_INSTANCE  = 4;
    /**
     * Indicate that the parse of the current schema document is triggered by
     * the occurrence of an element whose namespace is the target namespace
     * of this schema document. This value is only used if we do defer the
     * loading of schema documents until a component from that namespace is
     * referenced from the instance.
     */
    public final static short CONTEXT_ELEMENT   = 5;
    /**
     * Indicate that the parse of the current schema document is triggered by
     * the occurrence of an attribute whose namespace is the target namespace
     * of this schema document. This value is only used if we do defer the
     * loading of schema documents until a component from that namespace is
     * referenced from the instance.
     */
    public final static short CONTEXT_ATTRIBUTE = 6;
    /**
     * Indicate that the parse of the current schema document is triggered by
     * the occurrence of an "xsi:type" attribute, whose value (a QName) has
     * the target namespace of this schema document as its namespace.
     * This value is only used if we do defer the loading of schema documents
     * until a component from that namespace is referenced from the instance.
     */
    public final static short CONTEXT_XSITYPE   = 7;

    /**
     * Get the context. The returned value is one of the pre-defined
     * CONTEXT_xxx constants.
     *
     * @return  the value indicating the context
     */
    public short getContextType();

    /**
     * If the context is "include" or "redefine", then return the target
     * namespace of the enclosing schema document; otherwise, the expected
     * target namespace of this document.
     *
     * @return  the expected/enclosing target namespace
     */
    public String getTargetNamespace();

    /**
     * For import and references from the instance document, it's possible to
     * have multiple hints for one namespace. So this method returns an array,
     * which contains all location hints.
     *
     * @return  an array of all location hints associated to the expected
     *          target namespace
     */
    public String[] getLocationHints();

    /**
     * If a call is triggered by an element/attribute/xsi:type in the instance,
     * this call returns the name of such triggering component: the name of
     * the element/attribute, or the value of the xsi:type.
     *
     * @return  the name of the triggering component
     */
    public QName getTriggeringComponent();

    /**
     * If a call is triggered by an attribute or xsi:type, then this method
     * returns the enclosing element of such element.
     *
     * @return  the name of the enclosing element
     */
    public QName getEnclosingElementName();

    /**
     * If a call is triggered by an element/attribute/xsi:type in the instance,
     * this call returns all attribute of such element (or enclosing element).
     *
     * @return  all attributes of the tiggering/enclosing element
     */
    public XMLAttributes getAttributes();

} // XSDDescription
