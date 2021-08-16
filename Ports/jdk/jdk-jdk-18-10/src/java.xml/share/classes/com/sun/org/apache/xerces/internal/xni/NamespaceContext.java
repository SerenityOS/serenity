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

package com.sun.org.apache.xerces.internal.xni;

import java.util.Enumeration;

/**
 * Represents an interface to query namespace information.
 * <p>
 * The prefix and namespace must be identical references for equal strings, thus
 * each string should be internalized (@see String.intern())
 * or added to the <code>SymbolTable</code>
 *
 * @see <a href="../../../../../xerces2/com/sun/org/apache/xerces/internal/util/SymbolTable.html">
 * com.sun.org.apache.xerces.internal.util.SymbolTable</a>
 *
 * @author Andy Clark, IBM
 *
 * @LastModified: Oct 2017
 */
public interface NamespaceContext {

    //
    // Constants
    //

    /**
     * The XML Namespace ("http://www.w3.org/XML/1998/namespace"). This is
     * the Namespace URI that is automatically mapped to the "xml" prefix.
     */
    public final static String XML_URI = "http://www.w3.org/XML/1998/namespace".intern();

    /**
     * XML Information Set REC
     * all namespace attributes (including those named xmlns,
     * whose [prefix] property has no value) have a namespace URI of http://www.w3.org/2000/xmlns/
     */
    public final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/".intern();

    //
    // NamespaceContext methods
    //

    /**
     * Start a new Namespace context.
     * <p>
     * A new context should be pushed at the beginning
     * of each XML element: the new context will automatically inherit
     * the declarations of its parent context, but it will also keep
     * track of which declarations were made within this context.
     * <p>
     *
     * @see #popContext
     */
    public void pushContext();

   /**
     * Revert to the previous Namespace context.
     * <p>
     * The context should be popped at the end of each
     * XML element.  After popping the context, all Namespace prefix
     * mappings that were previously in force are restored.
     * <p>
     * Users must not attempt to declare additional Namespace
     * prefixes after popping a context, unless you push another
     * context first.
     *
     * @see #pushContext
     */
    public void popContext();

    /**
     * Declare a Namespace prefix.
     * <p>
     * This method declares a prefix in the current Namespace
     * context; the prefix will remain in force until this context
     * is popped, unless it is shadowed in a descendant context.
     * <p>
     * Note that to declare a default Namespace, use the empty string.
     * The prefixes "xml" and "xmlns" can't be rebound.
     * <p>
     * Note that you must <em>not</em> declare a prefix after
     * you've pushed and popped another Namespace.
     *
     * @param prefix The prefix to declare, or null for the empty
     *        string.
     * @param uri The Namespace URI to associate with the prefix.
     *
     * @return true if the prefix was legal, false otherwise
     *
     * @see #getURI
     * @see #getDeclaredPrefixAt
     */
    public boolean declarePrefix(String prefix, String uri);


    /**
     * Look up a prefix and get the currently-mapped Namespace URI.
     * <p>
     * This method looks up the prefix in the current context. If no mapping
     * is found, this methods will continue lookup in the parent context(s).
     * Use the empty string ("") for the default Namespace.
     *
     * @param prefix The prefix to look up.
     *
     * @return The associated Namespace URI, or null if the prefix
     *         is undeclared in this context.
     */
    public String getURI(String prefix);

    /**
     * Look up a namespace URI and get one of the mapped prefix.
     * <p>
     * This method looks up the namespace URI in the current context.
     * If more than one prefix is currently mapped to the same URI,
     * this method will make an arbitrary selection
     * If no mapping is found, this methods will continue lookup in the
     * parent context(s).
     *
     * @param uri The namespace URI to look up.
     *
     * @return One of the associated prefixes, or null if the uri
     *         does not map to any prefix.
     *
     * @see #getPrefix
     */
    public String getPrefix(String uri);


    /**
     * Return a count of locally declared prefixes, including
     * the default prefix if bound.
     */
    public int getDeclaredPrefixCount();

    /**
     * Returns the prefix at the specified index in the current context.
     */
    public String getDeclaredPrefixAt(int index);

        /**
         * Return an enumeration of all prefixes whose declarations are active
     * in the current context. This includes declarations from parent contexts
     * that have not been overridden.
         * @return Enumeration
         */
    public Enumeration<String> getAllPrefixes();

    /**
     * Reset this Namespace support object for reuse.
     *
     * <p>It is necessary to invoke this method before reusing the
     * Namespace support object for a new session.</p>
     *
     * <p>Note that implementations of this method need to ensure that
     * the declaration of the prefixes "xmlns" and "xml" are available.</p>
     */
    public void reset();


} // interface NamespaceContext
