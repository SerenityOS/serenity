/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4996318 6731937
 * @summary  There should be no duplicates returned by getSupportedDocFlavors.
 * @run main CheckDupFlavor
 */

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.util.ArrayList;


public class CheckDupFlavor {
    public static void main(String[] args){
        PrintService defService = PrintServiceLookup.lookupDefaultPrintService();
        PrintService[] pservice;
        if (defService == null) {
            pservice = PrintServiceLookup.lookupPrintServices(null, null);
            if (pservice.length == 0) {
                throw new RuntimeException("No printer found.  TEST ABORTED");
            }
            defService = pservice[0];
        }

        System.out.println("PrintService = "+defService);

        DocFlavor[] flavors = defService.getSupportedDocFlavors();
        if (flavors==null) {
            System.out.println("No flavors supported. Test PASSED.");
            return;
        }


        ArrayList flavorList = new ArrayList();
        for (int i=0; i<flavors.length; i++) {
            if (flavors[i] == null) {
                 throw new RuntimeException("Null flavor. Test FAILED.");
            } else if (flavorList.contains(flavors[i])) {
                 throw new RuntimeException("\n\tDuplicate flavor found : "+flavors[i]+" : Test FAILED.");
            } else {
                flavorList.add(flavors[i]);
            }
        }
        System.out.println("No duplicate found. Test PASSED.");
    }
}
