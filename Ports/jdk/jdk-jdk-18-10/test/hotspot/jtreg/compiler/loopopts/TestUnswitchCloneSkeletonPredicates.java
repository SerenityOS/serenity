/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8253644
 * @summary Test the complete cloning of skeleton predicates to unswitched loops as done when cloning them to the main loop.
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.loopopts.TestUnswitchCloneSkeletonPredicates::*
 *                   compiler.loopopts.TestUnswitchCloneSkeletonPredicates
 * @run main/othervm -Xcomp -XX:+IgnoreUnrecognizedVMOptions -XX:-PartialPeelLoop -XX:CompileCommand=compileonly,compiler.loopopts.TestUnswitchCloneSkeletonPredicates::*
 *                   compiler.loopopts.TestUnswitchCloneSkeletonPredicates
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-PartialPeelLoop compiler.loopopts.TestUnswitchCloneSkeletonPredicates
 */
package compiler.loopopts;

public class TestUnswitchCloneSkeletonPredicates {

    static int x = 0;
    static int y = 20;
    static int intArr[] = new int[21000];
    static int idx = 0;
    static boolean bFld = true;
    static int iFld = 20;
    static int iFld2 = 0 ;
    static int iArrFld[] = new int[50];
    static float fArrFld[] = new float[50];


    // Only triggers with -XX:-PartialPeelLoop
    /*
     * The inner loop is unswitched on (1) which creates a fast and a slow loop that both have (1) removed and instead
     * (1) is executed before the loop at (3). With the SplitIf optimization we find that (3) dominates (2) in both loops.
     *
     * As a result, we can remove (2) from both loops. This, however, has an influence on how the loop tree is built.
     * Before the SplitIf optimization, the loop tree looks like this:
     * Loop: N0/N0  has_sfpt
     *   Loop: N338/N314  limit_check profile_predicated predicated counted [0,100),+1 (2 iters)  has_sfpt
     *     Loop: N459/N458  profile_predicated predicated counted [0,10000),+1 (5271 iters)  has_sfpt (slow loop)
     *     Loop: N343/N267  profile_predicated predicated counted [0,10000),+1 (5271 iters)  has_sfpt (fast loop)
     *
     * Both unswitched loop have a copy of the skeleton predicate If node that share the same Opaque4 node with its inputs.
     * The inner loop is never exited normally due to always returning on (4). This means that the branch that exits the
     * loop on the loop limit check is never taken and has an uncommon trap. Nevertheless, the loop building algorithm still
     * identifies the fast and the slow loop as children of N338 because of the condition (2) over which the loop is left.
     * However, after the above mentioned SplitIf optimization the condition (2) is removed from both loops. As a result,
     * the slow loops (N459) is always exited immediately (x == 100 holds) because the break is executed on the first
     * iteration of the loop. The loop can be removed (but these nodes are still part of the parent loop N338). The fast loop
     * (N343), however, is now never exited normally and always returns on the 9800th iteration over (4). The normal loop exit
     * over the loop limit check is never taken (uncommon trap). Due to the last loop exit (2) being removed, N343 is no longer
     * recognized as a child loop of N338 due to not having a backedge to the parent loop. The loop tree looks like this:
     * Loop: N0/N0  has_sfpt
     *   Loop: N338/N314  limit_check profile_predicated predicated counted [0,100),+1 (2 iters)  has_sfpt
     *   Loop: N343/N267  profile_predicated predicated counted [0,10000),+1 (5274 iters)  has_sfpt
     *
     * As a next step, the original parent loop N338 is peeled. The fast and the slow loop still both share skeleton Opaque4 bool
     * nodes with all its inputs nodes up to and including the OpaqueLoopInit/Stride nodes. These skeleton predicates are still there
     * even though the slow loop N459 could have been removed (the Opaque4 nodes are only removed after loop opts). Let's look at one
     * of the skeleton If nodes for the fast loop that uses such a Opaque4 node. The skeleton 'If' is no longer part of the original
     * parent loop and is therefore not peeled. But now we need some phi nodes to select the correct nodes either from the peeled
     * iteration or from N338 for this skeleton If of the fast loop. This is done in PhaseIdealLoop::clone_iff() which creates
     * a new Opaque4 node together with new Bool and Cmp nodes and then inserts some phi nodes to do the selection.
     *
     * When afterwards creating pre/main/post loops for the fast loop (N343) that is no child anymore, we find these phi nodes on the
     * path to the OpaqueLoopInit/Stride nodes which lets the assertion PhaseIdealLoop::skeleton_predicate_has_opaque() fail. These
     * phi nodes on the path to the OpaqueLoopInit/Stride nodes are unexpected.
     *
     * The solution to this problem is to clone the skeleton predicates completely, including clones of all nodes up to and including
     * the OpaqueLoopInit/Stride nodes (similar to what is done when copying skeleton predicates to the main loop) instead of just
     * sharing Opaque4 nodes.
     */
    public static int test1() {
      int i = 0;
      while (i < 100) {
          int j = 0;
          // (3) <new unswitch condition>
          while (j < 10000)  {
              if (x == 100) { // (1) Loop is unswitched on this condition -> condition shared with (2)
                  y = 34;
              }

              intArr[idx] = 34;
              intArr[2*j + 35] = 45;

              if (x == 100) { // (2)
                  y = 35;
                  break;
              }
              if (j == 9800) { // (4)
                  return 2;
              }
              j++;
          }
          i++;
          intArr[i] = 45;
      }
      return y;
    }

    // Only triggers with -XX:-PartialPeelLoop
    public static int test2() {
      int i = 0;
      while (i < 100) {
          int j = 0;
          while (j < 10000)  {
              if (x == 100) {
                  y = 34;
              }

              intArr[2*j + 35] = 45;

              if (x == 100) {
                  y = 35;
                  break;
              }
              if (j == 9800) {
                  return 2;
              }
              j++;
          }
          i++;
          intArr[i] = 45;
      }
      return y;
    }

    // Only triggers with -XX:-PartialPeelLoop
    public static int test3() {
      int i = 0;
      while (i < 100) {
          int j = 0;
          while (j < 10000)  {
              if (x == 100) {
                  y = 34;
              }

              intArr[idx] = 34;
              intArr[2*j + 35] = 45;

              if (x == 100) {
                  y = 35;
                  break;
              }
              if (j == 9800) {
                  return 2;
              }
              j++;
          }
          i++;
      }
      return y;
}

    // Test that has two loop headers for a single loop (limitation of type flow, see JDK-8255663)
    // which also triggers the assertion failure of this bug.
    public static void test4() {
        int unused = 500; // Required, even though unused
        boolean b = true;
        int i = 1;
        while (++i < 35) {
            iArrFld[i] = 6;
            switch (iFld2) {
            case 40:
                if (b) {
                    continue;
                }
                b = false;
                break;
            }
        }
    }

    // Test that has two loop headers for a single loop (limitation of type flow, see JDK-8255663)
    // which also triggers the assertion failure of this bug. Only triggers with -XX:-PartialPeelLoop.
    public static void test5() {
        int j = 50;
        int i = 1;
        while (++i < 40) {
            j = 5;
            do {
                fArrFld[i] = 46;
                iFld = 5;
                if (bFld) break;
            } while (++j < 5);
            j = 2;
            do {
                try {
                    iFld = 56;
                } catch (ArithmeticException a_e) {}
                if (bFld) break;
            } while (++j < 2);
        }
    }

    public static void main(String[] strArr) {
        for (int i = 0; i < 5000; i++) {
            test1();
            test2();
            test3();
            x++;
            x = x % 106;
        }
        test4();
        test5();
    }
}
