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

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 4915524
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug4915524
 * @run testng/othervm dom.Bug4915524
 * @summary Test Document.adoptNode() shall not throw Exception when the source document object is created from different implementation.
 */

@Listeners({jaxp.library.BasePolicy.class})
public class Bug4915524 {

    String data = "<?xml version=\"1.0\" ?>" + "<!DOCTYPE root [" + "<!ELEMENT root ANY>" + "<!ATTLIST root attr1 ID #FIXED 'xxx'"
            + "               attr2 CDATA #IMPLIED> " + "]>" + "<root attr2='yyy'/>";

    DocumentBuilder docBuilder = null;

    /*
     * This method tries to adopt a node from Defered document to non-defered
     * document.
     */
    @Test
    public void testAdoptNode() {
        try {
            DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
            docBuilder = docBF.newDocumentBuilder();

            Document doc1 = parse(data);
            Document doc2 = docBuilder.newDocument();

            Node element = doc2.adoptNode(doc1.getDocumentElement());

            System.out.println("OK.");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Excpetion while adopting node: " + e.getMessage());
        }

    }

    private Document parse(String xmlData) throws Exception {
        StringReader in = new StringReader(xmlData);
        InputSource source = new InputSource(in);
        return docBuilder.parse(source);
    }
}
