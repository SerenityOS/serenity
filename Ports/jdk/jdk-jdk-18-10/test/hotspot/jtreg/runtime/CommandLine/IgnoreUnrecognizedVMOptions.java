/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;

/*
 * @test
 * @bug 8129855
 * @summary -XX:+IgnoreUnrecognizedVMOptions should work according to the spec from JDK-8129855
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver IgnoreUnrecognizedVMOptions
 */
public class IgnoreUnrecognizedVMOptions {

  private static void runJavaAndCheckExitValue(boolean shouldSucceed, String... args) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    if (shouldSucceed) {
      output.shouldHaveExitValue(0);
    } else {
      output.shouldHaveExitValue(1);
    }
  }

  public static void main(String[] args) throws Exception {
    boolean product = !Platform.isDebugBuild();

    /*
      #1.1 wrong value and non-existing flag:
                                    exists, invalid value           does not exist
                                    -XX:MinHeapFreeRatio=notnum     -XX:THIS_FLAG_DOESNT_EXIST
      -IgnoreUnrecognizedVMOptions               ERR                           ERR
      +IgnoreUnrecognizedVMOptions               ERR                           OK
    */
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:MinHeapFreeRatio=notnum", "-version");
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:THIS_FLAG_DOESNT_EXIST", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:MinHeapFreeRatio=notnum", "-version");
    runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:THIS_FLAG_DOESNT_EXIST", "-version");

    /*
      #1.2 normal flag with ranges:
                                      exists, in range                exists, out of range
                                      -XX:StackRedPages=1             -XX:StackRedPages=0
      -IgnoreUnrecognizedVMOptions               OK                            ERR
      +IgnoreUnrecognizedVMOptions               OK                            ERR
    */
    runJavaAndCheckExitValue(true, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:StackRedPages=1", "-version");
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:StackRedPages=0", "-version");
    runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:StackRedPages=1", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:StackRedPages=0", "-version");

    /*
      #1.3 develop & notproduct flag on debug VM:
                                      develop & !product_build        notproduct & !product_build
                                      -XX:+DeoptimizeALot             -XX:+VerifyCodeCache
      -IgnoreUnrecognizedVMOptions               OK                            OK
      +IgnoreUnrecognizedVMOptions               OK                            OK
    */
    if (!product) {
      runJavaAndCheckExitValue(true, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:+DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(true, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:+VerifyCodeCache", "-version");
      runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+VerifyCodeCache", "-version");
    }

    /*
      #1.4 develop & notproduct flag on product VM:
                                    develop & !product_build           notproduct & product_build
                                    -XX:+DeoptimizeALot                -XX:+VerifyCodeCache
      -IgnoreUnrecognizedVMOptions               ERR                           ERR
      +IgnoreUnrecognizedVMOptions               OK                            OK
    */
    if (product) {
      runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:+DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:+VerifyCodeCache", "-version");
      runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+VerifyCodeCache", "-version");
    }


    /*
      #1.5 malformed develop & notproduct flag on debug VM:
                                  develop & !product_build             notproduct & !product_build
                                  -XX:DeoptimizeALot                   -XX:VerifyCodeCache
      -IgnoreUnrecognizedVMOptions               ERR                           ERR
      +IgnoreUnrecognizedVMOptions               ERR                           ERR
    */
    if (!product) {
      runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:VerifyCodeCache", "-version");
      runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:VerifyCodeCache", "-version");
    }

    /*
      #1.6 malformed develop & notproduct flag on product VM:
                                    develop & !product_build           notproduct & product_build
                                    -XX:DeoptimizeALot                 -XX:VerifyCodeCache
      -IgnoreUnrecognizedVMOptions               ERR                           ERR
      +IgnoreUnrecognizedVMOptions               OK                            OK
    */
    if (product) {
      runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:VerifyCodeCache", "-version");
      runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:DeoptimizeALot", "-version");
      runJavaAndCheckExitValue(true, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:VerifyCodeCache", "-version");
    }

    /*
      #1.7 locked flag:
                                      diagnostic & locked             experimental & locked
                                      -XX:-UnlockDiagnosticVMOptions  -XX:-UnlockExperimentalVMOptions
                                      -XX:+PrintInlining              -XX:+AlwaysSafeConstructors
      -IgnoreUnrecognizedVMOptions               ERR                           ERR
      +IgnoreUnrecognizedVMOptions               ERR                           ERR
    */
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:-UnlockDiagnosticVMOptions", "-XX:+PrintInlining", "-version");
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:-UnlockExperimentalVMOptions", "-XX:+AlwaysSafeConstructors", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:-UnlockDiagnosticVMOptions", "-XX:+PrintInlining", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:-UnlockExperimentalVMOptions", "-XX:+AlwaysSafeConstructors", "-version");

    /*
      #1.8 malformed locked flag:
                                    diagnostic & locked             experimental & locked
                                    -XX:-UnlockDiagnosticVMOptions  -XX:-UnlockExperimentalVMOptions
                                    -XX:PrintInlining               -XX:AlwaysSafeConstructors
      -IgnoreUnrecognizedVMOptions               ERR                           ERR
      +IgnoreUnrecognizedVMOptions               ERR                           ERR
    */
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:-UnlockDiagnosticVMOptions", "-XX:PrintInlining", "-version");
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:-UnlockExperimentalVMOptions", "-XX:AlwaysSafeConstructors", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:-UnlockDiagnosticVMOptions", "-XX:PrintInlining", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:-UnlockExperimentalVMOptions", "-XX:AlwaysSafeConstructors", "-version");

    /*
      #1.9 malformed unlocked flag:
                                    diagnostic & locked             experimental & locked
                                    -XX:+UnlockDiagnosticVMOptions  -XX:+UnlockExperimentalVMOptions
                                    -XX:PrintInlining               -XX:AlwaysSafeConstructors
      -IgnoreUnrecognizedVMOptions               ERR                           ERR
      +IgnoreUnrecognizedVMOptions               ERR                           ERR
    */
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:+UnlockDiagnosticVMOptions", "-XX:PrintInlining", "-version");
    runJavaAndCheckExitValue(false, "-XX:-IgnoreUnrecognizedVMOptions", "-XX:+UnlockExperimentalVMOptions", "-XX:AlwaysSafeConstructors", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+UnlockDiagnosticVMOptions", "-XX:PrintInlining", "-version");
    runJavaAndCheckExitValue(false, "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+UnlockExperimentalVMOptions", "-XX:AlwaysSafeConstructors", "-version");
  }
}
