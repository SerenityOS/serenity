/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8181171
 * @summary Break ResolvedMethodTable with redefined nest class.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 * @compile ../../NamedBuffer.java
 * @compile redef/Xost.java
 * @run main RedefineClassHelper
 * @run main/othervm -XX:+AllowRedefinitionToAddDeleteMethods -javaagent:redefineagent.jar -Xlog:redefine+class+update*=debug,membername+table=debug MethodHandleDeletedMethod
 */

import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.*;

class Host {
    static MethodHandle fooMH;

    static class A {
        private static void foo() { System.out.println("OLD foo called"); }
    }
    static void bar() throws NoSuchMethodError {
        A.foo();
    }
    static void barMH() throws Throwable {
        fooMH.invokeExact();
    }

    public static void reresolve() throws Throwable {
        fooMH = MethodHandles.lookup().findStatic(A.class, "foo", MethodType.methodType(void.class));
    }

    static {
        try {
          fooMH = MethodHandles.lookup().findStatic(A.class, "foo", MethodType.methodType(void.class));
        } catch (ReflectiveOperationException ex) {
        }
    }
}

public class MethodHandleDeletedMethod {

    static final String DEST = System.getProperty("test.classes");
    static final boolean VERBOSE = false;

    private static byte[] bytesForHostClass(char replace) throws Throwable {
        return NamedBuffer.bytesForHostClass(replace, "Host$A");
    }

    public static void main(java.lang.String[] unused) throws Throwable {
        Host h = new Host();
        h.bar();
        h.barMH();
        byte[] buf = bytesForHostClass('X');
        RedefineClassHelper.redefineClass(Host.A.class, buf);
        try {
            h.bar();    // call deleted Method directly
            throw new RuntimeException("Failed, expected NSME");
        } catch (NoSuchMethodError nsme) {
            System.out.println("Received expected NSME");
        }
        try {
            h.barMH();  // call through MethodHandle for deleted Method
            throw new RuntimeException("Failed, expected NSME");
        } catch (NoSuchMethodError nsme) {
            System.out.println("Received expected NSME");
        }
        System.out.println("Passed.");
    }
}
