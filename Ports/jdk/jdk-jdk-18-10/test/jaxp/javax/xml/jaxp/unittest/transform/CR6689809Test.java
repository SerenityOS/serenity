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

import java.io.CharArrayWriter;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6689809
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.CR6689809Test
 * @run testng/othervm transform.CR6689809Test
 * @summary Test Transformer can handle XPath predicates in xsl:key elements.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR6689809Test {

    @Test
    public final void testTransform() {

        try {
            StreamSource input = new StreamSource(getClass().getResourceAsStream("PredicateInKeyTest.xml"));
            StreamSource stylesheet = new StreamSource(getClass().getResourceAsStream("PredicateInKeyTest.xsl"));
            CharArrayWriter buffer = new CharArrayWriter();
            StreamResult output = new StreamResult(buffer);

            TransformerFactory.newInstance().newTransformer(stylesheet).transform(input, output);

            Assert.assertEquals(buffer.toString(), "0|1|2|3", "XSLT xsl:key implementation is broken!");
            // expected success
        } catch (Exception e) {
            // unexpected failure
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }
}
