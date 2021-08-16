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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/*
 * @test
 * @bug 6631318
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.JaxpIssue43Test
 * @run testng/othervm validation.JaxpIssue43Test
 * @summary Test creating schema from a DOM fragment with namespace.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class JaxpIssue43Test {

    @Test
    public void test() throws Exception {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source[] sources = getSchemaSources();
        Schema schema = sf.newSchema(sources);
        Validator validator = schema.newValidator();
    }

    private Source[] getSchemaSources() throws Exception {
        List<Source> list = new ArrayList<Source>();
        String file = getClass().getResource("hello_literal.wsdl").getFile();
        Source source = new StreamSource(new FileInputStream(file), file);

        Transformer trans = TransformerFactory.newInstance().newTransformer();
        DOMResult result = new DOMResult();
        trans.transform(source, result);

        // Look for <xsd:schema> element in wsdl
        Element e = ((Document) result.getNode()).getDocumentElement();
        NodeList typesList = e.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "types");
        NodeList schemaList = ((Element) typesList.item(0)).getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "schema");
        Element elem = (Element) schemaList.item(0);
        list.add(new DOMSource(elem, file + "#schema0"));

        // trans.transform(new DOMSource(elem), new StreamResult(System.out));

        return list.toArray(new Source[list.size()]);
    }
}
