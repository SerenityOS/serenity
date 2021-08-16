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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


/**
 * Here a transformer is created using DOMSource. Some specific output property
 * is set on transformer. Then transform(StreamSource, StreamResult) is tested.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.TransformerTest02
 * @run testng/othervm javax.xml.transform.ptests.TransformerTest02
 */
@Listeners({jaxp.library.FilePolicy.class})
public class TransformerTest02 {
    /**
     * Unit test for transform(StreamSource, StreamResult).
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase01() throws Exception {
        String outputFile = USER_DIR + "transformer02.out";
        String goldFile = GOLDEN_DIR + "transformer02GF.out";
        String xsltFile = XML_DIR + "cities.xsl";
        String xmlFile = XML_DIR + "cities.xml";

        try (FileInputStream fis = new FileInputStream(xmlFile);
                FileOutputStream fos = new FileOutputStream(outputFile)) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DOMSource domSource = new DOMSource(dbf.newDocumentBuilder().
                    parse(new File(xsltFile)));

            Transformer transformer = TransformerFactory.newInstance().
                    newTransformer(domSource);
            StreamSource streamSource = new StreamSource(fis);
            StreamResult streamResult = new StreamResult(fos);

            transformer.setOutputProperty("indent", "no");
            transformer.transform(streamSource, streamResult);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }
}
