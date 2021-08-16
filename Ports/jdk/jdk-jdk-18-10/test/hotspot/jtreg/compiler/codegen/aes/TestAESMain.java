/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7184394
 * @key stress
 * @comment the test isn't marked w/ randomness as it uses predefined key b/c of keySize==128
 * @summary add intrinsics to use AES instructions
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CBC
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CBC -DencInputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CBC -DencOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CBC -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CBC -DencInputOffset=1 -DencOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CBC -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CBC -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1 -DpaddingStr=NoPadding -DmsgSize=640
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 *
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=ECB
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=ECB -DencInputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=ECB -DencOutputOffset=
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=ECB -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=ECB -DencInputOffset=1 -DencOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=ECB -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=ECB -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1 -DpaddingStr=NoPadding -DmsgSize=640
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 *
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=GCM
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=GCM -DencInputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=GCM -DencOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=GCM -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=GCM -DencInputOffset=1 -DencOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=GCM -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=GCM -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1 -DpaddingStr=NoPadding -DmsgSize=640
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 *
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CTR
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CTR -DencInputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CTR -DencOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CTR -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CTR -DencInputOffset=1 -DencOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CTR -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 * @run main/othervm/timeout=600 -Xbatch -DcheckOutput=true -Dmode=CTR -DencInputOffset=1 -DencOutputOffset=1 -DdecOutputOffset=1 -DpaddingStr=NoPadding -DmsgSize=640
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *      compiler.codegen.aes.TestAESMain
 *
 * @author Tom Deneau
 */

package compiler.codegen.aes;

import compiler.whitebox.CompilerWhiteBoxTest;
import sun.hotspot.code.Compiler;
import jtreg.SkippedException;

public class TestAESMain {
    public static void main(String[] args) {
        String mode = System.getProperty("mode", "CBC");
        if ((mode.equals("CBC") || mode.equals("ECB")) &&
            !Compiler.isIntrinsicAvailable(CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "com.sun.crypto.provider.AESCrypt", "implEncryptBlock", byte[].class, int.class, byte[].class, int.class)) {
            throw new SkippedException("AES intrinsic is not available");
        }
        if (mode.equals("GCM") &&
            !Compiler.isIntrinsicAvailable(CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "com.sun.crypto.provider.GHASH", "processBlocks", byte[].class, int.class, int.class, long[].class, long[].class)) {
            throw new SkippedException("GHASH intrinsic is not available");
        }
        if (mode.equals("CTR") &&
            !Compiler.isIntrinsicAvailable(CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "com.sun.crypto.provider.CounterMode", "implCrypt", byte[].class, int.class, int.class, byte[].class, int.class)) {
            throw new SkippedException("AES-CTR intrinsic is not available");
        }
        int iters = (args.length > 0 ? Integer.valueOf(args[0]) : 100000);
        int warmupIters = (args.length > 1 ? Integer.valueOf(args[1]) : 20000);
        System.out.println(iters + " iterations");
        TestAESEncode etest = new TestAESEncode();
        etest.prepare();
        // warm-up
        System.out.println("Starting encryption warm-up");
        for (int i = 0; i < warmupIters; i++) {
            etest.run();
        }
        System.out.println("Finished encryption warm-up");
        long start = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            etest.run();
        }
        long end = System.nanoTime();
        System.out.println("TestAESEncode runtime was " + (double) ((end - start) / 1000000.0) + " ms");

        TestAESDecode dtest = new TestAESDecode();
        dtest.prepare();
        // warm-up
        System.out.println("Starting decryption warm-up");
        for (int i = 0; i < warmupIters; i++) {
            dtest.run();
        }
        System.out.println("Finished decryption warm-up");
        start = System.nanoTime();
        for (int i = 0; i < iters; i++) {
            dtest.run();
        }
        end = System.nanoTime();
        System.out.println("TestAESDecode runtime was " + (double) ((end - start) / 1000000.0) + " ms");
    }
}
