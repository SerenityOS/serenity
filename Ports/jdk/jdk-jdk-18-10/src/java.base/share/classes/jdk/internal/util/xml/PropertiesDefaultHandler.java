/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util.xml;

import java.io.*;
import java.nio.charset.Charset;
import java.util.InvalidPropertiesFormatException;
import java.util.Map.Entry;
import java.util.Properties;
import jdk.internal.org.xml.sax.Attributes;
import jdk.internal.org.xml.sax.InputSource;
import jdk.internal.org.xml.sax.SAXException;
import jdk.internal.org.xml.sax.SAXParseException;
import jdk.internal.org.xml.sax.helpers.DefaultHandler;
import jdk.internal.util.xml.impl.SAXParserImpl;
import jdk.internal.util.xml.impl.XMLStreamWriterImpl;

/**
 * A class used to aid in Properties load and save in XML. This class is
 * re-implemented using a subset of SAX
 *
 * @author Joe Wang
 * @since 1.8
 */
public class PropertiesDefaultHandler extends DefaultHandler {

    // Elements specified in the properties.dtd
    private static final String ELEMENT_ROOT = "properties";
    private static final String ELEMENT_COMMENT = "comment";
    private static final String ELEMENT_ENTRY = "entry";
    private static final String ATTR_KEY = "key";
    // The required DTD URI for exported properties
    private static final String PROPS_DTD_DECL =
        "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">";
    private static final String PROPS_DTD_URI =
        "http://java.sun.com/dtd/properties.dtd";
    private static final String PROPS_DTD =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!-- DTD for properties -->"
            + "<!ELEMENT properties ( comment?, entry* ) >"
            + "<!ATTLIST properties"
            + " version CDATA #FIXED \"1.0\">"
            + "<!ELEMENT comment (#PCDATA) >"
            + "<!ELEMENT entry (#PCDATA) >"
            + "<!ATTLIST entry "
            + " key CDATA #REQUIRED>";
    /**
     * Version number for the format of exported properties files.
     */
    private static final String EXTERNAL_XML_VERSION = "1.0";
    private Properties properties;

    public void load(Properties props, InputStream in)
        throws IOException, InvalidPropertiesFormatException, UnsupportedEncodingException
    {
        this.properties = props;

        try {
            SAXParser parser = new SAXParserImpl();
            parser.parse(in, this);
        } catch (SAXException saxe) {
            throw new InvalidPropertiesFormatException(saxe);
        }

        /**
         * String xmlVersion = propertiesElement.getAttribute("version"); if
         * (xmlVersion.compareTo(EXTERNAL_XML_VERSION) > 0) throw new
         * InvalidPropertiesFormatException( "Exported Properties file format
         * version " + xmlVersion + " is not supported. This java installation
         * can read" + " versions " + EXTERNAL_XML_VERSION + " or older. You" +
         * " may need to install a newer version of JDK.");
         */
    }

    public void store(Properties props, OutputStream os, String comment, Charset charset)
        throws IOException
    {
        try {
            XMLStreamWriter writer = new XMLStreamWriterImpl(os, charset);
            writer.writeStartDocument();
            writer.writeDTD(PROPS_DTD_DECL);
            writer.writeStartElement(ELEMENT_ROOT);
            if (comment != null && !comment.isEmpty()) {
                writer.writeStartElement(ELEMENT_COMMENT);
                writer.writeCharacters(comment);
                writer.writeEndElement();
            }

            synchronized(props) {
                for (Entry<Object, Object> e : props.entrySet()) {
                    final Object k = e.getKey();
                    final Object v = e.getValue();
                    if (k instanceof String && v instanceof String) {
                        writer.writeStartElement(ELEMENT_ENTRY);
                        writer.writeAttribute(ATTR_KEY, (String)k);
                        writer.writeCharacters((String)v);
                        writer.writeEndElement();
                    } else {
                        throw new ClassCastException(
                                "Keys and values in Properties must be Strings");
                    }
                }
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            if (e.getCause() instanceof UnsupportedEncodingException) {
                throw (UnsupportedEncodingException) e.getCause();
            }
            throw new IOException(e);
        }

    }
    ////////////////////////////////////////////////////////////////////
    // Validate while parsing
    ////////////////////////////////////////////////////////////////////
    static final String ALLOWED_ELEMENTS = "comment, entry";
    static final String ALLOWED_COMMENT = "comment";
    ////////////////////////////////////////////////////////////////////
    // Handler methods
    ////////////////////////////////////////////////////////////////////
    StringBuilder buf = new StringBuilder();
    boolean sawRoot = false; // whether a valid root element exists
    boolean sawComment = false;
    boolean validEntry = false;
    String key;
    String rootElm;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        if (sawRoot) {
            if (!ALLOWED_ELEMENTS.contains(qName)) {
                fatalError(new SAXParseException("Element type \"" + qName + "\" must be declared.", null));
            }
        } else {
            // check whether the root has been declared in the DTD
            if (rootElm == null) {
                fatalError(new SAXParseException("An XML properties document must contain"
                    + " the DOCTYPE declaration as defined by java.util.Properties.", null));
            }

            // check whether the element name matches the declaration
            if (!rootElm.equals(qName)) {
                fatalError(new SAXParseException("Document root element \"" + qName
                    + "\", must match DOCTYPE root \"" + rootElm + "\"", null));
            }

            // this is a valid root element
            sawRoot = true;
        }

        if (qName.equals(ELEMENT_ENTRY)) {
            validEntry = true;
            key = attributes.getValue(ATTR_KEY);
            if (key == null) {
                fatalError(new SAXParseException("Attribute \"key\" is required and " +
                    "must be specified for element type \"entry\"", null));
            }
        } else if (qName.equals(ALLOWED_COMMENT)) {
            if (sawComment) {
                fatalError(new SAXParseException("Only one comment element may be allowed. "
                    + "The content of element type \"properties\" must match \"(comment?,entry*)\"", null));
            }
            sawComment = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (validEntry) {
            buf.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!ALLOWED_ELEMENTS.contains(qName) && !ELEMENT_ROOT.equals(qName)) {
            fatalError(new SAXParseException("Element: " + qName +
                " is invalid, must match  \"(comment?,entry*)\".", null));
        }

        if (validEntry) {
            properties.setProperty(key, buf.toString());
            buf.delete(0, buf.length());
            validEntry = false;
        }
    }

    @Override
    public InputSource resolveEntity(String pubid, String sysid)
        throws SAXException, IOException {
        {
            if (sysid.equals(PROPS_DTD_URI)) {
                // The properties DTD is known to the handler, no need to parse it
                return null;
            }
            throw new SAXException("Invalid system identifier: " + sysid);
        }
    }

    @Override
    public void error(SAXParseException x) throws SAXException {
        throw x;
    }

    @Override
    public void fatalError(SAXParseException x) throws SAXException {
        throw x;
    }

    @Override
    public void warning(SAXParseException x) throws SAXException {
        throw x;
    }

    // SAX2 extension from DTDHandler

    @Override
    public void startDTD (String name, String publicId, String systemId) throws SAXException
    {
        if (!ELEMENT_ROOT.equals(name) || !PROPS_DTD_URI.equals(systemId)) {
            fatalError(new SAXParseException("An XML properties document must contain"
                + " the DOCTYPE declaration as defined by java.util.Properties.", null));
        }
        rootElm = name;
    }

    @Override
    public void startInternalSub () throws SAXException
    {
        fatalError(new SAXParseException("Internal DTD subset is not allowed. " +
            "The Properties XML document must have the following DOCTYPE declaration: \n" +
            PROPS_DTD_DECL, null));
    }
}
