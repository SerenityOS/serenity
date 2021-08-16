/*
 * Copyright 2014 Google, Inc.  All Rights Reserved.
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
 * @bug 8043354
 * @summary bcEscapeAnalyzer allocated_escapes not conservative enough
 *
 * @run main/othervm
 *      -XX:CompileCommand=compileonly,compiler.escapeAnalysis.TestAllocatedEscapesPtrComparison::visitAndPop
 *      compiler.escapeAnalysis.TestAllocatedEscapesPtrComparison
 * @author Chuck Rasbold rasbold@google.com
 */

package compiler.escapeAnalysis;

/*
 * Test always passes with -XX:-OptmimizePtrCompare
 */

import java.util.ArrayList;
import java.util.List;

public class TestAllocatedEscapesPtrComparison {

  static TestAllocatedEscapesPtrComparison dummy;

  class Marker {
  }

  List<Marker> markerList = new ArrayList<>();

  // Suppress compilation of this method, it must be processed
  // by the bytecode escape analyzer.

  // Make a new marker and put it on the List
  Marker getMarker() {
    // result escapes through markerList
    final Marker result = new Marker();
    markerList.add(result);
    return result;
  }

  void visit(int depth) {
    // Make a new marker
    getMarker();

    // Call visitAndPop every once in a while
    // Cap the depth of our recursive visits
    if (depth % 10 == 2) {
      visitAndPop(depth + 1);
    } else if (depth < 15) {
      visit(depth + 1);
    }
  }

   void visitAndPop(int depth) {
    // Random dummy allocation to force EscapeAnalysis to process this method
    dummy = new TestAllocatedEscapesPtrComparison();

    // Make a new marker
    Marker marker = getMarker();

    visit(depth + 1);

    // Walk and pop the marker list up to the current marker
    boolean found = false;
    for (int i = markerList.size() - 1; i >= 0; i--) {
      Marker removed = markerList.remove(i);

      // In the failure, EA mistakenly converts this comparison to false
      if (removed == marker) {
        found = true;
        break;
      }
    }

    if (!found) {
      throw new RuntimeException("test fails");
    }
  }


  public static void main(String args[]) {
    TestAllocatedEscapesPtrComparison tc = new TestAllocatedEscapesPtrComparison();

    // Warmup and run enough times
    for (int i = 0; i < 20000; i++) {
      tc.visit(0);
    }
  }
}
