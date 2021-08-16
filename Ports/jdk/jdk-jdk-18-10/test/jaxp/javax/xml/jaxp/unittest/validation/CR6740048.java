/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/*
 * @test
 * @bug 6740048
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.CR6740048
 * @run testng/othervm validation.CR6740048
 * @summary Test DocumentBuilder can be reused when the DocumentBuilderFactory sets schema.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR6740048 {
    private static final String TAG_INFO = "containerInfo";
    private static final String SCHEMA_LANGUAGE_URL = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String SCHEMA_SOURCE_URL = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    private static final String XML_SCHEMA_URL = "http://www.w3.org/2001/XMLSchema";

    @Test
    public final void testReusingDocumentBuilder() {

        try {
            InputStream xsd = this.getClass().getResourceAsStream("CR6740048.xsd");
            // create document builder
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);

            if (xsd != null) {
                docBuilderFactory.setValidating(true);
                docBuilderFactory.setAttribute(SCHEMA_LANGUAGE_URL, XML_SCHEMA_URL);
                docBuilderFactory.setAttribute(SCHEMA_SOURCE_URL, xsd);
            }

            final DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new ErrorHandler() {

                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });

            // TODO set the system properties in order to match the ones from
            // the server environment
            /**
             * Properties props = new Properties();
             * props.load(loader.getResourceAsStream("sysprops.properties"));
             * System.setProperties(props);
             */

            // now parse the document
            // InputStream is = loader.getResourceAsStream("CR6740048.xml");
            InputStream is = this.getClass().getResourceAsStream("CR6740048.xml");
            try {
                Document doc = documentBuilder.parse(is);
            } catch (Exception se) {

                se.printStackTrace();
                Assert.fail(se.getMessage());

            } finally {
                is.close();
            }

            // now use the parser object as second time
            // is = loader.getResourceAsStream("CR6740048.xml");
            is = this.getClass().getResourceAsStream("CR6740048.xml");
            try {
                Document doc = documentBuilder.parse(is);
            } catch (Exception se) {

                se.printStackTrace();
                Assert.fail(se.getMessage());

            } finally {
                is.close();
            }

            System.err.println("Parse successful");

            is.close();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
