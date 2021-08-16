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

package sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6992561
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.Bug6992561Test
 * @run testng/othervm sax.Bug6992561Test
 * @summary Test encoding of SystemId in Locator.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6992561Test {

    @Test
    public void test() {
        ContentHandler handler = new DefaultHandler() {
            public void setDocumentLocator(Locator locator) {
                String sysId = locator.getSystemId();
                System.out.println(locator.getSystemId());
                if (sysId.indexOf("%7") > 0) {
                    Assert.fail("the original system id should be left as is and not encoded.");
                }
            }
        };

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            parser = spf.newSAXParser();

            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(handler);
            String xml = "<test>abc</test>";
            ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes());
            InputSource is = new InputSource("file:/home2/ramapulavarthi/w/bugs/jaxws861/foo~bla/test/src/wsdl/HelloTypes.xsd");
            is.setByteStream(bis);
            reader.parse(is);

        } catch (ParserConfigurationException ex) {
            Assert.fail(ex.toString());
        } catch (SAXException ex) {
            Assert.fail(ex.toString());
        } catch (IOException ex) {
            Assert.fail(ex.toString());
        }
    }

}
