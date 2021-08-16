/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8079613
 * @summary Ensure that compiler ascertains a class of patently non-poly expressions as such
 * @run main/timeout=10 DeeplyChainedNonPolyExpressionTest
 */

public class DeeplyChainedNonPolyExpressionTest {
    static class JSO {

        JSO put(String s, Object y) {
            return null;
        }

        JSO put(java.lang.String x, java.util.Collection<String> y) {
            return null;
        }

        JSO put(java.lang.String x, int y) {
            return null;
        }

        JSO put(java.lang.String x, long y) {
            return null;
        }

        JSO put(java.lang.String x, double y) {
            return null;
        }

        JSO put(java.lang.String x, java.util.Map<String, String> y) {
            return null;
        }

        JSO put(java.lang.String x, boolean y) {
            return null;
        }
    }

    static class JSA {

        JSA put(Object o) {
            return null;
        }

        JSA put(int i, Object x) {
            return null;
        }

        JSA put(boolean x) {
            return null;
        }

        JSA put(int x) {
            return null;
        }

        JSA put(int i, int x) {
            return null;
        }

        JSA put(int x, boolean y) {
            return null;
        }

        JSA put(int i, long x) {
            return null;
        }

        JSA put(long x) {
            return null;
        }

        JSA put(java.util.Collection<String> x) {
            return null;
        }

        JSA put(int i, java.util.Collection<String> x) {
            return null;
        }

        JSA put(int i, java.util.Map<String, String> x) {
            return null;
        }

        JSA put(java.util.Map<String, String> x) {
            return null;
        }

        JSA put(int i, double x) {
            return null;
        }

        JSA put(double x) {
            return null;
        }
    }

    public static void main(String [] args) {
    }
    public static void foo() {
         new JSO()
          .put("s", new JSA())
          .put("s", new JSA())
          .put("s", new JSO()
            .put("s", new JSO()
              .put("s", new JSA().put("s"))
              .put("s", new JSA())
              .put("s", new JSO()
                .put("s", new JSO()
                  .put("s", new JSA().put("s").put("s"))
                  .put("s", new JSA())
                  .put("s", new JSO()
                    .put("s", new JSO()
                      .put("s", new JSA().put("s").put("s").put("s")
                            .put("s").put("s").put("s")
                            .put("s").put("s"))
                      .put("s", new JSA())
                      .put("s", new JSO()
                        .put("s", new JSO()
                          .put("s", new JSA().put("s"))
                          .put("s", new JSA())
                        )
                      )
                    )
                  )
                )
                .put("s", new JSO()
                  .put("s", new JSA().put("s"))
                  .put("s", new JSA())
                  .put("s", new JSO()
                  .put("s", new JSO()
                    .put("s", new JSA().put("s").put("s"))
                    .put("s", new JSA())
                    .put("s", new JSO()
                      .put("s", new JSO()
                        .put("s", new JSA().put("s").put("s").put("s")
                                .put("s").put("s").put("s")
                                .put("s").put("s"))
                        .put("s", new JSA())
                        .put("s", new JSO()
                          .put("s", new JSO()
                            .put("s", new JSA().put("s"))
                            .put("s", new JSA()))
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          );
  }
}
