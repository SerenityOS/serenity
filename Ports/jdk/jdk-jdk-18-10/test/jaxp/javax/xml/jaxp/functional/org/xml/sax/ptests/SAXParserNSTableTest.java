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

import java.io.File;

import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * This class contains the testcases to test SAXParser with regard to
 * Namespace Table defined at http://www.megginson.com/SAX/Java/namespaces.html
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.SAXParserNSTableTest
 * @run testng/othervm org.xml.sax.ptests.SAXParserNSTableTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SAXParserNSTableTest {
    /**
     * namespace processing is enabled. namespace-prefix is also is enabled.
     * So it is a True-True combination.
     * The test is to test SAXParser with these conditions.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testWithTrueTrue() throws Exception {
        String outputFile = USER_DIR + "SPNSTableTT.out";
        String goldFile = GOLDEN_DIR + "NSTableTTGF.out";
        String xmlFile = XML_DIR + "namespace1.xml";
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setFeature("http://xml.org/sax/features/namespace-prefixes",
                                    true);
        try (MyNSContentHandler handler = new MyNSContentHandler(outputFile)) {
            spf.newSAXParser().parse(new File(xmlFile), handler);
        }
        assertTrue(compareWithGold(goldFile, outputFile));

    }
    /**
     * namespace processing is enabled. Hence namespace-prefix is
     * expected to be automatically off. So it is a True-False combination.
     * The test is to test SAXParser with these conditions.
     *
     * @throws Exception If any errors occur.
     */
    public void testWithTrueFalse() throws Exception {
        String outputFile = USER_DIR + "SPNSTableTF.out";
        String goldFile = GOLDEN_DIR + "NSTableTFGF.out";
        String xmlFile = XML_DIR + "namespace1.xml";
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        try (MyNSContentHandler handler = new MyNSContentHandler(outputFile)) {
            spf.newSAXParser().parse(new File(xmlFile), handler);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }

    /**
     * namespace processing is not enabled. Hence namespace-prefix is
     * expected to be automatically on. So it is a False-True combination.
     * The test is to test SAXParser with these conditions.
     *
     * @throws Exception If any errors occur.
     */
    public void testWithFalseTrue() throws Exception {
        String outputFile = USER_DIR + "SPNSTableFT.out";
        String goldFile = GOLDEN_DIR + "NSTableFTGF.out";
        String xmlFile = XML_DIR + "namespace1.xml";
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        try (MyNSContentHandler handler = new MyNSContentHandler(outputFile)) {
            spf.newSAXParser().parse(new File(xmlFile), handler);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }
}
