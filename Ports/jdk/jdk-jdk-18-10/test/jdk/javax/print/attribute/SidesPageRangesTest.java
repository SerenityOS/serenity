/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4903366
 * @summary No crash should occur.
 * @run main SidesPageRangesTest
 */

import java.awt.*;
import javax.print.*;
import javax.print.attribute.standard.*;
import javax.print.attribute.*;
import java.io.*;
import java.util.Locale;
import java.net.URL;

public class SidesPageRangesTest {
        /**
         * Constructor
         */
         public SidesPageRangesTest() {
                super();
        }
        /**
         * Starts the application.
         */
        public static void main(java.lang.String[] args) {
                SidesPageRangesTest pd = new SidesPageRangesTest();
                PrintService defService = null;
                DocFlavor flavors[]  = null;
                PrintService[] pservice;
                defService = PrintServiceLookup.lookupDefaultPrintService();
                if (defService == null) {
                    pservice = PrintServiceLookup.lookupPrintServices(null, null);
                    if (pservice.length == 0) {
                        throw new RuntimeException("Printer is required for this test.  TEST ABORTED");
                    }
                    defService = pservice[0];
                }
                System.out.println("Default Print Service "+defService);


                if (defService.isAttributeCategorySupported(PageRanges.class)) {
                        System.out.println("\nPageRanges Attribute category is supported");
                } else {
                        System.out.println("\nPageRanges Attribute category is not supported. terminating...");
                        return;
                }

                flavors = defService.getSupportedDocFlavors();
                System.out.println("\nGetting Supported values for PageRanges for each supported DocFlavor");
                System.out.println("===============================================================\n");
                for (int y = 0; y < flavors.length; y ++) {
                    System.out.println("\n\n");

                    System.out.println("Doc Flavor: "+flavors[y]);
                    System.out.println("-----------------------------");

                    Object vals = defService.getSupportedAttributeValues(PageRanges.class, flavors[y], null);
                    if (vals == null) {
                        System.out.println("No supported values for PageRanges for this doc flavor. ");
                    }

                    PageRanges[] pr = null;
                    if (vals instanceof PageRanges[]) {
                        pr = (PageRanges[]) vals;
                        for (int x = 0; x < pr.length; x ++) {
                            System.out.println("\nSupported Value "+pr[x]);
                            System.out.println("is "+pr[x]+" value supported? "+defService.isAttributeValueSupported(pr[x], flavors[y], null));

                            if (!defService.isAttributeValueSupported(pr[x], flavors[y], null)) {
                                throw new RuntimeException("PageRanges contradicts getSupportedAttributeValues");
                            }
                        }
                    } else if (vals instanceof PageRanges) {
                        System.out.println(vals);
                        System.out.println("is "+vals+" value supported? "+defService.isAttributeValueSupported((javax.print.attribute.Attribute)vals, flavors[y], null));
                        if (!defService.isAttributeValueSupported((javax.print.attribute.Attribute)vals, flavors[y], null)) {
                            throw new RuntimeException("PageRanges contradicts getSupportedAttributeValues");
                        }
                    }

                    // SIDES test
                    vals = defService.getSupportedAttributeValues(Sides.class, flavors[y], null);
                    if (vals == null) {
                        System.out.println("No supported values for Sides for this doc flavor. ");
                    }

                    Sides[] s = null;
                    if (vals instanceof Sides[]) {
                        s = (Sides[]) vals;
                        for (int x = 0; x < s.length; x ++) {
                            System.out.println("\nSupported Value "+s[x]);
                            System.out.println("is "+s[x]+" value supported? "+defService.isAttributeValueSupported(s[x], flavors[y], null));
                            if  (!defService.isAttributeValueSupported(s[x], flavors[y], null)) {
                                throw new RuntimeException("Sides contradicts getSupportedAttributeValues");
                            }
                        }
                    }
                }
        }
}
