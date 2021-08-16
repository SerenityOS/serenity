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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6879614
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug6879614Test
 * @run testng/othervm dom.Bug6879614Test
 * @summary Test DocumentBuilder can parse the certain xml.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6879614Test {

    @Test
    public void testAttributeCaching() {
        File xmlFile = new File(getClass().getResource("Bug6879614.xml").getFile());
        DocumentBuilderFactory _documentBuilderFactory = DocumentBuilderFactory.newInstance();
        _documentBuilderFactory.setValidating(false);
        _documentBuilderFactory.setIgnoringComments(true);
        _documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        _documentBuilderFactory.setCoalescing(true);
        _documentBuilderFactory.setExpandEntityReferences(true);
        _documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder _documentBuilder = null;
        try {
            _documentBuilder = _documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }

        Document xmlDoc = null;
        try {
            xmlDoc = _documentBuilder.parse(xmlFile);
            if (xmlDoc == null) {
                System.out.println("Hello!!!, there is a problem here");
            } else {
                System.out.println("Good, the parsing went through fine.");
            }
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
