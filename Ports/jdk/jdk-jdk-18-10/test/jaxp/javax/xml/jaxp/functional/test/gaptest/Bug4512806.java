/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test.gaptest;

import static javax.xml.transform.OutputKeys.ENCODING;
import static javax.xml.transform.OutputKeys.INDENT;
import static org.testng.Assert.assertEquals;

import java.io.StringReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 4512806
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.gaptest.Bug4512806
 * @run testng/othervm test.gaptest.Bug4512806
 * @summary test transformer.setOutputProperties(null)
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4512806 {

    @Test
    public void testProperty() throws TransformerConfigurationException {
        /* Create a transform factory instance */
        TransformerFactory tfactory = TransformerFactory.newInstance();

        /* Create a StreamSource instance */
        StreamSource streamSource = new StreamSource(new StringReader(xslData));

        transformer = tfactory.newTransformer(streamSource);
        transformer.setOutputProperty(INDENT, "no");
        transformer.setOutputProperty(ENCODING, "UTF-16");

        assertEquals(printPropertyValue(INDENT), "indent=no");
        assertEquals(printPropertyValue(ENCODING), "encoding=UTF-16");

        transformer.setOutputProperties(null);

        assertEquals(printPropertyValue(INDENT), "indent=yes");
        assertEquals(printPropertyValue(ENCODING), "encoding=UTF-8");

    }

    private String printPropertyValue(String name) {
        return name + "=" + transformer.getOutputProperty(name);
    }

    private Transformer transformer;

    private static final String xslData = "<?xml version='1.0'?>"
            + "<xsl:stylesheet"
            + " version='1.0'"
            + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'"
            + ">\n"
            + "   <xsl:output method='xml' indent='yes'"
            + " encoding='UTF-8'/>\n"
            + "   <xsl:template match='/'>\n"
            + "     Hello World! \n"
            + "   </xsl:template>\n"
            + "</xsl:stylesheet>";


}
