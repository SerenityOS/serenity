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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * This tests the Attributes interface. Here the startElement() callback of
 * ContentHandler has Attributes as one of its arguments. Attributes
 * pertaining to an element are taken into this argument and various methods
 * of Attributes interfaces are tested.
 * This program uses Namespace processing without any namepsaces in xml file.
 * This program uses Validation
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.AttributesTest
 * @run testng/othervm org.xml.sax.ptests.AttributesTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class AttributesTest {
    /**
     * Unit test for Attributes interface. Prints all attributes into output
     * file. Check it with golden file.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase01() throws Exception {
        String outputFile = USER_DIR + "Attributes.out";
        String goldFile = GOLDEN_DIR + "AttributesGF.out";
        String xmlFile = XML_DIR + "family.xml";

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setFeature("http://xml.org/sax/features/namespace-prefixes",
                                    true);
        spf.setValidating(true);
        SAXParser saxParser = spf.newSAXParser();
        MyAttrCHandler myAttrCHandler = new MyAttrCHandler(outputFile);
        saxParser.parse(new File(xmlFile), myAttrCHandler);
        myAttrCHandler.flushAndClose();
        assertTrue(compareWithGold(goldFile, outputFile));
    }
}
