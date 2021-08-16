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

import javax.xml.transform.Source;

import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.util.URI.MalformedURIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;

/**
 * {@link Source} that represents an {@link XMLInputSource}.
 *
 * <p>
 * Ideally, we should be able to have {@link XMLInputSource}
 * derive from {@link Source}, but the way
 * the {@link XMLInputSource#getSystemId()} method works is
 * different from the way {@link Source#getSystemId()} method works.
 *
 * <p>
 * In a long run, we should make them consistent so that we can
 * get rid of this awkward adaptor class.
 *
 * @author
 *     Kohsuke Kawaguchi
 */
public final class XMLInputSourceAdaptor implements Source {
    /**
     * the actual source information.
     */
    public final XMLInputSource fSource;

    public XMLInputSourceAdaptor( XMLInputSource core ) {
        fSource = core;
    }

    public void setSystemId(String systemId) {
        fSource.setSystemId(systemId);
    }

    public String getSystemId() {
        try {
            return XMLEntityManager.expandSystemId(
                    fSource.getSystemId(), fSource.getBaseSystemId(), false);
        } catch (MalformedURIException e) {
            return fSource.getSystemId();
        }
    }
}
