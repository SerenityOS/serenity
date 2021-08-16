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

package javax.xml.transform.ptests;

import static javax.xml.transform.ptests.TransformerTestConst.XML_DIR;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.TransformTest
 * @run testng/othervm javax.xml.transform.ptests.TransformTest
 * @summary Tests for variable combination of Transformer.transform(Source, Result)
 */
@Test(singleThreaded = true)
@Listeners({jaxp.library.FilePolicy.class})
public class TransformTest {

    /*
     * Initialize the share objects.
     */
    @BeforeClass
    public void setup() throws Exception {
        ifac = XMLInputFactory.newInstance();
        ofac = XMLOutputFactory.newInstance();
        tfac = TransformerFactory.newInstance();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        db = dbf.newDocumentBuilder();

        xml = Files.readAllBytes(Paths.get(XML_DIR + "cities.xml"));
        template = Files.readAllBytes(Paths.get(XML_DIR + "cities.xsl"));

        xmlDoc = db.parse(xmlInputStream());
    }

    @DataProvider(name = "input-provider")
    public Object[][] prepareTestCombination() throws Exception {

        Supplier<Source> staxStreamSource = () -> new StAXSource(getXMLStreamReader());
        Supplier<Source> staxEventSource = this::getStAXEventSource;
        Supplier<Source> domSource = () -> new DOMSource(xmlDoc);
        Supplier<Source> saxSource = () -> new SAXSource(new InputSource(xmlInputStream()));
        Supplier<Source> streamSource = () -> new StreamSource(xmlInputStream());

        Supplier<Result> staxStreamResult = () -> new StAXResult(getXMLStreamWriter());
        Supplier<Result> staxEventResult = () -> new StAXResult(getXMLEventWriter());
        Supplier<Result> saxResult = this::getHandlerSAXResult;
        Supplier<Result> streamResult = () -> new StreamResult(transOutputStream());

        Transformer domTemplateTransformer = createTransformer(getDomTemplate());
        Transformer saxTemplateTransformer = createTransformer(getSAXTemplate());
        Transformer streamTemplateTransformer = createTransformer(getStreamTemplate());
        Transformer noTemplateTransformer = createTransformer(null);
        Transformer staxStreamTemplateTransformer = createTransformer(getStAXStreamTemplate());
        Transformer staxEventTemplateTransformer = createTransformer(getStAXEventTemplate());

        return new Object[][] {
                // StAX Stream
                { staxStreamSource, staxStreamResult, domTemplateTransformer },
                { staxStreamSource, staxStreamResult, saxTemplateTransformer },
                { staxStreamSource, staxStreamResult, streamTemplateTransformer },
                { staxStreamSource, staxStreamResult, noTemplateTransformer },
                { staxStreamSource, staxStreamResult, staxStreamTemplateTransformer },
                { staxStreamSource, saxResult, domTemplateTransformer },
                { staxStreamSource, streamResult, domTemplateTransformer },
                { domSource, staxStreamResult, domTemplateTransformer },
                { saxSource, staxStreamResult, domTemplateTransformer },
                { streamSource, staxStreamResult, domTemplateTransformer },
                { staxStreamSource, streamResult, saxTemplateTransformer },
                { domSource, staxStreamResult, saxTemplateTransformer },
                { saxSource, staxStreamResult, saxTemplateTransformer },
                { streamSource, staxStreamResult, saxTemplateTransformer },
                { staxStreamSource, streamResult, streamTemplateTransformer },
                { domSource, staxStreamResult, streamTemplateTransformer },
                { saxSource, staxStreamResult, streamTemplateTransformer },
                { streamSource, staxStreamResult, streamTemplateTransformer },
                // StAX Event
                { staxEventSource, staxEventResult, domTemplateTransformer },
                { staxEventSource, staxEventResult, saxTemplateTransformer },
                { staxEventSource, staxEventResult, streamTemplateTransformer },
                { staxEventSource, staxEventResult, noTemplateTransformer },
                { staxEventSource, staxEventResult, staxEventTemplateTransformer },
                { staxEventSource, saxResult, domTemplateTransformer },
                { staxEventSource, streamResult, domTemplateTransformer },
                { domSource, staxEventResult, domTemplateTransformer },
                { saxSource, staxEventResult, domTemplateTransformer },
                { streamSource, staxEventResult, domTemplateTransformer },
                { staxEventSource, streamResult, saxTemplateTransformer },
                { domSource, staxEventResult, saxTemplateTransformer },
                { saxSource, staxEventResult, saxTemplateTransformer },
                { streamSource, staxEventResult, saxTemplateTransformer },
                { staxEventSource, streamResult, streamTemplateTransformer },
                { domSource, staxEventResult, streamTemplateTransformer },
                { saxSource, staxEventResult, streamTemplateTransformer },
                { streamSource, staxEventResult, streamTemplateTransformer } };
    }

