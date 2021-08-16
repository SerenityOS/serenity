/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jdeprusage;

import jdk.deprcases.types.DeprecatedClass;

public class UseClass {
    static class Extends extends DeprecatedClass {
    }

    static class ClassLiteral {
        Class<?> clazz = DeprecatedClass.class;
    }

    static class FieldType {
        DeprecatedClass obj = null;
    }

    static class MethodParameter {
        void foo(DeprecatedClass x) { }
    }

    static class MethodReturn {
        DeprecatedClass foo() { return null; }
    }

    static class ArrayCreation {
        Object foo() {
            return new DeprecatedClass[1];
        }
    }

    static class ArrayFieldUsage {
        int foo(Object o) {
            return ((DeprecatedClass[])o).length;
        }
    }

    static class ArrayMethodCall {
        Object foo(Object o) {
            return ((DeprecatedClass[])o).clone();
        }
    }

    static class InstanceOf {
        boolean foo(Object o) {
            return o instanceof DeprecatedClass;
        }
    }

    static class Cast {
        Object foo(Object o) {
            return (DeprecatedClass)o;
        }
    }

    static class Generic<T> {
        static <U> void g() { }
    }

    static class ClassTypeArg extends Generic<DeprecatedClass> { }

    static class MethodTypeArg {
        void foo() {
            Generic.<DeprecatedClass>g();
        }
    }

    static class ConstructorTypeArg {
        Object foo() {
            return new Generic<DeprecatedClass>();
        }
    }

    static class Construct {
        Object foo() {
            return new DeprecatedClass();
        }
    }

    static class Local {
        void foo() {
            DeprecatedClass obj = null;
        }
    }
}
