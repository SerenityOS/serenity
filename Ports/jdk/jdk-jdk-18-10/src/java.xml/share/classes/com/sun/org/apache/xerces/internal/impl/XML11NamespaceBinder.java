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

package com.sun.org.apache.xerces.internal.impl;


/**
 * This class performs namespace binding on the startElement and endElement
 * method calls in accordance with Namespaces in XML 1.1.  It extends the standard,
 * Namespace-1.0-compliant binder in order to do this.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 *
 */
public class XML11NamespaceBinder extends XMLNamespaceBinder {

    //
    // Constants
    //

    //
    // Data
    //

    //
    // Constructors
    //

    /** Default constructor. */
    public XML11NamespaceBinder() {
    } // <init>()
    //
    // Public methods
    //

    //
    // Protected methods
    //

    // returns true iff the given prefix is bound to "" *and*
    // this is disallowed by the version of XML namespaces in use.
    protected boolean prefixBoundToNullURI(String uri, String localpart) {
        return false;
    } // prefixBoundToNullURI(String, String):  boolean

} // class XML11NamespaceBinder
