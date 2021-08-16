/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.standard.PrinterName;

/*
 * @test
 * @bug 8013810 8025439
 * @summary Test that print service returned without filter are of the same class
 *          as with name filter
 */
public class GetPrintServices {

    public static void main(String[] args) throws Exception {
        for (PrintService service : PrintServiceLookup.lookupPrintServices(null, null)) {
            String serviceName = service.getName();
            PrinterName name = service.getAttribute(PrinterName.class);
            String printerName = name.getValue();

            PrintService serviceByName = lookupByName(printerName);
            System.out.println("service " + service);
            System.out.println("serviceByName " + serviceByName);
            if (!service.equals(serviceByName)) {
                throw new RuntimeException("NOK " + serviceName
                                   + " expected: " + service.getClass().getName()
                                   + " got: " + serviceByName.getClass().getName());
            }
        }
        System.out.println("Test PASSED");
    }

    private static PrintService lookupByName(String name) {
        AttributeSet attributes = new HashAttributeSet();
        attributes.add(new PrinterName(name, null));
        for (PrintService service :
             PrintServiceLookup.lookupPrintServices(null, attributes)) {
            return service;
        }
        return null;
    }
}
