/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6891750
 * @summary deopt blob kills values in O5
 * @run main/othervm compiler.runtime.cr6891750.Test6891750
 */

package compiler.runtime.cr6891750;

abstract class Base6891750 extends Thread {
    abstract public long m();
}
class Other6891750 extends Base6891750 {
    public long m() {
        return 0;
    }
}

public class Test6891750 extends Base6891750 {
    Base6891750 d;
    volatile long  value = 9;

    static int limit = 400000;

    Test6891750() {
        d = this;

    }
    public long m() {
        return value;
    }

    public long test(boolean doit) {
        if (doit) {
            long total0 = 0;
            long total1 = 0;
            long total2 = 0;
            long total3 = 0;
            long total4 = 0;
            long total5 = 0;
            long total6 = 0;
            long total7 = 0;
            long total8 = 0;
            long total9 = 0;
            for (int i = 0; i < limit; i++) {
                total0 += d.m();
                total1 += d.m();
                total2 += d.m();
                total3 += d.m();
                total4 += d.m();
                total5 += d.m();
                total6 += d.m();
                total7 += d.m();
                total8 += d.m();
                total9 += d.m();
            }
            return total0 + total1 + total2 + total3 + total4 + total5 + total6 + total7 + total8 + total9;
        }
        return 0;
    }

    public void run() {
        long result = test(true);
        for (int i = 0; i < 300; i++) {
            long result2 = test(true);
            if (result != result2) {
                throw new InternalError(result + " != " + result2);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Test6891750 Test6891750 = new Test6891750();
        // warm it up
        for (int i = 0; i < 200000; i++) {
            Test6891750.test(false);
        }
        // set in off running
        Test6891750.start();
        Thread.sleep(2000);

        // Load a class to invalidate CHA
        new Other6891750();
    }
}
