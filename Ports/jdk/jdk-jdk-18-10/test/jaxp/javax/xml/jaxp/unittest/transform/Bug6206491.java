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

package transform;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/*
 * @test
 * @bug 6206491
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6206491
 * @run testng/othervm transform.Bug6206491
 * @summary Test key searches over more than one document.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6206491 {

    private String getResource(String s) {
        return getClass().getResource(s).toString();

    }

    @Test
    public void test() {
        try {
            Document document = getNewXmlDoc(new File(getClass().getResource("Bug6206491.xml").getFile()));

            xmlxsl2html(TransformerFactory.newInstance(), new File(getClass().getResource("Bug6206491.xsl").getFile()), document);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            Assert.fail(ex.toString());
        }
    }

    void xmlxsl2html(TransformerFactory tFactory, File xslFile, Document document) throws Exception {
        try {
            // tFactory.setAttribute("generate-translet", Boolean.TRUE);
        } catch (Exception e) {
            // Ignore
        }

        try {
            StreamSource stylesource = new StreamSource(xslFile);
            Transformer transformer = tFactory.newTransformer(stylesource);

            transformer.clearParameters();

            DOMSource source = new DOMSource(document);

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            transformer.transform(source, result);
            String s = sw.toString();
            Assert.assertFalse(s.contains("<must-be-one>0</must-be-one>"));
        } catch (TransformerConfigurationException ex) {
            throw ex;

        } catch (TransformerException ex) {
            throw ex;
        }
    }

    Document getNewXmlDoc(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                public void fatalError(SAXParseException e) throws SAXParseException {
                    throw e;
                }

                public void error(SAXParseException e) throws SAXParseException {
                    throw e;
                }

                public void warning(SAXParseException e) throws SAXParseException {
                    throw e;
                }
            });
            return builder.parse(xmlFile);

        } catch (SAXException ex) {
            throw ex;
        } catch (ParserConfigurationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        }
    }

}
