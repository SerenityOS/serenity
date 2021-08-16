/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7179701
 * @summary MaxJavaStackTraceDepth of zero is not handled correctly/consistently in the VM
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc:open
 * @modules java.base/java.lang:open
 * @library /test/lib
 * @run driver TestMaxJavaStackTraceDepth runTest
 */

import java.lang.reflect.Field;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


public class TestMaxJavaStackTraceDepth {

  static final int maxDepth = 2010;

  // Inner class that throws a lot of exceptions
  static class Thrower {
    int count = 0;

    int getDepth(Throwable t) throws Exception {
      Field f = Throwable.class.getDeclaredField("depth");
      f.setAccessible(true); // it's private
      return f.getInt(t);
    }

    void callThrow() throws Exception {
      if (++count < maxDepth) {
        callThrow();
      } else {
        throw new RuntimeException("depth tested " + maxDepth);
      }
    }

    void testThrow() throws Exception {
      try {
        count = getDepth(new Throwable()); // count stack to this point.
        callThrow();
      } catch(Exception e) {
        e.getStackTrace();
        System.out.println(e.getMessage());
        int throwableDepth = getDepth(e);
        System.out.println("java.lang.RuntimeException, " + throwableDepth);
      }
    }
  }

  public static void main(String args[]) throws Exception {
    if (args.length > 0) {
      // Test values of MaxJavaStackTraceDepth
      int[] depths = {0, 20, 1024};
      for (int d : depths) {
        System.out.println("running test with -XX:MaxJavaStackTraceDepth=" + d);
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:stacktrace=info",
                                                                  "-XX:MaxJavaStackTraceDepth=" + d,
                                                                  "--add-opens",
                                                                  "java.base/java.lang=ALL-UNNAMED",
                                                                  "TestMaxJavaStackTraceDepth");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        if (d == 0) {
          // Should get all the elements in stack trace
          output.shouldContain("java.lang.RuntimeException, " + maxDepth);
        } else {
          output.shouldContain("java.lang.RuntimeException, " + d);
        }
        output.shouldHaveExitValue(0);
      }
    } else {
      // run the test
      Thrower t = new Thrower();
      t.testThrow();
    }
  }
}
