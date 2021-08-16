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
 * @bug 8015243
 * @summary verifies that illegal URI such as // is rejected
 * @run main/othervm AnyURITest
 */

import java.io.*;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 * Similar to java.net.URI, allow empty authority prior to non-empty
 * path, query component or fragment identifier
 *
 * @author huizhe.wang@oracle.com
 */
public class AnyURITest {
    static final String XSDFILE = "anyURI_b006.xsd";

    public static void main(String[] args) throws Exception {
        try{
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new File(System.getProperty("test.src", "."), XSDFILE));
            throw new RuntimeException("Illegal URI // should be rejected.");
        } catch (SAXException e) {
            //expected:
            //Enumeration value '//' is not in the value space of the base type, anyURI.
        }


    }

}
