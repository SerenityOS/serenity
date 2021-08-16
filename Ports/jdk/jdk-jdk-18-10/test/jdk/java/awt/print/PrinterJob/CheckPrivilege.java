/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4151151
 * @summary Confirm that low-level print code does doPrivilege.
 * @author Graham Hamilton
 */

import java.awt.print.*;

public class CheckPrivilege implements Printable {

    static boolean verbose;

    private static void println(String mess) {
        if (verbose) {
            System.err.println(mess);
        }
    }

    /**
     * SecurityManager that allows print requests, but
     * causes things like "exec" to get checked.
     */
    static class PrintLover extends SecurityManager {
        public void checkPrintJobAccess() {
        }
        public void checkPackageAccess(String pkg) {
        }
        public void checkPropertyAccess(String key) {
        }
    }

    /**
     * Internal exception to boucne us out of the print code
     */
    class Printing extends RuntimeException {
    }

    public static void main(String argv[]) {

        System.out.println( "-----------------------------------------------------------------------");
        System.out.println( "INSTRUCTIONS: You should have a printer configured in your system to do this test. Test fails if you get this error message:");
        System.out.println("   \"Regression: printing causes a NullPointerException\"");
    System.out.println( "-----------------------------------------------------------------------");

        if (argv.length > 0 && argv[0].equals("-v")) {
            verbose = true;
        }

        // We need to make sure AWT is initialized.  This is bug #4162674
        java.awt.Toolkit.getDefaultToolkit();

        // Try to install our own security manager.
        try {
            SecurityManager sm = new PrintLover();
            println("Installing PrintLover security manager");
            System.setSecurityManager(sm);
            println("Installed security manager OK");

        } catch (Throwable th) {
            System.err.println("Failed to install SecurityManager");
            th.printStackTrace();
            throw new RuntimeException("Failed to install SecurityManager");
        }

        try {
            println("calling getPrinterJob");
            PrinterJob pj = PrinterJob.getPrinterJob();
            if ((pj == null) || (pj.getPrintService() == null)){
                return;
            }

            println("PrinterJob class is " + pj.getClass());
            println("calling pj.setPrintable");
            pj.setPrintable(new CheckPrivilege());
            println("calling pj.print");
            pj.print();
            println("done pj.print");

        } catch (Printing ex) {
            // We get here if the print request started OK.
            println("Caught \"Printing\" exception OK");

        } catch (PrinterException ex) {
            System.err.println("Caught " + ex);
            throw new RuntimeException("" + ex);

        } catch (NullPointerException ex) {
            // This is the bug:
            System.err.println("Caught " + ex);
            System.err.println("Regression: printing causes a NullPointerException");
            throw ex;
        }

        //System.exit(0);

    }

    // Back-call from the new print APIs.
    // We always say we have bothing to print.
    public int print(java.awt.Graphics g, PageFormat pf, int index) {
        println("Started printing " + index);
        return Printable.NO_SUCH_PAGE;
    }


}
