/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.optimizations.partialpeel;

import nsk.share.GoldChecker;
import vm.compiler.share.CompilerTest;
import vm.compiler.share.CompilerTestLauncher;
import vm.compiler.share.Random;

import java.util.Arrays;
import java.util.List;

public class Do {


    public static void main(String[] args) {
        GoldChecker goldChecker = new GoldChecker("Do");

        for(CompilerTest test: doTests) {
            goldChecker.println(test + " = " + CompilerTestLauncher.launch(test));
        }

        goldChecker.check();
    }

    private final static int N = 1000;
    private final static int x0 = 232;
    private final static int x1 = 562;
    private final static int x2 = 526;
    private final static int x3 = 774;

    public static final List<CompilerTest<Integer>> doTests = Arrays.asList(
        new CompilerTest<Integer>("do1") {
            @Override
            public Integer execute(Random random) {
                int s = random.nextInt(1000);
                int i = 0;
                do {
                    if (s * i > x0) {
                        break;
                    }
                    s++;
                    i++;
                } while (i < N);
                return s + i;
            }
        },

        //do + break on sum of inductive vars
        new CompilerTest<Integer>("do2") {
            @Override
            public Integer execute(Random random) {
                int s = random.nextInt(1000);
                int i = 0;
                do {
                    if (s + i > x0) {
                        break;
                    }
                    s++;
                    i++;
                } while (i < N);
                return s + i;
            }
        },

        //do + break on shifted inductive vars
        new CompilerTest<Integer>("do3") {
            @Override
            public Integer execute(Random random) {
                int s = random.nextInt(1000);
                int i = 0;
                do {
                    if (x3 + s < x0) {
                        break;
                    }
                    s += i;
                    i++;
                } while (i < N);
                return s + i;
            }
        },

        //do + break on shifted inductive vars  + invariant condition
        new CompilerTest<Integer>("do4") {
            @Override
            public Integer execute(Random random) {
                int i = x0 + random.nextInt(1000);
                int j = x1;
                int k = x2;

                do {
                    if (x3 + k < x0) {
                        break;
                    }
                    i++;
                    k++;
                    if (x2 > x1) {
                        j += i;
                        k += j;
                    }

                } while (i < N);
                return k + i;
            }
        },

        //do + break on shifted inductive vars  + invariant condition
        new CompilerTest<Integer>("do5") {
            @Override
            public Integer execute(Random random) {
                int i = x0 + random.nextInt(1000);
                int j = x1;
                int k = x2;

                do {
                    if (k < x0) {
                        break;
                    }
                    i++;
                    if (x2 > x1) {
                        j += i;
                        k += j;
                    }

                } while (i < N);
                return k + i;
            }
        },

        //do + break on hidden inductive vars  + invariant condition
        new CompilerTest<Integer>("do6") {
            @Override
            public Integer execute(Random random) {
                int i = x0;
                int j = x1 + random.nextInt(1000);
                int k = x2;

                do {
                    if (k < x0) {
                        break;
                    }
                    i++;
                    k++;
                    if (k > x1) {
                        j += i;
                        k += j + i;
                    }

                } while (i < N);
                return k + i;
            }
        }

    );
}
