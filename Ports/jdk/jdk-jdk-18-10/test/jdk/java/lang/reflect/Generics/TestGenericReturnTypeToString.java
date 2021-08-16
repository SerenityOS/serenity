/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8054213
 * @summary Check that toString method works properly for generic return type
 * obtained via reflection
 * @run main TestGenericReturnTypeToString
 */

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

public class TestGenericReturnTypeToString {

    public static void main(String[] args) {
        boolean hasFailures = false;
        for (Method method : TestGenericReturnTypeToString.class.getMethods()) {
            if (method.isAnnotationPresent(ExpectedGenericString.class)) {
                ExpectedGenericString es = method.getAnnotation
                        (ExpectedGenericString.class);
                String result = method.getGenericReturnType().toString();
                if (!es.value().equals(result)) {
                    hasFailures = true;
                    System.err.println("Unexpected result of " +
                            "getGenericReturnType().toString() " +
                            " for " + method.getName()
                            + " expected: " + es.value() + " actual: " + result);
                }
            }
            if (hasFailures) {
                throw new RuntimeException("Test failed");
            }
        }
    }

    @ExpectedGenericString("TestGenericReturnTypeToString$" +
          "FirstInnerClassGeneric<Dummy>$SecondInnerClassGeneric<Dummy>")
    public FirstInnerClassGeneric<Dummy>.SecondInnerClassGeneric<Dummy> foo1() {
        return null;
    }

    @ExpectedGenericString("TestGenericReturnTypeToString$" +
          "FirstInnerClassGeneric<Dummy>$SecondInnerClass")
    public FirstInnerClassGeneric<Dummy>.SecondInnerClass foo2() {
        return null;
    }

    @ExpectedGenericString("TestGenericReturnTypeToString$" +
          "FirstInnerClass$SecondInnerClassGeneric<Dummy>")
    public FirstInnerClass.SecondInnerClassGeneric<Dummy> foo3() {
        return null;
    }

    @ExpectedGenericString("class TestGenericReturnTypeToString$" +
          "FirstInnerClass$SecondInnerClass")
    public FirstInnerClass.SecondInnerClass foo4() {
        return null;
    }

    @ExpectedGenericString(
          "java.util.List<java.lang.String>")
    public java.util.List<java.lang.String> foo5() {
        return null;
    }

    @ExpectedGenericString("interface TestGenericReturnTypeToString$" +
          "FirstInnerClass$Interface")
    public FirstInnerClass.Interface foo6() {
        return null;
    }

    @ExpectedGenericString("TestGenericReturnTypeToString$" +
          "FirstInnerClass$InterfaceGeneric<Dummy>")
    public FirstInnerClass.InterfaceGeneric<Dummy> foo7() {
        return null;
    }

    public static class FirstInnerClass {

        public class SecondInnerClassGeneric<T> {
        }

        public class SecondInnerClass {
        }

        interface Interface {
        }

        interface InterfaceGeneric<T> {
        }
    }

    public class FirstInnerClassGeneric<T> {

        public class SecondInnerClassGeneric<T> {
        }

        public class SecondInnerClass {
        }
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedGenericString {
    String value();
}

class Dummy {
}
