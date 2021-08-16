/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package common;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8144593
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @compile -XDignore.symbol.file TestSAXDriver.java
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.TransformationWarningsTest
 * @run testng/othervm common.TransformationWarningsTest
 * @summary Check that warnings about unsupported properties from parsers
 * are suppressed during the transformation process.
 */
@Listeners({jaxp.library.BasePolicy.class, jaxp.library.InternalAPIPolicy.class})
public class TransformationWarningsTest extends WarningsTestBase {

    @BeforeClass
    public void setup() {
        //Set test SAX driver implementation.
        setSystemProperty("org.xml.sax.driver", "common.TestSAXDriver");
    }

    @Test
    public void testTransformation() throws Exception {
        startTest();
    }

    //One iteration of xml transformation test case. It will be called from each
    //TestWorker task defined in WarningsTestBase class.
    void doOneTestIteration() throws Exception {
        // Prepare output stream
        StringWriter xmlResultString = new StringWriter();
        StreamResult xmlResultStream = new StreamResult(xmlResultString);
        // Prepare xml source stream
        Source src = new StreamSource(new StringReader(xml));
        Transformer t = createTransformer();
        //Transform the xml
        t.transform(src, xmlResultStream);
    }

    //Create transformer from xsl test string
    Transformer createTransformer() throws Exception {
        // Prepare sources for transormation
        Source xslsrc = new StreamSource(new StringReader(xsl));

        // Create factory and transformer
        TransformerFactory tf;
        // newTransformer() method doc states that different transformer
        // factories can be used concurrently by different Threads.
        synchronized (TransformerFactory.class) {
            tf = TransformerFactory.newInstance();
        }
        Transformer t = tf.newTransformer(xslsrc);

        // Set URI Resolver to return the newly constructed xml
        // stream source object from xml test string
        t.setURIResolver((String href, String base) -> new StreamSource(new StringReader(xml)));
        return t;
    }

    //Xsl and Xml contents used in the transformation test
    private static final String xsl = "<xsl:stylesheet version='2.0'"
            + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
            + " <xsl:output method='xml' indent='yes' omit-xml-declaration='yes'/>"
            + " <xsl:template match='/'>"
            + " <test>Simple Transformation Result. No warnings should be printed to console</test>"
            + " </xsl:template>"
            + "</xsl:stylesheet>";
    private static final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
}
