/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test KeepAliveObject
 * @summary This test case uses a class instance to keep the class alive.
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @library classes
 * @build sun.hotspot.WhiteBox test.Empty
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xmn8m -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI KeepAliveObject
 */

import sun.hotspot.WhiteBox;
import jdk.test.lib.classloader.ClassUnloadCommon;

/**
 * Test that verifies that classes are not unloaded when specific types of references are kept to them.
 */
public class KeepAliveObject {
  private static final String className = "test.Empty";
  private static final WhiteBox wb = WhiteBox.getWhiteBox();
  public static Object escape = null;

  public static void main(String... args) throws Exception {
    ClassLoader cl = ClassUnloadCommon.newClassLoader();
    Class<?> c = cl.loadClass(className);
    Object o = c.newInstance();
    cl = null; c = null;
    escape = o;

    {
        boolean isAlive = wb.isClassAlive(className);
        System.out.println("testObject (1) alive: " + isAlive);

        ClassUnloadCommon.failIf(!isAlive, "should be alive");
    }

    ClassUnloadCommon.triggerUnloading();

    {
        boolean isAlive = wb.isClassAlive(className);
        System.out.println("testObject (2) alive: " + isAlive);

        ClassUnloadCommon.failIf(!isAlive, "should be alive");
    }
    o = null;
    escape = null;
    ClassUnloadCommon.triggerUnloading();

    {
        boolean isAlive = wb.isClassAlive(className);
        System.out.println("testObject (3) alive: " + isAlive);
        ClassUnloadCommon.failIf(isAlive, "should be unloaded");
    }

  }
}
