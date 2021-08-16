/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.metadata.valuedescriptor;

import java.util.HashMap;
import java.util.Map;

import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.CommonHelper;
import jdk.test.lib.jfr.VoidFunction;

/**
 * @test
 * @summary Test ValueDescriptor.getAnnotations()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.valuedescriptor.TestClasses
 */
public class TestClasses {

    public static void main(String[] args) throws Throwable {
        @SuppressWarnings("rawtypes")
        Map<String, Class> valid = new HashMap<>();
        valid.put("byte", byte.class);
        valid.put("short", short.class);
        valid.put("int", int.class);
        valid.put("char", char.class);
        valid.put("float", float.class);
        valid.put("double", double.class);
        valid.put("boolean", boolean.class);
        valid.put("double", double.class);
        valid.put("long", long.class);
        valid.put("java.lang.String", String.class);
        valid.put("java.lang.Class", Class.class);
        valid.put("java.lang.Thread", Thread.class);

        for (String name : valid.keySet()) {
            Class<?> t = valid.get(name);
            System.out.println(t.getName());
            ValueDescriptor d = new ValueDescriptor(t, "dummy");
            String typeName = d.getTypeName() + (d.isArray() ? "[]" : "");
            System.out.printf("%s -> typeName %s%n", name, typeName);
            Asserts.assertEquals(name, typeName, "Wrong type name");
        }

        // Test some illegal classes
        verifyIllegalArg(()->{new ValueDescriptor(Float.class, "ids");}, "Arrays of non-primitives should give Exception");
        verifyIllegalArg(()->{new ValueDescriptor(Integer[].class, "ids");}, "Arrays of non-primitives should give Exception");
        verifyIllegalArg(()->{new ValueDescriptor(Class[].class, "ids");}, "Arrays of non-primitives should give Exception");
        verifyIllegalArg(()->{new ValueDescriptor(MyClass.class, "MyClass");}, "MyClass should give Exception");
    }

    private static class MyClass {
        @SuppressWarnings("unused")
        int id;
    }

    private static void verifyIllegalArg(VoidFunction f, String msg) throws Throwable {
        CommonHelper.verifyException(f, msg, IllegalArgumentException.class);
    }
}
