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

public class ForWhile {

    public static void main(String[] args) {
        GoldChecker goldChecker = new GoldChecker("ForWhile");

        for(CompilerTest test: forwhileTests) {
            goldChecker.println(test + " = " + CompilerTestLauncher.launch(test));
        }

        goldChecker.check();
    }

    private final static int N = 1000;
    static int x0 = 232;
    static int x1 = 562;
    static int x2 = 526;
    static int x3 = 774;

    public static final List<CompilerTest<Integer>> forwhileTests = Arrays.asList(
        //internal while don't use for counter
        new CompilerTest<Integer>("forwhile1") {
            @Override
            public Integer execute(Random random) {
                int k = x0;
                int s = x1 + random.nextInt(1000);
                int j = random.nextInt(1000);
                for (int i = 0; i < x0; i++) {
                    j = x0;
                    while (j < N) {
                        j++;
                        if (x2 > x1) {
                            j += 4;
                            k += j;
                        }

                    }
                }
                return s + k;
            }
        },

        //while with loop condition on for counter + an condition on counter
        new CompilerTest<Integer>("forwhile2") {
            @Override
            public Integer execute(Random random) {
                int k = x0;
                int s = x1 + random.nextInt(1000);
                int j = x0;
                for (int i = 0; i < x0; i++) {
                    j = x0;
                    while (j < N + i) {
                        j++;
                        if (x2 > x1) {
                            j += 4;
                            k += j;
                        }

                    }
                }
                return s + k;
            }
        },

        //while with loop condition on for counter + additional condition on counter
        new CompilerTest<Integer>("forwhile3") {
            @Override
            public Integer execute(Random random) {
                int k = x0;
                int s = x1 + random.nextInt(1000);
                int j = x0;
                for (int i = 0; i < x0; i++) {
                    j = x0;
                    while (j < N + i) {
                        j++;
                        s++;
                        if (x2 > x1 + i) {
                            j += 4;
                            k += j;
                        }

                    }
                }
                return s + k;
            }
        },

        //two while with loop condition on for counter
        new CompilerTest<Integer>("forwhile4") {
            @Override
            public Integer execute(Random random) {
                int k = x0;
                int s = x1 + random.nextInt(1000);
                int j = x2;
                for (int i = 0; i < x0; i++) {
                    while (j < x3 + i) {
                        j++;
                        if (x2 > i) {
                            k += x3;
                        } else {
                            break;
                        }
                        s++;
                    }
                    j = x0;
                    while (j < N + i) {
                        j++;
                        s++;
                        if (x2 > x1 + i) {
                            j += 4;
                            k += j;
                        }

                    }
                }
                return s + k;
            }
        }
    );
}
