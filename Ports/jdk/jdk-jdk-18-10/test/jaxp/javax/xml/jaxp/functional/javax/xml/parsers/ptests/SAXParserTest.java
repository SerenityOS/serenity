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

package javax.xml.parsers.ptests;

import static javax.xml.parsers.ptests.ParserTestConst.XML_DIR;
import static jaxp.library.JAXPTestUtilities.tryRunWithTmpPermission;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.PropertyPermission;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class contains the test cases for SAXParser API
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.parsers.ptests.SAXParserTest
 * @run testng/othervm javax.xml.parsers.ptests.SAXParserTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SAXParserTest {
    /**
     * Provide SAXParser.
     *
     * @return a data provider contains a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @DataProvider(name = "parser-provider")
    public Object[][] getParser() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser saxparser = spf.newSAXParser();
        return new Object[][] { { saxparser } };
    }

    /**
     * Test case with FileInputStream null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse01(SAXParser saxparser) throws Exception {
        FileInputStream instream = null;
        saxparser.parse(instream, new HandlerBase());
    }

    /**
     * Test with by setting URI as null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse02(SAXParser saxparser) throws Exception {
        String uri = null;
        saxparser.parse(uri, new HandlerBase());
    }

    /**
     * Test with non-existence URI, parsing should fail and throw IOException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = { SAXException.class },
            dataProvider = "parser-provider")
    public void testParse03(SAXParser saxparser) throws Exception {
        saxparser.parse("", new HandlerBase());
    }

    /**
     * Test with File null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse04(SAXParser saxparser) throws Exception {
        File file = null;
        saxparser.parse(file, new HandlerBase());
    }

    /**
     * Test with empty string as File, parsing should fail and throw
     * SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class, dataProvider = "parser-provider")
    public void testParse05(SAXParser saxparser) throws Exception {
        tryRunWithTmpPermission(() -> saxparser.parse(new File(""), new HandlerBase()), new PropertyPermission("user.dir", "read"));
    }

    /**
     * Test with input source null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse06(SAXParser saxparser) throws Exception {
        InputSource is = null;
        saxparser.parse(is, new HandlerBase());
    }

    /**
     * Test with FileInputStream null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse07(SAXParser saxparser) throws Exception {
        FileInputStream instream = null;
        saxparser.parse(instream, new DefaultHandler());
    }

    /**
     * Test with URI null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse08(SAXParser saxparser) throws Exception {
        String uri = null;
        saxparser.parse(uri, new DefaultHandler());
    }

    /**
     * Test with non-existence URI, parsing should fail and throw SAXException
     * or IOException.
     *
     * @param saxparser
     *            a SAXParser instance.
     * @throws Exception
     *             If any errors occur.
     */
    @Test(expectedExceptions = { SAXException.class, IOException.class }, dataProvider = "parser-provider")
    public void testParse09(SAXParser saxparser) throws Exception {
        saxparser.parse("no-such-file", new DefaultHandler());
    }

    /**
     * Test with empty string as File, parsing should fail and throw
     * SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class, dataProvider = "parser-provider")
    public void testParse10(SAXParser saxparser) throws Exception {
        File file = new File("");
        tryRunWithTmpPermission(() -> saxparser.parse(file, new DefaultHandler()), new PropertyPermission("user.dir", "read"));
    }

    /**
     * Test with File null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse11(SAXParser saxparser) throws Exception {
        saxparser.parse((File) null, new DefaultHandler());
    }

    /**
     * Test with input source null, parsing should fail and throw
     * IllegalArgumentException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            dataProvider = "parser-provider")
    public void testParse12(SAXParser saxparser) throws Exception {
        InputSource is = null;
        saxparser.parse(is, new DefaultHandler());
    }

    /**
     * Test with an error in XML file, parsing should fail and throw
     * SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class,
            dataProvider = "parser-provider")
    public void testParse13(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(new File(
                XML_DIR, "invalid.xml"))) {
            saxparser.parse(instream, new HandlerBase());
        }
    }

    /**
     * Test with a valid in XML file, parser should parse the XML document.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse14(SAXParser saxparser) throws Exception {
        saxparser.parse(new File(XML_DIR, "parsertest.xml"),
                new HandlerBase());
    }

    /**
     * Test with valid input stream, parser should parse the XML document
     * successfully.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse15(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(new File(XML_DIR,
                "correct.xml"))) {
            saxparser.parse(instream, new HandlerBase());
        }
    }

    /**
     * Test with valid input source, parser should parse the XML document
     * successfully.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse16(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(
                new File(XML_DIR, "parsertest.xml"))) {
            saxparser.parse(instream, new HandlerBase(),
                    new File(XML_DIR).toURI().toASCIIString());
        }
    }

    /**
     * Test with proper URI, parser should parse successfully.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse17(SAXParser saxparser) throws Exception {
        File file = new File(XML_DIR, "correct.xml");
        saxparser.parse(file.toURI().toASCIIString(), new HandlerBase());
    }

    /**
     * Test with XML file that has errors parsing should fail and throw
     * SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class,
            dataProvider = "parser-provider")
    public void testParse18(SAXParser saxparser) throws Exception {
        saxparser.parse(new File(XML_DIR, "valid.xml"), new HandlerBase());
    }

    /**
     * Test with XML file that has no errors Parser should successfully
     * parse the XML document.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse19(SAXParser saxparser) throws Exception {
        saxparser.parse(new File(XML_DIR, "correct.xml"), new HandlerBase());
    }

    /**
     * Test with input source attached an invalid XML, parsing should fail
     * and throw SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class,
            dataProvider = "parser-provider")
    public void testParse20(SAXParser saxparser) throws Exception {
        try(FileInputStream instream = new FileInputStream(new File(XML_DIR,
                "invalid.xml"))) {
            saxparser.parse(new InputSource(instream), new HandlerBase());
        }
    }

    /**
     * Test with input source attached an valid XML, parser should
     * successfully parse the XML document.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse21(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(new File(XML_DIR,
                "correct.xml"))) {
            saxparser.parse(new InputSource(instream), new HandlerBase());
        }
    }

    /**
     * Test with an error in xml file, parsing should fail and throw
     * SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class,
            dataProvider = "parser-provider")
    public void testParse22(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(
                new File(XML_DIR, "invalid.xml"))) {
            saxparser.parse(instream, new DefaultHandler());
        }
    }

    /**
     * Test with valid input stream, parser should parse the XML document
     * successfully.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse23(SAXParser saxparser) throws Exception {
        DefaultHandler handler = new DefaultHandler();
        saxparser.parse(new File(XML_DIR, "parsertest.xml"), handler);
    }

    /**
     * Test with valid input stream, parser should parse the XML document
     * successfully.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse24(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(new File(XML_DIR,
                "correct.xml"))) {
            DefaultHandler handler = new DefaultHandler();
            saxparser.parse(instream, handler);
        }
    }

    /**
     * Test with valid input source, parser should parse the XML document
     * successfully.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse25(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(
                new File(XML_DIR, "parsertest.xml"))) {
            saxparser.parse(instream, new DefaultHandler(),
                new File(XML_DIR).toURI().toASCIIString());
        }
    }

    /**
     * Test with proper URI, parser should parse successfully.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse26(SAXParser saxparser) throws Exception {
        File file = new File(XML_DIR, "correct.xml");
        saxparser.parse(file.toURI().toASCIIString(), new DefaultHandler());
    }

    /**
     * Test with XML file that has errors, parsing should fail and throw
     * SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class,
            dataProvider = "parser-provider")
    public void testParse27(SAXParser saxparser) throws Exception {
        saxparser.parse(new File(XML_DIR, "valid.xml"), new DefaultHandler());
    }

    /**
     * Test with XML file that has no errors, parser should successfully
     * parse the XML document.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse28(SAXParser saxparser) throws Exception {
        saxparser.parse(new File(XML_DIR, "correct.xml"), new DefaultHandler());
    }

    /**
     * Test with an invalid XML file, parser should throw SAXException.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class,
            dataProvider = "parser-provider")
    public void testParse29(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(
                new File(XML_DIR, "invalid.xml"))) {
            saxparser.parse(new InputSource(instream), new DefaultHandler());
        }
    }

    /**
     * Test case to parse an XML file that not use namespaces.
     *
     * @param saxparser a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParse30(SAXParser saxparser) throws Exception {
        try (FileInputStream instream = new FileInputStream(
                new File(XML_DIR, "correct.xml"))) {
            saxparser.parse(new InputSource(instream), new DefaultHandler());
        }
    }

    /**
     * Test case to parse an XML file that uses namespaces.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testParse31() throws Exception {
        try (FileInputStream instream = new FileInputStream(
                new File(XML_DIR, "ns4.xml"))) {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.newSAXParser().parse(instream, new HandlerBase());
        }
    }
}
