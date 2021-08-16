/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key printer
 * @bug 6387255
 * @summary  Tests conflict of Media values returned by isAttrValueSupported and getSupportedAttrValues.  No runtime exception should be thrown.
 * @run main AttributeTest
 */

import javax.print.*;
import javax.print.attribute.standard.*;
import javax.print.attribute.*;

public class AttributeTest {

        public AttributeTest() {

                PrintService service[] = PrintServiceLookup.lookupPrintServices(null, null);

                if (service.length == 0) {
                        throw new RuntimeException("No printer found.  TEST ABORTED");
                }

                for (int x = 0; x < service.length; x ++) {
                        DocFlavor flavors[] = service[x].getSupportedDocFlavors();

                        for (int y = 0; y < flavors.length; y ++) {
                                Object attrVal = service[x].getSupportedAttributeValues(Media.class, flavors[y], null);
                                if (attrVal == null) {
                                        continue;
                                }
                                Media attr[] = (Media[]) attrVal;
                                for (int z = 0; z < attr.length; z ++) {
                                        if (!service[x].isAttributeValueSupported(attr[z], flavors[y], null)) {
                                                throw new RuntimeException("ERROR: There is a conflict between getSupportedAttrValues " +
                                                " and isAttributeValueSupported, for the attribute: " + attr[z] +
                                                ", where the flavor is: " + flavors[y] + " and the print service is: " +
                                                service[x] + "\n");
                                        }
                                }
                        }
                }

                System.out.println("Test Passed");
        }

        public static void main (String args[]) {
                AttributeTest test = new AttributeTest();
        }
}