    /*
     * run Transformer.transform(Source, Result)
     */
    @Test(dataProvider = "input-provider")
    public void testTransform(Supplier<Source> src, Supplier<Result> res, Transformer transformer) throws Throwable {
        try {
            transformer.transform(src.get(), res.get());
        } catch (WrapperException e) {
            throw e.getCause();
        }
    }

    private InputStream xmlInputStream() {
        return new ByteArrayInputStream(xml);
    }

    private InputStream templateInputStream() {
        return new ByteArrayInputStream(template);
    }

    private OutputStream transOutputStream() {
        return new ByteArrayOutputStream(xml.length);
    }

    private XMLStreamReader getXMLStreamReader() {
        try {
            return ifac.createXMLStreamReader(xmlInputStream());
        } catch (XMLStreamException e) {
            throw new WrapperException(e);
        }
    }

    private XMLStreamWriter getXMLStreamWriter() {
        try {
            return ofac.createXMLStreamWriter(transOutputStream());
        } catch (XMLStreamException e) {
            throw new WrapperException(e);
        }
    }

    private StAXSource getStAXEventSource() {
        try {
            return new StAXSource(ifac.createXMLEventReader(xmlInputStream()));
        } catch (XMLStreamException e) {
            throw new WrapperException(e);
        }
    }

    private XMLEventWriter getXMLEventWriter() {
        try {
            return ofac.createXMLEventWriter(transOutputStream());
        } catch (XMLStreamException e) {
            throw new WrapperException(e);
        }
    }

    private SAXResult getHandlerSAXResult() {
        SAXResult res = new SAXResult();
        MyContentHandler myContentHandler = new MyContentHandler(transOutputStream());
        res.setHandler(myContentHandler);
        return res;
    }

    private Source getDomTemplate() throws SAXException, IOException {
        return new DOMSource(db.parse(templateInputStream()));
    }

    private Source getSAXTemplate() {
        return new SAXSource(new InputSource(templateInputStream()));
    }

    private Source getStreamTemplate() {
        return new StreamSource(templateInputStream());
    }

    private Source getStAXStreamTemplate() throws XMLStreamException {
        return new StAXSource(ifac.createXMLStreamReader(templateInputStream()));
    }

    private Source getStAXEventTemplate() throws XMLStreamException {
        return new StAXSource(ifac.createXMLEventReader(templateInputStream()));
    }

    private Transformer createTransformer(Source templateSource) throws TransformerConfigurationException {
        Transformer transformer = (templateSource == null) ? tfac.newTransformer() : tfac.newTransformer(templateSource);
        transformer.setOutputProperty("indent", "yes");
        return transformer;

    }

    private static class MyContentHandler implements ContentHandler {
        private BufferedWriter bWriter;

        public MyContentHandler(OutputStream os) {
            bWriter = new BufferedWriter(new OutputStreamWriter(os));
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() throws SAXException {
            String str = "startDocument";
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void endDocument() throws SAXException {
            String str = "endDocument";
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
                bWriter.flush();
                bWriter.close();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            String str = "startPrefixMapping: " + prefix + ", " + uri;
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            String str = "endPrefixMapping: " + prefix;
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            StringBuilder str = new StringBuilder("startElement: ").append(namespaceURI).append(", ").append(namespaceURI).append(", ").append(qName).append(" : ");
            int n = atts.getLength();
            for (int i = 0; i < n; i++) {
                str.append(", ").append(atts.getQName(i)).append(" : ").append(atts.getValue(i));
            }

            try {
                bWriter.write(str.toString(), 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            String str = "endElement: " + namespaceURI + ", " + namespaceURI + ", " + qName;
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }

        }

        public void characters(char ch[], int start, int length) throws SAXException {
            String str = new String(ch, start, length);
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            String str = "ignorableWhitespace";
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void processingInstruction(String target, String data) throws SAXException {
            String str = "processingInstruction: " + target + ", " + target;
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }

        public void skippedEntity(String name) throws SAXException {
            String str = "skippedEntity: " + name;
            try {
                bWriter.write(str, 0, str.length());
                bWriter.newLine();
            } catch (IOException e) {
                System.out.println("bWriter error");
            }
        }
    }

    private static class WrapperException extends RuntimeException {
        public WrapperException(Throwable cause) {
            super(cause);
        }
    }

    private XMLInputFactory ifac;
    private XMLOutputFactory ofac;
    private TransformerFactory tfac;
    private DocumentBuilder db;
    private byte[] xml;
    private byte[] template;
    private Document xmlDoc;

}
