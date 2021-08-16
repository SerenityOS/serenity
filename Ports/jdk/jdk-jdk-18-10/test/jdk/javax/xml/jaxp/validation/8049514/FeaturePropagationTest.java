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
 * @bug 8049514
 * @summary verifies that feature set on the factory is propagated properly
 *          to the validator
 * @run main/othervm FeaturePropagationTest
 */


import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

/**
 * JDK-8049514
 *
 * FEATURE_SECURE_PROCESSING can not be turned off on a validator through
 * SchemaFactory
 */
public class FeaturePropagationTest {

    static String xsd = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n"
            + "        xmlns:test='jaxp13_test'\n"
            + "        targetNamespace='jaxp13_test'\n"
            + "        elementFormDefault='qualified'>\n"
            + "    <element name='test' type='string'/>\n"
            + "</schema>\n";

    public static void main(String[] args) throws Exception {
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(xsd.getBytes()));
        StreamSource xsdSource = new StreamSource(reader);

        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        Schema schema = null;
        schema = schemaFactory.newSchema(xsdSource);

        Validator validator = schema.newValidator();

        if (validator.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            throw new RuntimeException("Feature set on the factory is not inherited!");
        }

    }
}
