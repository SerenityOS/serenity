/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.regression.b4689395;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/* This is debuggee aplication */
public class b4689395a {
        static b4689395a _b4689395a = new b4689395a();
        final static String ERROR_MESSAGE  = "ERROR_M";

        public static void main(String args[]) {
                System.exit(Consts.JCK_STATUS_BASE + _b4689395a.runIt(args, System.out));
        }

        public int runIt(String args[], PrintStream out) {
                JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
                Log log = new Log(out, argumentHandler);

                minor();

                log.display("Debuggee PASSED");
                return Consts.TEST_PASSED;
        }

        public static void minor() {
                System.out.println("In the top of the method minor()"); // b4689395.LINE_NUMBER
                System.out.println("A breakpoint is here.");
                System.out.println("In the bottom of the method minor().");
                System.out.println(ERROR_MESSAGE);
        }
}
