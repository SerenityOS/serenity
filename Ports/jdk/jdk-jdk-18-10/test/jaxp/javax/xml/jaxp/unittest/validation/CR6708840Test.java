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

import java.io.File;
import java.io.FileWriter;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6708840
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.CR6708840Test
 * @run testng/othervm validation.CR6708840Test
 * @summary Test Validator can process StAXSource.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CR6708840Test {

    @Test
    public final void testStream() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schemaGrammar = schemaFactory.newSchema(new File(getClass().getResource("gMonths.xsd").getFile()));

            Validator schemaValidator = schemaGrammar.newValidator();
            Source xmlSource = new javax.xml.transform.stream.StreamSource(new File(CR6708840Test.class.getResource("gMonths.xml").toURI()));
            schemaValidator.validate(xmlSource);

        } catch (NullPointerException ne) {
            Assert.fail("NullPointerException when result is not specified.");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * refer to http://forums.java.net/jive/thread.jspa?threadID=41626&tstart=0
     */
    @Test
    public final void testStAX() {
        try {
            XMLInputFactory xmlif = XMLInputFactory.newInstance();

            // XMLStreamReader staxReader =
            // xmlif.createXMLStreamReader((Source)new
            // StreamSource(getClass().getResource("Forum31576.xml").getFile()));
            XMLStreamReader staxReader = xmlif.createXMLStreamReader(this.getClass().getResourceAsStream("gMonths.xml"));

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schemaGrammar = schemaFactory.newSchema(new File(getClass().getResource("gMonths.xsd").getFile()));

            Validator schemaValidator = schemaGrammar.newValidator();

            Source staxSrc = new StAXSource(staxReader);
            schemaValidator.validate(staxSrc);

            while (staxReader.hasNext()) {
                int eventType = staxReader.next();
                System.out.println("Event of type: " + eventType);
            }
        } catch (NullPointerException ne) {
            Assert.fail("NullPointerException when result is not specified.");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * workaround before the fix: provide a result
     */
    @Test
    public final void testStAXWResult() {
        try {
            XMLInputFactory xmlif = XMLInputFactory.newInstance();

            // XMLStreamReader staxReader =
            // xmlif.createXMLStreamReader((Source)new
            // StreamSource(getClass().getResource("Forum31576.xml").getFile()));
            XMLStreamReader staxReader = xmlif.createXMLStreamReader(this.getClass().getResourceAsStream("gMonths.xml"));

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schemaGrammar = schemaFactory.newSchema(new File(getClass().getResource("gMonths.xsd").getFile()));

            Validator schemaValidator = schemaGrammar.newValidator();

            Source staxSrc = new StAXSource(staxReader);
            File resultFile = new File(USER_DIR + "gMonths.result.xml");
            if (resultFile.exists()) {
                resultFile.delete();
            }

            Result xmlResult = new javax.xml.transform.stax.StAXResult(XMLOutputFactory.newInstance().createXMLStreamWriter(new FileWriter(resultFile)));
            schemaValidator.validate(staxSrc, xmlResult);

            while (staxReader.hasNext()) {
                int eventType = staxReader.next();
                System.out.println("Event of type: " + eventType);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }
}
