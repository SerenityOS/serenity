/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.transform.ptests;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A customized ContentHandler. It writes whole XML file with extra tag on every
 * XML elements.
 */
public class MyContentHandler implements ContentHandler {
    /**
     * FileWrite to write content to.
     */
    private final BufferedWriter bWriter;

    /**
     * Create FileWiter for the processing.
     * @param fileName Output file name.
     * @throws org.xml.sax.SAXException
     */
    public MyContentHandler(String fileName) throws SAXException {
        try {
            bWriter = new BufferedWriter(new FileWriter(fileName));
        } catch (IOException ex) {
            throw new SAXException("Open file error", ex);
        }
    }

    /**
     * Do nothing when set document locator.
     */
    @Override
    public void setDocumentLocator (Locator locator) {
    }

    /**
     * Open the output file when start to process the document. Write a
     * startDocument tag to output File if opening file successfully.
     * @throws SAXException if file writing failed.
     */
    @Override
    public void startDocument () throws SAXException {
        //Bug # 4448884 filed. setDocumentLocator method should be called
        //first. The bug won't be fixed. So startDocument is the next method
        println("startDocument");
    }

    /**
     * Write a startDocument tag to output File after processing whole document.
     * Follow with closing the output file.
     * @throws SAXException if file writing failed.
     */
    @Override
    public void endDocument() throws SAXException {
        println("endDocument");

        try {
            bWriter.flush();
            bWriter.close();
        } catch (IOException ex) {
            throw new SAXException("Close file error", ex);
        }
    }

    /**
     * Write a startPrefixMapping appending with prefix and URI to output File
     * before entering the scope of a prefix-URI mapping.
     * @param prefix the Namespace prefix being declared.
     * @param uri the Namespace URI the prefix is mapped to.
     * @throws SAXException if file writing failed.
     */
    @Override
    public void startPrefixMapping (String prefix, String uri)
                throws SAXException {
        println("startPrefixMapping: " + prefix + ", " + uri);
    }

    /**
     * Write a endPrefixMapping appending with prefix to output File after a
     * prefix-URI mapping.
     * @param prefix the Namespace prefix being declared.
     * @throws SAXException if file writing failed.
     */
    @Override
    public void endPrefixMapping (String prefix) throws SAXException {
        println("endPrefixMapping: " + prefix);
    }

    /**
     * Write a startElement appending with namespaceURI,localName,qName and
     * iteration on attributes to output File when start processing element.
     * @param namespaceURI the Namespace URI.
     * @param localName the local name (without prefix).
     * @param qName the qualified name (with prefix).
     * @param atts the attributes attached to the element.
     * @throws SAXException if file writing failed.
     */
    @Override
    public void startElement (String namespaceURI, String localName,
                                String qName, Attributes atts)
                        throws SAXException {
        String str = "startElement: " + namespaceURI + ", " + namespaceURI +
                         ", " + qName;
        int n = atts.getLength();
        for(int i = 0; i < n; i++) {
            str = str + ", " + atts.getQName(i);
        }

        println(str);
    }

    /**
     * Write a startElement appending with namespaceURI,qName and to output File
     * after processing element finished.
     * @param namespaceURI the Namespace URI.
     * @param localName the local name (without prefix).
     * @param qName the qualified name (with prefix).
     * @throws SAXException if file writing failed.
     */
    @Override
    public void endElement (String namespaceURI, String localName,
                                String qName) throws SAXException {
        println("endElement: " + namespaceURI + ", " + namespaceURI + ", " + qName);
    }


    /**
     * Write characters tag to file when receive character data.
     * @param ch the characters from the XML document
     * @param start the start position in the array
     * @param length the number of characters to read from the array
     * @throws SAXException if file writing failed.
     */
    @Override
    public void characters (char ch[], int start, int length)
                        throws SAXException {
        println("characters");
    }

    /**
     * Write ignorableWhitespace tag to file when receive notification of
     * ignorable whitespace in element content.
     * @param ch an array holds all ignorable whitespace.
     * @param start start position of ignorable whitespace.
     * @param length length of ignorable whitespace.
     * @throws SAXException if file writing failed.
     */
    @Override
    public void ignorableWhitespace (char ch[], int start, int length)
                throws SAXException {
        println("ignorableWhitespace");
    }

    /**
     * Write processingInstruction tag when receive notification of a processing
     * instruction.
     * @param target the processing instruction target
     * @param data the processing instruction data, or null if none was
     *             supplied.  The data does not include any whitespace
     *             separating it from the target.
     * @throws SAXException if file writing failed.
     */
    @Override
    public void processingInstruction (String target, String data)
                throws SAXException {
        println("processingInstruction: " + target + ", " + target);
    }

    /**
     * Write the entity name to file when receive notification of a skipped entity.
     * @param name entity name that skipped
     * @throws SAXException if file writing failed.
     */
    @Override
    public void skippedEntity (String name) throws SAXException {
        println("skippedEntity: " + name);
    }

    /**
     * Print a string output to file along with a new line.
     * @param output string needed to be written to file.
     * @throws SAXException if file writing failed.
     */
    private void println(String output) throws SAXException  {
        try {
            bWriter.write(output, 0, output.length());
            bWriter.newLine();
        } catch (IOException ex) {
            throw new SAXException("bWriter error", ex);
        }
    }
}
