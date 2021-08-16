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

import static javax.xml.transform.ptests.TransformerTestConst.XML_DIR;
import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.failUnexpected;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Test a StreamResult using a file name that contains URL characters that need
 * to be encoded.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.StreamResultTest
 * @run testng/othervm javax.xml.transform.ptests.StreamResultTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class StreamResultTest {
    /**
     * Unit test for StreamResult.
     */
    @Test
    public void testcase01() {
        // Set Transformer properties
        Properties transformProperties = new Properties();
        transformProperties.put("method", "xml");
        transformProperties.put("encoding", "UTF-8");
        transformProperties.put("omit-xml-declaration", "yes");
        transformProperties.put("{http://xml.apache.org/xslt}indent-amount", "0");
        transformProperties.put("indent", "no");
        transformProperties.put("standalone", "no");
        transformProperties.put("version", "1.0");
        transformProperties.put("media-type", "text/xml");

        String[] fileNames = {
            "StreamResult01.out",
            "StreamResult 02.out",
            "StreamResult#03.out"
        };

        String xslFile = XML_DIR + "cities.xsl";
        String xmlFile = XML_DIR + "cities.xml";

        Arrays.stream(fileNames).forEach(file -> {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document document = db.parse(new File(xslFile));
                DOMSource domSource = new DOMSource(document);
                StreamSource streamSource = new StreamSource(new FileInputStream(xmlFile));

                File streamResultFile = new File(USER_DIR + file);
                StreamResult streamResult = new StreamResult(streamResultFile);

                Transformer transformer = TransformerFactory.newInstance().newTransformer(domSource);
                transformer.setOutputProperties(transformProperties);
                transformer.transform(streamSource, streamResult);
            } catch (SAXException | IOException | ParserConfigurationException
                    | TransformerException ex) {
                failUnexpected(ex);
            }
        });
    }
}
