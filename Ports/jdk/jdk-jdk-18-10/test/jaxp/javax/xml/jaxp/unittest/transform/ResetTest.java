/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Test;
import javax.xml.transform.OutputKeys;
import org.testng.Assert;

/*
 * @test
 * @bug 8180901
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm transform.ResetTest
 * @summary Verifies reset functions
 */
public class ResetTest {

    private static final String INPUT_XML = "<a><b>123</b><c/></a>";
    /*
     * Verifies that the reset method sets the Transformer to its original state
     * properly.
    */
    @Test
    public void testReset() throws Exception {
        TransformerFactory transformerFactory =
                new com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl();

        // run #1
        Transformer transformer = transformerFactory.newTransformer();

        setDefaultOutputProperties(transformer);
        String outputXml1 = transform(INPUT_XML, transformer);
        System.out.println("#1 output XML: " + outputXml1);

        // run #2
        transformer.reset();

        setDefaultOutputProperties(transformer);
        // use different output properties in run #2 (after the 1st reset):
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        String outputXml2 = transform(INPUT_XML, transformer);
        System.out.println("#2 output XML: " + outputXml2);

        // run #3
        transformer.reset();

        setDefaultOutputProperties(transformer);
        // use same output properties as run #1 => expect same output
        String outputXml3 = transform(INPUT_XML, transformer);
        System.out.println("#3 output XML: " + outputXml3);
        Assert.assertEquals(outputXml3, outputXml1, "Output is expected to be equal after reset.");
    }

    private static void setDefaultOutputProperties(Transformer transformer) {
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    private static String transform(String xml, Transformer transformer)
            throws TransformerException, UnsupportedEncodingException {
        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))),
                              new StreamResult(writer));
        return writer.toString();
    }
}
