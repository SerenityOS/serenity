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

import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.runWithTmpPermission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.PropertyPermission;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ErrorHandler;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.ValidatorTest
 * @run testng/othervm validation.ValidatorTest
 * @summary Test Validator.validate(Source, Result).
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ValidatorTest {

    @Test
    public void testValidateStAX() {

        File resultFile = null;
        try {
            resultFile = new File(USER_DIR + "stax.result");
            if (resultFile.exists()) {
                resultFile.delete();
            }

            Result xmlResult = new javax.xml.transform.stax.StAXResult(XMLOutputFactory.newInstance().createXMLStreamWriter(new FileWriter(resultFile)));
            Source xmlSource = new javax.xml.transform.stax.StAXSource(getXMLEventReader("toys.xml"));
            validate("toys.xsd", xmlSource, xmlResult);

            ((StAXResult) xmlResult).getXMLStreamWriter().close();
            Assert.assertTrue(resultFile.exists(), "result file is not created");

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Exception : " + ex.getMessage());
        } finally {
            if (resultFile != null && resultFile.exists()) {
                resultFile.delete();
            }
        }
    }

    @Test
    public void testValidateStream() {

        File resultFile = null;
        try {
            resultFile = new File(USER_DIR + "stax.result");
            if (resultFile.exists()) {
                resultFile.delete();
            }
            // Validate this instance document against the
            // Instance document supplied
            File resultAlias = resultFile;
            Result xmlResult = runWithTmpPermission(() -> new javax.xml.transform.stream.StreamResult(
                    resultAlias), new PropertyPermission("user.dir", "read"));
            Source xmlSource = new javax.xml.transform.stream.StreamSource(new File(ValidatorTest.class.getResource("toys.xml").toURI()));

            validate("toys.xsd", xmlSource, xmlResult);
            Assert.assertTrue(resultFile.exists(), "result file is not created");
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Exception : " + ex.getMessage());
        } finally {
            if (resultFile != null && resultFile.exists()) {
                resultFile.delete();
            }
        }
    }

    @Test
    public void testValidateGMonth() {

        // test valid gMonths
        File resultFile = null;
        try {
            resultFile = new File(USER_DIR + "gMonths.result.xml");
            if (resultFile.exists()) {
                resultFile.delete();
            }

            // Validate this instance document against the
            // Instance document supplied
            File resultAlias = resultFile;
            Result xmlResult = runWithTmpPermission(() -> new javax.xml.transform.stream.StreamResult(
                    resultAlias), new PropertyPermission("user.dir", "read"));
            Source xmlSource = new javax.xml.transform.stream.StreamSource(new File(ValidatorTest.class.getResource("gMonths.xml").toURI()));

            validate("gMonths.xsd", xmlSource, xmlResult);

            Assert.assertTrue(resultFile.exists(), "result file is not created");
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Exception : " + ex.getMessage());
        } finally {
            if (resultFile != null && resultFile.exists()) {
                resultFile.delete();
            }
        }

        // test invalid gMonths
        File invalidResultFile = null;
        try {
            invalidResultFile = new File(USER_DIR + "gMonths-invalid.result.xml");
            if (invalidResultFile.exists()) {
                invalidResultFile.delete();
            }

            // Validate this instance document against the
            // Instance document supplied
            Result xmlResult = new javax.xml.transform.stream.StreamResult(resultFile);
            Source xmlSource = new javax.xml.transform.stream.StreamSource(new File(ValidatorTest.class.getResource("gMonths-invalid.xml").toURI()));

            validate("gMonths.xsd", xmlSource, xmlResult);

            // should have failed with an Exception due to invalid gMonths
            Assert.fail("invalid gMonths were accepted as valid in " + ValidatorTest.class.getResource("gMonths-invalid.xml").toURI());
        } catch (Exception ex) {
            // expected failure
            System.out.println("Expected failure: " + ex.toString());
        } finally {
            if (invalidResultFile != null && invalidResultFile.exists()) {
                invalidResultFile.delete();
            }
        }
    }

    private void validate(final String xsdFile, final Source src, final Result result) throws Exception {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new File(ValidatorTest.class.getResource(xsdFile).toURI()));

            // Get a Validator which can be used to validate instance document
            // against this grammar.
            Validator validator = schema.newValidator();
            ErrorHandler eh = new ErrorHandlerImpl();
            validator.setErrorHandler(eh);

            // Validate this instance document against the
            // Instance document supplied
            validator.validate(src, result);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private XMLEventReader getXMLEventReader(final String filename) {

        XMLInputFactory xmlif = null;
        XMLEventReader xmlr = null;
        try {
            xmlif = XMLInputFactory.newInstance();
            xmlif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
            xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            xmlif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            xmlif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);

            // FileInputStream fis = new FileInputStream(filename);
            FileInputStream fis = new FileInputStream(new File(ValidatorTest.class.getResource(filename).toURI()));
            xmlr = xmlif.createXMLEventReader(filename, fis);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Exception : " + ex.getMessage());
        }
        return xmlr;
    }
}
