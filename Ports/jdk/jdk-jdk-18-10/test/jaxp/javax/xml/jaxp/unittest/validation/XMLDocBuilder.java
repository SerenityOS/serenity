/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package validation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLDocBuilder {

    private DocumentBuilderFactory factory = null;
    private DocumentBuilder builder = null;
    private Document doc = null;
    private Reader reader = null;
    private Reader schema = null;
    private String encoding = null;
    private String entityPath = null;

    public XMLDocBuilder(String file, String encoding, String schema) {
        this.encoding = encoding;
        reader = getReaderFromSystemResource(file, encoding);
        this.schema = getReaderFromSystemResource(schema, encoding);
    }

    public Document getDocument() {
        if (reader == null)
            return null;

        try {
            factory = DocumentBuilderFactory.newInstance();

            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new myErrorHandler());
            builder.setEntityResolver(new myEntityResolver());

            InputSource source = new InputSource(reader);
            source.setEncoding(encoding);

            try {
                doc = builder.parse(source);
                new XMLSchemaValidator(doc, schema).validate();

            } catch (SAXException e) {
                System.err.println(getClass() + " SAXException: " + e.getMessage());
                return null;
            } catch (IOException e) {
                System.err.println(getClass() + " IOException: " + e.getMessage());
                return null;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                System.err.println(e.getCause().getLocalizedMessage());
                return null;
            }

        } catch (ParserConfigurationException e) {
            System.err.println(getClass() + " ParserConfigurationException: " + e.getMessage());
            return null;
        }
        return doc;
    }

    public Reader getReaderFromSystemResource(String file, String encoding) {

        try {
            return new InputStreamReader(getClass().getResourceAsStream(file), encoding);
        } catch (UnsupportedEncodingException e) {
            System.err.println(getClass() + " UnsupportedEncodingException: " + e.getMessage());
        } catch (IOException e) {
            System.err.println(getClass() + " IOException: " + e.getMessage());
        }
        return null;
    }

    public void setEntityPath(String entityPath) {
        this.entityPath = entityPath;
    }

    private class myErrorHandler implements ErrorHandler {

        public void warning(SAXParseException e) {
            showErrorMessage(e);
        }

        public void error(SAXParseException e) {
            showErrorMessage(e);
        }

        public void fatalError(SAXParseException e) {
            showErrorMessage(e);
        }

        private void showErrorMessage(SAXParseException e) {
            System.err.println(getClass() + " SAXParseException" + e.getMessage());
            System.err.println("Line: " + e.getLineNumber() + " Column: " + e.getColumnNumber());
        }
    }

    private class myEntityResolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId) {
            if (entityPath == null)
                return null;

            systemId = entityPath + systemId.subSequence(systemId.lastIndexOf("/"), systemId.length());

            return new InputSource(systemId);
        }
    }
}
