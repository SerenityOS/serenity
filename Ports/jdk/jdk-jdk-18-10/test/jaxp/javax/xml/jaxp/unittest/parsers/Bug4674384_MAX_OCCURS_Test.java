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

package parsers;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 4674384
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug4674384_MAX_OCCURS_Test
 * @run testng/othervm parsers.Bug4674384_MAX_OCCURS_Test
 * @summary Test large maxOccurs.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4674384_MAX_OCCURS_Test {

    @Test
    public final void testLargeMaxOccurs() {

        String XML_FILE_NAME = "Bug4674384_MAX_OCCURS_Test.xml";

        try {
            // create and initialize the parser
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(true);

            SAXParser parser = spf.newSAXParser();
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

            File xmlFile = new File(getClass().getResource(XML_FILE_NAME).getPath());

            parser.parse(xmlFile, new DefaultHandler());
        } catch (Exception e) {
            System.err.println("Failure: File " + XML_FILE_NAME + " was parsed with a large value of maxOccurs.");
            e.printStackTrace();
            Assert.fail("Failure: File " + XML_FILE_NAME + " was parsed with a large value of maxOccurs.  " + e.getMessage());
        }

        System.out.println("Success: File " + XML_FILE_NAME + " was parsed with a large value of maxOccurs.");
    }
}
