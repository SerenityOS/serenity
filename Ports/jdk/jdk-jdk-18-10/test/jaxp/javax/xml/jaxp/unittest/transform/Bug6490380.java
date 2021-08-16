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
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6490380
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6490380
 * @run testng/othervm transform.Bug6490380
 * @summary Test only a single DOCTYPE declaration is generated through transforming.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6490380 {

    @Test
    public void test() {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            URL input = Bug6490380.class.getResource("Bug4693341.xml");
            StreamSource source = new StreamSource(input.openStream(), input.toString());
            StringWriter sw = new StringWriter();
            transformer.transform(source, new StreamResult(sw));
            String s = sw.toString();
            Assert.assertEquals(s.indexOf("!DOCTYPE"), s.lastIndexOf("!DOCTYPE"));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
