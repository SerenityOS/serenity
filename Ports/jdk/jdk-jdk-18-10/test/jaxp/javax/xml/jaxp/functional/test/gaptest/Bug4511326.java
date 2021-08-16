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

import java.io.StringReader;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 4511326
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.gaptest.Bug4511326
 * @run testng/othervm test.gaptest.Bug4511326
 * @summary In forwards-compatible mode the attribute isn't ignored
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4511326 {

    private static final String XSL = "<xsl:stylesheet version='2.0' "
                               + "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
                               + "<xsl:template a='1' match='/'>"
                               + "<H2><xsl:value-of select='//author'/></H2>"
                               + "<H1><xsl:value-of select='//title'/></H1>"
                               + "</xsl:template>"
                               + "</xsl:stylesheet>";


    @Test
    public void ignoreAttTest() throws TransformerConfigurationException {
        /* Create a TransformFactory instance */
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        /* Create and init a StreamSource instance */
        StreamSource source = new StreamSource(new StringReader(XSL));

        transformerFactory.newTransformer(source);
    }

}
