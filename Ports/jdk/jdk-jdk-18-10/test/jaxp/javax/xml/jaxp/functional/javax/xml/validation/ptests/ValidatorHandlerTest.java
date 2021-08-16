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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.File;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.validation.ptests.ValidatorHandlerTest
 * @run testng/othervm javax.xml.validation.ptests.ValidatorHandlerTest
 * @summary Class containing the test cases for ValidatorHandler API
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ValidatorHandlerTest {
    @BeforeClass
    public void setup() throws SAXException {
        schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(new File(XML_DIR + "test.xsd"));

        assertNotNull(schema);
    }

    @Test
    public void testErrorHandler() {
        ValidatorHandler validatorHandler = getValidatorHandler();
        assertNull(validatorHandler.getErrorHandler(), "When ValidatorHandler is created, initially ErrorHandler should not be set.");

        ErrorHandler handler = new MyErrorHandler();
        validatorHandler.setErrorHandler(handler);
        assertSame(validatorHandler.getErrorHandler(), handler);

    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testGetUnrecognizedProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        ValidatorHandler validatorHandler = getValidatorHandler();
        validatorHandler.getProperty(FEATURE_NAME);

    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testSetUnrecognizedProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        ValidatorHandler validatorHandler = getValidatorHandler();
        validatorHandler.setProperty(FEATURE_NAME, "test");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNullProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        ValidatorHandler validatorHandler = getValidatorHandler();
        assertNotNull(validatorHandler);
        validatorHandler.getProperty(null);

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        ValidatorHandler validatorHandler = getValidatorHandler();
        assertNotNull(validatorHandler);
        validatorHandler.setProperty(null, "test");
    }

    public void testFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        ValidatorHandler validatorHandler = getValidatorHandler();
        assertFalse(validatorHandler.getFeature(FEATURE_NAME), "The feature should be false by default.");

        validatorHandler.setFeature(FEATURE_NAME, true);
        assertTrue(validatorHandler.getFeature(FEATURE_NAME), "The feature should be false by default.");

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNullFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        ValidatorHandler validatorHandler = getValidatorHandler();
        assertNotNull(validatorHandler);
        validatorHandler.getFeature(null);

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        ValidatorHandler validatorHandler = getValidatorHandler();
        assertNotNull(validatorHandler);
        validatorHandler.setFeature(null, true);
    }

    @Test
    public void testContentHandler() {
        ValidatorHandler validatorHandler = getValidatorHandler();
        assertNull(validatorHandler.getContentHandler(), "When ValidatorHandler is created, initially ContentHandler should not be set.");

        ContentHandler handler = new DefaultHandler();
        validatorHandler.setContentHandler(handler);
        assertSame(validatorHandler.getContentHandler(), handler);

        validatorHandler.setContentHandler(null);
        assertNull(validatorHandler.getContentHandler());

    }

    private ValidatorHandler getValidatorHandler() {
        return schema.newValidatorHandler();
    }

    private static final String FEATURE_NAME = "http://xml.org/sax/features/namespace-prefixes";

    private Schema schema;

}
