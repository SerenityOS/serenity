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

package sax;

import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.getSystemProperty;
import static jaxp.library.JAXPTestUtilities.tryRunWithTmpPermission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.PropertyPermission;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

/*
 * @test
 * @bug 7057778
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.Bug7057778Test
 * @run testng/othervm sax.Bug7057778Test
 * @summary Test the file can be deleted after SAXParser.parse(File, DefaultHandler).
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug7057778Test {

    static final String xml = "Bug7057778.xml";
    static final String xml1 = "Bug7057778_1.xml";

    @Test
    public void testParse() {
        File src = new File(getClass().getResource(xml).getFile());
        File dst = new File(USER_DIR + xml1);
        try {
            copyFile(src, dst);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser parser = spf.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", new MyHandler1());
            tryRunWithTmpPermission(() -> parser.parse(dst, new MyHandler1()),
                    new PropertyPermission("user.dir", "read"));
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            // shouldn't happen
        } catch (ParserConfigurationException ex) {
            // shouldn't happen
        } catch (Exception ex) {
        }
        if (dst != null) {
            if (dst.delete()) {
                System.out.println("Delete: OK");
            } else {
                System.out.println("Delete: NG");
                Assert.fail("Error: denied to delete the file");
            }
        }

    }

    private void copyFile(File src, File dst) throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        // Transfer bytes
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public class MyHandler1 extends DefaultHandler2 implements ErrorHandler {
        private Writer out;

        StringBuffer textBuffer;
        private String indentString = "    "; // Amount to indent
        private int indentLevel = 0;

        public MyHandler1() {
            try {
                out = new OutputStreamWriter(System.out, "UTF8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            try {
                System.out.println("uri: " + uri);
                System.out.println("localName: " + localName);
                System.out.println("qName: " + qName);
            } catch (Exception e) {
                throw new SAXException(e);
            }

        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        public void characters(char ch[], int start, int length) throws SAXException {
        }

        public void comment(char[] ch, int start, int length) {
            String text = new String(ch, start, length);
            // System.out.println(text);
            try {
                nl();
                emit("COMMENT: " + text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void error(SAXParseException exception) {
            exception.printStackTrace();
        }

        public void fatalError(SAXParseException exception) {
            exception.printStackTrace();
        }

        public void warning(SAXParseException exception) {
            exception.printStackTrace();
        }

        // Wrap I/O exceptions in SAX exceptions, to
        // suit handler signature requirements
        private void emit(String s) throws SAXException {
            try {
                out.write(s);
                out.flush();
            } catch (IOException e) {
                throw new SAXException("I/O error", e);
            }
        }

        // Start a new line
        // and indent the next line appropriately
        private void nl() throws SAXException {
            String lineEnd = getSystemProperty("line.separator");

            try {
                out.write(lineEnd);

                for (int i = 0; i < indentLevel; i++)
                    out.write(indentString);
            } catch (IOException e) {
                throw new SAXException("I/O error", e);
            }
        }

    }
}
