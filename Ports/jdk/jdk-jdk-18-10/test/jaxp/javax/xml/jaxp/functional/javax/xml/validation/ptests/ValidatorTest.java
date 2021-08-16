/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
package javax.xml.validation.ptests;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.validation.ptests.ValidationTestConst.XML_DIR;
import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.validation.ptests.ValidatorTest
 * @run testng/othervm javax.xml.validation.ptests.ValidatorTest
 * @summary Class containing the test cases for Validator API
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ValidatorTest {

    @BeforeClass
    public void setup() throws SAXException, IOException, ParserConfigurationException {
        schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(new File(XML_DIR + "test.xsd"));

        assertNotNull(schema);

        xmlFileUri = filenameToURL(XML_DIR + "test.xml");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        xmlDoc = dbf.newDocumentBuilder().parse(xmlFileUri);
    }

    @Test
    public void testValidateStreamSource() throws SAXException, IOException {
        Validator validator = getValidator();
        validator.setErrorHandler(new MyErrorHandler());
        validator.validate(getStreamSource());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testValidateNullSource() throws SAXException, IOException {
        Validator validator = getValidator();
        assertNotNull(validator);
        validator.validate(null);
    }

    @Test
    public void testErrorHandler() {
        Validator validator = getValidator();
        assertNull(validator.getErrorHandler(), "When Validator is created, initially ErrorHandler should not be set.");

        ErrorHandler mh = new MyErrorHandler();
        validator.setErrorHandler(mh);
        assertSame(validator.getErrorHandler(), mh);

    }

    @DataProvider(name = "source-result")
    public Object[][] getSourceAndResult() {
        return new Object[][] {
                { getStreamSource(), null },
                { getSAXSource(), getSAXResult() },
                { getDOMSource(), getDOMResult() },
                { getSAXSource(), null },
                { getDOMSource(), null } };
    }

    @Test(dataProvider = "source-result")
    public void testValidateWithResult(Source source, Result result) throws SAXException, IOException {
        Validator validator = getValidator();
        validator.validate(source, result);
    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testGetUnrecognizedProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        validator.getProperty(UNRECOGNIZED_NAME);

    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testSetUnrecognizedProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        validator.setProperty(UNRECOGNIZED_NAME, "test");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNullProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        assertNotNull(validator);
        validator.getProperty(null);

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        assertNotNull(validator);
        validator.setProperty(null, "test");
    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testGetUnrecognizedFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        validator.getFeature(UNRECOGNIZED_NAME);

    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testSetUnrecognizedFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        validator.setFeature(UNRECOGNIZED_NAME, true);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNullFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        assertNotNull(validator);
        validator.getFeature(null);

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        Validator validator = getValidator();
        assertNotNull(validator);
        validator.setFeature(null, true);
    }

    private Validator getValidator() {
        return schema.newValidator();
    }

    private Source getStreamSource() {
        return new StreamSource(xmlFileUri);
    }

    private Source getSAXSource() {
        return new SAXSource(new InputSource(xmlFileUri));
    }

    private Result getSAXResult() {
        SAXResult saxResult = new SAXResult();
        saxResult.setHandler(new DefaultHandler());
        return saxResult;
    }

    private Source getDOMSource() {
        return new DOMSource(xmlDoc);
    }

    private Result getDOMResult() {
        return new DOMResult();
    }

    private static final String UNRECOGNIZED_NAME = "http://xml.org/sax/features/namespace-prefixes";
    private String xmlFileUri;
    private Schema schema;
    private Document xmlDoc;

}
