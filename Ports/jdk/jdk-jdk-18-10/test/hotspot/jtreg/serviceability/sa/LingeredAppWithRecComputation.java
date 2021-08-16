/*
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

import jdk.test.lib.apps.LingeredApp;

public class LingeredAppWithRecComputation extends LingeredApp {

    public static final String THREAD_NAME = "LingeredAppWithRecComputation.factorial()";

    private long factorial(int n) {
        if (n <= 1) {
                return 1;
        }
        if (n == 2) {
                return 2;
        }
        return n * factorial(n - 1);
    }

    public void testLoop() {
        long result = 0;
        long[] lastNResults = new long[20];
        int i = 0;
        int j = 0;
        while (true) {
            result = factorial(i);
            lastNResults[j] = result;
            if (i % 12 == 0) {
                    i = -1; // reset i
            }
            if (j % 19 == 0) {
                    j = -1; // reset j
            }
            i++; j++;
        }
    }

    public static void main(String args[]) {
        LingeredAppWithRecComputation app = new LingeredAppWithRecComputation();
        Thread factorial = new Thread(() -> {
            app.testLoop();
        });
        factorial.setName(THREAD_NAME);
        factorial.start();
        LingeredApp.main(args);
    }
 }
