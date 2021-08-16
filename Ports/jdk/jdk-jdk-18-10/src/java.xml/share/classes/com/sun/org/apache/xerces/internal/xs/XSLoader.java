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

package com.sun.org.apache.xerces.internal.xs;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.ls.LSInput;

/**
 * An interface that provides a method to load XML Schema documents. This
 * interface uses the DOM Level 3 Core and Load and Save interfaces.
 */
public interface XSLoader {
    /**
     *  The configuration of a document. It maintains a table of recognized
     * parameters. Using the configuration, it is possible to change the
     * behavior of the load methods. The configuration may support the
     * setting of and the retrieval of the following non-boolean parameters
     * defined on the <code>DOMConfiguration</code> interface:
     * <code>error-handler</code> (<code>DOMErrorHandler</code>) and
     * <code>resource-resolver</code> (<code>LSResourceResolver</code>).
     * <br> The following list of boolean parameters is defined:
     * <dl>
     * <dt>
     * <code>"validate"</code></dt>
     * <dd>
     * <dl>
     * <dt><code>true</code></dt>
     * <dd>[required] (default) Validate an XML
     * Schema during loading. If validation errors are found, the error
     * handler is notified. </dd>
     * <dt><code>false</code></dt>
     * <dd>[optional] Do not
     * report errors during the loading of an XML Schema document. </dd>
     * </dl></dd>
     * </dl>
     */
    public DOMConfiguration getConfig();

    /**
     * Parses the content of XML Schema documents specified as the list of URI
     * references. If the URI contains a fragment identifier, the behavior
     * is not defined by this specification.
     * @param uriList The list of URI locations.
     * @return An XSModel representing the schema documents.
     */
    public XSModel loadURIList(StringList uriList);

    /**
     *  Parses the content of XML Schema documents specified as a list of
     * <code>LSInput</code>s.
     * @param is  The list of <code>LSInput</code>s from which the XML
     *   Schema documents are to be read.
     * @return An XSModel representing the schema documents.
     */
    public XSModel loadInputList(LSInputList is);

    /**
     * Parse an XML Schema document from a location identified by a URI
     * reference. If the URI contains a fragment identifier, the behavior is
     * not defined by this specification.
     * @param uri The location of the XML Schema document to be read.
     * @return An XSModel representing this schema.
     */
    public XSModel loadURI(String uri);

    /**
     *  Parse an XML Schema document from a resource identified by a
     * <code>LSInput</code> .
     * @param is  The <code>LSInput</code> from which the source
     *   document is to be read.
     * @return An XSModel representing this schema.
     */
    public XSModel load(LSInput is);

}
