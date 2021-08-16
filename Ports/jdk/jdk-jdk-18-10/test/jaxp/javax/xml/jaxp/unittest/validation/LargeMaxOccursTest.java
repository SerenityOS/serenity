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

package validation;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.LargeMaxOccursTest
 * @run testng/othervm validation.LargeMaxOccursTest
 * @summary Test Validator shall report error for maxOccurs > 5000 when FEATURE_SECURE_PROCESSING is on, except the schema can be applied for constant-space algorithm.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class LargeMaxOccursTest {

    @Test
    public void testParticlesR005() {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String xsdFile = "particlesR005.xsd";
            Schema schema = sf.newSchema(new File(getClass().getResource(xsdFile).toURI()));
            Validator validator = schema.newValidator();
        } catch (Exception ex) {
            return; // expected
        }
        Assert.fail("Parser configuration error expected since maxOccurs > 5000 " + "and constant-space optimization does not apply");
    }

    @Test
    public void testParticlesIe003() {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String xsdFile = "particlesIe003.xsd";
            Schema schema = sf.newSchema(new File(getClass().getResource(xsdFile).toURI()));
            Validator validator = schema.newValidator();
        } catch (Exception ex) {
            return; // expected
        }
        Assert.fail("Parser configuration error expected since maxOccurs > 5000 " + "and constant-space optimization does not apply");
    }

    @Test
    public void testmgG014() {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String xsdFile = "mgG014.xsd";
            Schema schema = sf.newSchema(new File(getClass().getResource(xsdFile).toURI()));
            Validator validator = schema.newValidator();
        } catch (Exception ex) {
            return; // expected
        }
        Assert.fail("Parser configuration error expected since maxOccurs > 5000 " + "and constant-space optimization does not apply");
    }

    @Test
    public void testmgJ014() {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String xsdFile = "mgJ014.xsd";
            Schema schema = sf.newSchema(new File(getClass().getResource(xsdFile).toURI()));
            Validator validator = schema.newValidator();
        } catch (Exception ex) {
            return; // expected
        }
        Assert.fail("Parser configuration error expected since maxOccurs > 5000 " + "and constant-space optimization does not apply");
    }

    // particlesOptimize.xsd missing.
    @Test
    public void testParticlesOptimize() {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String xsdFile = "particlesOptimize.xsd";
            Schema schema = sf.newSchema(new File(getClass().getResource(xsdFile).toURI()));
            Validator validator = schema.newValidator();
        } catch (Exception ex) {
            Assert.fail("Parser configuration error not expected since maxOccurs " + "> 5000 but constant-space optimization applies");
        }
    }
}
