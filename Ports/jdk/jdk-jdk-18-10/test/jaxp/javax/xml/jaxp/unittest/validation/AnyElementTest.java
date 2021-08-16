/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8080907
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.AnyElementTest
 * @run testng/othervm validation.AnyElementTest
 * @summary Test processContents attribute of any element
 */
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.net.URISyntaxException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Listeners({jaxp.library.FilePolicy.class})
public class AnyElementTest {
    @BeforeClass
    public void setup() throws URISyntaxException, SAXException {
        validator = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(getUri("ProcessContents.xsd"))).newValidator();
    }

    /*
     * processContents attribute - Specifies how the XML processor should handle
     * validation against the elements specified by this any element. Can be set
     * to one of the following:
     * strict - the XML processor must obtain the schema for the required
     * namespaces and validate the elements (this is default)
     * lax - same as strict, but if the schema cannot be obtained, no errors
     * will occur
     * skip - The XML processor does not attempt to validate any elements from
     * the specified namespaces
     */
    @Test
    public void testProcessContents() throws Exception {
        validator.validate(new StreamSource(getUri("ProcessContents-ok.xml")));
    }

    /*
     * When processContents="lax", validation will be performed when the element
     * is declared in the schema.
     */
    @Test(expectedExceptions = SAXParseException.class)
    public void testProcessContentsLax() throws Exception {
        validator.validate(new StreamSource(getUri("ProcessContents-lax-error.xml")));
    }

    /*
     * Get the URI of the file, which is in the same path as this class
     */
    private String getUri(String fileName) throws URISyntaxException {
        return this.getClass().getResource(fileName).toURI().toASCIIString();
    }

    private Validator validator;
}
