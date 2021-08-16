/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.xml.sax.ptests;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Unit test for XMLReaderFactory.createXMLReader API.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.XMLReaderFactoryTest
 * @run testng/othervm org.xml.sax.ptests.XMLReaderFactoryTest
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XMLReaderFactoryTest {
    /**
     * No exception expected when create XMLReader by default.
     * @throws org.xml.sax.SAXException when xml reader creation failed.
     */
    @Test
    public void createReader01() throws SAXException {
        assertNotNull(XMLReaderFactory.createXMLReader());
    }

    /**
     * No exception expected when create XMLReader with driver name
     * org.apache.xerces.parsers.SAXParser
     * or com.sun.org.apache.xerces.internal.parsers.SAXParser.
     * @throws org.xml.sax.SAXException when xml reader creation failed.
     */
    @Test
    public void createReader02() throws SAXException {
        setSystemProperty("org.xml.sax.driver",
            "com.sun.org.apache.xerces.internal.parsers.SAXParser");
        assertNotNull(XMLReaderFactory.
            createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser"));
    }

    /**
     * SAXException expected when create XMLReader with an invalid driver name.
     * @throws org.xml.sax.SAXException expected Exception
     */
    @Test(expectedExceptions = SAXException.class,
            expectedExceptionsMessageRegExp =
                    "SAX2 driver class org.apache.crimson.parser.ABCD not found")
    public void createReader03() throws SAXException{
        XMLReaderFactory.createXMLReader("org.apache.crimson.parser.ABCD");
    }
}
