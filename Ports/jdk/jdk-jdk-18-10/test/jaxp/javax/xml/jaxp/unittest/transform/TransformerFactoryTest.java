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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.TransformerFactoryTest
 * @run testng/othervm transform.TransformerFactoryTest
 * @summary Test TransformerFactory.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class TransformerFactoryTest {

    private static URIResolver resolver = new URIResolver() {

        private int n = 0;

        public Source resolve(String href, String base) throws TransformerException {

            System.out.println("resolving: " + href);

            if (n++ > 10) {
                Assert.fail("Nesting too deep when resolving: " + href);
            }

            return new StreamSource(this.getClass().getResourceAsStream(href));
        }
    };

    private static Document load(InputStream in) throws IOException {

        Document document = null;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(in);
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
            Assert.fail(parserConfigurationException.toString());
        } catch (SAXException saxException) {
            saxException.printStackTrace();
            Assert.fail(saxException.toString());
        }

        return document;
    }

    /**
     * <p>
     * Test stylesheets that import other stylesheets.
     * </p>
     *
     * <p>
     * Inspired by: CR 6236727-2125981 XSLTC never stops resolving imported
     * stylesheets when outer stylesheet is a DOMSource
     * </p>
     */
    @Test
    public final void testImport() {

        TransformerFactory tff = TransformerFactory.newInstance();
        tff.setURIResolver(resolver);
        Templates tt = null;
        Transformer tf = null;

        // work-a-round is to use a StreamSource.
        // test should complete
        System.out.println("StreamSource: pre-Transformer creation");
        System.out.flush(); // in case import hangs
        try {
            InputStream xin = this.getClass().getResourceAsStream("outer.xsl");
            tt = tff.newTemplates(new StreamSource(xin));
            tf = tt.newTransformer();
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.toString());
        }
        System.out.println("StreamSource: post-Transformer creation");

        // CR is that DOMSource never stops resolving
        System.out.println("DOMSource: pre-Transformer creation");
        System.out.flush(); // in case import hangs
        try {
            InputStream xin = this.getClass().getResourceAsStream("outer.xsl");
            tt = tff.newTemplates(new DOMSource(load(xin)));
            tf = tt.newTransformer();
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            Assert.fail(ioException.toString());
        }
        System.out.println("DOMSource: post-Transformer creation");
    }

    /**
     * Refer to 6631168 : StAXSource & StAXResult support in JavaSE6
     */
    @Test
    public final void testFeatures() {
        TransformerFactory tff = TransformerFactory.newInstance();
        Assert.assertTrue(tff.getFeature(StAXSource.FEATURE));
        Assert.assertTrue(tff.getFeature(StAXResult.FEATURE));
    }

}
