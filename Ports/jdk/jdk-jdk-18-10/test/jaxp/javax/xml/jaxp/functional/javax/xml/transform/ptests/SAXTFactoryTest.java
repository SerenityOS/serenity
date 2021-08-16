/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static javax.xml.transform.ptests.TransformerTestConst.GOLDEN_DIR;
import static javax.xml.transform.ptests.TransformerTestConst.XML_DIR;
import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TemplatesHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Test newTransformerhandler() method which takes StreamSource as argument can
 * be set to XMLReader.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.SAXTFactoryTest
 * @run testng/othervm javax.xml.transform.ptests.SAXTFactoryTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SAXTFactoryTest {
    /**
     * Test style-sheet file.
     */
    private static final String XSLT_FILE = XML_DIR + "cities.xsl";

    /**
     * Test style-sheet file.
     */
    private static final String XSLT_INCL_FILE = XML_DIR + "citiesinclude.xsl";

    /**
     * Test XML file.
     */
    private static final String XML_FILE = XML_DIR + "cities.xml";

    /**
     * SAXTFactory.newTransformerhandler() method which takes SAXSource as
     * argument can be set to XMLReader. SAXSource has input XML file as its
     * input source. XMLReader has a transformer handler which write out the
     * result to output file. Test verifies output file is same as golden file.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase01() throws Exception {
        String outputFile = USER_DIR + "saxtf001.out";
        String goldFile = GOLDEN_DIR + "saxtf001GF.out";

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory) TransformerFactory.newInstance();
            TransformerHandler handler = saxTFactory.newTransformerHandler(new StreamSource(XSLT_FILE));
            Result result = new StreamResult(fos);
            handler.setResult(result);
            reader.setContentHandler(handler);
            reader.parse(XML_FILE);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * SAXTFactory.newTransformerhandler() method which takes SAXSource as
     * argument can be set to XMLReader. SAXSource has input XML file as its
     * input source. XMLReader has a content handler which write out the result
     * to output file. Test verifies output file is same as golden file.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase02() throws Exception {
        String outputFile = USER_DIR + "saxtf002.out";
        String goldFile = GOLDEN_DIR + "saxtf002GF.out";

        try (FileOutputStream fos = new FileOutputStream(outputFile);
                FileInputStream fis = new FileInputStream(XSLT_FILE)) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory) TransformerFactory.newInstance();
            SAXSource ss = new SAXSource();
            ss.setInputSource(new InputSource(fis));

            TransformerHandler handler = saxTFactory.newTransformerHandler(ss);
            Result result = new StreamResult(fos);
            handler.setResult(result);
            reader.setContentHandler(handler);
            reader.parse(XML_FILE);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Unit test for newTransformerhandler(Source). DcoumentBuilderFactory is
     * namespace awareness, DocumentBuilder parse xslt file as DOMSource.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase03() throws Exception {
        String outputFile = USER_DIR + "saxtf003.out";
        String goldFile = GOLDEN_DIR + "saxtf003GF.out";

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document document = docBuilder.parse(new File(XSLT_FILE));
            Node node = (Node)document;
            DOMSource domSource= new DOMSource(node);

            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler =
                        saxTFactory.newTransformerHandler(domSource);
            Result result = new StreamResult(fos);
            handler.setResult(result);
            reader.setContentHandler(handler);
            reader.parse(XML_FILE);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Negative test for newTransformerHandler when relative URI is in XML file.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = TransformerConfigurationException.class)
    public void transformerHandlerTest04() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document document = docBuilder.parse(new File(XSLT_INCL_FILE));
        DOMSource domSource= new DOMSource(document);
        SAXTransformerFactory saxTFactory
                = (SAXTransformerFactory)TransformerFactory.newInstance();
        saxTFactory.newTransformerHandler(domSource);
    }

    /**
     * Unit test for XMLReader parsing when relative URI is used in xsl file and
     * SystemId was set.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase05() throws Exception {
        String outputFile = USER_DIR + "saxtf005.out";
        String goldFile = GOLDEN_DIR + "saxtf005GF.out";

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory)TransformerFactory.newInstance();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document document = docBuilder.parse(new File(XSLT_INCL_FILE));
            Node node = (Node)document;
            DOMSource domSource= new DOMSource(node);

            domSource.setSystemId("file:///" + XML_DIR);

            TransformerHandler handler =
                        saxTFactory.newTransformerHandler(domSource);
            Result result = new StreamResult(fos);

            handler.setResult(result);
            reader.setContentHandler(handler);
            reader.parse(XML_FILE);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Unit test newTransformerHandler with a DOMSource.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase06() throws Exception {
        String outputFile = USER_DIR + "saxtf006.out";
        String goldFile = GOLDEN_DIR + "saxtf006GF.out";

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory)TransformerFactory.newInstance();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Node node = (Node)docBuilder.parse(new File(XSLT_INCL_FILE));

            DOMSource domSource = new DOMSource(node, "file:///" + XML_DIR);
            TransformerHandler handler =
                        saxTFactory.newTransformerHandler(domSource);

            Result result = new StreamResult(fos);
            handler.setResult(result);
            reader.setContentHandler(handler);
            reader.parse(XML_FILE);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Test newTransformerHandler with a Template Handler.
     *
     * @throws Exception If any errors occur.
     */
    public void testcase08() throws Exception {
        String outputFile = USER_DIR + "saxtf008.out";
        String goldFile = GOLDEN_DIR + "saxtf008GF.out";

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory)TransformerFactory.newInstance();

            TemplatesHandler thandler = saxTFactory.newTemplatesHandler();
            reader.setContentHandler(thandler);
            reader.parse(XSLT_FILE);
            TransformerHandler tfhandler
                    = saxTFactory.newTransformerHandler(thandler.getTemplates());

            Result result = new StreamResult(fos);
            tfhandler.setResult(result);

            reader.setContentHandler(tfhandler);
            reader.parse(XML_FILE);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Test newTransformerHandler with a Template Handler along with a relative
     * URI in the style-sheet file.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase09() throws Exception {
        String outputFile = USER_DIR + "saxtf009.out";
        String goldFile = GOLDEN_DIR + "saxtf009GF.out";

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory)TransformerFactory.newInstance();

            TemplatesHandler thandler = saxTFactory.newTemplatesHandler();
            thandler.setSystemId("file:///" + XML_DIR);
            reader.setContentHandler(thandler);
            reader.parse(XSLT_INCL_FILE);
            TransformerHandler tfhandler=
                saxTFactory.newTransformerHandler(thandler.getTemplates());
            Result result = new StreamResult(fos);
            tfhandler.setResult(result);
            reader.setContentHandler(tfhandler);
            reader.parse(XML_FILE);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Unit test for contentHandler setter/getter along reader as handler's
     * parent.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase10() throws Exception {
        String outputFile = USER_DIR + "saxtf010.out";
        String goldFile = GOLDEN_DIR + "saxtf010GF.out";
        // The transformer will use a SAX parser as it's reader.
        XMLReader reader = XMLReaderFactory.createXMLReader();
        SAXTransformerFactory saxTFactory
                = (SAXTransformerFactory)TransformerFactory.newInstance();
        XMLFilter filter =
            saxTFactory.newXMLFilter(new StreamSource(XSLT_FILE));
        filter.setParent(reader);
        filter.setContentHandler(new MyContentHandler(outputFile));

        // Now, when you call transformer.parse, it will set itself as
        // the content handler for the parser object (it's "parent"), and
        // will then call the parse method on the parser.
        filter.parse(new InputSource(XML_FILE));
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Unit test for contentHandler setter/getter with parent.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase11() throws Exception {
        String outputFile = USER_DIR + "saxtf011.out";
        String goldFile = GOLDEN_DIR + "saxtf011GF.out";
        // The transformer will use a SAX parser as it's reader.
        XMLReader reader = XMLReaderFactory.createXMLReader();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document document = docBuilder.parse(new File(XSLT_FILE));
        Node node = (Node)document;
        DOMSource domSource= new DOMSource(node);

        SAXTransformerFactory saxTFactory
                = (SAXTransformerFactory)TransformerFactory.newInstance();
        XMLFilter filter = saxTFactory.newXMLFilter(domSource);

        filter.setParent(reader);
        filter.setContentHandler(new MyContentHandler(outputFile));

        // Now, when you call transformer.parse, it will set itself as
        // the content handler for the parser object (it's "parent"), and
        // will then call the parse method on the parser.
        filter.parse(new InputSource(XML_FILE));
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Unit test for contentHandler setter/getter.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase12() throws Exception {
        String outputFile = USER_DIR + "saxtf012.out";
        String goldFile = GOLDEN_DIR + "saxtf012GF.out";
        // The transformer will use a SAX parser as it's reader.
        XMLReader reader = XMLReaderFactory.createXMLReader();

        InputSource is = new InputSource(new FileInputStream(XSLT_FILE));
        SAXSource saxSource = new SAXSource();
        saxSource.setInputSource(is);

        SAXTransformerFactory saxTFactory = (SAXTransformerFactory)TransformerFactory.newInstance();
        XMLFilter filter = saxTFactory.newXMLFilter(saxSource);

        filter.setParent(reader);
        filter.setContentHandler(new MyContentHandler(outputFile));

        // Now, when you call transformer.parse, it will set itself as
        // the content handler for the parser object (it's "parent"), and
        // will then call the parse method on the parser.
        filter.parse(new InputSource(XML_FILE));
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Unit test for TemplatesHandler setter/getter.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase13() throws Exception {
        String outputFile = USER_DIR + "saxtf013.out";
        String goldFile = GOLDEN_DIR + "saxtf013GF.out";
        try(FileInputStream fis = new FileInputStream(XML_FILE)) {
            // The transformer will use a SAX parser as it's reader.
            XMLReader reader = XMLReaderFactory.createXMLReader();

            SAXTransformerFactory saxTFactory
                    = (SAXTransformerFactory) TransformerFactory.newInstance();
            TemplatesHandler thandler = saxTFactory.newTemplatesHandler();
            // I have put this as it was complaining about systemid
            thandler.setSystemId("file:///" + USER_DIR);

            reader.setContentHandler(thandler);
            reader.parse(XSLT_FILE);
            XMLFilter filter
                    = saxTFactory.newXMLFilter(thandler.getTemplates());
            filter.setParent(reader);

            filter.setContentHandler(new MyContentHandler(outputFile));
            filter.parse(new InputSource(fis));
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }
}
