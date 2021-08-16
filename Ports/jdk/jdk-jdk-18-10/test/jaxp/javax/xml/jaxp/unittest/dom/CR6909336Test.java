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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/*
 * @test
 * @bug 6909336
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.CR6909336Test
 * @run testng/othervm dom.CR6909336Test
 * @summary Test DOM writer can write more that 20 nested elements.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class CR6909336Test {

    @Test
    public void test() {
        try {
            Document doc = DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0").createDocument("", "root", null);
            XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(new DOMResult(doc.getDocumentElement()));
            for (int i = 0; i < 30; ++i) {
                xsw.writeStartElement("nested");
            }
        } catch (RuntimeException ex) {
            System.out.println("RuntimeException ex" + ex.getMessage());
            if (ex.getMessage().equals("20")) {
                Assert.fail("XMLDOMWriter cannot write more that 20 nested elements");
            }
        } catch (XMLStreamException ex) {
            System.out.println("XMLStreamException ex" + ex.getMessage());
        } catch (ClassNotFoundException ex) {
            System.out.println("ClassNotFoundException ex" + ex.getMessage());
        } catch (InstantiationException ex) {
            System.out.println("InstantiationException ex" + ex.getMessage());
        } catch (IllegalAccessException ex) {
            System.out.println("IllegalAccessException ex" + ex.getMessage());

        }

    }

}
