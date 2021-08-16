/*
 * Copyright (c) 2001, 2009, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @test
 * @bug 4446106
 * @key printer
 * @requires (os.family == "windows")
 * @summary Test for chromaticity values.
 * @run main ChromaticityValues
*/



import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.util.ArrayList;

public class ChromaticityValues {

    public static void main(String args[]) {
        System.out.println("=======================================================================");
        System.out.println("INSTRUCTIONS: This test is only for WINDOWS platform. ");
        System.out.println("You should have a printer configured as your default printer in your system.");
        System.out.println("=======================================================================");

        String os=System.getProperty("os.name").toLowerCase();

        if (!(os.indexOf("win")>=0)) {
            System.out.println("Not a Windows System.  TEST ABORTED");
            return;
        }

        PrintService pservice = PrintServiceLookup.lookupDefaultPrintService();
        if (pservice == null) {
            throw new RuntimeException("A printer is required for this test.");
        }

        System.out.println("Default Service is "+pservice);
        ColorSupported psa = (ColorSupported)pservice.getAttribute(ColorSupported.class);
        ArrayList cValues = new ArrayList();

        if (pservice.isAttributeCategorySupported(Chromaticity.class)) {
            Chromaticity[] values =(Chromaticity[])
                (pservice.getSupportedAttributeValues(Chromaticity.class, DocFlavor.SERVICE_FORMATTED.PAGEABLE, null));
            if ((values != null) && (values.length > 0)) {
                for (int i=0; i<values.length; i++) {
                    cValues.add(values[i]);
                }
            } else {
                System.out.println("Chromaticity value is unknown. TEST ABORTED");
                return;
            }

        } else {
            System.out.println("Chromaticity is not supported. TEST ABORTED");
            return;

        }

        if (psa != null) {
            if (psa.equals(ColorSupported.SUPPORTED)) {
                if (cValues.size() < 2) {
                    throw new RuntimeException("ColorSupported is supported, values for Chromaticity should be monochrome and color.");
                }
            } else {
                if ((cValues.size() != 1) ||
                    (!cValues.contains(Chromaticity.MONOCHROME))) {
                    throw new RuntimeException("ColorSupported is not supported, values for Chromaticity should only be monochrome.");
                }
            }
        } else { // ColorSupported unknown
            if (!cValues.contains(Chromaticity.COLOR)) {
                throw new RuntimeException("ColorSupported is unknown, values for Chromaticity should at least include color.");
            }

        }
    }
}
