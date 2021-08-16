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
 * @bug 6905829
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Issue2204Test
 * @run testng/othervm transform.Issue2204Test
 * @summary Test XSLT can work against the certain xsl.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Issue2204Test {

    @Test
    public final void testTransform() {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(getClass().getResourceAsStream("Issue2204.xsl")));

            System.out.printf("transformer: %s%n", t.getClass().getName());

            StringWriter streamResult = new StringWriter();
            t.transform(new StreamSource(getClass().getResourceAsStream("Issue2204.xml")), new StreamResult(streamResult));

            System.out.println(streamResult.toString());
            if (streamResult.toString().indexOf("3") > 0) {
                Assert.fail("Function Count on variable modifies number of nodes in variable.");
            }
            // expected success
        } catch (Exception e) {
            // unexpected failure
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }
}
