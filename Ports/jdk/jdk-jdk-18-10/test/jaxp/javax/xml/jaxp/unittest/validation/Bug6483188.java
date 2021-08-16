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

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXParseException;

/*
 * @test
 * @bug 6483188
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug6483188
 * @run testng/othervm validation.Bug6483188
 * @summary Test Schema Validator can handle element with having large maxOccurs, but doesn't accept sequence with having large maxOccurs in FEATURE_SECURE_PROCESSING mode.
 */
@Test(singleThreaded = true)
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6483188 {
    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    public void testLargeElementNoSecurity() {
        if (System.getSecurityManager() != null)
            return; // jaxp secure feature can not be turned off when security
                    // manager is present
        try {
            sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.FALSE);
            URL url = getClass().getResource("test-element.xsd");
            Schema s = sf.newSchema(url);
            Validator v = s.newValidator();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public void testLargeElementWithSecurity() {
        try {
            sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            URL url = getClass().getResource("test-element.xsd");
            Schema s = sf.newSchema(url);
            Validator v = s.newValidator();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public void testLargeSequenceWithSecurity() {
        try {
            sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            URL url = getClass().getResource("test-sequence.xsd");
            Schema s = sf.newSchema(url);
            Validator v = s.newValidator();
            Assert.fail("Schema was accepted even with secure processing enabled.");
        } catch (SAXParseException e) {
            // falls through - exception expected
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
