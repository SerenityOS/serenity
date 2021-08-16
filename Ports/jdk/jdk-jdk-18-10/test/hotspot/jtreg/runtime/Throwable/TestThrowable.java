/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8150778
 * @summary Test exception depths, and code to get stack traces
 * @modules java.base/jdk.internal.misc:open
 * @modules java.base/java.lang:open
 * @library /test/lib
 * @run main/othervm -XX:MaxJavaStackTraceDepth=1024 TestThrowable
 */

import java.lang.reflect.Field;
import jdk.test.lib.Asserts;

public class TestThrowable {

  // Inner class that throws a lot of exceptions
  static class Thrower {
    static int MaxJavaStackTraceDepth = 1024; // as above
    int[] depths = {10, 34, 100, 1023, 1024, 1025};
    int count = 0;

    int getDepth(Throwable t) throws Exception {
      Field f = Throwable.class.getDeclaredField("depth");
      f.setAccessible(true); // it's private
      return f.getInt(t);
    }

    void callThrow(int depth) {
      if (++count < depth) {
        callThrow(depth);
      } else {
        throw new RuntimeException("depth tested " + depth);
      }
    }
    void testThrow() throws Exception {
      for (int d : depths) {
        try {
          count = getDepth(new Throwable());
          callThrow(d);
        } catch(Exception e) {
          e.getStackTrace();
          System.out.println(e.getMessage());
          int throwableDepth = getDepth(e);
          Asserts.assertTrue(throwableDepth == d ||
                     (d > MaxJavaStackTraceDepth && throwableDepth == MaxJavaStackTraceDepth),
                     "depth should return the correct value: depth tested=" +
                     d + " throwableDepth=" + throwableDepth);
        }
      }
    }
  }

  public static void main(String... unused) throws Exception {
    Thrower t = new Thrower();
    t.testThrow();
  }
}
