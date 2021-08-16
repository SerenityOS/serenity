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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6467424
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug6467424Test
 * @run testng/othervm validation.Bug6467424Test
 * @summary Test Validator augments the default delement value if feature element-default is on.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6467424Test {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    @Test
    public void test() {
        // System.setSecurityManager(new SecurityManager());
        try {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            File schemaLocation = new File(getClass().getResource("Bug6467424.xsd").getFile());
            Schema schema = factory.newSchema(schemaLocation);
            Validator validator = schema.newValidator();

            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true); // never forget this
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(new File(getClass().getResource("Bug6467424.xml").getFile()));

            DOMSource source = new DOMSource(doc);
            DOMResult result = new DOMResult();

            validator.validate(source, result);
            Document augmented = (Document) result.getNode();

            TransformerFactory tFactory = TransformerFactory.newInstance();

            Transformer transformer = tFactory.newTransformer();

            DOMSource domSource = new DOMSource(augmented);
            StringWriter sw = new StringWriter();
            // StreamResult streamResult = new StreamResult(System.out);
            StreamResult streamResult = new StreamResult(sw);
            transformer.transform(domSource, streamResult);
            String s = sw.toString();
            if (s.indexOf("Schema Validation") == -1) {
                Assert.fail("Failed: result is expected to be augmented");
            }
        }

        catch (TransformerConfigurationException e) {
            // e.printStackTrace();
            System.out.println(e.getMessage());
        } catch (TransformerException e) {
            System.out.println(e.getMessage());
        } catch (SAXException e) {
            System.out.println(e.getMessage());
        } catch (ParserConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * this test is to make sure the element-default feature works for
     * validation using DOM parser reference: parser feature:
     * http://xerces.apache.org/xerces2-j/feature.html#validation
     */
    @Test
    public void testDOMValidation() throws Exception {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            // domFactory.setFeature("http://xml.org/sax/features/validation",
            // true);
            domFactory.setNamespaceAware(true); // never forget this
            domFactory.setValidating(true);

            domFactory.setAttribute(SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
            domFactory.setAttribute(SCHEMA_SOURCE, Bug6467424Test.class.getResource("Bug6467424.xsd").toExternalForm());

            domFactory.setFeature("http://apache.org/xml/features/validation/schema", true);
            domFactory.setFeature("http://apache.org/xml/features/validation/schema/element-default", true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(new File(getClass().getResource("Bug6467424.xml").getFile()));

            TransformerFactory tFactory = TransformerFactory.newInstance();

            Transformer transformer = tFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            StringWriter sw = new StringWriter();
            // StreamResult streamResult = new StreamResult(System.out);
            StreamResult streamResult = new StreamResult(sw);
            transformer.transform(domSource, streamResult);
            String s = sw.toString();
            if (s.indexOf("Schema Validation") == -1) {
                Assert.fail("Failed: result is expected to be augmented");
            }

        }

        catch (TransformerConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (TransformerException e) {
            System.out.println(e.getMessage());
        } catch (SAXException e) {
            System.out.println(e.getMessage());
        } catch (ParserConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testDOMValidation1() throws Exception {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            // domFactory.setFeature("http://xml.org/sax/features/validation",
            // true);
            domFactory.setNamespaceAware(true); // never forget this
            domFactory.setValidating(true);

            domFactory.setAttribute(SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
            domFactory.setAttribute(SCHEMA_SOURCE, Bug6467424Test.class.getResource("Bug6467424.xsd").toExternalForm());

            domFactory.setFeature("http://apache.org/xml/features/validation/schema", true);
            domFactory.setFeature("http://apache.org/xml/features/validation/schema/element-default", false);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(new File(getClass().getResource("Bug6467424.xml").getFile()));

            TransformerFactory tFactory = TransformerFactory.newInstance();

            Transformer transformer = tFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            StringWriter sw = new StringWriter();
            // StreamResult streamResult = new StreamResult(System.out);
            StreamResult streamResult = new StreamResult(sw);
            transformer.transform(domSource, streamResult);
            String s = sw.toString();
            if (s.indexOf("Schema Validation") > 0) {
                Assert.fail("Failed: result is not expected to be augmented");
            }

        }

        catch (TransformerConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (TransformerException e) {
            System.out.println(e.getMessage());
        } catch (SAXException e) {
            System.out.println(e.getMessage());
        } catch (ParserConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
