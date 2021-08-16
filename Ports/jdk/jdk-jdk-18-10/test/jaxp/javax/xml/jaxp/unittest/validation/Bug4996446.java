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

import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 4996446
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4996446
 * @run testng/othervm validation.Bug4996446
 * @summary Test SchemaFactory can detect violations of the "Schema Component Constraint: Element Declarations Consistent".
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4996446 {

    SchemaFactory schemaFactory = null;

    @BeforeMethod
    public void setUp() {
        schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    }

    @AfterMethod
    public void tearDown() {
        schemaFactory = null;
    }

    @Test
    public void testOne() {

        ErrorHandler errorHandler = new ErrorHandler();
        schemaFactory.setErrorHandler(errorHandler);
        URL fileName = Bug4996446.class.getResource("Bug4996446.xsd");
        try {
            schemaFactory.newSchema(fileName);
        } catch (SAXException e) {
        }

        if (errorHandler.errorCounter == 0) {
            Assert.fail(" No Errors reported: " + errorHandler.errorCounter);
        }
        return;
    }
}

class ErrorHandler extends DefaultHandler {
    public int errorCounter = 0;

    public void error(SAXParseException e) throws SAXException {
        // System.out.println(e);
        errorCounter++;
    }

    public void fatalError(SAXParseException e) throws SAXException {
        // System.out.println(e);
        errorCounter++;
    }
}
