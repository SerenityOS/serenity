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

package stream.XMLStreamFilterTest;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamFilterTest.HasNextTest
 * @run testng/othervm stream.XMLStreamFilterTest.HasNextTest
 * @summary Test Filtered XMLStreamReader hasNext() always return the correct value if repeat to call it.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class HasNextTest {

    private static String INPUT_FILE = "HasNextTest.xml";

    private HasNextTypeFilter createFilter() {

        HasNextTypeFilter f = new HasNextTypeFilter();

        f.addType(XMLEvent.START_ELEMENT);
        f.addType(XMLEvent.END_ELEMENT);
        f.addType(XMLEvent.PROCESSING_INSTRUCTION);
        f.addType(XMLEvent.CHARACTERS);
        f.addType(XMLEvent.COMMENT);
        f.addType(XMLEvent.SPACE);
        f.addType(XMLEvent.START_DOCUMENT);
        f.addType(XMLEvent.END_DOCUMENT);
        return f;
    }

    private XMLStreamReader createStreamReader(HasNextTypeFilter f) {

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory = XMLInputFactory.newInstance();
            return factory.createFilteredReader(factory.createXMLStreamReader(this.getClass().getResourceAsStream(INPUT_FILE)), (StreamFilter) f);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Unexpected Exception: " + e.getMessage());
            return null;
        }
    }

    private void checkHasNext(XMLStreamReader r1) throws XMLStreamException {

        // try asking 3 times, insure all results are the same
        boolean hasNext_1 = r1.hasNext();
        boolean hasNext_2 = r1.hasNext();
        boolean hasNext_3 = r1.hasNext();

        System.out.println("XMLStreamReader.hasNext() (1): " + hasNext_1);
        System.out.println("XMLStreamReader.hasNext() (2): " + hasNext_2);
        System.out.println("XMLStreamReader.hasNext() (3): " + hasNext_3);

        Assert.assertTrue((hasNext_1 == hasNext_2) && (hasNext_1 == hasNext_3),
                "XMLStreamReader.hasNext() returns inconsistent values for each subsequent call: " + hasNext_1 + ", " + hasNext_2 + ", " + hasNext_3);
    }

    @Test
    public void testFilterUsingNextTag() {

        try {
            HasNextTypeFilter f = createFilter();
            XMLStreamReader r1 = createStreamReader(f);

            while (r1.hasNext()) {
                try {
                    r1.nextTag();
                } catch (Exception e) {
                    System.err.println("Expected Exception: " + e.getMessage());
                    e.printStackTrace();
                }

                checkHasNext(r1);
            }

        } catch (XMLStreamException e) {
            System.err.println("Unexpected Exception: " + e.getMessage());
            e.printStackTrace();
            Assert.fail("Unexpected Exception: " + e.toString());
        } catch (Exception e) {
            // if this is END_DOCUMENT, it is expected
            if (e.toString().indexOf("END_DOCUMENT") != -1) {
                // expected
                System.err.println("Expected Exception:");
                e.printStackTrace();
            } else {
                // unexpected
                System.err.println("Unexpected Exception: " + e.getMessage());
                e.printStackTrace();
                Assert.fail("Unexpected Exception: " + e.toString());
            }
        }
    }

    @Test
    public void testFilterUsingNext() {

        try {
            HasNextTypeFilter f = createFilter();
            XMLStreamReader r1 = createStreamReader(f);

            while (r1.hasNext()) {
                r1.next();
                checkHasNext(r1);
            }

        } catch (Exception e) {
            // unexpected
            System.err.println("Unexpected Exception: " + e.getMessage());
            e.printStackTrace();
            Assert.fail("Unexpected Exception: " + e.toString());
        }
    }
}
