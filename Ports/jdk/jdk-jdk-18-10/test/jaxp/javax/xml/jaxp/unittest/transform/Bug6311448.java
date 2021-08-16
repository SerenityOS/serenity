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

package transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * @test
 * @bug 6311448
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6311448
 * @run testng/othervm transform.Bug6311448
 * @summary Test XML transformer can output Unicode surrorate pair.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6311448 {

    @Test
    public void test01() {
        try {
            String attrKey = "key";
            String attrValue = "\ud800\udc00"; // 17-bit code point in UTF-16

            // Some obvious assertions for documentation purposes
            Assert.assertTrue(Character.isSurrogatePair('\ud800', '\udc00'));
            Assert.assertTrue(Character.toCodePoint('\ud800', '\udc00') == 65536);
            Assert.assertTrue(Character.charCount(Character.toCodePoint('\ud800', '\udc00')) == 2);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Transformer t = TransformerFactory.newInstance().newTransformer();

            // Create a DOM with 'attrValue' in it
            Document doc = dbf.newDocumentBuilder().getDOMImplementation().createDocument(null, null, null);
            Element xmlRoot = doc.createElement("root");
            xmlRoot.setAttribute(attrKey, attrValue);
            doc.appendChild(xmlRoot);

            // Serialize DOM into a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.setOutputProperty("encoding", "utf-8");
            t.transform(new DOMSource(doc), new StreamResult(baos));

            // Re-parse byte array back into a DOM
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            doc = dbf.newDocumentBuilder().parse(bais);
            String newValue = doc.getDocumentElement().getAttribute(attrKey);
            Assert.assertTrue(newValue.charAt(0) == '\ud800' && newValue.charAt(1) == '\udc00');
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
