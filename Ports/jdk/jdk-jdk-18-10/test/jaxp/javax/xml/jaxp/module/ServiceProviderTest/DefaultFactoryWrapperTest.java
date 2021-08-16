/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/*
 * @test
 * @library src/DefaultFactoryWrapperTest
 * @build xmlwrapperprovider/*
 * @run testng/othervm --add-modules=xmlwrapperprovider DefaultFactoryWrapperTest
 * @bug 8169948 8169778
 * @summary test customized provider wraps the built-in system-default implementation of JAXP factories
 */
public class DefaultFactoryWrapperTest {
    private static final Module XML_MODULE = ModuleLayer.boot().findModule("java.xml").get();

    private static final String PROVIDER_PACKAGE = "xwp";

    /*
     * Return JAXP factory and corresponding factory function.
     */
    @DataProvider(name = "jaxpFactories")
    public Object[][] jaxpFactories() throws Exception {
        return new Object[][] {
                { DocumentBuilderFactory.newInstance(), (Produce)factory -> ((DocumentBuilderFactory)factory).newDocumentBuilder() },
                { SAXParserFactory.newInstance(), (Produce)factory -> ((SAXParserFactory)factory).newSAXParser() },
                { SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI), (Produce)factory -> ((SchemaFactory)factory).newSchema() },
                { TransformerFactory.newInstance(), (Produce)factory -> ((TransformerFactory)factory).newTransformer() },
                { XMLEventFactory.newInstance(), (Produce)factory -> ((XMLEventFactory)factory).createStartDocument() },
                { XMLInputFactory.newInstance(), (Produce)factory -> ((XMLInputFactory)factory).createXMLEventReader(new StringReader("")) },
                { XMLOutputFactory.newInstance(), (Produce)factory -> ((XMLOutputFactory)factory).createXMLEventWriter(new StringWriter()) },
                { XPathFactory.newInstance(), (Produce)factory -> ((XPathFactory)factory).newXPath() },
                { DatatypeFactory.newInstance(), (Produce)factory -> ((DatatypeFactory)factory).newXMLGregorianCalendar() }
        };
    }

    /*
     * Verify the factory comes from customized provider, and produces a built-in type.
     */
    @Test(dataProvider = "jaxpFactories")
    public void testFactory(Object factory, Produce<Object, Object> p) throws Exception {
        assertEquals(factory.getClass().getPackageName(), PROVIDER_PACKAGE);
        assertSame(p.produce(factory).getClass().getModule(), XML_MODULE);
    }

    @FunctionalInterface
    public interface Produce<T, R> {
        R produce(T t) throws Exception;
    }
}
