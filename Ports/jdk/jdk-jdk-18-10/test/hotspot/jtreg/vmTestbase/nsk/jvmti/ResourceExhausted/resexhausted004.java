/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.ResourceExhausted;

import java.io.*;
import java.util.Random;
import jdk.test.lib.Utils;

import nsk.share.Consts;
import jtreg.SkippedException;

public class resexhausted004 {
    public static int run(String args[], PrintStream out) {

        Random selector = Utils.getRandomInstance();
        int r;

        for ( int i = 4 + selector.nextInt() & 3; i > 0; i-- ) {
            try {
                switch (selector.nextInt() % 3) {
                case 0:
                    r = resexhausted001.run(args, out);
                    break;
                case 1:
                    r = resexhausted002.run(args, out);
                    break;
                default:
                    r = resexhausted003.run(args, out);
                    break;
                }
                if (r != Consts.TEST_PASSED) {
                    return r;
                }
            } catch (SkippedException ex) {
                // it's ok
            }
       }

       return Consts.TEST_PASSED;
    }

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        int result = run(args, System.out);
        System.out.println(result == Consts.TEST_PASSED ? "TEST PASSED" : "TEST FAILED");
        System.exit(result + Consts.JCK_STATUS_BASE);
    }
}
