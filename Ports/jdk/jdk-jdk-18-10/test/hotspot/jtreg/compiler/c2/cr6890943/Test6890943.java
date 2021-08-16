/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6890943
 * @summary JVM mysteriously gives wrong result on 64-bit 1.6 VMs in hotspot mode.
 *
 * @run main/othervm/timeout=240 compiler.c2.cr6890943.Test6890943
 */

package compiler.c2.cr6890943;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Test6890943 {
    public static final boolean AIR = true, ROCK = false;
    private static final Path PATH = Paths.get(System.getProperty("test.src", "."));
    private static final Path INPUT_FILE = PATH.resolve("input6890943.txt");
    private static final Path GOLDEN_FILE = PATH.resolve("output6890943.txt");

    public static void main(String[] args) {
        new Test6890943().go();
    }

    int r, c, f, t;
    boolean[][] grid;

    public void go() {
        Scanner in, golden;
        try {
            in = new Scanner(new FileInputStream(INPUT_FILE.toFile()));
            golden = new Scanner(new FileInputStream(GOLDEN_FILE.toFile()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("TEST failure: can't open test file", e);
        }
        in.useDelimiter("\\s+");
        golden.useDelimiter("\\s+");

        int T = in.nextInt();
        for (t = 0; t < T; t++) {
            r = in.nextInt();
            c = in.nextInt();
            f = in.nextInt();
            grid = new boolean[r][c];
            for (int x = 0; x < r; x++) {
                String line = in.next();
                for (int y = 0; y < c; y++) {
                    grid[x][y] = line.charAt(y) == '.';
                }
            }
            int digs = solve();
            String result = "Case #" + (t + 1) + ": " + (digs == -1 ? "No" : "Yes " + digs);
            System.out.println(result);
            // Compare with golden string from the file
            String goldenStr = golden.nextLine();
            if (!result.equals(goldenStr)) {
                System.err.println("FAIL: strings are not equal\n"
                        + "-- Result: " + result + "\n"
                        + "-- Golden: " + goldenStr);
                throw new RuntimeException("FAIL: Result string is not equal to the golden");
            }
        }
    }

    Map<Integer, Integer> M = new HashMap<Integer, Integer>();

    private int solve() {
        M = new HashMap<Integer, Integer>();
        M.put(calcWalkingRange(0, 0), 0);
        for (int digDown = 0; digDown < r; digDown++) {
            Map<Integer, Integer> tries = new HashMap<Integer, Integer>();
            for (Map.Entry<Integer, Integer> m : M.entrySet()) {
                int q = m.getKey();
                if (depth(q) != (digDown)) continue;
                if (stuck(q)) continue;
                tries.put(q, m.getValue());
            }

            for (Map.Entry<Integer, Integer> m : tries.entrySet()) {
                int q = m.getKey();
                int fallLeftDelta = 0, fallRightDelta = 0;
                //fall left
                int fallLeft = fall(digDown, start(q));
                if (fallLeft > 0) {
                    fallLeftDelta = 1;
                    if (fallLeft <= f) addToM(calcWalkingRange(digDown + fallLeft, start(q)), m.getValue());
                }

                //fall right
                int fallRight = fall(digDown, end(q));
                if (fallRight > 0) {
                    fallRightDelta = 1;

                    if (fallRight <= f) addToM(calcWalkingRange(digDown + fallRight, end(q)), m.getValue());
                }

                for (int p = start(q) + fallLeftDelta; p <= end(q) - fallRightDelta; p++) {
                    //goLeft
                    for (int digSpot = p; digSpot > start(q) + fallLeftDelta; digSpot--) {
                        int fallDown = 1 + fall(digDown + 1, digSpot);
                        if (fallDown <= f) {
                            if (fallDown == 1) {
                                addToM(calcWalkingRange(digDown + 1, digSpot, digSpot, p),
                                        m.getValue() + Math.abs(digSpot - p) + 1);
                            } else {
                                addToM(calcWalkingRange(digDown + fallDown, digSpot),
                                        m.getValue() + Math.abs(digSpot - p) + 1);
                            }
                        }
                    }

                    //goRight
                    for (int digSpot = p; digSpot < end(q) - fallRightDelta; digSpot++) {
                        int fallDown = 1 + fall(digDown + 1, digSpot);
                        if (fallDown <= f) {
                            if (fallDown == 1) {
                                addToM(calcWalkingRange(digDown + 1, digSpot, p, digSpot),
                                        m.getValue() + Math.abs(digSpot - p) + 1);
                            } else {
                                addToM(calcWalkingRange(digDown + fallDown, digSpot),
                                        m.getValue() + Math.abs(digSpot - p) + 1);
                            }
                        }
                    }
                }
            }
        }

        int result = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> m : M.entrySet()) {
            if (depth(m.getKey()) == r - 1) result = Math.min(m.getValue(), result);
        }

        if (result == Integer.MAX_VALUE) return -1;
        return result;
    }

    private void addToM(int q, int i) {
        Integer original = M.get(q);
        if (original == null) M.put(q, i);
        else M.put(q, Math.min(original, i));
    }

    private int fall(int row, int column) {
        int res = 0;
        for (int p = row + 1; p < r; p++) {
            if (grid[p][column] == AIR) res++;
            else break;
        }
        return res;
    }

    private boolean stuck(int q) {
        return start(q) == end(q);
    }

    private int depth(int q) {
        return q % 50;
    }

    private int start(int q) {
        return q / (50 * 50);
    }

    private int end(int q) {
        return (q / 50) % 50;
    }

    private int calcWalkingRange(int depth, int pos) {
        return calcWalkingRange(depth, pos, Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    private int calcWalkingRange(int depth, int pos, int airOverrideStart, int airOverrideEnd) {
        int left = pos, right = pos;
        if (depth >= r) return (c - 1) * 50 + depth;

        while (left > 0) {
            if (grid[depth][left - 1] == ROCK && (left - 1 < airOverrideStart || left - 1 > airOverrideEnd)) break;
            if (depth < r - 1 && grid[depth + 1][left - 1] == AIR) {
                left--;
                break;
            }
            left--;
        }
        while (right < c - 1) {
            if (grid[depth][right + 1] == ROCK && (right + 1 < airOverrideStart || right + 1 > airOverrideEnd)) break;
            if (depth < r - 1 && grid[depth + 1][right + 1] == AIR) {
                right++;
                break;
            }
            right++;
        }

        return left * 50 * 50 + right * 50 + depth;
    }
}
