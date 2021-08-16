/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serializer.dom3;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Namespace support for XML document handlers. This class doesn't
 * perform any error checking and assumes that all strings passed
 * as arguments to methods are unique symbols. The SymbolTable class
 * can be used for this purpose.
 *
 * Derived from org.apache.xerces.util.NamespaceSupport
 *
 * @author Andy Clark, IBM
 *
 * @version $Id: Exp $
 * @LastModified: Oct 2017
 */
public class NamespaceSupport {

    static final String PREFIX_XML = "xml".intern();

    static final String PREFIX_XMLNS = "xmlns".intern();

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
    // Data
    //

    /**
     * Namespace binding information. This array is composed of a
     * series of tuples containing the namespace binding information:
     * &lt;prefix, uri&gt;. The default size can be set to anything
     * as long as it is a power of 2 greater than 1.
     *
     * @see #fNamespaceSize
     * @see #fContext
     */
    protected String[] fNamespace = new String[16 * 2];

    /** The top of the namespace information array. */
    protected int fNamespaceSize;

    // NOTE: The constructor depends on the initial context size
    //       being at least 1. -Ac

    /**
     * Context indexes. This array contains indexes into the namespace
     * information array. The index at the current context is the start
     * index of declared namespace bindings and runs to the size of the
     * namespace information array.
     *
     * @see #fNamespaceSize
     */
    protected int[] fContext = new int[8];

    /** The current context. */
    protected int fCurrentContext;

    protected String[] fPrefixes = new String[16];

    //
    // Constructors
    //

    /** Default constructor. */
    public NamespaceSupport() {
    } // <init>()

    //
    // Public methods
    //

        /**
         * @see org.apache.xerces.xni.NamespaceContext#reset()
         */
    public void reset() {

        // reset namespace and context info
        fNamespaceSize = 0;
        fCurrentContext = 0;
        fContext[fCurrentContext] = fNamespaceSize;

        // bind "xml" prefix to the XML uri
        fNamespace[fNamespaceSize++] = PREFIX_XML;
        fNamespace[fNamespaceSize++] = XML_URI;
        // bind "xmlns" prefix to the XMLNS uri
        fNamespace[fNamespaceSize++] = PREFIX_XMLNS;
        fNamespace[fNamespaceSize++] = XMLNS_URI;
        ++fCurrentContext;

    } // reset(SymbolTable)


        /**
         * @see org.apache.xerces.xni.NamespaceContext#pushContext()
         */
    public void pushContext() {

        // extend the array, if necessary
        if (fCurrentContext + 1 == fContext.length) {
            int[] contextarray = new int[fContext.length * 2];
            System.arraycopy(fContext, 0, contextarray, 0, fContext.length);
            fContext = contextarray;
        }

        // push context
        fContext[++fCurrentContext] = fNamespaceSize;

    } // pushContext()


        /**
         * @see org.apache.xerces.xni.NamespaceContext#popContext()
         */
    public void popContext() {
        fNamespaceSize = fContext[fCurrentContext--];
    } // popContext()

        /**
         * @see org.apache.xerces.xni.NamespaceContext#declarePrefix(String, String)
         */
    public boolean declarePrefix(String prefix, String uri) {
        // ignore "xml" and "xmlns" prefixes
        if (prefix == PREFIX_XML || prefix == PREFIX_XMLNS) {
            return false;
        }

        // see if prefix already exists in current context
        for (int i = fNamespaceSize; i > fContext[fCurrentContext]; i -= 2) {
            //if (fNamespace[i - 2] == prefix) {
                if (fNamespace[i - 2].equals(prefix) )  {
                // REVISIT: [Q] Should the new binding override the
                //          previously declared binding or should it
                //          it be ignored? -Ac
                // NOTE:    The SAX2 "NamespaceSupport" helper allows
                //          re-bindings with the new binding overwriting
                //          the previous binding. -Ac
                fNamespace[i - 1] = uri;
                return true;
            }
        }

        // resize array, if needed
        if (fNamespaceSize == fNamespace.length) {
            String[] namespacearray = new String[fNamespaceSize * 2];
            System.arraycopy(fNamespace, 0, namespacearray, 0, fNamespaceSize);
            fNamespace = namespacearray;
        }

        // bind prefix to uri in current context
        fNamespace[fNamespaceSize++] = prefix;
        fNamespace[fNamespaceSize++] = uri;

        return true;

    } // declarePrefix(String,String):boolean

