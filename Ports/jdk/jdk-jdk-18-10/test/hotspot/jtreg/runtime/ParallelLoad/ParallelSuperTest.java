/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test MyLoaderTest
 * @bug 8262046
 * @summary Call handle_parallel_super_load, loading parallel threads that throw CCE
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 * @library /test/lib
 * @compile -XDignore.symbol.file AsmClasses.java
 * @compile test-classes/ClassInLoader.java test-classes/A.java test-classes/B.java
 * @run main/othervm ParallelSuperTest
 * @run main/othervm ParallelSuperTest -parallel
 * @run main/othervm ParallelSuperTest -parallel -parallelCapable
 */

public class ParallelSuperTest {
    public static void main(java.lang.String[] args) throws Exception {
        boolean parallel = false;
        boolean parallelCapable = false;
        boolean success = true;
        for (int i = 0; i < args.length; i++) {
            try {
                // Don't print debug info
                if (args[i].equals("-parallel")) {
                    parallel = true;
                } else if (args[i].equals("-parallelCapable")) {
                    parallelCapable = true;
                } else {
                    System.out.println("Unrecognized " + args[i]);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid parameter: " + args[i - 1] + " " + args[i]);
            }
        }
        // The -parallel -parallelCapable case will deadlock on locks for A and B if
        // the jvm doesn't eagerly try to load A's superclass from the second thread.
        // ie. needs to call SystemDictionary::handle_parallel_super_load
        if (parallelCapable) {
            MyLoader ldr = new MyLoader(parallel);
            ldr.startLoading();
            success = ldr.report_success();
        } else {
            MyNonParallelLoader ldr = new MyNonParallelLoader(parallel);
            ldr.startLoading();
            success = ldr.report_success();
        }
        if (success) {
            System.out.println("PASSED");
        } else {
            throw new RuntimeException("FAILED");
        }
    }
}
