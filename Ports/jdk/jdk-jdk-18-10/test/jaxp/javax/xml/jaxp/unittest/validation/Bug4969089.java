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

package validation;

import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 4969089
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4969089
 * @run testng/othervm validation.Bug4969089
 * @summary Test when an ErrorHandler is set for a SchemaFactory, SchemaFactory.newSchema(Source[])
 * method throws an exception that is not equal to the exception thrown from the ErrorHandler.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4969089 {

    @Test
    public void test1() {
        String xsd1 = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n" + "        xmlns:test='jaxp13_test1'\n"
                + "        targetNamespace='jaxp13_test1'\n" + "        elementFormDefault='qualified'>\n" + "    <element name='test'>\n" + "</schema>\n";

        final SAXException EUREKA = new SAXException("NewSchema007");
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        StringReader reader = new StringReader(xsd1);
        StreamSource source = new StreamSource(reader);
        DefaultHandler errorHandler = new DefaultHandler() {
            public void fatalError(SAXParseException unused) throws SAXException {
                throw EUREKA;
            }

            public void error(SAXParseException unused) throws SAXException {
                throw EUREKA;
            }
        };
        schemaFactory.setErrorHandler(errorHandler);

        try {
            schemaFactory.newSchema(new Source[] { source });
            Assert.fail("SAXException was not thrown.");
        } catch (SAXException e) {
            Assert.assertSame(e, EUREKA);
        }
    }
}
