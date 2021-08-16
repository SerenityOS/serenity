/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package transform;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.SecureProcessingTest
 * @run testng/othervm transform.SecureProcessingTest
 * @summary Test XSLT shall report TransformerException for unsafe xsl when FEATURE_SECURE_PROCESSING is true.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SecureProcessingTest {
    @Test
    public void testSecureProcessing() {
        boolean _isSecureMode = System.getSecurityManager() != null;
        // SECURE_PROCESSING == false

        // the style sheet
        InputStream xslStream = this.getClass().getResourceAsStream("SecureProcessingTest.xsl");
        StreamSource xslSource = new StreamSource(xslStream);

        // the xml source
        InputStream xmlStream = this.getClass().getResourceAsStream("SecureProcessingTest.xml");
        StreamSource xmlSource = new StreamSource(xmlStream);

        // the xml result
        StringWriter xmlResultString = new StringWriter();
        StreamResult xmlResultStream = new StreamResult(xmlResultString);

        // the transformer
        TransformerFactory transformerFactory = null;
        Transformer transformer = null;

        // transform with a non-secure Transformer
        // expect success
        String xmlResult;
        if (!_isSecureMode) { // jaxp secure feature can not be turned off when
                              // security manager is present
            try {
                transformerFactory = TransformerFactory.newInstance();
                transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
                transformer = transformerFactory.newTransformer(xslSource);
                transformer.transform(xmlSource, xmlResultStream);
            } catch (TransformerConfigurationException ex) {
                ex.printStackTrace();
                Assert.fail(ex.toString());
            } catch (TransformerException ex) {
                ex.printStackTrace();
                Assert.fail(ex.toString());
            }

            // expected success
            // and the result is ...
            xmlResult = xmlResultString.toString();
            System.out.println("Transformation result (SECURE_PROCESSING == false) = \"" + xmlResult + "\"");
        }

        // now do same transformation but with SECURE_PROCESSING == true
        // expect Exception
        boolean exceptionCaught = false;

        // the style sheet
        xslStream = this.getClass().getResourceAsStream("SecureProcessingTest.xsl");
        xslSource = new StreamSource(xslStream);

        // the xml source
        xmlStream = this.getClass().getResourceAsStream("SecureProcessingTest.xml");
        xmlSource = new StreamSource(xmlStream);

        // the xml result
        xmlResultString = new StringWriter();
        xmlResultStream = new StreamResult(xmlResultString);

        // the transformer
        transformerFactory = null;
        transformer = null;

        // transform with a secure Transformer
        try {
            transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformer = transformerFactory.newTransformer(xslSource);
            transformer.transform(xmlSource, xmlResultStream);
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.toString());
        } catch (TransformerException ex) {
            // expected failure
            System.out.println("expected failure: " + ex.toString());
            ex.printStackTrace(System.out);
            exceptionCaught = true;
        }

        // unexpected success?
        if (!exceptionCaught) {
            // and the result is ...
            xmlResult = xmlResultString.toString();
            System.err.println("Transformation result (SECURE_PROCESSING == true) = \"" + xmlResult + "\"");
            Assert.fail("SECURITY_PROCESSING == true, expected failure but got result: \"" + xmlResult + "\"");
        }
    }
}
