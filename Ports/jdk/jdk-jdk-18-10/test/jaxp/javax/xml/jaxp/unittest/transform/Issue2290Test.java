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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Issue2290Test
 * @run testng/othervm transform.Issue2290Test
 * @summary Test XSL extension for RTF works, for https://issues.apache.org/jira/i#browse/XALANJ-2290.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Issue2290Test {

    @Test
    public final void testTransform() throws Exception {
        DocumentFragment outNode = null;
        DocumentBuilder docBuilder = null;
        Document outDoc = null;
        // TransformerImpl transformer = null;
        StringReader execReaderXML = null;
        Properties propFormat = null;
        StringWriter sw = null;

        try {
            // template = TransformerFactory.newInstance().newTemplates(new
            // StreamSource("D:/Work/Apache/TestVar.xsl"));
            // transformer = (TransformerImpl) template.newTransformer();
            Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(getClass().getResourceAsStream("Issue2290.xsl")));
            System.out.print("Created Transformer");
            execReaderXML = new StringReader("<?xml version=\"1.0\"?> <doc>Stuff</doc>");


            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            outDoc = docBuilder.newDocument();
            outNode = outDoc.createDocumentFragment();
            System.out.println("Created Fragment");
            System.out.println("execute transformer.");
            // transformer.transform(new StreamSource(execReaderXML),new
            // DOMResult(outNode));
            t.transform(new StreamSource(execReaderXML), new DOMResult(outNode));
            System.out.println("Finsished transformer.");
            sw = new StringWriter();

            StreamResult sr = new StreamResult(sw);
            t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.transform(new DOMSource(outNode), sr);
            System.out.println(sw.toString());
        } catch (Exception e) {
            Assert.fail(e.toString());
        } finally {
        }

    }
}
