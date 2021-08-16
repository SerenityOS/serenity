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

import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;

/*
 * @test
 * @bug 4969110
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4969110
 * @run testng/othervm validation.Bug4969110
 * @summary Test ValidationHandler.set/getProperty() throws a correct exception
 * instead of a sun internal exception in case the "property name" parameter is invalid.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4969110 {

    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

    @Test
    public void test1() throws SAXException {
        try {
            ValidatorHandler validatorHandler = schemaFactory.newSchema().newValidatorHandler();
            validatorHandler.getProperty("unknown1234");
            Assert.fail("SAXNotRecognizedException was not thrown.");
        } catch (SAXNotRecognizedException e) {
        }
    }

    @Test
    public void test2() throws SAXException {
        try {
            doTest(null);
            Assert.fail("NullPointerException was not thrown.");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void test3() throws SAXException {
        try {
            doTest("unknown1234");
            Assert.fail("SAXNotRecognizedException was not thrown.");
        } catch (SAXNotRecognizedException e) {
        }
    }

    public void doTest(String name) throws SAXException {
        ValidatorHandler validatorHandler = schemaFactory.newSchema().newValidatorHandler();
        validatorHandler.setProperty(name, "123");
    }
}
