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

/*
 * @test
 * @bug 6551616
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6551616
 * @run testng/othervm transform.Bug6551616
 * @summary Test SAX2StAXEventWriter.
 */

package transform;

import java.io.InputStream;
import java.io.StringBufferInputStream;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.org.apache.xalan.internal.xsltc.trax.SAX2StAXEventWriter;

@Listeners({jaxp.library.InternalAPIPolicy.class})
public class Bug6551616 {
    String _cache = "";


    @Test
    public void test() throws Exception {
        final String XML = "" + "<?xml version='1.0'?>" + "<doc xmlns:foo='http://example.com/foo/' xml:lang='us-en'><p>Test</p></doc>";

        javax.xml.parsers.SAXParserFactory saxFactory = javax.xml.parsers.SAXParserFactory.newInstance();

        javax.xml.parsers.SAXParser parser = saxFactory.newSAXParser();

        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        XMLEventWriter writer = outFactory.createXMLEventWriter(System.out);

        SAX2StAXEventWriter handler = new SAX2StAXEventWriter(writer);

        InputStream is = new StringBufferInputStream(XML);

        parser.parse(is, handler);

        // if it doesn't blow up, it succeeded.
    }
}
