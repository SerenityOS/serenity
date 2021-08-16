/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLStreamFilterTest;

import static org.testng.Assert.assertThrows;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.testng.annotations.Test;

/*
* @test
* @bug 8255918
* @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
* @run testng stream.XMLStreamFilterTest.XMLStreamReaderFilterTest
* @summary Test the implementation of {@code XMLStreamReader} using a {@code StreamFilter}
*/
public class XMLStreamReaderFilterTest {

    static final String XMLSOURCE1 = "<root>\n"
            + "  <element1>\n"
            + "    <element2>\n" // Unclosed element2
            + "  </element1>\n"
            + "  <element3>\n"
            + "  </element3>\n"
            + "</root>";

    /**
     * Verifies that XMLStreamException is thrown as specified by the
     * {@code XMLInputFactory::createFilteredReader} method when an error
     * is encountered. This test illustrates the scenario by creating a
     * reader with a filter that requires the original reader to advance
     * past the invalid element in the underlying XML.
     *
     * @throws Exception When an unexpected exception is encountered (test failure)
     */
    @Test
    public void testCreateFilteredReader() throws Exception {
        StreamFilter filter = r -> r.getEventType() == XMLStreamConstants.START_ELEMENT
                                && r.getLocalName().equals("element3");

        XMLInputFactory factory = XMLInputFactory.newInstance();

        try (Reader source = new StringReader(XMLSOURCE1)) {
            XMLStreamReader reader = factory.createXMLStreamReader(source);
            assertThrows(XMLStreamException.class, () -> factory.createFilteredReader(reader, filter));
        }
    }

}
