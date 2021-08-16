/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: SerializationHandler.java,v 1.2.4.1 2005/09/15 08:15:22 suresh_emailid Exp $
 */
package com.sun.org.apache.xml.internal.serializer;

import java.io.IOException;

import javax.xml.transform.Transformer;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;

/**
 * This interface is the one that a serializer implements. It is a group of
 * other interfaces, such as ExtendedContentHandler, ExtendedLexicalHandler etc.
 * In addition there are other methods, such as reset().
 *
 * This class is public only because it is used in another package,
 * it is not a public API.
 *
 * @xsl.usage internal
 * @LastModified: May 2021
 */
public interface SerializationHandler
    extends
        ExtendedContentHandler,
        ExtendedLexicalHandler,
        XSLOutputAttributes,
        DeclHandler,
        org.xml.sax.DTDHandler,
        ErrorHandler,
        DOMSerializer,
        Serializer
{
    /**
     * Set the SAX Content handler that the serializer sends its output to. This
     * method only applies to a ToSAXHandler, not to a ToStream serializer.
     *
     * @see Serializer#asContentHandler()
     * @see ToSAXHandler
     */
    public void setContentHandler(ContentHandler ch);

    public void close();

    /**
     * Notify that the serializer should take this DOM node as input to be
     * serialized.
     *
     * @param node the DOM node to be serialized.
     * @throws IOException
     */
    public void serialize(Node node) throws IOException;
    /**
     * Turns special character escaping on/off.
     *
     * Note that characters will
     * never, even if this option is set to 'true', be escaped within
     * CDATA sections in output XML documents.
     *
     * @param escape true if escaping is to be set on.
     */
    public boolean setEscaping(boolean escape) throws SAXException;

    /**
     * Set the number of spaces to indent for each indentation level.
     * @param spaces the number of spaces to indent for each indentation level.
     */
    public void setIndentAmount(int spaces);

    /**
     * Set the transformer associated with the serializer.
     * @param transformer the transformer associated with the serializer.
     */
    public void setTransformer(Transformer transformer);

    /**
     * Get the transformer associated with the serializer.
     * @return Transformer the transformer associated with the serializer.
     */
    public Transformer getTransformer();

    /**
     * Used only by TransformerSnapshotImpl to restore the serialization
     * to a previous state.
     *
     * @param mappings NamespaceMappings
     */
    public void setNamespaceMappings(NamespaceMappings mappings);

    /**
     * A SerializationHandler accepts SAX-like events, so
     * it can accumulate attributes or namespace nodes after
     * a startElement().
     * <p>
     * If the SerializationHandler has a Writer or OutputStream,
     * a call to this method will flush such accumulated
     * events as a closed start tag for an element.
     * <p>
     * If the SerializationHandler wraps a ContentHandler,
     * a call to this method will flush such accumulated
     * events as a SAX (not SAX-like) calls to
     * startPrefixMapping() and startElement().
     * <p>
     * If one calls endDocument() then one need not call
     * this method since a call to endDocument() will
     * do what this method does. However, in some
     * circumstances, such as with document fragments,
     * endDocument() is not called and it may be
     * necessary to call this method to flush
     * any pending events.
     * <p>
     * For performance reasons this method should not be called
     * very often.
     */
    public void flushPending() throws SAXException;

    /**
     * Default behavior is to expand DTD entities,
     * that is the initall default value is true.
     * @param expand true if DTD entities are to be expanded,
     * false if they are to be left as DTD entity references.
     */
    public void setDTDEntityExpansion(boolean expand);

    /**
     * Specify if the output will be treated as a standalone  property
     * @param b true if the property "jdk.xml.xsltcIsStandalone" is set to yes
     * @see JdkConstants.SP_XSLTC_IS_Standalone
     */
    public void setIsStandalone(boolean b);

}
