/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6805724
 * @summary ModLNode::Ideal() generates functionally incorrect graph
 *          when divisor is any (2^k-1) constant.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6805724::fcomp
 *      compiler.c2.Test6805724
 */

package compiler.c2;

import jdk.test.lib.Utils;

public class Test6805724 implements Runnable {
    // Initialize DIVISOR so that it is final in this class.
    static final long DIVISOR;  // 2^k-1 constant

    static {
        long value = 0;
        try {
            value = Long.decode(System.getProperty("divisor"));
        } catch (Throwable t) {
            // This one is required for the Class.forName() in main.
        }
        DIVISOR = value;
    }

    static long fint(long x) {
        return x % DIVISOR;
    }

    static long fcomp(long x) {
        return x % DIVISOR;
    }

    public void run() {
        long a = 0x617981E1L;

        long expected = fint(a);
        long result = fcomp(a);

        if (result != expected)
            throw new InternalError(result + " != " + expected);
    }

    public static void main(String args[]) throws Exception {
        Class cl = Test6805724.class;
        ClassLoader apploader = cl.getClassLoader();

        // Iterate over all 2^k-1 divisors.
        for (int k = 1; k < Long.SIZE; k++) {
            long divisor = (1L << k) - 1;
            System.setProperty("divisor", "" + divisor);
            ClassLoader loader
                    = Utils.getTestClassPathURLClassLoader(apploader.getParent());
            Class c = loader.loadClass(Test6805724.class.getName());
            Runnable r = (Runnable) c.newInstance();
            r.run();
        }
    }
}
