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
package vm.compiler.optimizations.stringconcat.explicit;

import nsk.share.StringGoldChecker;
import vm.compiler.share.CompilerTest;
import vm.compiler.share.CompilerTestLauncher;
import vm.compiler.share.Random;

public class Explicit01 {

    private static final String GOLDEN_HASH = "-914118083";

    private static StringBuilder staticSB = new StringBuilder();
    private static Random random = new Random(11);
    private static final String PRE  = "pre\u307E\u3048\u306B";
    private static final String POST = "post\u3042\u3068\u3067";

    private static void crop(StringBuilder sb) {
        int max = 100;
        if (sb.length() > max) {
            sb.delete(0, 20);
            sb.delete(60, sb.length());
        }
    }

    private static String randomCut(String s, int size) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int r = random.nextInt(s.length());
            res.append(s.charAt(r));
        }
        return res.toString();
    }

    public static void main(String[] args) {
        StringGoldChecker goldChecker = new StringGoldChecker(GOLDEN_HASH);
        goldChecker.print(CompilerTestLauncher.launch(test));
        goldChecker.check();
    }

    public static String randomString() {
        int p = random.nextInt(100);
        StringBuilder prefix = new StringBuilder();
        prefix.append(p);
        prefix.append(PRE);
        prefix.append(random.nextInt(1000));
        StringBuilder postfix = new StringBuilder();
        postfix.append(POST);
        postfix.append(p);
        if (p > 50) {
            postfix.append(p);
        } else {
            postfix.append(p + 1);
        }
        postfix.append(random.nextInt(42));
        staticSB.append(prefix.toString());
        crop(staticSB);
        postfix.append(staticSB.toString());
        String s = prefix.toString() + postfix.toString();

        return randomCut(s, 10);
    }

    private static final CompilerTest<Integer> test = new CompilerTest<Integer>("Explicit01") {
        @Override
        public Integer execute(Random random) {
            StringBuilder res = new StringBuilder();
            int s = 200;
            while (s > 0) {
                s -= random.nextInt(50);
                int p = random.nextInt(1000);
                if (p > s) {
                    res.append(s);
                } else {
                    res.append(p);
                }
                crop(res);
                res.append(s + "x\u306B");
                res.append(randomString());
            }

            return res.toString().hashCode();
        }
    };


}
