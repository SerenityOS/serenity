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

import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/*
 * @test
 * @bug 4966082
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug4966082
 * @run testng/othervm dom.Bug4966082
 * @summary Test Element.getSchemaTypeInfo() returns an instance of TypeInfo instead of null when the document's schema is an XML DTD.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4966082 {

    @Test
    public void testOne() {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Bug4966082.class.getResource("Bug4966082.xml").toExternalForm());
            if (document.getDocumentElement().getSchemaTypeInfo() == null) {
                Assert.fail("getSchemaTypeInfo returns null");
            }
        } catch (Exception ex) {
            Assert.fail("Unexpected  error" + ex);
        }
    }
}
