/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestArrayAllocatorMallocLimit
 * @summary Sanity check that the ArrayAllocatorMallocLimit flag can be set.
 * The test helps verifying that size_t flags can be set/read.
 * @bug 8054823
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestArrayAllocatorMallocLimit
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.math.BigInteger;

public class TestArrayAllocatorMallocLimit {
  public static void main(String [] args) throws Exception {
    testDefaultValue();
    testSetValue();
  }

  private static final String flagName = "ArrayAllocatorMallocLimit";

  //     size_t ArrayAllocatorMallocLimit                 = 18446744073709551615{experimental}
  private static final String printFlagsFinalPattern = " *size_t *" + flagName + " *:?= *(\\d+) *\\{experimental\\} *";

  public static void testDefaultValue()  throws Exception {
    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
      "-XX:+UnlockExperimentalVMOptions", "-XX:+PrintFlagsFinal", "-version");

    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    String value = output.firstMatch(printFlagsFinalPattern, 1);

    try {
      Asserts.assertNotNull(value, "Couldn't find size_t flag " + flagName);

      // A size_t is not always parseable with Long.parseValue,
      // use BigInteger instead.
      BigInteger biValue = new BigInteger(value);

      // Sanity check that we got a non-zero value.
      Asserts.assertNotEquals(biValue, "0");

      output.shouldHaveExitValue(0);
    } catch (Exception e) {
      System.err.println(output.getOutput());
      throw e;
    }
  }

  public static void testSetValue() throws Exception {
    long flagValue = 2048;

    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
      "-XX:+UnlockExperimentalVMOptions", "-XX:" + flagName + "=" + flagValue, "-XX:+PrintFlagsFinal", "-version");

    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    String value = output.firstMatch(printFlagsFinalPattern, 1);

    try {
      Asserts.assertNotNull("Couldn't find size_t flag " + flagName);

      long longValue = Long.parseLong(value);

      Asserts.assertEquals(longValue, flagValue);

      output.shouldHaveExitValue(0);
    } catch (Exception e) {
      System.err.println(output.getOutput());
      throw e;
    }
  }

}
