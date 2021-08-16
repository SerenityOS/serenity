/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8028739
 * @summary javac generates incorrect descriptor for MethodHandle::invoke
 * @run testng MethodReferenceTestMethodHandle
 */

import java.lang.invoke.*;
import java.util.*;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

@Test
public class MethodReferenceTestMethodHandle {

  MethodHandles.Lookup lookup = MethodHandles.lookup();

  interface ReplaceItf {
      Object apply(String a, char b, char c) throws Throwable;
  }

  interface FormatItf {
      Object apply(String a, Object... args) throws Throwable;
  }

  interface AddItf {
      void apply(List st, int idx, Object v) throws Throwable;
  }

  public void testVirtual() throws Throwable {

      MethodType mt = MethodType.methodType(String.class, char.class, char.class);
      MethodHandle ms = lookup.findVirtual(String.class, "replace", mt);

      // --- String.replace(String, char, char) ---

      assertEquals("oome otring to oearch", ms.invoke("some string to search", 's', 'o'));

      ReplaceItf f1 = (a, b, c) -> ms.invoke(a,b,c);
      assertEquals("oome otring to oearch", f1.apply("some string to search", 's', 'o'));

      ReplaceItf f2 = ms::invoke;
      assertEquals("oome otring to oearch", f2.apply("some string to search", 's', 'o'));
      assertEquals("oome otring to oearch", f2.apply("some string to search", new Character('s'), 'o'));
      assertEquals("oome otring to oearch", ((ReplaceItf) ms::invoke).apply("some string to search", 's', 'o'));
  }

  public void testStatic() throws Throwable {
      MethodType fmt = MethodType.methodType(String.class, String.class, (new Object[1]).getClass());
      MethodHandle fms = lookup.findStatic(String.class, "format", fmt);

      // --- String.format(String, Object...) ---

      assertEquals("Testing One 2 3", fms.invoke("Testing %s %d %x", "One", new Integer(2), 3));

      FormatItf ff2 = fms::invoke;
      assertEquals("Testing One 2 3", ff2.apply("Testing %s %d %x", "One", new Integer(2), 3));
      assertEquals("Testing One 2 3", ((FormatItf) fms::invoke).apply("Testing %s %d %x", "One", new Integer(2), 3));
      assertEquals("Testing One 2 3 four", ff2.apply("Testing %s %d %x %s", "One", new Integer(2), 3, "four"));
  }

  public void testVoid() throws Throwable {
      MethodType pmt = MethodType.methodType(void.class, int.class, Object.class);
      MethodHandle pms = lookup.findVirtual(List.class, "add", pmt);
      List<String> list = new ArrayList<>();

      // --- List.add(int,String) ---

      pms.invoke(list, 0, "Hi");

      AddItf pf2 = pms::invoke;
      pf2.apply(list, 1, "there");
      AddItf pf3 = pms::invokeExact;
      pf3.apply(list, 2, "you");
      assertEquals("Hi", list.get(0));
      assertEquals("there", list.get(1));
      assertEquals("you", list.get(2));
   }
}
