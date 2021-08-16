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

package validation.tck;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6977201
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.tck.Bug6977201Test
 * @run testng/othervm validation.tck.Bug6977201Test
 * @summary Test Validator interprets regex "" correctly.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6977201Test {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    @Test
    public void test() {
        validate(Bug6977201Test.class.getResource("Bug6977201/reA2.xsd").getPath(), Bug6977201Test.class.getResource("Bug6977201/reA2.xml").getPath());
        validate(Bug6977201Test.class.getResource("Bug6977201/reA3.xsd").getPath(), Bug6977201Test.class.getResource("Bug6977201/reA3.xml").getPath());
        validate(Bug6977201Test.class.getResource("Bug6977201/reA4.xsd").getPath(), Bug6977201Test.class.getResource("Bug6977201/reA4.xml").getPath());
        validate(Bug6977201Test.class.getResource("Bug6977201/reA5.xsd").getPath(), Bug6977201Test.class.getResource("Bug6977201/reA5.xml").getPath());
        validate(Bug6977201Test.class.getResource("Bug6977201/reA6.xsd").getPath(), Bug6977201Test.class.getResource("Bug6977201/reA6.xml").getPath());
    }

    // JCK negative test
    public void validate(String xsd, String xml) {
        try {
            Schema schema = schemaFactory.newSchema(new StreamSource(xsd));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(xml));
            Assert.fail("should report error");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // e.printStackTrace();
        }
    }

}
