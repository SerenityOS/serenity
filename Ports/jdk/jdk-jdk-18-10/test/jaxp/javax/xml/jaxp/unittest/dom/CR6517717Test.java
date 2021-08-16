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

package dom;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Entity;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6517717
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.CR6517717Test
 * @run testng/othervm dom.CR6517717Test
 * @summary Test Node.setPrefix(prefix) shall throw DOMException.NO_MODIFICATION_ALLOWED_ERR if the node is read-only.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class CR6517717Test {

    @Test
    public void testCanonicalForm001() {
        String data = "<?xml version=\"1.0\" ?>" + "<!DOCTYPE test:root [" + "<!ELEMENT test:root ANY>" + "<!ENTITY ent \"foo\">"
                + "<!ATTLIST test:root test:a CDATA #FIXED \"qqq\">" + "]>" + "<test:root xmlns:test=\"http://xxxx.xx/\">" + "</test:root>";

        Document document = null;
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = docBuilder.parse(new InputSource(new StringReader(data)));
        } catch (ParserConfigurationException e) {
            System.out.println(e.toString());
        } catch (IOException e) {
            System.out.println(e.toString());
        } catch (SAXException e) {
            System.out.println(e.toString());
        }

        Entity anEntity = (Entity) document.getDoctype().getEntities().item(0);
        boolean success = false;
        try {
            anEntity.setPrefix("test1");
            System.out.println("Should throw DOMException: NO_MODIFICATION_ALLOWED_ERR ");
        } catch (DOMException e) {
            if (e.code == DOMException.NO_MODIFICATION_ALLOWED_ERR) {
                System.out.println("OK");
                success = true;
            } else {
                System.out.println("should throw DOMException.NO_MODIFICATION_ALLOWED_ERR (7). The error returned is (" + e.code + ")" + e.getMessage());
            }
        }
        if (!success) {
            Assert.fail("should throw DOMException.NO_MODIFICATION_ALLOWED_ERR (7).");
        }

    }
}
