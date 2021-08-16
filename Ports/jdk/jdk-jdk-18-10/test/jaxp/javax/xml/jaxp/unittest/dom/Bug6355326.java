/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package dom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6355326
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug6355326
 * @run testng/othervm dom.Bug6355326
 * @summary Test DOM implementation encoding.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6355326 {

    DOMImplementationLS implLS = null;
    String encodingXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><encodingXML/>";

    @BeforeMethod
    public void setUp() {
        Document doc = null;
        DocumentBuilder parser = null;
        String xml1 = "<?xml version=\"1.0\"?><ROOT></ROOT>";
        try {
            parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        StringBufferInputStream is = new StringBufferInputStream(xml1);
        try {
            doc = parser.parse(is);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DOMImplementation impl = doc.getImplementation();
        implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
    }

    @Test
    public void testExternalEncoding() {

        try {
            LSInput src = null;
            LSParser dp = null;

            src = createLSInputEncoding();
            dp = createLSParser();

            src.setEncoding("UTF-16");
            Document doc = dp.parse(src);
            Assert.assertTrue("encodingXML".equals(doc.getDocumentElement().getNodeName()), "XML document is not parsed correctly");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    private LSInput createLSInputEncoding() {
        LSInput src = implLS.createLSInput();
        Assert.assertFalse(src == null, "Could not create LSInput from DOMImplementationLS");

        try {
            src.setByteStream(new ByteArrayInputStream(encodingXML.getBytes("UTF-16")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
        return src;
    }

    private LSParser createLSParser() {
        LSParser p = implLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/2001/XMLSchema");
        Assert.assertFalse(p == null, "Could not create Synchronous LSParser from DOMImplementationLS");
        return p;
    }
}
