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

package transform;

import java.io.StringReader;
import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.TransformerConfigurationException;
import org.testng.annotations.Listeners;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8058152
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.StylesheetTest
 * @run testng/othervm transform.StylesheetTest
 * @summary this test contains test cases for verifying stylesheet
 */
@Listeners(jaxp.library.FilePolicy.class)
public class StylesheetTest {

    /**
     * @bug 8058152
     * Verifies that an error is reported if the xsl:import element
     * is not at the top of the stylesheet.
     * @throws TransformerConfigurationException
     */
    @Test(dataProvider = "invalidImport", expectedExceptions = TransformerConfigurationException.class)
    public void testInvalidImport(String xsl) throws TransformerConfigurationException {
        StringReader xsl1 = new StringReader(xsl);
        TransformerFactory factory = TransformerFactory.newInstance();
        SAXSource xslSource = new SAXSource(new InputSource(xsl1));
        Transformer transformer = factory.newTransformer(xslSource);
    }

    /**
     * @bug 8058152
     * Verifies that valid xsl:import elements are accepted
     * @throws TransformerConfigurationException
     */
    @Test(dataProvider = "validImport")
    public void testValidImport(String file) throws TransformerConfigurationException {
        String xsl = getClass().getResource(file).getFile();
        TransformerFactory factory = TransformerFactory.newInstance();
        SAXSource xslSource = new SAXSource(new InputSource(xsl));
        Transformer transformer = factory.newTransformer(xslSource);
    }

    /*
       DataProvider: for testing with xsl:import placed incorrectly
       Data: stylesheet
     */
    @DataProvider(name = "invalidImport")
    public Object[][] getInvalid() {

        return new Object[][]{
            // xsl:import after template and include elements
            {"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n"
                + "\n"
                + "    <xsl:template match=\"content\">\n"
                + "        <html>\n"
                + "            <xsl:apply-templates/>\n"
                + "        </html>\n"
                + "    </xsl:template>\n"
                + "  \n"
                + "    <xsl:include href=\"XSLInclude_header.xsl\"/>\n"
                + "\n"
                + "    <xsl:template match=\"content/title\">\n"
                + "        <h1>\n"
                + "            <xsl:apply-templates/>\n"
                + "        </h1>\n"
                + "    </xsl:template>\n"
                + "  \n"
                + "    <xsl:import href=\"XSLInclude_footer.xsl\"/>\n"
                + "\n"
                + "</xsl:stylesheet>"},

            // xsl:import inside template
            {"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n"
                + "\n"
                + "    <xsl:template match=\"content\">\n"
                + "        <xsl:import href=\"XSLInclude_header.xsl\"/>"
                + "        <html>\n"
                + "            <xsl:apply-templates/>\n"
                + "        </html>\n"
                + "    </xsl:template>\n"
                + "\n"
                + "</xsl:stylesheet>"},

            // xsl:import after xsl:include
            {"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n"
                + "    <xsl:include href=\"XSLInclude_header.xsl\"/>\n"
                + "    <xsl:import href=\"XSLInclude_footer.xsl\"/>\n"
                + "\n"
                + "    <xsl:template match=\"content/title\">\n"
                + "        <h1>\n"
                + "            <xsl:apply-templates/>\n"
                + "        </h1>\n"
                + "    </xsl:template>\n"
                + "\n"
                + "</xsl:stylesheet>"}
        };
    }

    /*
       DataProvider: for testing with xsl:import placed correctly
       Data: path to stylesheet
     */
    @DataProvider(name = "validImport")
    public Object[][] getValid() {

        return new Object[][]{
            // xsl:import at the top
            {"XSLInclude_main.xsl"},

            // two xsl:import elements at the top
            {"XSLImport.xsl"}
        };
    }
}
