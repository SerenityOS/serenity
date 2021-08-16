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

/**
 * <p>A light wrapper around an <code>XMLLocator</code>.</p>
 *
 * @author Michael Glavassevich, IBM
 *
 * @version $Id: XMLLocatorWrapper.java 533423 2007-04-28 20:47:15Z mrglavas $
 */
public final class XMLLocatorWrapper implements XMLLocator {

    private XMLLocator fLocator = null;

    public XMLLocatorWrapper() {}

    public void setLocator(XMLLocator locator) {
        fLocator = locator;
    }

    public XMLLocator getLocator() {
        return fLocator;
    }

    /*
     * XMLLocator methods
     */

    public String getPublicId() {
        if (fLocator != null) {
            return fLocator.getPublicId();
        }
        return null;
    }

    public String getLiteralSystemId() {
        if (fLocator != null) {
            return fLocator.getLiteralSystemId();
        }
        return null;
    }

    public String getBaseSystemId() {
        if (fLocator != null) {
            return fLocator.getBaseSystemId();
        }
        return null;
    }

    public String getExpandedSystemId() {
        if (fLocator != null) {
            return fLocator.getExpandedSystemId();
        }
        return null;
    }

    public int getLineNumber() {
        if (fLocator != null) {
            return fLocator.getLineNumber();
        }
        return -1;
    }

    public int getColumnNumber() {
        if (fLocator != null) {
            return fLocator.getColumnNumber();
        }
        return -1;
    }

    public int getCharacterOffset() {
        if (fLocator != null) {
            return fLocator.getCharacterOffset();
        }
        return -1;
    }

    public String getEncoding() {
        if (fLocator != null) {
            return fLocator.getEncoding();
        }
        return null;
    }

    public String getXMLVersion() {
        if (fLocator != null) {
            return fLocator.getXMLVersion();
        }
        return null;
    }
}
