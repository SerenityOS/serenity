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

package stream.XMLEventReaderTest;

import javax.xml.stream.XMLStreamException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6846133
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.Bug6846133Test
 * @run testng/othervm stream.XMLEventReaderTest.Bug6846133Test
 * @summary Test method getDocumentTypeDeclaration() of DTD Event returns a valid value.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6846133Test {
    private static final String xml = "<!DOCTYPE html PUBLIC \"-//W3C//DTDXHTML 1.0 Transitional//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + "<html><body><p>I am some simple html</p></body> </html>";

    @Test
    public void test() {
        try {
            javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
            factory.setXMLResolver(new DTDResolver());
            factory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD, true);
            factory.setProperty(javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
            java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(xml.getBytes("UTF-8"));

            // createXMLEventReader (source) not supported
            // javax.xml.transform.stream.StreamSource source = new
            // javax.xml.transform.stream.StreamSource (is);
            // javax.xml.stream.XMLEventReader reader =
            // factory.createXMLEventReader (source);

            javax.xml.stream.XMLEventReader reader = factory.createXMLEventReader(is);
            while (reader.hasNext()) {
                javax.xml.stream.events.XMLEvent event = reader.nextEvent();
                if (event.getEventType() == javax.xml.stream.XMLStreamConstants.DTD) {
                    String temp = ((javax.xml.stream.events.DTD) event).getDocumentTypeDeclaration();
                    if (temp.length() < 120) {
                        Assert.fail("DTD truncated");
                    }
                    System.out.println(temp);
                }
            }
        } catch (XMLStreamException xe) {
            Assert.fail(xe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class DTDResolver implements javax.xml.stream.XMLResolver {
        public Object resolveEntity(String arg0, String arg1, String arg2, String arg3) throws XMLStreamException {
            System.out.println("DTD is parsed");
            return new java.io.ByteArrayInputStream(new byte[0]);
        }
    }

}
