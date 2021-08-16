/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package dom.ls;

import static org.w3c.dom.ls.DOMImplementationLS.MODE_SYNCHRONOUS;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 8080906 8114834 8206132
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ls.LSSerializerTest
 * @run testng/othervm dom.ls.LSSerializerTest
 * @summary Test LSSerializer.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class LSSerializerTest {

    class DOMErrorHandlerImpl implements DOMErrorHandler {

        boolean NoOutputSpecifiedErrorReceived = false;

        public boolean handleError(final DOMError error) {
            // consume "no-output-specified" errors
            if ("no-output-specified".equalsIgnoreCase(error.getType())) {
                NoOutputSpecifiedErrorReceived = true;
                return true;
            }

            // unexpected error
            Assert.fail("Unexpected Error Type: " + error.getType() + " @ (" + error.getLocation().getLineNumber() + ", "
                    + error.getLocation().getColumnNumber() + ")" + ", " + error.getMessage());

            return false;
        }
    }

    class Output implements LSOutput {
        public OutputStream getByteStream() {
            return null;
        }

        public void setByteStream(final OutputStream byteStream) {
        }

        public Writer getCharacterStream() {
            return null;
        }

        public void setCharacterStream(final Writer characterStream) {
        }

        public String getSystemId() {
            return null;
        }

        public void setSystemId(final String systemId) {
        }

        public String getEncoding() {
            return "UTF8";
        }

        public void setEncoding(final String encoding) {
        }
    }

    /*
     * @bug 8080906
     */
    @Test
    public void testDefaultLSSerializer() throws Exception {
        DOMImplementationLS domImpl = (DOMImplementationLS) DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        LSSerializer lsSerializer = domImpl.createLSSerializer();
        Assert.assertTrue(lsSerializer.getClass().getName().endsWith("dom3.LSSerializerImpl"));
    }

    @Test
    public void testDOMErrorHandler() {

        final String XML_DOCUMENT = "<?xml version=\"1.0\"?>" + "<hello>" + "world" + "</hello>";

        StringReader stringReader = new StringReader(XML_DOCUMENT);
        InputSource inputSource = new InputSource(stringReader);
        Document doc = null;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            // LSSerializer defaults to Namespace processing
            // so parsing must also
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();
            doc = parser.parse(inputSource);

        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }

        DOMImplementation impl = doc.getImplementation();
        DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
        LSSerializer writer = implLS.createLSSerializer();

        System.out.println("Serializer is: " + implLS.getClass().getName() + " " + implLS);

        DOMErrorHandlerImpl eh = new DOMErrorHandlerImpl();
        writer.getDomConfig().setParameter("error-handler", eh);

        boolean serialized = false;
        try {
            serialized = writer.write(doc, new Output());

            // unexpected success
            Assert.fail("Serialized without raising an LSException due to " + "'no-output-specified'.");
        } catch (LSException lsException) {
            // expected exception
            System.out.println("Expected LSException: " + lsException.toString());
            // continue processing
        }

        Assert.assertFalse(serialized, "Expected writer.write(doc, new Output()) == false");

        Assert.assertTrue(eh.NoOutputSpecifiedErrorReceived, "'no-output-specified' error was expected");
    }

    @Test
    public void testXML11() {

        /**
         * XML 1.1 document to parse.
         */
        final String XML11_DOCUMENT = "<?xml version=\"1.1\" encoding=\"UTF-16\"?>\n" + "<hello>" + "world" + "<child><children/><children/></child>"
                + "</hello>";

        /**JDK-8035467
         * no newline in default output
         */
        final String XML11_DOCUMENT_OUTPUT =
                "<?xml version=\"1.1\" encoding=\"UTF-16\"?>"
                + "<hello>"
                + "world"
                + "<child><children/><children/></child>"
                + "</hello>";

        // it all begins with a Document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
            Assert.fail(parserConfigurationException.toString());
        }
        Document document = null;

        StringReader stringReader = new StringReader(XML11_DOCUMENT);
        InputSource inputSource = new InputSource(stringReader);
        try {
            document = documentBuilder.parse(inputSource);
        } catch (SAXException saxException) {
            saxException.printStackTrace();
            Assert.fail(saxException.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            Assert.fail(ioException.toString());
        }

        // query DOM Interfaces to get to a LSSerializer
        DOMImplementation domImplementation = documentBuilder.getDOMImplementation();
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation;
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();

        System.out.println("Serializer is: " + lsSerializer.getClass().getName() + " " + lsSerializer);

        // get default serialization
        String defaultSerialization = lsSerializer.writeToString(document);

        System.out.println("XML 1.1 serialization = \"" + defaultSerialization + "\"");

        // output should == input
        Assert.assertEquals(XML11_DOCUMENT_OUTPUT, defaultSerialization, "Invalid serialization of XML 1.1 document: ");
    }

    // XML source
    private static final String XML =
            "<?xml version=\"1.1\" encoding=\"UTF-16\"?>\n" +
            "<!DOCTYPE author [\n" +
            " <!ENTITY name \"Jo Smith\">" +
            " <!ENTITY name1 \"&name;\">" +
            " <!ENTITY name2 \"&name1;\">" +
            "<!ENTITY ele \"<aa><bb>text</bb></aa>\">" +
            " <!ENTITY ele1 \"&ele;\">" +
            " <!ENTITY ele2 \"&ele1;\">" +
            " ]>" +
            " <author><a>&name1;</a>" +
            "<b>b &name2; &name1; b</b>" +
            "<c> &name; </c>" +
            "<d>&ele1;d</d>" +
            "<e> &ele2;eee </e>" +
            "<f>&lt;att&gt;</f>" +
            "<g> &ele; g</g>" +
            "<h>&ele2;</h></author>" ;

    // result when "entities" = true, equvalent to setting ExpandEntityReference to false
    private static final String RESULT_TRUE =
            "<?xml version=\"1.1\" encoding=\"UTF-16\"?><!DOCTYPE author [ \n" +
            "<!ENTITY name 'Jo Smith'>\n" +
            "<!ENTITY name1 '&name;'>\n" +
            "<!ENTITY name2 '&name1;'>\n" +
            "<!ENTITY ele '<aa><bb>text</bb></aa>'>\n" +
            "<!ENTITY ele1 '&ele;'>\n" +
            "<!ENTITY ele2 '&ele1;'>\n" +
            "]>\n" +
            "<author>\n" +
            "    <a>&name1;</a>\n" +
            "    <b>b &name2;&name1; b</b>\n" +
            "    <c>&name;</c>\n" +
            "    <d>&ele1;d</d>\n" +
            "    <e>&ele2;eee </e>\n" +
            "    <f>&lt;att&gt;</f>\n" +
            "    <g>&ele; g</g>\n" +
            "    <h>&ele2;</h>\n" +
            "</author>\n";

    // result when "entities" = false, equvalent to setting ExpandEntityReference to true
    private static final String RESULT_FALSE =
            "<?xml version=\"1.1\" encoding=\"UTF-16\"?><!DOCTYPE author [ \n" +
            "<!ENTITY name 'Jo Smith'>\n" +
            "<!ENTITY name1 '&name;'>\n" +
            "<!ENTITY name2 '&name1;'>\n" +
            "<!ENTITY ele '<aa><bb>text</bb></aa>'>\n" +
            "<!ENTITY ele1 '&ele;'>\n" +
            "<!ENTITY ele2 '&ele1;'>\n" +
            "]>\n" +
            "<author>\n" +
            "    <a>Jo Smith</a>\n" +
            "    <b>b Jo Smith Jo Smith b</b>\n" +
            "    <c> Jo Smith </c>\n" +
            "    <d>\n" +
            "        <aa>\n" +
            "            <bb>text</bb>\n" +
            "        </aa>\n" +
            "        d\n" +
            "    </d>\n" +
            "    <e>\n" +
            "        <aa>\n" +
            "            <bb>text</bb>\n" +
            "        </aa>\n" +
            "        eee \n" +
            "    </e>\n" +
            "    <f>&lt;att&gt;</f>\n" +
            "    <g>\n" +
            "        <aa>\n" +
            "            <bb>text</bb>\n" +
            "        </aa>\n" +
            "         g\n" +
            "    </g>\n" +
            "    <h>\n" +
            "        <aa>\n" +
            "            <bb>text</bb>\n" +
            "        </aa>\n" +
            "    </h>\n" +
            "</author>\n";

    /*
     * DataProvider: for testing the entities parameter
     * Data columns: xml source, entities setting, expected result
     */
    @DataProvider(name = "entities")
    Object[][] getData() throws Exception {
        return new Object[][]{
            {XML, Boolean.TRUE, RESULT_TRUE},
            {XML, Boolean.FALSE, RESULT_FALSE},
        };
    }

    /**
     * Tests serializing DOM Document with DOMConfiguration's "entities" parameter.
     *
     * @param source the XML source
     * @param entities the entities parameter setting
     * @param expected expected string result
     * @throws Exception
     * @bug 8114834 8206132
     */
    @Test(dataProvider = "entities")
    public void testEntityReference(String source, Boolean entities, String expected)
            throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        DOMImplementation domImplementation = documentBuilder.getDOMImplementation();
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation;

        LSParser domParser = domImplementationLS.createLSParser(MODE_SYNCHRONOUS, null);
        domParser.getDomConfig().setParameter("entities", entities);

        LSInput src = domImplementationLS.createLSInput();
        src.setStringData(source);
        Document document = domParser.parse(src);

        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("format-pretty-print", true);
        System.out.println("test with default entities is " +
                lsSerializer.getDomConfig().getParameter("entities"));

        String result = lsSerializer.writeToString(document);
        Assert.assertEquals(result, expected);
    }
}
