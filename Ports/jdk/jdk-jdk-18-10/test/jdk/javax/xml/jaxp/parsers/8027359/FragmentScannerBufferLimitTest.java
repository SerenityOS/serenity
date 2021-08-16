/*
 * Copyright (c) 2014 SAP SE. All rights reserved.
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

/**
 * @test
 * @bug 8034087 8027359
 * @summary XML parser may overwrite element content if that content falls onto the border of an entity scanner buffer
 * @run main FragmentScannerBufferLimitTest
 */
import java.io.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Test for overwriting of XML element content by the XML parser when reading over buffer
 * limits.
 *
 * We create a simple XML document of the form:
 *
 * <?xml version=\"1.1\"?>
 * <ROOT>
 *    <FILLER>ffffffff...fffff</FILLER>
 *    <TEST>content</TEST><TEST2>content2</TEST2>
 *    <FILLER>ffffffff...fffffffff</FILLER>
 * </ROOT>
 *
 * What's important here is, that the test content is at the border of an entity scanner
 * buffer (XMLEntityScanner uses 8192 byte buffers). That's why there are filler elements
 * of sufficient length that ensure there is a buffer break inside the test content
 * and there is enough to read to require another buffer read after the content has been
 * read.
 *
 * With the faulty implementation, the test content gets overwritten with characters
 * read from the next buffer, i.e. 'f's.
 *
 * @author steffen.schreiber@sap.com
 */
public class FragmentScannerBufferLimitTest {

    static int errCount = 0;

    /**
     * Check the test content.
     */
    public static void main(String[] args) throws ParserConfigurationException,
            SAXException, IOException, TransformerConfigurationException,
            TransformerException, TransformerFactoryConfigurationError {

        String testString = "<TEST>content</TEST><TEST2>content2</TEST2>";

        for (int i = 0; i < testString.length(); i++) {
            test(createDocument(testString.toString(), i), ""+ i);
        }

        if (errCount == 0) {
            System.out.println("OK");
        }
        else {
            System.out.println("ERROR");
            throw new RuntimeException("Parsing error: element content has been overwritten");
        }
    }

    /**
     * Create the test XML document.
     * @param testString the test content string
     * @param bufferLimitPosition the position in the string where the buffer should break
     * @return the document
     */
    private static String createDocument(String testString, int bufferLimitPosition) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        result.append("<?xml version=\"1.1\"?>");
        result.append("<ROOT>");

        int fillerLength = 8192 - bufferLimitPosition;
        createFiller(result, fillerLength);

        result.append(testString);

        createFiller(result, 9000);
        result.append("</ROOT>");
        return result.toString();
    }

    /**
     * Create the filler element of the given length.
     * @param buffer the output buffer
     * @param length the required length of the element, including the element tags
     */
    private static void createFiller(StringBuilder buffer, int length) {
        buffer.append("<FILLER>");
        int fillLength = length - "<FILLER></FILLER>".length();
        for (int i=0; i<fillLength; i++) {
            buffer.append('f');
        }
        buffer.append("</FILLER>");
    }


    private static void test(String document, String testName) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new ByteArrayInputStream(document.getBytes("UTF-8")));

        // check that there is the root node
        NodeList roots = doc.getElementsByTagName("ROOT");
        assert roots.getLength() == 1;
        Node root = roots.item(0);

        // check that root has children "FILLER" and "TEST"
        NodeList children = root.getChildNodes();
        assert children.getLength() == 4;
        assert children.item(0).getNodeName().equals("FILLER");
        assert children.item(1).getNodeName().equals("TEST");
        assert children.item(2).getNodeName().equals("TEST2");
        assert children.item(3).getNodeName().equals("FILLER");

        // check that the test node has content "content"
        checkContent(children.item(1).getTextContent(), "content", document);
        checkContent(children.item(2).getTextContent(), "content2", document);
    }

    private static void checkContent(String found, String expected, String document) {
        if (! (found.equals(expected))) {
            errCount++;
            int bufferStart = "<?xml version=\"1.1\"?><ROOT>".length() +1;
            int bufferStart2 = bufferStart + 8192;
            System.err.println("\nError:: expected \"" + expected
                    + "\", but found \"" + found + "\"!");
            System.err.println("Buffer was (probably): [ ... "
                    + document.substring(bufferStart2 - 20, bufferStart2) + "] ["
                    + document.substring(bufferStart2, bufferStart2 + 30) + " ... ]");
        }
    }
}
