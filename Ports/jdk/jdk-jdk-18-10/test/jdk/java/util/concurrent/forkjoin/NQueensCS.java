/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 6865571
 * @summary Solve NQueens using fork/join
 * @run main NQueensCS maxBoardSize=11 reps=1
 * @run main NQueensCS maxBoardSize=11 reps=1 procs=8
 */

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class NQueensCS extends RecursiveAction {

    static long lastStealCount;
    static int boardSize;

    static final int[] expectedSolutions = new int[] {
        0, 1, 0, 0, 2, 10, 4, 40, 92, 352, 724, 2680, 14200,
        73712, 365596, 2279184, 14772512, 95815104, 666090624
    }; // see http://www.durangobill.com/N_Queens.html

    static String keywordValue(String[] args, String keyword) {
        for (String arg : args)
            if (arg.startsWith(keyword))
                return arg.substring(keyword.length() + 1);
        return null;
    }

    static int intArg(String[] args, String keyword, int defaultValue) {
        String val = keywordValue(args, keyword);
        return (val == null) ? defaultValue : Integer.parseInt(val);
    }

    /** for time conversion */
    static final long NPS = (1000L * 1000L * 1000L);

    /**
     * Usage: NQueensCS [minBoardSize=N] [maxBoardSize=N] [procs=N] [reps=N]
     */
    public static void main(String[] args) throws Exception {
        // Board sizes too small: hard to measure well.
        // Board sizes too large: take too long to run.
        final int minBoardSize = intArg(args, "minBoardSize",  8);
        final int maxBoardSize = intArg(args, "maxBoardSize", 15);

        final int procs = intArg(args, "procs", 0);

        for (int reps = intArg(args, "reps", 10); reps > 0; reps--) {
            ForkJoinPool g = (procs == 0) ?
                new ForkJoinPool() :
                new ForkJoinPool(procs);
            lastStealCount = g.getStealCount();
            for (int i = minBoardSize; i <= maxBoardSize; i++)
                test(g, i);
            System.out.println(g);
            g.shutdown();
        }
    }

    static void test(ForkJoinPool g, int i) throws Exception {
        boardSize = i;
        int ps = g.getParallelism();
        long start = System.nanoTime();
        NQueensCS task = new NQueensCS(new int[0]);
        g.invoke(task);
        int solutions = task.solutions;
        long time = System.nanoTime() - start;
        double secs = (double) time / NPS;
        if (solutions != expectedSolutions[i])
            throw new Error();
        System.out.printf("NQueensCS %3d", i);
        System.out.printf(" Time: %7.3f", secs);
        long sc = g.getStealCount();
        long ns = sc - lastStealCount;
        lastStealCount = sc;
        System.out.printf(" Steals/t: %5d", ns/ps);
        System.out.println();
    }

    // Boards are represented as arrays where each cell
    // holds the column number of the queen in that row

    final int[] sofar;
    NQueensCS nextSubtask; // to link subtasks
    int solutions;
    NQueensCS(int[] a) {
        this.sofar = a;
    }

    public final void compute() {
        NQueensCS subtasks;
        int bs = boardSize;
        if (sofar.length >= bs)
            solutions = 1;
        else if ((subtasks = explore(sofar, bs)) != null)
            solutions = processSubtasks(subtasks);
    }

    private static NQueensCS explore(int[] array, int bs) {
        int row = array.length;
        NQueensCS s = null; // subtask list
        outer:
        for (int q = 0; q < bs; ++q) {
            for (int i = 0; i < row; i++) {
                int p = array[i];
                if (q == p || q == p - (row - i) || q == p + (row - i))
                    continue outer; // attacked
            }
            NQueensCS first = s; // lag forks to ensure 1 kept
            if (first != null)
                first.fork();
            int[] next = Arrays.copyOf(array, row+1);
            next[row] = q;
            NQueensCS subtask = new NQueensCS(next);
            subtask.nextSubtask = first;
            s = subtask;
        }
        return s;
    }

    private static int processSubtasks(NQueensCS s) {
        // Always run first the task held instead of forked
        s.compute();
        int ns = s.solutions;
        s = s.nextSubtask;
        // Then the unstolen ones
        while (s != null && s.tryUnfork()) {
            s.compute();
            ns += s.solutions;
            s = s.nextSubtask;
        }
        // Then wait for the stolen ones
        while (s != null) {
            s.join();
            ns += s.solutions;
            s = s.nextSubtask;
        }
        return ns;
    }
}
