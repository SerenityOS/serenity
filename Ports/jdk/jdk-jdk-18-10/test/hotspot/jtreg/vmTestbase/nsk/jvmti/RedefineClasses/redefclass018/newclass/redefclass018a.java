/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.RedefineClasses;

import java.io.PrintStream;

class redefclass018a extends Thread {
    final static int LOOP_COUNT = 30000;
    public static boolean testFailed = false;
    public static PrintStream sout;

    public void run() {
        try {
            for (int i = 0; ; i++) {
                if (i > LOOP_COUNT) {
                    thrower();
                    break;
                }
            }
            sout.println("# thrower() does not throw any exception");
            testFailed = true;
        } catch (NumberFormatException ex) {
            // OK
        } catch (Exception ex) {
            sout.println("# thrower() throws unexpected exception:");
            sout.println("# " + ex);
            testFailed = true;
        }
    }

    // method to be breakpointed in agent
    private void thrower() throws NumberFormatException {
        throw new NumberFormatException("redefined");
    }
}
