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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jaxp.library.JAXPDataProvider;

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
import org.xml.sax.SAXParseException;

/*
 * @test
 * @bug 8080907 8169778
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.validation.ptests.SchemaFactoryTest
 * @run testng/othervm javax.xml.validation.ptests.SchemaFactoryTest
 * @summary Class containing the test cases for SchemaFactory
 */
@Test(singleThreaded = true)
@Listeners({jaxp.library.FilePolicy.class})
public class SchemaFactoryTest {

    @BeforeClass
    public void setup() throws SAXException, IOException, ParserConfigurationException {
        sf = newSchemaFactory();
        assertNotNull(sf);

        ifac = XMLInputFactory.newInstance();

        xsd1 = Files.readAllBytes(Paths.get(XML_DIR + "test.xsd"));
        xsd2 = Files.readAllBytes(Paths.get(XML_DIR + "test1.xsd"));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        xsdDoc1 = db.parse(newInputStream(xsd1));
        xsdDoc2 = db.parse(newInputStream(xsd2));

        xml = Files.readAllBytes(Paths.get(XML_DIR + "test.xml"));
    }


    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] { { W3C_XML_SCHEMA_NS_URI, SCHEMA_FACTORY_CLASSNAME, null },
                { W3C_XML_SCHEMA_NS_URI, SCHEMA_FACTORY_CLASSNAME, this.getClass().getClassLoader() } };
    }

    /**
     * Test if newDefaultInstance() method returns an instance
     * of the expected factory.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testDefaultInstance() throws Exception {
        SchemaFactory sf1 = SchemaFactory.newDefaultInstance();
        SchemaFactory sf2 = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        assertNotSame(sf1, sf2, "same instance returned:");
        assertSame(sf1.getClass(), sf2.getClass(),
                  "unexpected class mismatch for newDefaultInstance():");
        assertEquals(sf1.getClass().getName(), DEFAULT_IMPL_CLASS);
        assertTrue(sf1.isSchemaLanguageSupported(W3C_XML_SCHEMA_NS_URI),
                   "isSchemaLanguageSupported(W3C_XML_SCHEMA_NS_URI):");
        assertFalse(sf1.isSchemaLanguageSupported(UNRECOGNIZED_NAME),
                   "isSchemaLanguageSupported(UNRECOGNIZED_NAME):");
    }

    /*
     * test for SchemaFactory.newInstance(java.lang.String schemaLanguage,
     * java.lang.String factoryClassName, java.lang.ClassLoader classLoader)
     * factoryClassName points to correct implementation of
     * javax.xml.validation.SchemaFactory , should return newInstance of
     * SchemaFactory
     */
    @Test(dataProvider = "parameters")
    public void testNewInstance(String schemaLanguage, String factoryClassName, ClassLoader classLoader) throws SAXException {
        SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI, SCHEMA_FACTORY_CLASSNAME, null);
        Schema schema = sf.newSchema();
        assertNotNull(schema);
    }

    /*
     * test for SchemaFactory.newInstance(java.lang.String schemaLanguage,
     * java.lang.String factoryClassName, java.lang.ClassLoader classLoader)
     * factoryClassName is null , should throw IllegalArgumentException
     */
    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewInstanceWithNullFactoryClassName(String factoryClassName, ClassLoader classLoader) {

        SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI, factoryClassName, classLoader);
    }

    /*
     * test for SchemaFactory.newInstance(java.lang.String schemaLanguage,
     * java.lang.String factoryClassName, java.lang.ClassLoader classLoader)
     * schemaLanguage is null , should throw NPE
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testNewInstanceWithNullSchemaLanguage() {
        SchemaFactory.newInstance(null, SCHEMA_FACTORY_CLASSNAME, this.getClass().getClassLoader());
    }

    /*
     * test for SchemaFactory.newInstance(java.lang.String schemaLanguage,
     * java.lang.String factoryClassName, java.lang.ClassLoader classLoader)
     * schemaLanguage is empty , should throw IllegalArgumentException
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNewInstanceWithEmptySchemaLanguage() {
        SchemaFactory.newInstance("", SCHEMA_FACTORY_CLASSNAME, this.getClass().getClassLoader());
    }


    @Test(expectedExceptions = SAXParseException.class)
    public void testNewSchemaDefault() throws SAXException, IOException {
        validate(sf.newSchema());
    }

    @Test
    public void testNewSchemaWithFile() throws SAXException, IOException {
        validate(sf.newSchema(new File(XML_DIR + "test.xsd")));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNewSchemaWithNullFile() throws SAXException {
        sf.newSchema((File) null);
    }

    @DataProvider(name = "valid-source")
    public Object[][] getValidSource() throws XMLStreamException {
        return new Object[][] {
                { streamSource(xsd1) },
                { saxSource(xsd1) },
                { domSource(xsdDoc1) },
                { staxStreamSource(xsd1) },
                { staxEventSource(xsd1) } };

    }

    @Test(dataProvider = "valid-source")
    public void testNewSchemaWithValidSource(Source schema) throws SAXException, IOException {
        validate(sf.newSchema(schema));
    }

    @DataProvider(name = "invalid-source")
    public Object[][] getInvalidSource() {
        return new Object[][] {
                { nullStreamSource() },
                { nullSaxSource() } };
    }

    @Test(dataProvider = "invalid-source", expectedExceptions = SAXParseException.class)
    public void testNewSchemaWithInvalidSource(Source schema) throws SAXException {
        sf.newSchema(schema);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNewSchemaWithNullSource() throws SAXException {
        sf.newSchema((Source)null);
    }

    @DataProvider(name = "valid-sources")
    public Object[][] getValidSources() {
        return new Object[][] {
                { streamSource(xsd1), streamSource(xsd2) },
                { saxSource(xsd1), saxSource(xsd2) },
                { domSource(xsdDoc1), domSource(xsdDoc2) } };

    }

    @Test(dataProvider = "valid-sources")
    public void testNewSchemaWithValidSourceArray(Source schema1, Source schema2) throws SAXException, IOException {
        validate(sf.newSchema(new Source[] { schema1, schema2 }));
    }

    @DataProvider(name = "invalid-sources")
    public Object[][] getInvalidSources() {
        return new Object[][] {
                { streamSource(xsd1), nullStreamSource() },
                { nullStreamSource(), nullStreamSource() },
                { saxSource(xsd1), nullSaxSource() },
                { nullSaxSource(), nullSaxSource() } };
    }

    @Test(dataProvider = "invalid-sources", expectedExceptions = SAXParseException.class)
    public void testNewSchemaWithInvalidSourceArray(Source schema1, Source schema2) throws SAXException {
        sf.newSchema(new Source[] { schema1, schema2 });
    }

    @DataProvider(name = "null-sources")
    public Object[][] getNullSources() {
        return new Object[][] {
                { new Source[] { domSource(xsdDoc1), null } },
                { new Source[] { null, null } },
                { null } };

    }

    @Test(dataProvider = "null-sources", expectedExceptions = NullPointerException.class)
    public void testNewSchemaWithNullSourceArray(Source[] schemas) throws SAXException {
        sf.newSchema(schemas);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNewSchemaWithNullUrl() throws SAXException {
        sf.newSchema((URL) null);
    }


    @Test
    public void testErrorHandler() {
        SchemaFactory sf = newSchemaFactory();
        assertNull(sf.getErrorHandler(), "When SchemaFactory is created, initially ErrorHandler should not be set.");

        ErrorHandler handler = new MyErrorHandler();
        sf.setErrorHandler(handler);
        assertSame(sf.getErrorHandler(), handler);

        sf.setErrorHandler(null);
        assertNull(sf.getErrorHandler());
    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testGetUnrecognizedProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        sf.getProperty(UNRECOGNIZED_NAME);

    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testSetUnrecognizedProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        sf.setProperty(UNRECOGNIZED_NAME, "test");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNullProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        assertNotNull(sf);
        sf.getProperty(null);

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullProperty() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        assertNotNull(sf);
        sf.setProperty(null, "test");
    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testGetUnrecognizedFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        sf.getFeature(UNRECOGNIZED_NAME);

    }

    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void testSetUnrecognizedFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        sf.setFeature(UNRECOGNIZED_NAME, true);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetNullFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        assertNotNull(sf);
        sf.getFeature(null);

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testSetNullFeature() throws SAXNotRecognizedException, SAXNotSupportedException {
        SchemaFactory sf = newSchemaFactory();
        assertNotNull(sf);
        sf.setFeature(null, true);
    }

    @DataProvider(name = "source-feature")
    public Object[][] getSourceFeature() {
        return new Object[][] {
                { StreamSource.FEATURE },
                { SAXSource.FEATURE },
                { DOMSource.FEATURE },
                { DOMSource.FEATURE } };

    }

    /*
     * Return true for each of the JAXP Source features to indicate that this
     * SchemaFactory supports all of the built-in JAXP Source types.
     */
    @Test(dataProvider = "source-feature")
    public void testSourceFeatureGet(String sourceFeature) throws Exception {
        assertTrue(newSchemaFactory().getFeature(sourceFeature));
    }

    /*
     * JAXP Source features are read-only because this SchemaFactory always
     * supports all JAXP Source types.
     */
    @Test(dataProvider = "source-feature", expectedExceptions = SAXNotSupportedException.class)
    public void testSourceFeatureSet(String sourceFeature) throws Exception {
        newSchemaFactory().setFeature(sourceFeature, false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidSchemaLanguage() {
        final String INVALID_SCHEMA_LANGUAGE = "http://relaxng.org/ns/structure/1.0";
        SchemaFactory.newInstance(INVALID_SCHEMA_LANGUAGE);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullSchemaLanguage() {
        SchemaFactory.newInstance(null);
    }

    private void validate(Schema schema) throws SAXException, IOException {
        schema.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
    }
    private InputStream newInputStream(byte[] xsd) {
        return new ByteArrayInputStream(xsd);
    }

    private Source streamSource(byte[] xsd) {
        return new StreamSource(newInputStream(xsd));
    }

    private Source nullStreamSource() {
        return new StreamSource((InputStream) null);
    }

    private Source saxSource(byte[] xsd) {
        return new SAXSource(new InputSource(newInputStream(xsd)));
    }

    private Source nullSaxSource() {
        return new SAXSource(new InputSource((InputStream) null));
    }

    private Source domSource(Document xsdDoc) {
        return new DOMSource(xsdDoc);
    }

    private Source staxStreamSource(byte[] xsd) throws XMLStreamException {
        return new StAXSource(ifac.createXMLStreamReader(newInputStream(xsd)));
    }

    private Source staxEventSource(byte[] xsd) throws XMLStreamException {
        return new StAXSource(ifac.createXMLEventReader(newInputStream(xsd)));
    }


    private SchemaFactory newSchemaFactory() {
        return SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    }

    private static final String UNRECOGNIZED_NAME = "http://xml.org/sax/features/namespace-prefixes";

    private static final String DEFAULT_IMPL_CLASS =
        "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory";

    private static final String SCHEMA_FACTORY_CLASSNAME = DEFAULT_IMPL_CLASS;

    private SchemaFactory sf;
    private XMLInputFactory ifac;
    private byte[] xsd1;
    private byte[] xsd2;
    private Document xsdDoc1;
    private Document xsdDoc2;
    private byte[] xml;
}
