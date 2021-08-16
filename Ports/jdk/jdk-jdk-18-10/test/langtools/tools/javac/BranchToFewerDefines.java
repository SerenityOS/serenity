/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8067429
 * @summary java.lang.VerifyError: Inconsistent stackmap frames at branch target
 * @author srikanth
 *
 * @compile  BranchToFewerDefines.java
 * @run main BranchToFewerDefines
 */

public class BranchToFewerDefines {
        public static void main(String[] args) {
        }
        private void problematicMethod(int p) {
                switch (p) {
                        case 3:
                                long n;
                                while (true) {
                                        if (false) {
                                                break;
                                        }
                                }
                                break;
                        case 2:
                                loop: while (true) {
                                        while (true) {
                                                int i = 4;
                                                if (p != 16) {
                                                        return;
                                                }
                                                break loop;
                                        }
                                }
                                break;
                        default:
                                while (true) {
                                        if (false) {
                                                break;
                                        }
                                }
                                break;
                }
                long b;
                if (p != 7) {
                        switch (p) {
                                case 1:
                                        long a = 17;
                                        break;
                                case 2:
                                        break;
                                default:
                                        break;
                        }
                }
        }
        private void problematicMethod2(int p) {
                switch (p) {
                        case 3:
                                long n;
                                {
                                        int i = 4;
                                        break;
                                }
                        case 2:
                                {
                                        int i = 4;
                                        break;
                                }
                        default:
                                {
                                        int i = 4;
                                        break;
                                }
                }
                long b;
                if (p != 7) {
                        switch (p) {
                                case 1:
                                        long a = 17;
                                        break;
                                case 2:
                                        break;
                                default:
                                        break;
                        }
                }
        }
}
