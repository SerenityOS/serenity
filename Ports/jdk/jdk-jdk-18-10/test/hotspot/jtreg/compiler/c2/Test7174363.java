/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7174363
 * @summary crash with Arrays.copyOfRange(original, from, to) when from > original.length
 *
 * @run main/othervm -XX:-BackgroundCompilation compiler.c2.Test7174363
 */

package compiler.c2;

import java.util.Arrays;

public class Test7174363 {

  static Object[] m(Object[] original, int from, int to) {
    return Arrays.copyOfRange(original, from, to, Object[].class);
  }

  static public void main(String[] args) {
    Object[] orig = new Object[10];
    for (int i = 0; i < 20000; i++) {
      try {
        m(orig, 15, 20);
      } catch (ArrayIndexOutOfBoundsException excp) {
      }
    }
  }
}
