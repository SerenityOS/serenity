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

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Consumer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.IssueTracker70
 * @run testng/othervm stream.XMLStreamReaderTest.IssueTracker70
 * @summary Test it can retrieve attribute with null or empty name space.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class IssueTracker70 {

    static private final File testFile = new File(IssueTracker70.class.getResource("IssueTracker70.xml").getFile());

    @Test
    public void testGetAttributeValueWithNullNs() throws Exception {
        testGetAttributeValueWithNs(null, "attribute2", this::checkNull);
    }

    @Test
    public void testGetAttributeValueWithEmptyNs() throws Exception {
        testGetAttributeValueWithNs("", "attribute1", this::checkNull);
    }


    private void testGetAttributeValueWithNs(String nameSpace, String attrName, Consumer<String> checker) throws Exception {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = xif.createXMLStreamReader(new FileInputStream(testFile));

        while (xsr.hasNext()) {
            xsr.next();
            if (xsr.isStartElement()) {
                String v;
                v = xsr.getAttributeValue(nameSpace, attrName);
                checker.accept(v);
            }
        }
    }

    private void checkNull(String value)
    {
        Assert.assertNotNull(value, "should have attribute value");
    }
}
