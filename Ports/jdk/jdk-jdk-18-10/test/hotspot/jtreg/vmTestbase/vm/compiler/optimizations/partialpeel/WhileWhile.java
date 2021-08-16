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

public class WhileWhile {


    public static void main(String[] args) {
        GoldChecker goldChecker = new GoldChecker("WhileWhile");

        for (CompilerTest test : whilewhileTests) {
            goldChecker.println(test + " = " + CompilerTestLauncher.launch(test));
        }

        goldChecker.check();
    }

    private final static int N = 1000;
    static int x0 = 232;
    static int x1 = 562;
    static int x2 = 526;
    static int x3 = 774;

    public static final List<CompilerTest<Integer>> whilewhileTests = Arrays.asList(

        //invariant condition
        new CompilerTest<Integer>("whilewhile1") {
            @Override
            public Integer execute(Random random) {
                int k = x0 + random.nextInt(1000);
                int i = k + x2;
                int s = x1;
                int j = x2;

                while (i < N + x3) {
                    i++;
                    while (j < N) {
                        j++;
                        s++;
                        if (x2 > x1) {
                            k += j;
                        }

                    }
                }

                return s + k;
            }
        },


        //inner while with condition on outer while counter
        new CompilerTest<Integer>("whilewhile2") {
            @Override
            public Integer execute(Random random) {
                int k = x0 + random.nextInt(1000);
                int i = k + x2;
                int s = x1;
                int j = x2;

                while (i < N + x3) {
                    i++;
                    while (j < N + i) {
                        j++;
                        s++;
                        if (x2 > x1) {
                            k += j;
                        }

                    }
                }
                return s + k;
            }
        },

        //inner while in if branch
        new CompilerTest<Integer>("whilewhile3") {
            @Override
            public Integer execute(Random random) {
                int k = x0 + random.nextInt(1000);
                int i = k + x2;
                int s = x1;
                int j = x2;

                while (i < N + x3) {
                    i++;
                    if (i > x2) {
                        while (j < N + i) {
                            j++;
                            s += k;
                            if (x2 > x1) {
                                k += j;
                            }
                        }
                    }
                }
                return s + k;
            }
        },

        //two inner while
        new CompilerTest<Integer>("whilewhile4") {
            @Override
            public Integer execute(Random random) {
                int k = x0 + random.nextInt(1000);
                int i = k + x2;
                int s = x1;
                int j = x2;

                while (i < N + x3) {
                    i++;
                    s++;
                    if (i > x2) {
                        while (j < N + i) {
                            j++;
                            s++;
                            if (x2 > x1) {
                                k += j;
                            }
                        }
                    }

                    j = x2;
                    while (j < x2 + x3) {
                        j++;
                        if (x2 > x1) {
                            j += x0;
                            s++;
                            k += j;
                        }
                    }
                }
                return s + k;
            }
        }
    );

}
