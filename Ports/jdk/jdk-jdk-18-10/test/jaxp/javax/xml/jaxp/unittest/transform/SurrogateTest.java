/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static jaxp.library.JAXPTestUtilities.compareStringWithGold;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8268457
 * @library /javax/xml/jaxp/libs
 * @run testng transform.SurrogateTest
 * @summary XML Transformer outputs Unicode supplementary character incorrectly to HTML
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SurrogateTest {

    final static String TEST_SRC = System.getProperty("test.src", ".");

    @Test
    public void toHTMLTest() throws Exception {
        String out = "SurrogateTest1out.html";
        String expected = TEST_SRC + File.separator + "SurrogateTest1.html";
        String xsl = TEST_SRC + File.separator + "SurrogateTest1.xsl";

        try (FileInputStream tFis = new FileInputStream(xsl);
            InputStream fis = this.getClass().getResourceAsStream("SurrogateTest1.xml");
            FileOutputStream fos = new FileOutputStream(out)) {

            Source tSrc = new StreamSource(tFis);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer(tSrc);
            t.setOutputProperty("method", "html");

            Source src = new StreamSource(fis);
            Result res = new StreamResult(fos);
            t.transform(src, res);
        }
        compareWithGold(expected, out);
    }

    @Test
    public void handlerTest() throws Exception {
        File xmlFile = new File(TEST_SRC, "SurrogateTest2.xml");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser sp = spf.newSAXParser();
        TestHandler th = new TestHandler();
        sp.parse(xmlFile, th);
        compareStringWithGold(TEST_SRC + File.separator + "SurrogateTest2.txt", th.sb.toString());
    }

    private static class TestHandler extends DefaultHandler {
        private StringBuilder sb = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            sb.append( localName + "@attr:" + attributes.getValue("attr") + '\n');
        }
    }
}
