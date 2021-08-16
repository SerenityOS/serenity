/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package common;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;

import static jaxp.library.JAXPTestUtilities.clearSystemProperty;
import static jaxp.library.JAXPTestUtilities.setSystemProperty;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 7143711
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.Bug7143711Test
 * @summary Test set use-service-mechanism shall not override what's set by the constructor in secure mode.
 */
@Listeners({ jaxp.library.BasePolicy.class })
@Test(singleThreaded = true)
public class Bug7143711Test {
    private static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    private static final String DOM_FACTORY_ID = "javax.xml.parsers.DocumentBuilderFactory";
    private static final String SAX_FACTORY_ID = "javax.xml.parsers.SAXParserFactory";

    // impl specific feature
    private static final String ORACLE_FEATURE_SERVICE_MECHANISM = "http://www.oracle.com/feature/use-service-mechanism";

    @Test
    public void testValidation_SAX_withSM() {
        System.out.println("Validation using SAX Source with security manager:");
        setSystemProperty(SAX_FACTORY_ID, "MySAXFactoryImpl");

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // should not allow
            factory.setFeature(ORACLE_FEATURE_SERVICE_MECHANISM, true);
            if ((boolean) factory.getFeature(ORACLE_FEATURE_SERVICE_MECHANISM)) {
                Assert.fail("should not override in secure mode");
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());

        } finally {
            clearSystemProperty(SAX_FACTORY_ID);
        }
    }

    @Test(enabled=false) //skipped due to bug JDK-8080097
    public void testTransform_DOM_withSM() {
        System.out.println("Transform using DOM Source;  Security Manager is set:");
        setSystemProperty(DOM_FACTORY_ID, "MyDOMFactoryImpl");

        try {
            TransformerFactory factory = TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                    TransformerFactory.class.getClassLoader());
            factory.setFeature(ORACLE_FEATURE_SERVICE_MECHANISM, true);
            if ((boolean) factory.getFeature(ORACLE_FEATURE_SERVICE_MECHANISM)) {
                Assert.fail("should not override in secure mode");
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            clearSystemProperty(DOM_FACTORY_ID);
        }
    }

    @Test
    public void testXPath_DOM_withSM() {
        System.out.println("Evaluate DOM Source;  Security Manager is set:");
        setSystemProperty(DOM_FACTORY_ID, "MyDOMFactoryImpl");

        try {
            XPathFactory xPathFactory = XPathFactory.newInstance("http://java.sun.com/jaxp/xpath/dom",
                    "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl", null);
            xPathFactory.setFeature(ORACLE_FEATURE_SERVICE_MECHANISM, true);
            if ((boolean) xPathFactory.getFeature(ORACLE_FEATURE_SERVICE_MECHANISM)) {
                Assert.fail("should not override in secure mode");
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            clearSystemProperty(DOM_FACTORY_ID);
        }
    }
}
