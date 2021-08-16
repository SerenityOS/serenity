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
package org.xml.sax.ptests;

import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static org.testng.Assert.assertTrue;
import static org.xml.sax.ptests.SAXTestConst.GOLDEN_DIR;
import static org.xml.sax.ptests.SAXTestConst.XML_DIR;

import java.io.FileInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/** This class contains the testcases to test XMLReader with regard to
  * Namespace Table defined at
  * http://www.megginson.com/SAX/Java/namespaces.html
  */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.XMLReaderNSTableTest
 * @run testng/othervm org.xml.sax.ptests.XMLReaderNSTableTest
 */
@Test
@Listeners({jaxp.library.FilePolicy.class})
public class XMLReaderNSTableTest {
    /**
     * XML file that used to be parsed.
     */
    private static final String xmlFile = XML_DIR + "namespace1.xml";

    /**
     * XML namespaces prefixes.
     */
    private static final String NAMESPACE_PREFIXES =
                "http://xml.org/sax/features/namespace-prefixes";
    /**
     * namespace processing is enabled. namespace-prefix is also is enabled.
     * So it is a True-True combination.
     * The test is to test XMLReader with these conditions.
     *
     * @throws Exception If any errors occur.
     */
    public void testWithTrueTrue() throws Exception {
        String outputFile = USER_DIR + "XRNSTableTT.out";
        String goldFile = GOLDEN_DIR + "NSTableTTGF.out";

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        xmlReader.setFeature(NAMESPACE_PREFIXES, true);

        try (FileInputStream fis = new FileInputStream(xmlFile);
            MyNSContentHandler handler = new MyNSContentHandler(outputFile);) {
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(fis));
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * Namespace processing is enabled. Hence namespace-prefix is
     * expected to be automatically off. So it is a True-False combination.
     * The test is to test XMLReader with these conditions.
     *
     * @throws Exception If any errors occur.
     */
    public void testWithTrueFalse() throws Exception {
        String outputFile = USER_DIR + "XRNSTableTF.out";
        String goldFile = GOLDEN_DIR + "NSTableTFGF.out";

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();

        try (FileInputStream fis = new FileInputStream(xmlFile);
            MyNSContentHandler handler = new MyNSContentHandler(outputFile)) {
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(fis));
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * namespace processing is not enabled. Hence namespace-prefix is
     * expected to be automaically on. So it is a False-True combination.
     * The test is to test XMLReader with these conditions.
     *
     * @throws Exception If any errors occur.
     */
    public void testWithFalseTrue()throws Exception {
        String outputFile = USER_DIR + "XRNSTableFT.out";
        String goldFile = GOLDEN_DIR + "NSTableFTGF.out";

        SAXParserFactory spf = SAXParserFactory.newInstance();
        //NamespaceAware is false by default, so don't need to set here
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        try (FileInputStream fis = new FileInputStream(xmlFile);
            MyNSContentHandler handler = new MyNSContentHandler(outputFile)) {
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(fis));
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }
}
