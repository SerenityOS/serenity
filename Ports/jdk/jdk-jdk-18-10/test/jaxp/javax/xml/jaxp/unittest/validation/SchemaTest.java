/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/*
 * @test
 * @bug 8149915 8222991
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm validation.SchemaTest
 * @summary Test Schema creation
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SchemaTest {
    /**
     * Verifies that an over-the-limit value of an enumeration is caught as a
     * warning.
     * @throws Exception if the test fails
     */
    @Test
    public void testSchema() throws Exception {
        String xsd = "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "    <xsd:simpleType name=\"PaymentStatus\">\n" +
                "        <xsd:restriction base=\"xsd:string\">\n" +
                "            <xsd:maxLength value=\"15\"/>\n" +
                "            <xsd:enumeration value=\"AWAIT_PAY_INFO\"/>\n" +
                "            <xsd:enumeration value=\"AWAIT_AUTH\"/>\n" +
                "            <xsd:enumeration value=\"REQUESTED_AUTH\"/>\n" +
                "            <xsd:enumeration value=\"REQUESTED_CHARGE\"/>\n" +
                "            <xsd:enumeration value=\"PAID\"/>\n" +
                "        </xsd:restriction>\n" +
                "    </xsd:simpleType> \n" +
                "</xsd:schema>";
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final List<SAXParseException> exceptions = new ArrayList<>();

        factory.setErrorHandler(new ErrorHandler()
        {
          @Override
          public void warning(SAXParseException exception) throws SAXException
          {
            exceptions.add(exception);
          }

          @Override
          public void fatalError(SAXParseException exception) throws SAXException
          {}

          @Override
          public void error(SAXParseException exception) throws SAXException
          {}
        });
        factory.newSchema(new StreamSource(new StringReader(xsd)));
        Assert.assertTrue(exceptions.get(0).toString().contains("FacetsContradict"),
                "Report warning when the maxLength limit is exceeded in an enumeration");
    }

    /*
     * @bug 8149915
     * Verifies that the annotation validator is initialized with the security manager for schema
     * creation with http://apache.org/xml/features/validate-annotations=true.
     */
    @Test
    public void testValidation() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature("http://apache.org/xml/features/validate-annotations", true);
        factory.newSchema(new File(getClass().getResource("Bug8149915.xsd").getFile()));
    }
}
