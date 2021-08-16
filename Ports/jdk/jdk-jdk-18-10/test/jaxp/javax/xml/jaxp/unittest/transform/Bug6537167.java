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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6537167
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6537167
 * @run testng/othervm transform.Bug6537167
 * @summary Test transforming for particular xsl files.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6537167 {

    @Test
    public void test926007_1() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        File f = new File(getClass().getResource("logon.xsl").getPath());
        Templates t = factory.newTemplates(new StreamSource(f));
        Transformer transformer = t.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new StreamSource(getClass().getResourceAsStream("src.xml")), new StreamResult(System.out));
    }

    @Test
    public void test926007_2() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        // factory.setAttribute("generate-translet", Boolean.TRUE);
        File f = new File(getClass().getResource("home.xsl").getPath());
        Templates t = factory.newTemplates(new StreamSource(f));
        Transformer transformer = t.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new StreamSource(getClass().getResourceAsStream("src.xml")), new StreamResult(System.out));
    }

    @Test
    public void test926007_3() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        // factory.setAttribute("generate-translet", Boolean.TRUE);
        File f = new File(getClass().getResource("upload-media.xsl").getPath());
        Templates t = factory.newTemplates(new StreamSource(f));
        Transformer transformer = t.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new StreamSource(getClass().getResourceAsStream("src.xml")), new StreamResult(System.out));
    }

}
