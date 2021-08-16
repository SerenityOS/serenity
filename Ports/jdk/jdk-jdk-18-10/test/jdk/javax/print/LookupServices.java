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
 * @bug 4510477 6520186
 * @summary No crash with HP OfficeJet 600 installed.
 * @run main LookupServices
*/

import javax.print.attribute.*;
import javax.print.*;

public class LookupServices {
    public static void main (String [] args) {

        DocFlavor flavor = DocFlavor.INPUT_STREAM.GIF;
        HashPrintRequestAttributeSet prSet = null;

        PrintService[] serv = PrintServiceLookup.lookupPrintServices(flavor, null);
        System.out.println("default "+PrintServiceLookup.lookupDefaultPrintService());
        if (serv.length==0) {
            System.out.println("no PrintService supports GIF");
            return;
        }

        System.out.println("Selected print service: "+ serv[0]);
    }

}
