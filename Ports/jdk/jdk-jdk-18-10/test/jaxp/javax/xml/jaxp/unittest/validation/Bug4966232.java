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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 4966232
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4966232
 * @run testng/othervm validation.Bug4966232
 * @summary Test SchemaFactory.newSchema(Source) returns a Schema instance for DOMSource & SAXSource.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4966232 {

    // test for W3C XML Schema 1.0 - newSchema(Source schema)
    // supports and return a valid Schema instance
    // SAXSource - valid schema

    @Test
    public void testSchemaFactory01() throws Exception {
        SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        InputSource is = new InputSource(Bug4966232.class.getResourceAsStream("test.xsd"));
        SAXSource ss = new SAXSource(is);
        Schema s = sf.newSchema(ss);
        Assert.assertNotNull(s);
    }

    // test for W3C XML Schema 1.0 - newSchema(Source schema)
    // supports and return a valid Schema instance
    // DOMSource - valid schema

    @Test
    public void testSchemaFactory02() throws Exception {
        Document doc = null;
        SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        doc = dbf.newDocumentBuilder().parse(Bug4966232.class.getResource("test.xsd").toExternalForm());
        DOMSource ds = new DOMSource(doc);
        Schema s = sf.newSchema(ds);
        Assert.assertNotNull(s);
    }
}
