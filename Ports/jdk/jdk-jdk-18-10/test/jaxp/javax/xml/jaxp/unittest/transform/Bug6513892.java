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

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/*
 * @test
 * @bug 6513892
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6513892
 * @run testng/othervm transform.Bug6513892
 * @summary Test the output encoding of the transform is the same as that of the redirect extension.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6513892 {
    @BeforeClass
    public void setup(){
        if (System.getSecurityManager() != null)
            System.setSecurityManager(null);
    }

    @Test
    public void test0() {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer(new StreamSource(getClass().getResourceAsStream("redirect.xsl"), getClass().getResource("redirect.xsl")
                    .toString()));

            StreamSource src1 = new StreamSource(getClass().getResourceAsStream("redirect.xml"));
            t.transform(src1, new StreamResult("redirect1.xml"));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document d1 = db.parse(new File("redirect1.xml"));
            Document d2 = db.parse(new File("redirect2.xml"));

            Assert.assertTrue(d1.getDocumentElement().getFirstChild().getNodeValue().equals(d2.getDocumentElement().getFirstChild().getNodeValue()));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
