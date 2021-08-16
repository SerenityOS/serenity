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
 * @bug 6194361
 * @bug 8202669
 * @summary Make sure the VM doesn't crash and throws a SecurityException
 *          if defineClass() is called on a byte buffer that parses into a invalid
 *          java.lang.Object class.
 *          Also, make sure the vm doesn't crash on notification for unloading an invalid
 *          java.lang.Object class.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 * @run main TestUnloadClassError
 */

import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.classloader.ClassUnloadCommon;

public class TestUnloadClassError extends ClassLoader {

   static String source =
       " package java.lang;" +
       " public class Object" +
       " {" +
       "   int field;" +
       "   public boolean equals(Object o) {" +
       "     System.out.println(o.field);" +
       "     return false;" +
       "   }" +
       " }";

  public static void main(String[] args) throws Exception
  {
    try {
      TestUnloadClassError loader = new TestUnloadClassError();
      byte[] buf = InMemoryJavaCompiler.compile("java.lang.Object", source,
                                                "--patch-module=java.base");
      Class c = loader.defineClass(buf, 0, buf.length);
      System.out.println("test FAILS");
      throw new RuntimeException("Did not get security exception");
    } catch(SecurityException e) {
      System.out.println("test expects SecurityException");
    }

    // Unload bad class
    ClassUnloadCommon.triggerUnloading();
    System.out.println("test PASSES if it doesn't crash");
  }
}
