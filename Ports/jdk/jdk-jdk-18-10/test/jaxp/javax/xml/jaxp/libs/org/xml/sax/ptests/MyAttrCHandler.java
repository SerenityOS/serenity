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
package org.xml.sax.ptests;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Simple attributes handler.
 */
public class MyAttrCHandler extends DefaultHandler {
    /**
     * FileWriter to write string to output file.
     */
    private final BufferedWriter bWriter;

    /**
     * Initiate FileWriter
     * @param fileName output file name.
     * @throws IOException
     */
    public MyAttrCHandler(String fileName) throws IOException {
        bWriter = new BufferedWriter(new FileWriter(fileName));
    }

    /**
     * Write element content before start access every element.
     * @throws org.xml.sax.SAXException
     */
    @Override
    public void startElement(String uri, String localName,
                String qName, Attributes attributes) throws SAXException {
        try {
            String string = "uri <" + uri + "> localName <" + localName +
                        "> qName <" + qName + ">";

            bWriter.write( string, 0, string.length());
            bWriter.newLine();

            int length = attributes.getLength();
            string = "length: " + length;

            bWriter.write( string, 0, string.length());
            bWriter.newLine();

            for (int ind=0; ind < length ; ind++) {
                string = "For index = " + ind + "\n";
                string += "getLocalName <" + attributes.getLocalName(ind)
                                +">" + "\n";
                string += "getQName <" + attributes.getQName(ind) +">" + "\n";
                string += "getType <" + attributes.getType(ind) +">" + "\n";
                string += "getURI <" + attributes.getURI(ind) +">" + "\n";
                string += "getValue <" + attributes.getValue(ind) +">" + "\n";

                bWriter.write( string, 0, string.length());
                bWriter.newLine();

                String gotLocalName = attributes.getLocalName(ind);
                String gotQName = attributes.getQName(ind);
                String gotURI = attributes.getURI(ind);

                string ="Using localName, qname and uri pertaining to index = "
                                + ind;
                bWriter.write( string, 0, string.length());
                bWriter.newLine();

                string = "getIndex(qName) <" + attributes.getIndex(gotQName)
                                +">" + "\n";
                string += "getIndex(uri, localName) <" +
                        attributes.getIndex(gotURI, gotLocalName) +">" + "\n";

                string += "getType(qName) <" +
                        attributes.getType(gotQName) +">" + "\n";
                string += "getType(uri, localName) <" +
                        attributes.getType(gotURI, gotLocalName) +">" + "\n";

                string += "getValue(qName) <" +
                        attributes.getValue(gotQName) +">" + "\n";
                string += "getValue(uri, localName) <" +
                        attributes.getValue(gotURI, gotLocalName) +">" + "\n";

                bWriter.write( string, 0, string.length());
                bWriter.newLine();
            }
            bWriter.newLine();
        } catch(IOException ex){
            throw new SAXException(ex);
        }
    }

    /**
     * Flush the stream and close the file.
     * @throws IOException when writing or closing file failed.
     */
    public void flushAndClose() throws IOException {
        bWriter.flush();
        bWriter.close();
    }
}
