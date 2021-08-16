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
package stream.CoalesceTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.CoalesceTest.CoalesceTest
 * @run testng/othervm stream.CoalesceTest.CoalesceTest
 * @summary Test Coalesce property works.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CoalesceTest {

    String countryElementContent = "START India  CS}}}}}} India END";
    String descriptionElementContent = "a&b";
    String fooElementContent = "&< cdatastart<><>>><>><<<<cdataend entitystart insert entityend";

    @Test
    public void testCoalesceProperty() {
        try {
            XMLInputFactory xifactory = XMLInputFactory.newInstance();
            xifactory.setProperty(XMLInputFactory.IS_COALESCING, new Boolean(true));
            InputStream xml = this.getClass().getResourceAsStream("coalesce.xml");
            XMLStreamReader streamReader = xifactory.createXMLStreamReader(xml);
            while (streamReader.hasNext()) {
                int eventType = streamReader.next();
                if (eventType == XMLStreamConstants.START_ELEMENT && streamReader.getLocalName().equals("country")) {
                    eventType = streamReader.next();
                    if (eventType == XMLStreamConstants.CHARACTERS) {
                        String text = streamReader.getText();
                        if (!text.equals(countryElementContent)) {
                            System.out.println("String dont match");
                            System.out.println("text = " + text);
                            System.out.println("countryElementContent = " + countryElementContent);
                        }
                        // assertTrue(text.equals(countryElementContent));
                    }
                }
                if (eventType == XMLStreamConstants.START_ELEMENT && streamReader.getLocalName().equals("description")) {
                    eventType = streamReader.next();
                    if (eventType == XMLStreamConstants.CHARACTERS) {
                        String text = streamReader.getText();
                        if (!text.equals(descriptionElementContent)) {
                            System.out.println("String dont match");
                            System.out.println("text = " + text);
                            System.out.println("descriptionElementContent = " + descriptionElementContent);
                        }
                        Assert.assertTrue(text.equals(descriptionElementContent));
                    }
                }
                if (eventType == XMLStreamConstants.START_ELEMENT && streamReader.getLocalName().equals("foo")) {
                    eventType = streamReader.next();
                    if (eventType == XMLStreamConstants.CHARACTERS) {
                        String text = streamReader.getText();
                        if (!text.equals(fooElementContent)) {
                            System.out.println("String dont match");
                            System.out.println("text = " + text);
                            System.out.println("fooElementContent = " + fooElementContent);
                        }

                        Assert.assertTrue(text.equals(fooElementContent));
                    }
                }

            }
        } catch (XMLStreamException ex) {

            if (ex.getNestedException() != null) {
                ex.getNestedException().printStackTrace();
            }
            // ex.printStackTrace() ;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
