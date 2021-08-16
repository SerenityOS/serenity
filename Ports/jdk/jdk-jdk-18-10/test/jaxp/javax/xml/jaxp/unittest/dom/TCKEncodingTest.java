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

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.TCKEncodingTest
 * @run testng/othervm dom.TCKEncodingTest
 * @summary Test Document.getInputEncoding().
 */
@Listeners({jaxp.library.BasePolicy.class})
public class TCKEncodingTest {

    /**
     * Assertion testing
     * for public String getInputEncoding(),
     * An attribute specifying the actual encoding of this document..
     */
    @Test
    public void testGetInputEncoding001() {
        String data = "<?xml version=\"1.0\"?>" + "<!DOCTYPE root [" + "<!ELEMENT root ANY>" + "]>" + "<root/>";

        Document doc = null;
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inSource = new InputSource(new StringReader(data));
            inSource.setEncoding("UTF-8");
            inSource.setSystemId("test.xml");
            doc = docBuilder.parse(inSource);
        } catch (ParserConfigurationException e) {
            Assert.fail(e.toString());
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (SAXException e) {
            Assert.fail(e.toString());
        }

        String encoding = doc.getInputEncoding();
        if (encoding == null || !encoding.equals("UTF-8")) {
            Assert.fail("expected encoding: UTF-8, returned: " + encoding);
        }

        System.out.println("OK");
    }

    /**
     * Assertion testing
     * for public String getInputEncoding(),
     * Encoding is not specified. getInputEncoding returns null..
     */
    @Test
    public void testGetInputEncoding002() {
        Document doc = null;
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = db.newDocument();
        } catch (ParserConfigurationException e) {
            Assert.fail(e.toString());
        }

        String encoding = doc.getInputEncoding();
        if (encoding != null) {
            Assert.fail("expected encoding: null, returned: " + encoding);
        }

        System.out.println("OK");
    }
}
