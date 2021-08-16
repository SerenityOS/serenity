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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 6388460
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.Bug6388460
 * @run testng/othervm stream.XMLStreamReaderTest.Bug6388460
 * @summary Test StAX parser can parse UTF-16 wsdl.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6388460 {

    @Test
    public void test() {
        try {

            Source source = new StreamSource(util.BOMInputStream.createStream("UTF-16BE", this.getClass().getResourceAsStream("Hello.wsdl.data")),
                        this.getClass().getResource("Hello.wsdl.data").toExternalForm());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, new StreamResult(baos));
            System.out.println(new String(baos.toByteArray()));
            ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
            InputSource inSource = new InputSource(bis);

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inSource.getSystemId(), inSource.getByteStream());
            while (reader.hasNext()) {
                reader.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            Assert.fail("Exception occured: " + ex.getMessage());
        }
    }
}
