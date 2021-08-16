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

package stream.XMLInputFactoryTest;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLInputFactoryTest.IssueTracker38
 * @run testng/othervm stream.XMLInputFactoryTest.IssueTracker38
 * @summary Test createXMLEventReader from DOM or SAX source is unsupported.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class IssueTracker38 {

    @Test
    public void testXMLEventReaderFromDOMSource() throws Exception {
        try {
                createEventReaderFromSource(new DOMSource());
            Assert.fail("Expected UnsupportedOperationException not thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testXMLStreamReaderFromDOMSource() throws Exception {
        try {
                createStreamReaderFromSource(new DOMSource());
            Assert.fail("Expected UnsupportedOperationException not thrown");
        } catch (UnsupportedOperationException oe) {
        }
    }

    @Test
    public void testXMLEventReaderFromSAXSource() throws Exception {
        try {
                createEventReaderFromSource(new SAXSource());
            Assert.fail("Expected UnsupportedOperationException not thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testXMLStreamReaderFromSAXSource() throws Exception {
        try {
                createStreamReaderFromSource(new SAXSource());
            Assert.fail("Expected UnsupportedOperationException not thrown");
        } catch (UnsupportedOperationException oe) {
        }
    }

    private void createEventReaderFromSource(Source source) throws Exception {
        XMLInputFactory xIF = XMLInputFactory.newInstance();
        xIF.createXMLEventReader(source);
    }

    private void createStreamReaderFromSource(Source source) throws Exception {
        XMLInputFactory xIF = XMLInputFactory.newInstance();
        xIF.createXMLStreamReader(source);
    }


}
