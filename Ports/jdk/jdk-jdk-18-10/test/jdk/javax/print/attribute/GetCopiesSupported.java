/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4463280
 * @summary No ClassCastException should occur.
 * @run main GetCopiesSupported
 */

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class GetCopiesSupported {

    public static void main(String args[]) {
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        PrintService[] pservice;
        if (service == null) {
             pservice = PrintServiceLookup.lookupPrintServices(null, null);
            if (pservice.length == 0) {
                    throw new RuntimeException("No printer found.  TEST ABORTED");
            }
            service = pservice[0];
        }

        if (service != null) {
            CopiesSupported c = (CopiesSupported)
               service.getSupportedAttributeValues(Copies.class,
                                                   null, null);

           System.out.println("CopiesSupported : "+c);
        }
    }


}
