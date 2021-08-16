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

import java.io.File;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6941869
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.CR6941869Test
 * @run testng/othervm transform.CR6941869Test
 * @summary Test XSLT evaluate "count(.|key('props', d/e)[1])" correctly.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR6941869Test {

    @Test
    public final void testTransform() {
        File xml = new File(getClass().getResource("CR6941869.xml").getFile());
        File xsl = new File(getClass().getResource("CR6941869.xsl").getFile());
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            StreamSource source = new StreamSource(xsl);
            transformer = tFactory.newTransformer(source);
            // the xml result
            StringWriter xmlResultString = new StringWriter();
            StreamResult xmlResultStream = new StreamResult(xmlResultString);

            transformer.transform(new StreamSource(xml), xmlResultStream);
            System.out.println(xmlResultString.toString());
            String temp = xmlResultString.toString();
            int pos = temp.lastIndexOf("count");
            if (temp.substring(pos + 8, pos + 9).equals("1")) {
                Assert.fail("count=1");
            } else if (temp.substring(pos + 8, pos + 9).equals("2")) {
                // expected success
                System.out.println("count=2");
            }
        } catch (Exception e) {
            // unexpected failure
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }
}
