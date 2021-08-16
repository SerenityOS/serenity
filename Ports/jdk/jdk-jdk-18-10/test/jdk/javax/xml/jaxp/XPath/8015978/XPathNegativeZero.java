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

/*
 * @test
 * @bug 8015978
 * @summary Incorrect transformation of XPath expression "string(-0)"
 * @run main XPathNegativeZero
 * @author aleksej.efimov@oracle.com
 */

import java.io.File;
import java.io.StringWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;


public class XPathNegativeZero {

    static final String EXPECTEDXML = "<newtop>\"0\"</newtop>";

    public static void main(final String[] args) throws Exception {
        //file name of XML file to transform
        final String xml = System.getProperty("test.src", ".")+"/dummy.xml";
        //file name of XSL file w/ transformation
        final String xsl = System.getProperty("test.src", ".")+"/negativezero.xsl";
        final String result = xform(xml, xsl).trim();

        System.out.println("transformed XML: '"+result+ "' expected XML: '"+EXPECTEDXML+"'");
        if (!result.equals(EXPECTEDXML))
            throw new Exception("Negative zero was incorrectly transformed");
    }

    private static String xform(final String xml, final String xsl) throws Exception {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Source xslsrc = new StreamSource(new File(xsl));
        final Templates tmpl = tf.newTemplates(xslsrc);
        final Transformer t = tmpl.newTransformer();

        StringWriter writer = new StringWriter();
        final Source src = new StreamSource(new File(xml));
        final Result res = new StreamResult(writer);

        t.transform(src, res);
        return writer.toString();
    }
}
