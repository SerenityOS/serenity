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

package transform;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6467808
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6467808
 * @run testng/othervm transform.Bug6467808
 * @summary Test Transformer can parse re-declare prefixed namespace mappings.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6467808 {

    private static final String TESTXML = "<?xml version='1.0' ?>\n"
            + "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:ns1='http://faulttestservice.org/wsdl'>\n"
            + "<soapenv:Body>\n" + "<soapenv:Fault xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>\n" + "<faultcode>\n"
            + "soapenv:Server</faultcode>\n" + "<faultstring>\n" + "com.sun.ts.tests.jaxws.sharedwebservices.faultservice.DummyException</faultstring>\n"
            + "<detail>\n" + "<ns1:DummyException>\n" + "<dummyField1>\n" + "dummyString1</dummyField1>\n" + "<dummyField2>\n" + "dummyString2</dummyField2>\n"
            + "</ns1:DummyException>\n" + "</detail>\n" + "</soapenv:Fault>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>\n";

    // simplest XML to re-declare same prefix/namespace mappings
    private static final String SIMPLE_TESTXML = "<?xml version='1.0' ?>\n" + "<prefix:ElementName xmlns:prefix='URI'>\n"
            + "<prefix:ElementName xmlns:prefix='URI'>\n" + "</prefix:ElementName>\n" + "</prefix:ElementName>\n";

    @Test
    public void test() {
        try {
            SAXParserFactory fac = SAXParserFactory.newInstance();
            fac.setNamespaceAware(true);
            SAXParser saxParser = fac.newSAXParser();

            StreamSource src = new StreamSource(new StringReader(SIMPLE_TESTXML));
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMResult result = new DOMResult();
            transformer.transform(src, result);
        } catch (Throwable ex) {
            // unexpected failure
            ex.printStackTrace();
            Assert.fail(ex.toString());
        }
    }
}
