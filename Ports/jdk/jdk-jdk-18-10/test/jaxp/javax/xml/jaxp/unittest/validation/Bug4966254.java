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

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import util.DraconianErrorHandler;

/*
 * @test
 * @bug 4966254
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4966254
 * @run testng/othervm validation.Bug4966254
 * @summary Test validate(StreamSource) & validate(StreamSource,null) works instead of throws IOException.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4966254 {

    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    @Test
    public void testValidator01() throws Exception {
        getValidator().validate(getInstance());
    }

    @Test
    public void testValidator02() throws Exception {
        getValidator().validate(getInstance(), null);
    }

    private StreamSource getInstance() {
        return new StreamSource(Bug4966254.class.getResource(("Bug4966254.xml")).toExternalForm());
    }

    private Validator getValidator() throws SAXException {
        Schema s = getSchema();
        Validator v = s.newValidator();
        Assert.assertNotNull(v);
        v.setErrorHandler(new DraconianErrorHandler());
        return v;
    }

    private Schema getSchema() throws SAXException {
        SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema s = sf.newSchema(Bug4966254.class.getResource("Bug4966254.xsd"));
        Assert.assertNotNull(s);
        return s;
    }
}
