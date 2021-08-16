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

import static jaxp.library.JAXPTestUtilities.runWithAllPerm;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/*
 * @test
 * @bug 6773084
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug6773084Test
 * @run testng/othervm validation.Bug6773084Test
 * @summary Test Schema object is thread safe.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6773084Test {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    private static final int NTHREADS = 25;
    private static final ExecutorService EXEC = Executors.newCachedThreadPool();

    private static final CyclicBarrier BARRIER = new CyclicBarrier(NTHREADS);
    private static final int TIMEOUT = 110;

    public static final String IN_FOLDER = Bug6773084Test.class.getResource("Bug6773084In").getPath();
    public static final String XSD_PATH = Bug6773084Test.class.getResource("Bug6773084.xsd").getPath();

    private static Schema schema;

    @Test
    public void test() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(XSD_PATH);
        try {
            schema = factory.newSchema(schemaFile);
        } catch (SAXException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        File incoming = new File(IN_FOLDER);
        File[] files = incoming.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".xml");
            }
        });

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        for (int i = 0; i < files.length; i++) {
            EXEC.execute(new XMLValiddator(dbf.newDocumentBuilder().parse(files[i]), i));
        }
        runWithAllPerm(() -> EXEC.shutdown());
        EXEC.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
    }

    private static class XMLValiddator implements Runnable {

        private Document document;
        private int index;

        public XMLValiddator(Document document, int index) {
            this.document = document;
            this.index = index;
        }

        public void run() {

            try {
                System.out.printf("Waiting for barrier: %s%n", index);
                BARRIER.await();
                System.out.println("Validating....");

                Validator validator = schema.newValidator();
                validator.setErrorHandler(new ErrorHandlerImpl());
                validator.validate(new DOMSource(document));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
                Assert.fail("Test failed.");
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private static class ErrorHandlerImpl implements ErrorHandler {

        public void warning(SAXParseException exception) throws SAXException {
            System.out
                    .printf("**Parsing Warning. Line: %s  URI: %s  Message: %s%n", exception.getLineNumber(), exception.getSystemId(), exception.getMessage());
        }

        public void error(SAXParseException exception) throws SAXException {
            String msg = String.format("**Parsing Error. Line: %s  URI: %s  Message: %s%n", exception.getLineNumber(), exception.getSystemId(),
                    exception.getMessage());
            System.out.println(msg);
            throw new SAXException(msg);
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            String msg = String.format("**Parsing Fatal Error. Line: %s  URI: %s  Message: %s%n", exception.getLineNumber(), exception.getSystemId(),
                    exception.getMessage());
            System.out.println(msg);
            throw new SAXException(msg);
        }
    }

}
