/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.w3c.dom.ls.DOMImplementationLS.MODE_SYNCHRONOUS;
import static test.astro.AstroConstants.ASTROCAT;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.astro.DocumentLSTest
 * @run testng/othervm test.astro.DocumentLSTest
 * @summary org.w3c.dom.ls tests
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DocumentLSTest {
    /*
     * Test creating an empty Document
     */
    @Test
    public void testNewDocument() throws ParserConfigurationException {
        Document doc = getDocumentBuilder().newDocument();
        assertNull(doc.getDocumentElement());
    }

    /*
     * Test creating an LSInput instance, and parsing ByteStream
     */
    @Test
    public void testLSInputParsingByteStream() throws Exception {
        DOMImplementationLS impl = (DOMImplementationLS) getDocumentBuilder().getDOMImplementation();
        LSParser domParser = impl.createLSParser(MODE_SYNCHRONOUS, null);
        LSInput src = impl.createLSInput();

        try (InputStream is = new FileInputStream(ASTROCAT)) {
            src.setByteStream(is);
            assertNotNull(src.getByteStream());
            // set certified accessor methods
            boolean origCertified = src.getCertifiedText();
            src.setCertifiedText(true);
            assertTrue(src.getCertifiedText());
            src.setCertifiedText(origCertified); // set back to orig

            src.setSystemId(filenameToURL(ASTROCAT));

            Document doc = domParser.parse(src);
            Element result = doc.getDocumentElement();
            assertEquals(result.getTagName(), "stardb");
        }
    }

    /*
     * Test creating an LSInput instance, and parsing String
     */
    @Test
    public void testLSInputParsingString() throws Exception {
        DOMImplementationLS impl = (DOMImplementationLS) getDocumentBuilder().getDOMImplementation();
        String xml = "<?xml version='1.0'?><test>runDocumentLS_Q6</test>";

        LSParser domParser = impl.createLSParser(MODE_SYNCHRONOUS, null);
        LSSerializer domSerializer = impl.createLSSerializer();
        // turn off xml decl in serialized string for comparison
        domSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
        LSInput src = impl.createLSInput();
        src.setStringData(xml);
        assertEquals(src.getStringData(), xml);

        Document doc = domParser.parse(src);
        String result = domSerializer.writeToString(doc);

        assertEquals(result, "<test>runDocumentLS_Q6</test>");
    }

    /*
     * Test writing with an LSOutput instance
     */
    @Test
    public void testLSOutput() throws Exception {
        DocumentBuilder db = getDocumentBuilder();
        // Create the Document using the supplied builder...
        Document doc = db.parse(ASTROCAT);

        DOMImplementationLS impl = null;

        impl = (DOMImplementationLS) db.getDOMImplementation();
        LSSerializer domSerializer = impl.createLSSerializer();
        MyDOMOutput mydomoutput = new MyDOMOutput();
        try (OutputStream os = new FileOutputStream(USER_DIR + "test.out")) {
            mydomoutput.setByteStream(os);
            mydomoutput.setEncoding("UTF-8");
            assertTrue(domSerializer.write(doc, mydomoutput));
        }
    }

    private static class MyDOMOutput implements LSOutput {
        private OutputStream bytestream = null;
        private String encoding = null;
        private String sysId = null;
        private Writer writer = null;

        public OutputStream getByteStream() {
            return bytestream;
        }

        public Writer getCharacterStream() {
            return writer;
        }

        public String getEncoding() {
            return encoding;
        }

        public String getSystemId() {
            return sysId;
        }

        public void setByteStream(OutputStream bs) {
            bytestream = bs;
        }

        public void setCharacterStream(Writer cs) {
            writer = cs;
        }

        public void setEncoding(String enc) {
            encoding = enc;
        }

        public void setSystemId(String sysId) {
            this.sysId = sysId;
        }
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder();
    }
}
