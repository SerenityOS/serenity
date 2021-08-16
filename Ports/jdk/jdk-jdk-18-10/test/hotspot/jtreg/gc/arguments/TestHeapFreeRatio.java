/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/*
 * @test TestHeapFreeRatio
 * @bug 8025661
 * @summary Test parsing of -Xminf and -Xmaxf
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestHeapFreeRatio
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestHeapFreeRatio {

  enum Validation {
    VALID,
    MIN_INVALID,
    MAX_INVALID,
    OUT_OF_RANGE,
    COMBINATION_INVALID
  }

  private static void testMinMaxFreeRatio(String min, String max, Validation type) throws Exception {
    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
        "-Xminf" + min,
        "-Xmaxf" + max,
        "-version");
    OutputAnalyzer output = new OutputAnalyzer(pb.start());

    switch (type) {
    case VALID:
      output.shouldNotContain("Error");
      output.shouldHaveExitValue(0);
      break;
    case MIN_INVALID:
      output.shouldContain("Bad min heap free percentage size: -Xminf" + min);
      output.shouldContain("Error");
      output.shouldHaveExitValue(1);
      break;
    case MAX_INVALID:
      output.shouldContain("Bad max heap free percentage size: -Xmaxf" + max);
      output.shouldContain("Error");
      output.shouldHaveExitValue(1);
      break;
    case OUT_OF_RANGE:
      output.shouldContain("outside the allowed range");
      output.shouldContain("Error");
      output.shouldHaveExitValue(1);
      break;
    case COMBINATION_INVALID:
      output.shouldContain("must be less than or equal to MaxHeapFreeRatio");
      output.shouldContain("Error");
      output.shouldHaveExitValue(1);
      break;
    default:
      throw new IllegalStateException("Must specify expected validation type");
    }

    System.out.println(output.getOutput());
  }

  public static void main(String args[]) throws Exception {
    testMinMaxFreeRatio( "0.1", "0.5", Validation.VALID);
    testMinMaxFreeRatio(  ".1",  ".5", Validation.VALID);
    testMinMaxFreeRatio( "0.5", "0.5", Validation.VALID);

    testMinMaxFreeRatio("=0.1", "0.5", Validation.MIN_INVALID);
    testMinMaxFreeRatio("0.1f", "0.5", Validation.MIN_INVALID);
    testMinMaxFreeRatio(
                     "INVALID", "0.5", Validation.MIN_INVALID);

    testMinMaxFreeRatio( "0.1", "0.5f", Validation.MAX_INVALID);
    testMinMaxFreeRatio( "0.1", "=0.5", Validation.MAX_INVALID);
    testMinMaxFreeRatio(
                     "0.1",  "INVALID", Validation.MAX_INVALID);

    testMinMaxFreeRatio("-0.1", "0.5", Validation.OUT_OF_RANGE);
    testMinMaxFreeRatio( "1.1", "0.5", Validation.OUT_OF_RANGE);
    testMinMaxFreeRatio(
                  "2147483647", "0.5", Validation.OUT_OF_RANGE);
    testMinMaxFreeRatio( "0.1", "-0.5", Validation.OUT_OF_RANGE);
    testMinMaxFreeRatio( "0.1",  "1.5", Validation.OUT_OF_RANGE);
    testMinMaxFreeRatio(
                   "0.1", "2147483647", Validation.OUT_OF_RANGE);

    testMinMaxFreeRatio( "0.5",  "0.1", Validation.COMBINATION_INVALID);
    testMinMaxFreeRatio(  ".5",  ".10", Validation.COMBINATION_INVALID);
    testMinMaxFreeRatio("0.12","0.100", Validation.COMBINATION_INVALID);
  }
}
