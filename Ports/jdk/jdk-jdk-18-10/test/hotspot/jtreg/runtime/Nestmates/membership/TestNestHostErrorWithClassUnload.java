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

/*
 * @test
 * @summary Test the ability to safely unload a class that has an error
 *          with its designated nest host. The nest host class must resolve
 *          successfully but fail validation. This tests a specific, otherwise
 *          untested, code path in ResolutionErrorTable::free_entry.
 *
 * @library /test/lib
 * @compile TestNestHostErrorWithClassUnload.java
 *          Helper.java
 *          PackagedNestHost.java
 *          PackagedNestHost2.java
 * @compile PackagedNestHost2Member.jcod
 *
 * @run main/othervm -Xlog:class+unload=trace TestNestHostErrorWithClassUnload
 */

// Test setup:
//   PackagedNestHost.java defines P1.PackagedNestHost, which is referenced
//     by P2.PackageNestHost2.Member
//   PackagedNestHost2.java defines P2.PackagedNestHost2 and its nested Member
//     class.
//   PackagedNestHost2Member.jcod changes P2.PackagedNestHost.Member to claim
//     it is in the nest of P1.PackagedNestHost.
//   Helper.java is a helper class to run the test under the other classloader.
//     A separate class is used to avoid confusion because if a public nested
//     class were to be used, then the main test class would be loaded a second
//     time in the other loader, and also subsequently unloaded.
//
// We load all classes into a new classloader and then try to call
// P2.PackagedNestHost.Member.m() which will be private at runtime. That will
// trigger nest host resolution and validation and the invocation will fail
// with IllegalAccessError. We then drop the classloader and invoke GC to force
// the unloading of the classes. Not all GC configurations are guaranteed to
// result in unloading, but that isn't essential for the test to succeed - we
// know that when unloading does occur we have tested the desired code path.

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.test.lib.classloader.ClassUnloadCommon;

public class TestNestHostErrorWithClassUnload {

    static final MethodType INVOKE_T = MethodType.methodType(void.class);

    public static void main(String[] args) throws Throwable {
        ClassLoader cl = ClassUnloadCommon.newClassLoader();
        Class<?> c = cl.loadClass("Helper");
        MethodHandle mh = MethodHandles.lookup().findStatic(c, "test", INVOKE_T);
        mh.invoke();
        // now drop all references so we can trigger unloading
        mh = null;
        c = null;
        cl = null;
        ClassUnloadCommon.triggerUnloading();
    }
}
