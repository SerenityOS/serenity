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

package com.sun.org.apache.xerces.internal.util;

import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import org.xml.sax.Locator;
import org.xml.sax.ext.Locator2;

/**
 * Wraps {@link XMLLocator} and make it look like a SAX {@link Locator}.
 *
 * @author Arnaud Le Hors, IBM
 * @author Andy Clark, IBM
 *
 */
public class LocatorProxy implements Locator2 {

    //
    // Data
    //

    /** XML locator. */
    private final XMLLocator fLocator;

    //
    // Constructors
    //

    /** Constructs an XML locator proxy. */
    public LocatorProxy(XMLLocator locator) {
        fLocator = locator;
    }

    //
    // Locator methods
    //

    /** Public identifier. */
    public String getPublicId() {
        return fLocator.getPublicId();
    }

    /** System identifier. */
    public String getSystemId() {
        return fLocator.getExpandedSystemId();
    }

    /** Line number. */
    public int getLineNumber() {
        return fLocator.getLineNumber();
    }

    /** Column number. */
    public int getColumnNumber() {
        return fLocator.getColumnNumber();
    }

    //
    // Locator2 methods
    //

    public String getXMLVersion() {
        return fLocator.getXMLVersion();
    }

    public String getEncoding() {
        return fLocator.getEncoding();
    }

}
