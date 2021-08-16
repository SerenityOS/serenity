/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLStreamReaderTest;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.Assert;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/*
 * @test
 * @bug 6847819
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.Bug6847819Test
 * @run testng/othervm stream.XMLStreamReaderTest.Bug6847819Test
 * @summary Test StAX parser shall throw XMLStreamException for illegal xml declaration.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6847819Test {

    @Test
    public void testIllegalDecl() throws XMLStreamException {
        String xml = "<?xml ?><root>abc]]>xyz</root>";
        String msg = "illegal declaration";
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.createXMLStreamReader(new StringReader(xml));
            Assert.fail("Expected an exception for " + msg);
        } catch (XMLStreamException ex) { // good
            System.out.println("Expected failure: '" + ex.getMessage() + "' " + "(matching message: '" + msg + "')");
        } catch (Exception ex2) { // ok; iff links to XMLStreamException
            Throwable t = ex2;
            while (t.getCause() != null && !(t instanceof XMLStreamException)) {
                t = t.getCause();
            }
            if (t instanceof XMLStreamException) {
                System.out.println("Expected failure: '" + ex2.getMessage() + "' " + "(matching message: '" + msg + "')");
            }
            if (t == ex2) {
                Assert.fail("Expected an XMLStreamException (either direct, or getCause() of a primary exception) for " + msg + ", got: " + ex2);
            }
            Assert.fail("Expected an XMLStreamException (either direct, or getCause() of a primary exception) for " + msg + ", got: " + ex2 + " (root: " + t + ")");
        }

    }

}
