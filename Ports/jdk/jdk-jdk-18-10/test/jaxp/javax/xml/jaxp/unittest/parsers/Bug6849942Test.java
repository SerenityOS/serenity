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

package parsers;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 6849942
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6849942Test
 * @run testng/othervm parsers.Bug6849942Test
 * @summary Test parsing an XML that starts with a processing instruction and no prolog.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6849942Test {

    @Test
    public void test() throws Exception {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream("<?xmltarget foo?><test></test>".getBytes());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder xmlParser = factory.newDocumentBuilder();
            // DOMParser p = new DOMParser();
            Document document = xmlParser.parse(new InputSource(bais));
            String result = ((ProcessingInstruction) document.getFirstChild()).getData();
            System.out.println(result);
            if (!result.equalsIgnoreCase("foo")) {
                Assert.fail("missing PI data");
            }

        } catch (Exception e) {
        }
    }

    @Test
    public void testWProlog() throws Exception {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream("<?xml version=\"1.1\" encoding=\"UTF-8\"?><?xmltarget foo?><test></test>".getBytes());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder xmlParser = factory.newDocumentBuilder();
            // DOMParser p = new DOMParser();
            Document document = xmlParser.parse(new InputSource(bais));
            String result = ((ProcessingInstruction) document.getFirstChild()).getData();
            System.out.println(result);
            if (!result.equalsIgnoreCase("foo")) {
                Assert.fail("missing PI data");
            }
        } catch (Exception e) {
        }
    }
}
