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
 * This file is copied from open/test/jdk/java/lang/invoke/lambda.
 * It is being used as the main class for the appcds/LambdaEagerInit.java test.
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static jdk.test.lib.Asserts.*;

public class LambdaEagerInitTest {

    interface H {Object m(String s);}

    private static Set<String> allowedStaticFields(boolean nonCapturing) {
        Set<String> s = new HashSet<>();
        if (Boolean.getBoolean("jdk.internal.lambda.disableEagerInitialization")) {
            if (nonCapturing) s.add("LAMBDA_INSTANCE$");
        }
        return s;
    }

    private void nonCapturingLambda() {
        H la = s -> s;
        assertEquals("hi", la.m("hi"));
        Class<? extends H> c1 = la.getClass();
        verifyLambdaClass(la.getClass(), true);
    }

    private void capturingLambda() {
        H la = s -> concat(s, "foo");
        assertEquals("hi foo", la.m("hi"));
        verifyLambdaClass(la.getClass(), false);
    }

    private void verifyLambdaClass(Class<?> c, boolean nonCapturing) {
        Set<String> staticFields = new HashSet<>();
        Set<String> instanceFields = new HashSet<>();
        for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                staticFields.add(f.getName());
            } else {
                instanceFields.add(f.getName());
            }
        }
        assertEquals(instanceFields.size(), nonCapturing ? 0 : 1, "Unexpected instance fields");
        assertEquals(staticFields, allowedStaticFields(nonCapturing), "Unexpected static fields");
    }

    private String concat(String... ss) {
        return Arrays.stream(ss).collect(Collectors.joining(" "));
    }

    public static void main(String[] args) {
        LambdaEagerInitTest test = new LambdaEagerInitTest();
        test.nonCapturingLambda();
        test.capturingLambda();
    }
}
