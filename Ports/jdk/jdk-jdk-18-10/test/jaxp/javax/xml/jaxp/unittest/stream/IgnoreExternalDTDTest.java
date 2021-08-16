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

package stream;

import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.IgnoreExternalDTDTest
 * @run testng/othervm stream.IgnoreExternalDTDTest
 * @summary Test feature ignore-external-dtd.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class IgnoreExternalDTDTest {

    final static String FACTORY_KEY = "javax.xml.stream.XMLInputFactory";
    static final String IGNORE_EXTERNAL_DTD = "ignore-external-dtd";
    static final String ZEPHYR_PROPERTY_PREFIX = "http://java.sun.com/xml/stream/properties/";

    @Test
    public void testFeaturePositive() throws Exception {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(ZEPHYR_PROPERTY_PREFIX + IGNORE_EXTERNAL_DTD, Boolean.TRUE);
        parse(xif);
    }

    @Test
    public void testFeatureNegative() throws Exception {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(ZEPHYR_PROPERTY_PREFIX + IGNORE_EXTERNAL_DTD, Boolean.FALSE);
        try {
            parse(xif);
            // refer to 6440324, absent of that change, an exception would be
            // thrown;
            // due to the change made for 6440324, parsing will continue without
            // exception
            // fail();
        } catch (XMLStreamException e) {
            // the error is expected that no DTD was found
        }
    }

    private void parse(XMLInputFactory xif) throws XMLStreamException {
        XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader("<?xml version='1.0'?><!DOCTYPE root PUBLIC 'abc' 'def'><abc />"));
        while (xsr.next() != XMLStreamConstants.END_DOCUMENT)
            ;
    }

}
