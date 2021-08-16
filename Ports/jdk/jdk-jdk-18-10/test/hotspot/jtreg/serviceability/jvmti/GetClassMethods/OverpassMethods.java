/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8216324
 * @summary GetClassMethods is confused by the presence of default methods in super interfaces
 * @requires vm.jvmti
 * @library /test/lib
 * @compile OverpassMethods.java
 * @run main/othervm/native -agentlib:OverpassMethods OverpassMethods
 * @run main/othervm/native -agentlib:OverpassMethods=maintain_original_method_order OverpassMethods
  */

import java.lang.reflect.Method;
import java.util.Arrays;

public class OverpassMethods {

    static {
        try {
            System.loadLibrary("OverpassMethods");
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Could not load OverpassMethods library");
            System.err.println("java.library.path:" + System.getProperty("java.library.path"));
            throw ex;
        }
    }

    static private void log(Object msg) {
        System.out.println(String.valueOf(msg));
    }

    static private native Method[] getJVMTIDeclaredMethods(Class<?> klass);

    public interface Parent {
        default String def() { return "Parent.def"; }
        String method0();
        String method1();
    }

    public interface Child extends Parent {
        String method2();
    }

    public static class Impl implements Child {
        public String method0() { return "Impl.method0"; }
        public String method1() { return "Impl.method1"; }
        public String method2() { return "Impl.method2"; }
    }

    public static void main(String[] args) {
        new Impl(); // To get classes initialized

        Method[] reflectMethods = Child.class.getDeclaredMethods();
        Method[] jvmtiMethods = getJVMTIDeclaredMethods(Child.class);

        if (jvmtiMethods == null) {
            throw new RuntimeException("getJVMTIDeclaredMethods failed");
        }

        log("Reflection getDeclaredMethods returned: " + Arrays.toString(reflectMethods));
        log("JVMTI GetClassMethods returned: " + Arrays.toString(jvmtiMethods));

        if (reflectMethods.length != jvmtiMethods.length) {
            throw new RuntimeException("OverpassMethods failed: Unexpected method count from JVMTI GetClassMethods!");
        }
        if (!reflectMethods[0].equals(jvmtiMethods[0])) {
            throw new RuntimeException("OverpassMethods failed: Unexpected method from JVMTI GetClassMethods!");
        }
        log("Test passed: Got expected output from JVMTI GetClassMethods!");
    }
}
