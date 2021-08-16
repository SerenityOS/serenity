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

package stream.XMLStreamReaderTest;

import java.io.StringReader;
import javax.xml.stream.XMLEventReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8069098
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.BugTest
 * @run testng/othervm stream.XMLStreamReaderTest.BugTest
 * @summary Test StAX parser can parse xml without declaration.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class BugTest {

    /**
     * Verifies that the initial event of an XMLStreamReader instance is
     * START_DOCUMENT.
     *
     * @param xml the xml input
     * @param type1 the type of the 1st event
     * @param type2 the type of the 2nd event
     * @throws Exception if the test fails to run properly
     */
    @Test(dataProvider = "xmls")
    public static void test1(String xml, int type1, int type2) throws Exception {
       XMLInputFactory factory = XMLInputFactory.newFactory();

       XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
       int type1stEvent = reader.getEventType();
       int type2ndEvent = reader.next();
       System.out.println("First event: " + type1stEvent);
       System.out.println("2nd event: " + type2ndEvent);
       Assert.assertEquals(type1, type1stEvent);
       Assert.assertEquals(type2, type2ndEvent);
    }


    /**
     * Verifies that the initial event of an XMLEventReader instance is
     * START_DOCUMENT. XMLEventReader depends on XMLStreamReader.
     *
     * @param xml the xml input
     * @param type1 the type of the 1st event
     * @param type2 the type of the 2nd event
     * @throws Exception if the test fails to run properly
     */
    @Test(dataProvider = "xmls")
    public static void test2(String xml, int type1, int type2) throws Exception {
       XMLInputFactory factory = XMLInputFactory.newFactory();

       XMLEventReader reader = factory.createXMLEventReader(new StringReader(xml));
       int type1stEvent = reader.nextEvent().getEventType();
       int type2ndEvent = reader.nextEvent().getEventType();
       System.out.println("First event: " + type1stEvent);
       System.out.println("2nd event: " + type2ndEvent);
       Assert.assertEquals(type1, type1stEvent);
       Assert.assertEquals(type2, type2ndEvent);
    }

    /*
       DataProvider: for testing beginning event type
       Data: xml, 1st event type, 2nd event type
     */
    @DataProvider(name = "xmls")
    public Object[][] getXMLs() {

        return new Object[][]{
            {"<?xml version='1.0'?><foo/>",
                XMLStreamConstants.START_DOCUMENT, XMLStreamConstants.START_ELEMENT},
            {"<foo/>",
                XMLStreamConstants.START_DOCUMENT, XMLStreamConstants.START_ELEMENT},
            {"<?xml version='1.0'?>"
                + "<?xml-stylesheet href=\"bar.xsl\" type=\"text/xsl\"?>" +
                    "<foo/>",
                XMLStreamConstants.START_DOCUMENT, XMLStreamConstants.PROCESSING_INSTRUCTION},
            {"<?xml-stylesheet href=\"bar.xsl\" type=\"text/xsl\"?>" +
                    "<foo/>",
                XMLStreamConstants.START_DOCUMENT, XMLStreamConstants.PROCESSING_INSTRUCTION},
        };
    }
}
