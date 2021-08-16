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
package vm.compiler.optimizations.stringconcat.implicit;

import nsk.share.StringGoldChecker;
import vm.compiler.share.CompilerTest;
import vm.compiler.share.CompilerTestLauncher;
import vm.compiler.share.Random;

public class Implicit01 {

    private static final String GOLDEN_HASH = "-1447526817";

    private static String staticS = "";
    private static Random random = new Random(11);

    private static String crop(String s) {
        int max = 100;
        if (s.length() > max) {
            return s.substring(5, 50);
        }
        return s;
    }

    private static String randomCut(String s, int size) {
        String res = new String();
        for (int i = 0; i < size; i++) {
            int r = random.nextInt(s.length());
            res += s.charAt(r);
        }
        return res;
    }

    public static void main(String[] args) {
        StringGoldChecker goldChecker = new StringGoldChecker(GOLDEN_HASH);
        goldChecker.print(CompilerTestLauncher.launch(test));
        goldChecker.check();
    }

    public static String randomString() {
        int t = random.nextInt(100);
        String pre = "" + t / 10;
        String post = "" + t + 'a' + '\u307E' + random.nextInt(20);
        staticS = crop(staticS + pre + post);
        int r = pre.hashCode() * post.hashCode() - random.nextInt(1000);
        return randomCut(pre + "" + r % 50 + "-" + r + "o" + "\u306B" + post + staticS, 10);
    }

    private static final CompilerTest<Integer> test = new CompilerTest<Integer>("Implicit01") {
        @Override
        public Integer execute(Random random) {
            String res = randomString() + "t\u306B";
            int t = 200;
            while (t > 0) {
                t -= random.nextInt(50);
                if (t > res.hashCode() % 100) {
                    res += randomString().hashCode() + randomString();
                } else {
                    res += randomString();
                }
                res = crop(res);
            }

            return (res).hashCode();
        }
    };
}
