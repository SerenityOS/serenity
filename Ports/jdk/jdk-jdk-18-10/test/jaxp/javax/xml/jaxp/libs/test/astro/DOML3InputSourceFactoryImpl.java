/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.astro;

import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static org.w3c.dom.ls.DOMImplementationLS.MODE_SYNCHRONOUS;
import static org.w3c.dom.traversal.NodeFilter.SHOW_ELEMENT;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;
import org.xml.sax.InputSource;

/*
 * A specialized implementation of an Input Source factory that utilizes
 * DOM Level 3 implementations to build a Document (DOM) from the
 * astro input file (XML) and then will serialize the DOM. The serialized DOM
 * of the astro input file is then used to create the sax InputSource
 * and set it's system id. It is then returned to the caller.
 *
 */
public class DOML3InputSourceFactoryImpl implements InputSourceFactory {
    public InputSource newInputSource(String filename) throws Exception {
        // Create DOMImplementationLS, and DOM L3 LSParser
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder bldr = fact.newDocumentBuilder();
        DOMImplementationLS impl = (DOMImplementationLS) bldr.getDOMImplementation();
        LSParser domparser = impl.createLSParser(MODE_SYNCHRONOUS, null);
        domparser.setFilter(new MyDOMBuilderFilter());

        // Parse the xml document to create the DOM Document using
        // the DOM L3 LSParser and a LSInput (formerly LSInputSource)
        Document doc = null;
        LSInput src = impl.createLSInput();
        // register the input file with the input source...
        String systemId = filenameToURL(filename);
        src.setSystemId(systemId);
        try (Reader reader = new FileReader(filename)) {
            src.setCharacterStream(reader);
            src.setEncoding("UTF-8");
            doc = domparser.parse(src);
        }

        // Use DOM L3 LSSerializer (previously called a DOMWriter)
        // to serialize the xml doc DOM to a file stream.
        String tmpCatalog = Files.createTempFile(Paths.get(USER_DIR), "catalog.xml", null).toString();

        LSSerializer domserializer = impl.createLSSerializer();
        domserializer.setFilter(new MyDOMWriterFilter());
        domserializer.getNewLine();
        DOMConfiguration config = domserializer.getDomConfig();
        config.setParameter("xml-declaration", Boolean.TRUE);
        String result = domserializer.writeToString(doc);
        try (FileWriter os = new FileWriter(tmpCatalog, false)) {
            os.write(result);
            os.flush();
        }

        // Return the Input Source created from the Serialized DOM L3 Document.
        InputSource catsrc = new InputSource(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(tmpCatalog)))));
        catsrc.setSystemId(systemId);
        return catsrc;
    }

    /*
     * Implementation of a DOM L3 DOM Builder Filter. The filter is capable of
     * examining nodes as they are available during the parse. This
     * implementation both rejects (filters) and modifies particular nodes
     * during the parse of the document. As such, the document in memory will
     * become a subset of the document on the stream in accordance with the DOM
     * Level 3 Load and Save Specification, v1.0, sect. 1.3 Load Interfaces.
     */
    private static class MyDOMBuilderFilter implements LSParserFilter {

        /*
         * Filter the DOM. Define element(s) and their children that should be
         * efficiently skipped thereby filtering them out of the stream.
         */
        @Override
        public short startElement(Element e) {
            return "_test01".equals(e.getTagName()) ? FILTER_REJECT : FILTER_ACCEPT;
        }

        /*
         * Modify the DOM 'in situ'. Find a particular Node and change the Node
         * value of its child, allow other nodes to pass through unchanged.
         */
        @Override
        public short acceptNode(Node n) {
            String localname = n.getLocalName();
            if (localname.equals("_test-04")) {
                Node child = n.getFirstChild();
                String text = child.getNodeValue();
                if (text.equals("T%e!s#t$")) {
                    child.setNodeValue("T%E!S#T$");
                }
            }
            return FILTER_ACCEPT;
        }

        /*
         * Tells the DOMBuilder what types of nodes to show to the filter.
         */
        @Override
        public int getWhatToShow() {
            return SHOW_ELEMENT;
        }
    }

    /*
     * Implementation of a DOM Serializer Filter (previously called a DOM Writer
     * Filter) which is a specialization of the NodeFilter interface.
     */
    private static class MyDOMWriterFilter implements LSSerializerFilter {
        public MyDOMWriterFilter() {
        }

        /*
         * Must implement method from NodeFilter interface
         */
        @Override
        public short acceptNode(Node node) {
            return FILTER_ACCEPT;
        }

        /*
         * Tells the DOMBuilder what types of nodes to show to the filter.
         */
        @Override
        public int getWhatToShow() {
            return SHOW_ELEMENT;
        }
    }
}
