/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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


/**
 * @test
 * @bug 8015092
 * @summary whitespace within patterns in Selector XPath expression is accepted
 * @run main/othervm XPathWhiteSpaceTest
 */

import java.io.*;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 * http://www.w3.org/TR/xmlschema-1/#coss-identity-constraint
 * For readability, whitespace may be used in selector XPath expressions even though
 * not explicitly allowed by the grammar:
 * whitespace may be freely added within patterns before or after any token.
 *
 * @author huizhe.wang@oracle.com
 */
public class XPathWhiteSpaceTest {
    static final String XSDFILE = "idJ029.xsd";

    public static void main(String[] args) throws Exception {
        try{
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new File(System.getProperty("test.src", "."), XSDFILE));
        } catch (SAXException e) {
            throw new RuntimeException(e.getMessage());
        }


    }

}
