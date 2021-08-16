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

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.StringConcatFactory;

/**
 * @test
 * @summary StringConcatFactory allow recipes with repeated constants, but this
 *          is not expressible with java code and needs an explicit sanity test
 * @bug 8222852
 *
 * @compile StringConcatFactoryRepeatedConstants.java
 *
 * @run main/othervm -Xverify:all StringConcatFactoryRepeatedConstants
 */
public class StringConcatFactoryRepeatedConstants {

    public static void main(String[] args) throws Throwable {

        CallSite site = StringConcatFactory.makeConcatWithConstants(
            MethodHandles.lookup(),
            "foo",
            MethodType.methodType(String.class),
            "\u0002\u0002",
            "foo", "bar"
        );
        String string = (String)site.dynamicInvoker().invoke();
        if (!"foobar".equals(string)) {
            throw new IllegalStateException("Expected: foobar, got: " + string);
        }

        site = StringConcatFactory.makeConcatWithConstants(
                MethodHandles.lookup(),
                "foo",
                MethodType.methodType(String.class),
                "\u0002\u0002test\u0002\u0002",
                "foo", 17.0f, 4711L, "bar"
        );
        string = (String)site.dynamicInvoker().invoke();
        StringBuilder sb = new StringBuilder();
        sb.append("foo").append(17.0f).append("test").append(4711L).append("bar");
        if (!sb.toString().equals(string)) {
            throw new IllegalStateException("Expected: " + sb.toString() + ", got: " + string);
        }
    }

}
