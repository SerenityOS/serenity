/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8146416
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI -Xbatch -Xmx100m
 *      -XX:CompileCommand=exclude,compiler.uncommontrap.DeoptReallocFailure::main
 *      compiler.uncommontrap.DeoptReallocFailure
 *
 */

package compiler.uncommontrap;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

public class DeoptReallocFailure {
    static class MemoryChunk {
        MemoryChunk other;
        Object[][] array;

        MemoryChunk(MemoryChunk other) {
            this.other = other;
            array = new Object[1024 * 256][];
        }
    }

    static class NoEscape {
        long f1;
    }

    static MemoryChunk root;
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static synchronized long  test() {
        NoEscape[] noEscape = new NoEscape[45];
        noEscape[0] = new NoEscape();
        for (int i=0;i<1024*256;i++) {
           root.array[i]= new Object[45];
        }
        return noEscape[0].f1;
    }

    public static void main(String[] args) throws Throwable {

        //Exhaust Memory
        root = null;
        try {
            while (true) {
                root = new MemoryChunk(root);
            }
        } catch (OutOfMemoryError oom) {
        }

        if (root == null) {
          return;
        }

        try {
            NoEscape dummy = new NoEscape();
            Method m = DeoptReallocFailure.class.getMethod("test");
            WB.enqueueMethodForCompilation(m, 4);
            test();
        } catch (OutOfMemoryError oom) {
            root = null;
            oom.printStackTrace();
        }
        System.out.println("TEST PASSED");
    }
}
