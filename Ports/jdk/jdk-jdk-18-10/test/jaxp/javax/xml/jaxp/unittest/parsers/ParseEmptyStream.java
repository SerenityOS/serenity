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

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.ParseEmptyStream
 * @run testng/othervm parsers.ParseEmptyStream
 * @summary Test SAXParser doesn't accept empty stream.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class ParseEmptyStream {

    SAXParserFactory factory = null;

    public ParseEmptyStream(String name) {
        try {
            factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void testEmptyStream() {
        try {
            SAXParser parser = factory.newSAXParser();
            InputSource source = new InputSource(new StringReader(""));
            parser.parse(source, new MyHandler());
            Assert.fail("Inputstream without document element accepted");
        } catch (Exception ex) {
            System.out.println("Exception thrown: " + ex.getMessage());
            // Premature end of file exception expected
        }
    }

    @Test
    public void testXmlDeclOnly() {
        try {
            SAXParser parser = factory.newSAXParser();
            InputSource source = new InputSource(new StringReader("<?xml version='1.0' encoding='utf-8'?>"));
            parser.parse(source, new MyHandler());
            Assert.fail("Inputstream without document element accepted");
        } catch (Exception ex) {
            System.out.println("Exception thrown: " + ex.getMessage());
            // Premature end of file exception expected
        }
    }

    static class MyHandler extends DefaultHandler {
        public void startDocument() {
            System.out.println("Start document called");
        }

        public void endDocument() {
            System.out.println("End document called");
        }
    }

}
