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

package parsers;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 7157608
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug7157608Test
 * @run testng/othervm parsers.Bug7157608Test
 * @summary Test feature standard-uri-conformant works.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug7157608Test {
    public static boolean isWindows = false;
    static {
        if (getSystemProperty("os.name").indexOf("Windows") > -1) {
            isWindows = true;
        }
    };

    String xml1, xml2;

    @BeforeMethod
    public void setUp() throws IOException {
        File file1 = new File(getClass().getResource("Bug7157608.xml").getFile());
        xml1 = file1.getPath().replace("\\", "\\\\");
        File file2 = new File(getClass().getResource("Bug7157608_1.xml").getFile());
        xml2 = file2.getPath();
    }

    // case 1
    // standard-uri-confomant is false
    // dtd-validation is false
    @Test
    public void test1() {
        if (isWindows) {
            try {
                ParserSettings ps = new ParserSettings();

                DocumentBuilder db = getDocumentBuilder(ps);
                InputSource is = new InputSource();
                is.setSystemId(xml1);
                Document doc = db.parse(is);
                System.out.println("test1() :OK");
            } catch (Exception e) {
                Assert.fail("test1() :NG");

            }
        }
    }

    // case 2
    // standard-uri-confomant is false
    // dtd-validation is true
    @Test
    public void test2() {
        if (isWindows) {
            try {
                ParserSettings ps = new ParserSettings();
                ps.validating = true;

                DocumentBuilder db = getDocumentBuilder(ps);
                InputSource is = new InputSource(xml2);
                Document doc = db.parse(is);
                System.out.println("test2() :OK");
            } catch (Exception e) {
                Assert.fail("test2() :NG");
                // logger.info(e.getMessage());
            }
        }
    }

    // case 3
    // standard-uri-confomant is true
    @Test
    public void test3() {
        if (isWindows) {
            try {
                ParserSettings ps = new ParserSettings();
                ps.standardUriConformant = true;

                DocumentBuilder db = getDocumentBuilder(ps);
                InputSource is = new InputSource();
                is.setSystemId(xml1);
                Document doc = db.parse(is);
                Assert.fail("test3() :NG");
            } catch (IOException e) {
                String returnedErr = e.getMessage();
                String expectedStr = "Opaque part contains invalid character";

                if (returnedErr.indexOf(expectedStr) >= 0) {
                    System.out.println("test3() :OK");
                } else {
                    Assert.fail("test3() :NG");
                }
            } catch (Exception e) {
                System.out.println("test3() :NG");
            }
        }
    }

    // case 4
    // standard-uri-confomant is true
    // dtd-validation is true
    @Test
    public void test4() {
        if (isWindows) {
            try {
                ParserSettings ps = new ParserSettings();
                ps.standardUriConformant = true;
                ps.validating = true;

                DocumentBuilder db = getDocumentBuilder(ps);
                InputSource is = new InputSource(xml2);
                Document doc = db.parse(is);
                Assert.fail("test4() :NG");
            } catch (IOException e) {
                String returnedErr = e.getMessage();
                String expectedStr = "Opaque part contains invalid character";

                if (returnedErr.indexOf(expectedStr) >= 0) {
                    System.out.println("test3() :OK");
                } else {
                    Assert.fail("test3() :NG");
                }
            } catch (Exception e) {
                Assert.fail("test4() :NG");
            }
        }
    }

    public DocumentBuilder getDocumentBuilder(ParserSettings ps) {
        DocumentBuilder db = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (ps.standardUriConformant) {
                dbf.setFeature("http://apache.org/xml/features/standard-uri-conformant", true);
            }
            dbf.setValidating(ps.validating);
            db = dbf.newDocumentBuilder();
            db.setErrorHandler(new MyHandler());
        } catch (Exception e) {
            Assert.fail("standard-uri-conformant not recognized");
        }
        return db;
    }

    class MyHandler extends DefaultHandler {
        @Override
        public void warning(SAXParseException e) throws SAXException {
            printDetail("**Warning**", e);
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            printDetail("**Error**", e);
            throw new SAXException("Error encountered");
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            printDetail("**Fatal Error**", e);
            throw new SAXException("Fatal Error encountered");
        }

        public void printDetail(String msg, SAXParseException e) {
            System.out.println(msg);
            System.out.println(e.getMessage());
            System.out.println("  Line:    " + e.getLineNumber());
            System.out.println("  Column:  " + e.getColumnNumber());
            System.out.println("  URI:     " + e.getSystemId());
        }

    }

    class ParserSettings {
        boolean standardUriConformant = false;
        boolean validating = false;
    }
}
