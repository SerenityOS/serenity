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

/**
 * @test
 * @summary test for VarHandle signature polymorphic methods
 * @run testng MethodReferenceTestVarHandle
 */

import java.lang.invoke.*;
import java.util.*;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

@Test
public class MethodReferenceTestVarHandle {

  interface Setter {
      void apply(int[] arr, int idx, int val);
  }

  interface Getter {
      int apply(int[] arr, int idx);
  }

  public void testSet() throws Throwable {
      VarHandle vh = MethodHandles.arrayElementVarHandle(int[].class);

      Setter f = vh::set;

      int[] data = {0};
      f.apply(data, 0, 42);
      assertEquals(42, data[0]);
  }

  public void testGet() throws Throwable {
      VarHandle vh = MethodHandles.arrayElementVarHandle(int[].class);

      Getter f = vh::get;

      int[] data = {42};
      int v = f.apply(data, 0);
      assertEquals(42, v);
  }
}
