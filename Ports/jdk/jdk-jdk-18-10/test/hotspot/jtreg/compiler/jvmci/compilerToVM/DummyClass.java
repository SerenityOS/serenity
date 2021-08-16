/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jvmci.compilerToVM;

import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

import java.util.Random;

class DummyClass extends DummyAbstractClass {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    int p1 = 5;
    int p2 = 6;

    public int dummyInstanceFunction() {
        String str1 = "123123123";
        double x = 3.14;
        int y = Integer.parseInt(str1);

        return y / (int) x;
    }

    public int dummyEmptyInstanceFunction() {
        return 42;
    }

    public static int dummyEmptyStaticFunction() {
        return -42;
    }

    @Override
    public int dummyAbstractFunction() {
        int z = p1 * p2;
        return (int) (Math.cos(p2 - p1 + z) * 100);
    }

    @Override
    public void dummyFunction() {
        dummyEmptyInstanceFunction();
    }

    public void withLoop() {
        long tier4 = (Long) WB.getVMFlag("Tier4BackEdgeThreshold");
        for (long i = 0; i < tier4; ++i) {
            randomProfile();
        }
    }

    private double randomProfile() {
        String str1 = "123123123";
        double x = 3.14;
        int y = Integer.parseInt(str1);

        Random rnd = Utils.getRandomInstance();
        if (rnd.nextDouble() > 0.2) {
            return y / (int) x;
        } else {
            return x / y;
        }
    }

}
