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
package test.gaptest;

import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static test.gaptest.GapTestConst.XML_DIR;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/*
 * @test
 * @bug 4848653
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.gaptest.Bug4848653
 * @run testng/othervm test.gaptest.Bug4848653
 * @summary Verify JAXP schemaLanguage property is ignored if setValidating(false)
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4848653 {

    @Test
    public void test() throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser parser = factory.newSAXParser();
        parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", XMLConstants.W3C_XML_SCHEMA_NS_URI);

        String filename = XML_DIR + "Bug4848653.xml";
        InputSource is = new InputSource(filenameToURL(filename));
        XMLReader xmlReader = parser.getXMLReader();
        xmlReader.setErrorHandler(new MyErrorHandler());
        xmlReader.parse(is);
    }

    class MyErrorHandler implements ErrorHandler {
        public void error(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXParseException {
            throw exception;
        }

    }

}
