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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/*
 * @test
 * @bug 6652519
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.CR6652519Test
 * @run testng/othervm transform.CR6652519Test
 * @summary Test transfoming from StreamSource to DOMResult.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR6652519Test {

    @Test
    public final void test1() {
        try {
            long start = System.currentTimeMillis();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            File file = new File(getClass().getResource("msgAttach.xml").getFile());
            StreamSource source = new StreamSource(file);
            DOMResult result = new DOMResult();
            t.transform(source, result);

            long end = System.currentTimeMillis();
            System.out.println("Test2:Total Time Taken=" + (end - start));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public final void xtest2() {
        try {
            long start = System.currentTimeMillis();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(getClass().getResource("msgAttach.xml").getFile()));
            long end = System.currentTimeMillis();
            System.out.println("Test1: Total Time Taken=" + (end - start));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
