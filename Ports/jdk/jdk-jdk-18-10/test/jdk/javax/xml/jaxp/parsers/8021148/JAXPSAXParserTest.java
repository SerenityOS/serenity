/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8021148
 * @summary test that JAXPSAXParser works even if referenced directly
 * @run main/othervm JAXPSAXParserTest
 */
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * test that JAXPSAXParser works even if referenced directly as
 * NetBeans did. **Note that JAXPSAXParser is an internal implementation, this
 * may therefore change.
 *
 * @author huizhe.wang@oracle.com
 */
public class JAXPSAXParserTest extends TestBase {

    public JAXPSAXParserTest(String name) {
        super(name);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JAXPSAXParserTest test = new JAXPSAXParserTest("JAXP 1.5 regression");
        test.setUp();
        test.testTransform();
        test.tearDown();
    }

    public final void testTransform() {
        String data =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<r>\n"
                + "    <e/>\n"
                + "</r>\n";
        String IDENTITY_XSLT_WITH_INDENT = // #5064280 workaround
                "<xsl:stylesheet version='1.0' "
                + "xmlns:xsl='http://www.w3.org/1999/XSL/Transform' "
                + "xmlns:xalan='http://xml.apache.org/xslt' "
                + "exclude-result-prefixes='xalan'>"
                + "<xsl:output method='xml' indent='yes' xalan:indent-amount='4'/>"
                + "<xsl:template match='@*|node()'>"
                + "<xsl:copy>"
                + "<xsl:apply-templates select='@*|node()'/>"
                + "</xsl:copy>"
                + "</xsl:template>"
                + "</xsl:stylesheet>";
        try {
            //Skip the default XMLReader
            System.setProperty("org.xml.sax.driver", "com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl$JAXPSAXParser");

            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer(new StreamSource(new StringReader(IDENTITY_XSLT_WITH_INDENT)));
            Result result = new StreamResult(sw);
            t.transform(new StreamSource(new StringReader(data)), result);
            success("JAXPSAXParserTest passed");
        } catch (Exception e) {
            /**
             * JAXPSAXParser throws NullPointerException since the jaxp 1.5 security
             * manager is not initialized when JAXPSAXParser is instantiated using
             * the default constructor.
            */
            fail(e.toString());
        } finally {
            System.clearProperty("org.xml.sax.driver");
        }
    }
}
