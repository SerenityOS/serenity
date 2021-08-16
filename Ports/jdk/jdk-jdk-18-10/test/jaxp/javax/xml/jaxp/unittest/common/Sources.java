/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package common;

import static jaxp.library.JAXPTestUtilities.runWithTmpPermission;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.PropertyPermission;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/*
 * @test
 * @bug 8144967
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.Sources
 * @run testng/othervm common.Sources
 * @summary Tests related to the javax.xml.transform.Source
 * and org.xml.sax.InputSource
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Sources {

    /**
     * @bug 8144967
     * Tests whether a Source object is empty
     * @param source the Source object
     */
    @Test(dataProvider = "emptySources")
    public void testIsEmpty(Source source) {
        Assert.assertTrue(source.isEmpty(), "The source is not empty");
    }

    /**
     * @bug 8144967
     * Tests that the source is not empty
     * @param source the Source object
     */
    @Test(dataProvider = "nonEmptySources")
    public void testIsNotEmpty(Source source) {
        Assert.assertTrue(!source.isEmpty(), "The source is empty");
    }

    /**
     * @bug 8144967
     * Tests whether an InputSource object is empty
     * @param source the InputSource object
     */
    @Test(dataProvider = "emptyInputSource")
    public void testISIsEmpty(InputSource source) {
        Assert.assertTrue(source.isEmpty(), "The source is not empty");
    }

    /*
     * DataProvider: sources that are empty
     */
    @DataProvider(name = "emptySources")
    public Object[][] getSources() throws URISyntaxException {

        return new Object[][]{
            {new DOMSource()},
            {new DOMSource(getDocument())},
            {new SAXSource()},
            {new SAXSource(new InputSource(new StringReader("")))},
            {new SAXSource(getXMLReader(), new InputSource(new StringReader("")))},
            {new StreamSource()},
            {new StreamSource(new ByteArrayInputStream("".getBytes()))},
            {new StreamSource(new StringReader(""))},
            {new StreamSource(new StringReader(""), null)},
            {new StreamSource((String) null)}
        };
    }

    /*
     * DataProvider: sources that are not empty
     */
    @DataProvider(name = "nonEmptySources")
    public Object[][] getSourcesEx() {
        StAXSource ss = null;
        try {
            ss = new StAXSource(getXMLEventReader());
        } catch (XMLStreamException ex) {
        }

        return new Object[][] {
            // This will set a non-null systemId on the resulting
            // StreamSource
            { runWithTmpPermission(() -> new StreamSource(new File("")), new PropertyPermission("user.dir", "read")) },
            // Can't tell because XMLStreamReader is a pull parser, cursor
            // advancement
            // would have been required in order to examine the reader.
            { new StAXSource(getXMLStreamReader()) },
            { ss } };
    }

    /*
     * DataProvider: sources that are empty
     */
    @DataProvider(name = "emptyInputSource")
    public Object[][] getInputSources() throws URISyntaxException {
        byte[] utf8Bytes = null;
        try {
            utf8Bytes = "".getBytes("UTF8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getMessage());
        }
        return new Object[][]{
            {new InputSource()},
            {new InputSource(new ByteArrayInputStream(utf8Bytes))},
            {new InputSource(new StringReader(""))},
            {new InputSource((String) null)}
        };
    }

    /**
     * Returns an instance of Document.
     *
     * @return an instance of Document.
     */
    private Document getDocument() {
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            doc = dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {}
        return doc;
    }

    /**
     * Returns an instance of XMLReader.
     *
     * @return an instance of XMLReader.
     */
    private XMLReader getXMLReader() {
        XMLReader reader = null;
        try {
            reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        } catch (ParserConfigurationException | SAXException ex) {}
        return reader;
    }

    /**
     * Returns an instance of XMLStreamReader.
     *
     * @return an instance of XMLStreamReader.
     */
    private XMLStreamReader getXMLStreamReader() {
        XMLStreamReader r = null;
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            r = xif.createXMLStreamReader(new ByteArrayInputStream("".getBytes()));
        } catch (XMLStreamException ex) {}

        return r;
    }

    /**
     * Returns an instance of XMLEventReader.
     *
     * @return an instance of XMLEventReader.
     */
    private XMLEventReader getXMLEventReader() {
        XMLEventReader r = null;
        try {
            r = XMLInputFactory.newInstance().createXMLEventReader(
                            new ByteArrayInputStream("".getBytes()));
        } catch (XMLStreamException ex) {}

        return r;
    }
}
