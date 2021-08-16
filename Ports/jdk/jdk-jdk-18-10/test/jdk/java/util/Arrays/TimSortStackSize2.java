/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072909
 * @summary Test TimSort stack size on big arrays
 * @library /test/lib
 * @modules java.management
 * @requires (vm.debug == false)
 * @build TimSortStackSize2
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI TimSortStackSize2
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

public class TimSortStackSize2 {

    public static void main(String[] args) {
        if ( args == null || args.length == 0 ){
            startMeWithArgs();
        } else {
            doTestOfTwoTimSorts(Integer.parseInt(args[0]));
        }
    }

    private static void startMeWithArgs(){
        /*
         * big tests not for regular execution on all platforms:
         * run main/othervm -Xmx8g TimSortStackSize2 1073741824
         * run main/othervm -Xmx16g TimSortStackSize2 2147483644
         */
        try {
            Boolean compressedOops = WhiteBox.getWhiteBox()
                                             .getBooleanVMFlag("UseCompressedOops");
            long memory = (compressedOops == null || compressedOops) ? 385 : 770;
            final String xmsValue = "-Xms" +     memory + "m";
            final String xmxValue = "-Xmx" + 2 * memory + "m";

            System.out.printf("compressedOops: %s; Test will be started with \"%s %s\"%n",
                              compressedOops, xmsValue, xmxValue);
            OutputAnalyzer output = ProcessTools.executeTestJava(xmsValue,
                                                                 xmxValue,
                                                                 "TimSortStackSize2",
                                                                 "67108864");
            System.out.println(output.getOutput());
            output.shouldHaveExitValue(0);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void doTestOfTwoTimSorts(final int lengthOfTest){
        boolean passed = doTest("TimSort", lengthOfTest,
            (Integer [] a) -> Arrays.sort(a));
        passed = doTest("ComparableTimSort", lengthOfTest, (Integer [] a) ->
            Arrays.sort(a, (Object first, Object second) -> {
                return ((Comparable<Object>)first).compareTo(second);
            }))
            && passed;
        if ( !passed ){
            throw new RuntimeException();
        }
    }

    private static boolean doTest(final String msg, final int lengthOfTest,
                                  final  Consumer<Integer[]> c){
        Integer [] a = null;
        try {
            a = new TimSortStackSize2(lengthOfTest).createArray();
            long begin = System.nanoTime();
            c.accept(a);
            long end = System.nanoTime();
            System.out.println(msg + " OK. Time: " + (end - begin) + "ns");
        } catch (ArrayIndexOutOfBoundsException e){
            System.out.println(msg + " broken:");
            e.printStackTrace();
            return false;
        } finally {
            a = null;
        }
        return true;
    }

    private static final int MIN_MERGE = 32;
    private final int minRun;
    private final int length;
    private final List<Long> runs = new ArrayList<Long>();

    public TimSortStackSize2(final int len) {
        this.length = len;
        minRun = minRunLength(len);
        fillRunsJDKWorstCase();
    }

    private static int minRunLength(int n) {
        assert n >= 0;
        int r = 0;      // Becomes 1 if any 1 bits are shifted off
        while (n >= MIN_MERGE) {
            r |= (n & 1);
            n >>= 1;
        }
        return n + r;
    }

    /**
     * Adds a sequence x_1, ..., x_n of run lengths to <code>runs</code> such that:<br>
     * 1. X = x_1 + ... + x_n <br>
     * 2. x_j >= minRun for all j <br>
     * 3. x_1 + ... + x_{j-2}  <  x_j  <  x_1 + ... + x_{j-1} for all j <br>
     * These conditions guarantee that TimSort merges all x_j's one by one
     * (resulting in X) using only merges on the second-to-last element.
     * @param X  The sum of the sequence that should be added to runs.
     */
    private void generateJDKWrongElem(long X) {
        for(long newTotal; X >= 2 * minRun + 1; X = newTotal) {
            //Default strategy
            newTotal = X / 2 + 1;
            //Specialized strategies
            if(3 * minRun + 3 <= X && X <= 4*minRun+1) {
                // add x_1=MIN+1, x_2=MIN, x_3=X-newTotal  to runs
                newTotal = 2 * minRun + 1;
            } else if (5 * minRun + 5 <= X && X <= 6 * minRun + 5) {
                // add x_1=MIN+1, x_2=MIN, x_3=MIN+2, x_4=X-newTotal  to runs
                newTotal = 3 * minRun + 3;
            } else if (8 * minRun + 9 <= X && X <= 10 * minRun + 9) {
                // add x_1=MIN+1, x_2=MIN, x_3=MIN+2, x_4=2MIN+2, x_5=X-newTotal  to runs
                newTotal = 5 * minRun + 5;
            } else if (13 * minRun + 15 <= X && X <= 16 * minRun + 17) {
                // add x_1=MIN+1, x_2=MIN, x_3=MIN+2, x_4=2MIN+2, x_5=3MIN+4, x_6=X-newTotal  to runs
                newTotal = 8 * minRun + 9;
            }
            runs.add(0, X - newTotal);
        }
        runs.add(0, X);
    }

    /**
     * Fills <code>runs</code> with a sequence of run lengths of the form<br>
     * Y_n     x_{n,1}   x_{n,2}   ... x_{n,l_n} <br>
     * Y_{n-1} x_{n-1,1} x_{n-1,2} ... x_{n-1,l_{n-1}} <br>
     * ... <br>
     * Y_1     x_{1,1}   x_{1,2}   ... x_{1,l_1}<br>
     * The Y_i's are chosen to satisfy the invariant throughout execution,
     * but the x_{i,j}'s are merged (by <code>TimSort.mergeCollapse</code>)
     * into an X_i that violates the invariant.
     * X is the sum of all run lengths that will be added to <code>runs</code>.
     */
    private void fillRunsJDKWorstCase() {
        long runningTotal = 0;
        long Y = minRun + 4;
        long X = minRun;

        while (runningTotal + Y + X <= length) {
            runningTotal += X + Y;
            generateJDKWrongElem(X);
            runs.add(0, Y);

            // X_{i+1} = Y_i + x_{i,1} + 1, since runs.get(1) = x_{i,1}
            X = Y + runs.get(1) + 1;

            // Y_{i+1} = X_{i+1} + Y_i + 1
            Y += X + 1;
        }

        if (runningTotal + X <= length) {
            runningTotal += X;
            generateJDKWrongElem(X);
        }

        runs.add(length - runningTotal);
    }

    private Integer [] createArray() {
        Integer [] a = new Integer[length];
        Arrays.fill(a, 0);
        int endRun = -1;
        for (long len : runs) {
            a[endRun += len] = 1;
        }
        a[length - 1] = 0;
        return a;
    }

}
