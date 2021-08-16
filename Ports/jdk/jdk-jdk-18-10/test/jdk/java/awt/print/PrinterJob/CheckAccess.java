/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4151121
 * @run main/othervm -Djava.security.manager=allow CheckAccess
 * @summary Confirm that PrinterJob.getPrinterJob is access checked.
 * @author Graham Hamilton
 */

import java.awt.print.*;
import java.security.*;

public class CheckAccess {

    static boolean verbose;

    private static void println(String mess) {
        if (verbose) {
            System.err.println(mess);
        }
    }

    /**
     * SecurityManager that rejects all print requests,
     * but allows everything else.
     */
    static class PrintHater extends SecurityManager {

        public void checkPermission(Permission p) {
           // We're easy.
        }

        public void checkPrintJobAccess() {
            throw new SecurityException("No way!");
        }
    }

    public static void main(String argv[]) {

        if (argv.length > 0 && argv[0].equals("-v")) {
            verbose = true;
        }

        // Try to install our own security manager.
        try {
            SecurityManager sm = new PrintHater();
            println("Installing PrintHater security manager");
            System.setSecurityManager(sm);
            println("Installed security manager OK");

        } catch (Throwable th) {
            System.err.println("Failed to install SecurityManager");
            th.printStackTrace();
            throw new RuntimeException("Failed to install SecurityManager");
        }

        try {

            println("Calling PrinterJob.getPrinterJob()");
            PrinterJob.getPrinterJob();

            // Woops.  We did not get the SecurityException we expected.
            println("Failed to get SecurityException");
            throw new RuntimeException("Failed to get expected SecurityException");

        } catch (SecurityException ex) {
            // Happy, happy.  This is what we want.
            println("Got expected SecurityException OK.");
            return;
        }

    }
}
