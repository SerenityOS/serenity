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

/**
 * Wraps SAX {@link Locator} into Xerces {@link XMLLocator}.
 *
 * @author
 *     Kohsuke Kawaguchi
 */
public class LocatorWrapper implements XMLLocator {

    private final Locator locator;

    public LocatorWrapper( Locator _loc ) { this.locator=_loc; }

    public int getColumnNumber()  { return locator.getColumnNumber(); }
    public int getLineNumber()    { return locator.getLineNumber(); }
    public String getBaseSystemId() { return null; }
    public String getExpandedSystemId() { return locator.getSystemId(); }
    public String getLiteralSystemId() { return locator.getSystemId(); }
    public String getPublicId()   { return locator.getPublicId(); }
    public String getEncoding() { return null; }

    /**
     * <p>Returns the character offset,
     * or <code>-1</code>,
     * if no character offset is available.<p>
     *
     * <p>As this information is not available from
     * {@link org.xml.sax.Locator},
     * always return <code>-1</code>.</p>
     */
    public int getCharacterOffset() {
        return -1;
    }

    /**
     * <p>Returns the XML version of the current entity.</p>
     *
     * <p>As this information is not available from
     * {@link org.xml.sax.Locator},
     * always return <code>null</code>.</p>
     */
    public String getXMLVersion() {
        return null;
    }

}