        /**
         * @see org.apache.xerces.xni.NamespaceContext#getURI(String)
         */
    public String getURI(String prefix) {

        // find prefix in current context
        for (int i = fNamespaceSize; i > 0; i -= 2) {
            //if (fNamespace[i - 2] == prefix) {
                if (fNamespace[i - 2].equals(prefix) ) {
                return fNamespace[i - 1];
            }
        }

        // prefix not found
        return null;

    } // getURI(String):String


        /**
         * @see org.apache.xerces.xni.NamespaceContext#getPrefix(String)
         */
    public String getPrefix(String uri) {

        // find uri in current context
        for (int i = fNamespaceSize; i > 0; i -= 2) {
            //if (fNamespace[i - 1] == uri) {
                if (fNamespace[i - 1].equals(uri) ) {
                //if (getURI(fNamespace[i - 2]) == uri)
                        if (getURI(fNamespace[i - 2]).equals(uri) )
                    return fNamespace[i - 2];
            }
        }

        // uri not found
        return null;

    } // getPrefix(String):String


        /**
         * @see org.apache.xerces.xni.NamespaceContext#getDeclaredPrefixCount()
         */
    public int getDeclaredPrefixCount() {
        return (fNamespaceSize - fContext[fCurrentContext]) / 2;
    } // getDeclaredPrefixCount():int

        /**
         * @see org.apache.xerces.xni.NamespaceContext#getDeclaredPrefixAt(int)
         */
    public String getDeclaredPrefixAt(int index) {
        return fNamespace[fContext[fCurrentContext] + index * 2];
    } // getDeclaredPrefixAt(int):String

        /**
         * @see org.apache.xerces.xni.NamespaceContext#getAllPrefixes()
         */
        public Enumeration<String> getAllPrefixes() {
            int count = 0;
            if (fPrefixes.length < (fNamespace.length/2)) {
                // resize prefix array
                String[] prefixes = new String[fNamespaceSize];
                fPrefixes = prefixes;
            }
            String prefix = null;
            boolean unique = true;
            for (int i = 2; i < (fNamespaceSize-2); i += 2) {
                prefix = fNamespace[i + 2];
                for (int k=0;k<count;k++){
                    if (fPrefixes[k]==prefix){
                        unique = false;
                        break;
                    }
                }
                if (unique){
                    fPrefixes[count++] = prefix;
                }
                unique = true;
            }
            return new Prefixes(fPrefixes, count);
        }

    protected final class Prefixes implements Enumeration<String> {
        private String[] prefixes;
        private int counter = 0;
        private int size = 0;

                /**
                 * Constructor for Prefixes.
                 */
                public Prefixes(String [] prefixes, int size) {
                        this.prefixes = prefixes;
            this.size = size;
                }

       /**
                 * @see java.util.Enumeration#hasMoreElements()
                 */
                public boolean hasMoreElements() {
                        return (counter< size);
                }

                /**
                 * @see java.util.Enumeration#nextElement()
                 */
                public String nextElement() {
                    if (counter< size){
                        return fPrefixes[counter++];
                    }
                    throw new NoSuchElementException("Illegal access to Namespace prefixes enumeration.");
                }

        public String toString(){
            StringBuilder buf = new StringBuilder();
            for (int i=0;i<size;i++){
                buf.append(prefixes[i]);
                buf.append(" ");
            }

            return buf.toString();
        }

}

} // class NamespaceSupport
