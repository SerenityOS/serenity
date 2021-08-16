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

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Node;

/*
 * @test
 * @bug 6684227
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.JaxpIssue49
 * @run testng/othervm validation.JaxpIssue49
 * @summary Test property current-element-node works.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class JaxpIssue49 {

    private Schema schema;
    private Validator validator;

    @Test
    public void testValidatorTest() throws Exception {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String file = getClass().getResource("types.xsd").getFile();
            Source[] sources = new Source[] { new StreamSource(new FileInputStream(file), file) };
            Schema schema = sf.newSchema(sources);
            validator = schema.newValidator();
            validate();
        } catch (Exception e) {
            Node node = (Node) validator.getProperty("http://apache.org/xml/properties/dom/current-element-node");
            if (node != null) {
                System.out.println("Node: " + node.getLocalName());
            } else
                Assert.fail("No node returned");
        }
    }

    public void validate() throws Exception {
        validator.reset();
        Source source = new StreamSource(getClass().getResourceAsStream("JaxpIssue49.xml"));
        // If you comment the following line, it works
        source = toDOMSource(source);
        validator.validate(source);
    }

    DOMSource toDOMSource(Source source) throws Exception {
        if (source instanceof DOMSource) {
            return (DOMSource) source;
        }
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        DOMResult result = new DOMResult();
        trans.transform(source, result);
        trans.transform(new DOMSource(result.getNode()), new StreamResult(System.out));
        return new DOMSource(result.getNode());
    }

}
