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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.file.Paths;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6457662
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug6457662
 * @run testng/othervm validation.Bug6457662
 * @summary Test a Validator checks sequence maxOccurs correctly when it validates document repeatedly.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6457662 {

    public static final String xml = "<ACL xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" + "<Tokens access=\"full\">" + "<Token>CheetahTech</Token>"
            + "<Token>CheetahView</Token>" + "</Tokens>" + "</ACL>";
    /** Schema */
    public static final String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">"
            + "<xs:element name=\"ACL\">" + "<xs:complexType mixed=\"false\">" + "<xs:sequence><xs:element ref=\"Tokens\" maxOccurs=\"3\"/></xs:sequence>"
            + "<xs:attribute name=\"ACL\" type=\"xs:string\" use=\"optional\"/>" + "</xs:complexType>" + "</xs:element><xs:element name=\"Tokens\">"
            + "<xs:complexType mixed=\"false\">" + "<xs:sequence><xs:element ref=\"Token\" maxOccurs=\"unbounded\"/></xs:sequence>"
            + "<xs:attribute name=\"access\" type=\"xs:string\" use=\"required\"/>" + "</xs:complexType></xs:element><xs:element name=\"Token\"/>"
            + "</xs:schema>";
    /** Schema factory */
    private static final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    @Test
    public void test() throws Exception {
        final Schema sc = factory.newSchema(writeSchema());
        final Validator validator = sc.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
        validator.validate(new StreamSource(new StringReader(xml)));
        validator.validate(new StreamSource(new StringReader(xml)));
        validator.validate(new StreamSource(new StringReader(xml)));
    }

    private File writeSchema() throws IOException {
        final File rtn = File.createTempFile("scheam", "xsd", Paths.get(USER_DIR).toFile());
        final OutputStream out = new FileOutputStream(rtn);
        final OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
        writer.write(schema);
        writer.close();
        out.close();
        return rtn;
    }
}
