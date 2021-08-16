/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.util.xml.impl;

import java.io.IOException;
import java.io.InputStream;
import jdk.internal.org.xml.sax.InputSource;
import jdk.internal.org.xml.sax.SAXException;
import jdk.internal.org.xml.sax.XMLReader;
import jdk.internal.org.xml.sax.helpers.DefaultHandler;
import jdk.internal.util.xml.SAXParser;

public class SAXParserImpl extends SAXParser {

    private ParserSAX parser;

    public SAXParserImpl() {
        super();
        parser = new ParserSAX();
    }

    /**
     * Returns the {@link org.xml.sax.XMLReader} that is encapsulated by the
     * implementation of this class.
     *
     * @return The XMLReader that is encapsulated by the
     *         implementation of this class.
     *
     * @throws SAXException If any SAX errors occur during processing.
     */
    public XMLReader getXMLReader()
            throws SAXException {
        return parser;
    }

    /**
     * Indicates whether or not this parser is configured to
     * understand namespaces.
     *
     * @return true if this parser is configured to
     *         understand namespaces; false otherwise.
     */
    public boolean isNamespaceAware() {
        return parser.mIsNSAware;
    }

    /**
     * Indicates whether or not this parser is configured to validate
     * XML documents.
     *
     * @return true if this parser is configured to validate XML
     *          documents; false otherwise.
     */
    public boolean isValidating() {
        return false;
    }

    /**
     * Parse the content of the given {@link java.io.InputStream}
     * instance as XML using the specified
     * {@link org.xml.sax.helpers.DefaultHandler}.
     *
     * @param src InputStream containing the content to be parsed.
     * @param handler The SAX DefaultHandler to use.
     * @exception IOException If any IO errors occur.
     * @exception IllegalArgumentException If the given InputStream or handler is null.
     * @exception SAXException If the underlying parser throws a
     * SAXException while parsing.
     * @see org.xml.sax.helpers.DefaultHandler
     */
    public void parse(InputStream src, DefaultHandler handler)
        throws SAXException, IOException
    {
        parser.parse(src, handler);
    }

    /**
     * Parse the content given {@link org.xml.sax.InputSource}
     * as XML using the specified
     * {@link org.xml.sax.helpers.DefaultHandler}.
     *
     * @param is The InputSource containing the content to be parsed.
     * @param handler The SAX DefaultHandler to use.
     * @exception IOException If any IO errors occur.
     * @exception IllegalArgumentException If the InputSource or handler is null.
     * @exception SAXException If the underlying parser throws a
     * SAXException while parsing.
     * @see org.xml.sax.helpers.DefaultHandler
     */
    public void parse(InputSource is, DefaultHandler handler)
        throws SAXException, IOException
    {
        parser.parse(is, handler);
    }
}
