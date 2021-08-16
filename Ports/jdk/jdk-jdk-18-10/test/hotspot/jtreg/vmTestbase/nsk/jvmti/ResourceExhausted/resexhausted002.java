/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.ResourceExhausted;


import java.io.PrintStream;
import nsk.share.Consts;
import nsk.share.test.Stresser;
import jtreg.SkippedException;

public class resexhausted002 {
    static final long MAX_ITERATIONS = Long.MAX_VALUE;

    static class Node {
        long hair;
        Node next;
    }

    public static int run(String args[], PrintStream out) {

        Stresser stress = new Stresser(args);

        int count = 0;
        Helper.resetExhaustedEvent();

        out.println("Creating objects...");
        stress.start(MAX_ITERATIONS);
        try {
            Node list = null;

            while ( stress.iteration() ) {
                Node n = new Node();
                n.next = list;
                list = n;
                ++count;
            }

            System.out.println("Can't reproduce OOME due to a limit on iterations/execution time. Test was useless.");
            throw new SkippedException("Test did not get an OutOfMemory error");

        } catch (OutOfMemoryError e) {
        } finally {
            stress.finish();
        }

        System.gc();
        if (!Helper.checkResult(Helper.JVMTI_RESOURCE_EXHAUSTED_JAVA_HEAP, "creating " + count + " objects")) {
            return Consts.TEST_FAILED;
        }

        return Consts.TEST_PASSED;
    }

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        int result = run(args, System.out);
        System.out.println(result == Consts.TEST_PASSED ? "TEST PASSED" : "TEST FAILED");
        System.exit(result + Consts.JCK_STATUS_BASE);
    }
}
