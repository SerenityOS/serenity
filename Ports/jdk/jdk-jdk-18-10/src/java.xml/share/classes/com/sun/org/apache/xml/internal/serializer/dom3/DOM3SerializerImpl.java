/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

import java.io.IOException;

import com.sun.org.apache.xml.internal.serializer.DOM3Serializer;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.serializer.utils.WrappedRuntimeException;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSSerializerFilter;

/**
 * This class implements the DOM3Serializer interface.
 *
 * @xsl.usage internal
 */
public final class DOM3SerializerImpl implements DOM3Serializer {

    /**
     * Private class members
     */
    // The DOMErrorHandler
    private DOMErrorHandler fErrorHandler;

    // A LSSerializerFilter
    private LSSerializerFilter fSerializerFilter;

    // The end-of-line character sequence
    private String fNewLine;

    // A SerializationHandler ex. an instance of ToXMLStream
    private SerializationHandler fSerializationHandler;

    /**
     * Constructor
     *
     * @param handler An instance of the SerializationHandler interface.
     */
    public DOM3SerializerImpl(SerializationHandler handler) {
        fSerializationHandler = handler;
    }

    // Public memebers

    /**
     * Returns a DOMErrorHandler set on the DOM Level 3 Serializer.
     *
     * This interface is a public API.
     *
     * @return A Level 3 DOMErrorHandler
     */
    public DOMErrorHandler getErrorHandler() {
        return fErrorHandler;
    }

    /**
     * Returns a LSSerializerFilter set on the DOM Level 3 Serializer to filter nodes
     * during serialization.
     *
     * This interface is a public API.
     *
     * @return The Level 3 LSSerializerFilter
     */
    public LSSerializerFilter getNodeFilter() {
        return fSerializerFilter;
    }


    /**
     * Serializes the Level 3 DOM node by creating an instance of DOM3TreeWalker
     * which traverses the DOM tree and invokes handler events to serialize
     * the DOM NOde. Throws an exception only if an I/O exception occured
     * while serializing.
     * This interface is a public API.
     *
     * @param node the Level 3 DOM node to serialize
     * @throws IOException if an I/O exception occured while serializing
     */
    public void serializeDOM3(Node node) throws IOException {
        try {
            DOM3TreeWalker walker = new DOM3TreeWalker(fSerializationHandler,
                    fErrorHandler, fSerializerFilter, fNewLine);

            walker.traverse(node);
        } catch (org.xml.sax.SAXException se) {
            throw new WrappedRuntimeException(se);
        }
    }

    /**
     * Sets a DOMErrorHandler on the DOM Level 3 Serializer.
     *
     * This interface is a public API.
     *
     * @param handler the Level 3 DOMErrorHandler
     */
    public void setErrorHandler(DOMErrorHandler handler) {
        fErrorHandler = handler;
    }

    /**
     * Sets a LSSerializerFilter on the DOM Level 3 Serializer to filter nodes
     * during serialization.
     *
     * This interface is a public API.
     *
     * @param filter the Level 3 LSSerializerFilter
     */
    public void setNodeFilter(LSSerializerFilter filter) {
        fSerializerFilter = filter;
    }

    /**
     * Sets a SerializationHandler on the DOM Serializer.
     *
     * This interface is a public API.
     *
     * @param handler An instance of SerializationHandler
     */
    public void setSerializationHandler(SerializationHandler handler) {
        fSerializationHandler = handler;
    }

    /**
     * Sets the new line character to be used during serialization
     * @param newLine a String that is the end-of-line character sequence to be
     * used in serialization.
     */
    public void setNewLine(String newLine) {
        fNewLine = newLine;
    }
}
