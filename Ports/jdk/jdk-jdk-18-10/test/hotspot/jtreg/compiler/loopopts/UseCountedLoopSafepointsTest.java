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
 *
 */

/**
 * @test
 * @bug 6869327
 * @summary Test that C2 flag UseCountedLoopSafepoints ensures a safepoint is kept in a CountedLoop
 * @library /test/lib /
 * @requires vm.compMode != "Xint" & vm.flavor == "server" & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4) & vm.debug == true
 * @requires !vm.emulatedClient & !vm.graal.enabled
 * @modules java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver compiler.loopopts.UseCountedLoopSafepointsTest
 */

package compiler.loopopts;

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import jdk.test.lib.Asserts;

/* Idea of this test is to check if ideal graph has CountedLoopEnd->SafePoint edge in case
   of UseCountedLoopSafepoint enabled and has no such edge in case it's disabled. Restricting
   compilation to testMethod only will leave only one counted loop (the one in testedMethod) */
public class UseCountedLoopSafepointsTest {

    public static void main (String args[]) {
        check(true); // check ideal graph with UseCountedLoopSafepoint enabled
        check(false); // ... and disabled
    }

    private static void check(boolean enabled) {
        OutputAnalyzer oa;
        try {
            oa = ProcessTools.executeTestJvm("-XX:+UnlockDiagnosticVMOptions", "-Xbootclasspath/a:.",
                                             "-XX:" + (enabled ? "+" : "-") + "UseCountedLoopSafepoints",
                                             "-XX:+WhiteBoxAPI",
                    "-XX:-Inline", "-Xbatch", "-XX:+PrintIdeal", "-XX:LoopUnrollLimit=0",
                    "-XX:CompileOnly=" + UseCountedLoopSafepoints.class.getName() + "::testMethod",
                    UseCountedLoopSafepoints.class.getName());
        } catch (Exception e) {
            throw new Error("Exception launching child for case enabled=" + enabled + " : " + e, e);
        }
        oa.shouldHaveExitValue(0);
        // parse output in seach of SafePoint and CountedLoopEnd nodes
        List<Node> safePoints = new ArrayList<>();
        List<Node> loopEnds = new ArrayList<>();
        for (String line : oa.getOutput().split("\\n")) {
            int separatorIndex = line.indexOf("  ===");
            if (separatorIndex > -1) {
                String header = line.substring(0, separatorIndex);
                if (header.endsWith("SafePoint")) {
                    safePoints.add(new Node("SafePoint", line));
                } else if (header.endsWith("CountedLoopEnd")) {
                    loopEnds.add(new Node("CountedLoopEnd", line));
                }
            }
        }
        // now, find CountedLoopEnd -> SafePoint edge
        boolean found = false;
        for (Node loopEnd : loopEnds) {
            found |= loopEnd.to.stream()
                                 .filter(id -> nodeListHasElementWithId(safePoints, id))
                                 .findAny()
                                 .isPresent();
        }
        Asserts.assertEQ(enabled, found, "Safepoint " + (found ? "" : "not ") + "found");
    }

    private static boolean nodeListHasElementWithId(List<Node> list, int id) {
        return list.stream()
                   .filter(node -> node.id == id)
                   .findAny()
                   .isPresent();
    }

    private static class Node {
        public final int id;
        public final List<Integer> from;
        public final List<Integer> to;

        public Node(String name, String str) {
            List<Integer> tmpFrom = new ArrayList<>();
            List<Integer> tmpTo = new ArrayList<>();
            // parse string like: " $id    $name       ===  $to1 $to2 ...   [[ $from1 $from2 ... ]] $anything"
            // example:  318    SafePoint  ===  317  1  304  1  1  10  308  [[ 97  74 ]]  ...
            id = Integer.parseInt(str.substring(1, str.indexOf(name)).trim());
            Arrays.stream(str.substring(str.indexOf("===") + 4, str.indexOf("[[")).trim().split("\\s+"))
                  .map(Integer::parseInt)
                  .forEach(tmpTo::add);
            Arrays.stream(str.substring(str.indexOf("[[") + 3, str.indexOf("]]")).trim().split("\\s+"))
                  .map(Integer::parseInt)
                  .forEach(tmpFrom::add);
            this.from = Collections.unmodifiableList(tmpFrom);
            this.to = Collections.unmodifiableList(tmpTo);
        }
    }
}
