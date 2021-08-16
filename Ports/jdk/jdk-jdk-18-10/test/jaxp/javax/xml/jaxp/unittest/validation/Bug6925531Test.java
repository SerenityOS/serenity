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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedAction;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/*
 * @test
 * @bug 6925531
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug6925531Test
 * @run testng/othervm -Djava.security.manager=allow validation.Bug6925531Test
 * @summary Test Validator can validate SAXSource when SecurityManager is set or FEATURE_SECURE_PROCESSING is on.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6925531Test {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    String xsd = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n" + "        xmlns:test='jaxp13_test'\n"
            + "        targetNamespace='jaxp13_test'\n" + "        elementFormDefault='qualified'>\n" + "    <element name='test' type='string'/>\n"
            + "</schema>\n";

    String xml = "<?xml version='1.0'?>\n" + "<ns:test xmlns:ns='jaxp13_test'>\n" + "    abc\n" + "</ns:test>\n";

    StreamSource xsdSource;
    SAXSource xmlSource;

    public void init() {
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(xsd.getBytes()));
        xsdSource = new StreamSource(reader);
        reader = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()));
        InputSource inSource = new InputSource(reader);
        xmlSource = new SAXSource(inSource);
    }

    /**
     * when security manager is present, secure feature is on automatically
     */
    @Test
    public void test_SM() {
        init();
        Permissions granted = new java.security.Permissions();
        granted.add(new AllPermission());

        System.setSecurityManager(new MySM(granted));

        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        Schema schema = null;
        try {
            schema = schemaFactory.newSchema(xsdSource);
        } catch (SAXException e) {
            Assert.fail(e.toString());
        }

        Validator validator = schema.newValidator();

        try {
            validator.validate(xmlSource, null);
        } catch (SAXException e) {
            Assert.fail(e.toString());
        } catch (IOException e) {
            Assert.fail(e.toString());
        } finally {
            System.setSecurityManager(null);
        }

        System.out.println("OK");
    }

    /**
     * set secure feature on SchemaFactory
     */
    @Test
    public void test_SF() {
        init();
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                System.setSecurityManager(null);
                return null; // nothing to return
            }
        });

        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        try {
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (SAXNotRecognizedException ex) {
            System.out.println(ex.getMessage());
        } catch (SAXNotSupportedException ex) {
            System.out.println(ex.getMessage());
        }

        Schema schema = null;
        try {
            schema = schemaFactory.newSchema(xsdSource);
        } catch (SAXException e) {
            Assert.fail(e.toString());
        }

        Validator validator = schema.newValidator();

        try {
            validator.validate(xmlSource, null);
        } catch (SAXException e) {
            Assert.fail(e.toString());
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
        System.out.println("OK");
    }

    /**
     * set secure feature on the Validator
     */
    @Test
    public void test_Val() {
        init();
        System.setSecurityManager(null);
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        Schema schema = null;
        try {
            schema = schemaFactory.newSchema(xsdSource);
        } catch (SAXException e) {
            Assert.fail(e.toString());
        }

        Validator validator = schema.newValidator();
        try {
            validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (SAXNotRecognizedException ex) {
            System.out.println(ex.getMessage());
        } catch (SAXNotSupportedException ex) {
            System.out.println(ex.getMessage());
        }

        try {
            validator.validate(xmlSource, null);
        } catch (SAXException e) {
            Assert.fail(e.toString());
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
        System.out.println("OK");
    }

    class MySM extends SecurityManager {
        Permissions granted;

        public MySM(Permissions perms) {
            granted = perms;
        }

        /**
         * The central point in checking permissions. Overridden from
         * java.lang.SecurityManager
         *
         * @param perm The permission requested.
         */
        @Override
        public void checkPermission(Permission perm) {
            if (granted.implies(perm)) {
                return;
            }
            super.checkPermission(perm);
        }

    }
}
