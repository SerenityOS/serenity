/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004729
 * @summary javac should generate method parameters correctly.
 */

import java.lang.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.List;
import java.util.Objects;

import static java.lang.annotation.ElementType.*;

public class WithoutParameters {
    int errors = 0;

    private WithoutParameters() {}

    public static void main(String argv[]) throws Exception {
        WithoutParameters wp = new WithoutParameters();
        wp.runTests(Foo.class.getMethods());
        wp.runTests(Foo.Inner.class.getConstructors());
        wp.checkForErrors();
    }

    void runTests(Method[] methods) throws Exception {
        for(Method m : methods) {runTest(m);}
    }

    void runTests(Constructor[] constructors) throws Exception {
        for(Constructor c : constructors) {runTest(c);}
    }

    void runTest(Executable e) throws Exception {
        System.err.println("Inspecting executable " + e);
        Parameter[] parameters = e.getParameters();
        Objects.requireNonNull(parameters, "getParameters should never be null");

        ExpectedParameterInfo epi = e.getAnnotation(ExpectedParameterInfo.class);
        if (epi != null) {
            abortIfTrue(epi.parameterCount() != e.getParameterCount(), "Bad parameter count for "+ e);
            abortIfTrue(epi.isVarArgs() != e.isVarArgs(),"Bad varargs value for "+ e);
        }
        abortIfTrue(e.getParameterCount() != parameters.length, "Mismatched of parameter counts.");

        for(int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            errorIfTrue(p.isNamePresent(), p + ".isNamePresent == true");
            errorIfTrue(!p.getDeclaringExecutable().equals(e), p + ".getDeclaringExecutable != " + e);
            Objects.requireNonNull(p.getType(), "getType() should not be null");
            Objects.requireNonNull(p.getParameterizedType(), "getParameterizedType() should not be null");

            if (epi != null) {
                Class<?> expectedParameterType = epi.parameterTypes()[i];
                errorIfTrue(!p.getType().equals(expectedParameterType),
                            "Wrong parameter type for " + p + ": expected " + expectedParameterType  +
                            ", but got " + p.getType());

                ParameterizedInfo[] expectedParameterizedTypes = epi.parameterizedTypes();
                if (expectedParameterizedTypes.length > 0) {
                    Type parameterizedType = p.getParameterizedType();
                    Class<? extends Type> expectedParameterziedTypeType = expectedParameterizedTypes[i].value();
                    errorIfTrue(!expectedParameterziedTypeType.isAssignableFrom(parameterizedType.getClass()),
                                "Wrong class of parameteried type of " + p + ": expected " + expectedParameterziedTypeType  +
                                ", but got " + parameterizedType.getClass());

                    if (expectedParameterziedTypeType.equals(Class.class)) {
                        errorIfTrue(!parameterizedType.equals(expectedParameterType),
                                    "Wrong parameteried type for " + p + ": expected " + expectedParameterType  +
                                    ", but got " + parameterizedType);
                    } else {
                        if (expectedParameterziedTypeType.equals(ParameterizedType.class)) {
                            ParameterizedType ptype = (ParameterizedType)parameterizedType;
                            errorIfTrue(!ptype.getRawType().equals(expectedParameterType),
                                        "Wrong raw type for " + p + ": expected " + expectedParameterType  +
                                        ", but got " + ptype.getRawType());
                        }

                        // Check string representation
                        String expectedStringOfType = epi.parameterizedTypes()[i].string();
                        errorIfTrue(!expectedStringOfType.equals(parameterizedType.toString()),
                                    "Bad type string" + p + ": expected " + expectedStringOfType  +
                                    ", but got " + parameterizedType.toString());
                    }
                }
            }
        }
    }

    private void checkForErrors() {
        if (errors > 0)
            throw new RuntimeException("Failed " + errors + " tests");
    }

    private void errorIfTrue(boolean predicate, String errMessage) {
        if (predicate) {
            errors++;
            System.err.println(errMessage);
        }
    }

    private void abortIfTrue(boolean predicate, String errMessage) {
        if (predicate) {
            throw new RuntimeException(errMessage);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({METHOD, CONSTRUCTOR})
    @interface ExpectedParameterInfo {
        int parameterCount() default 0;
        Class<?>[] parameterTypes() default {};
        ParameterizedInfo[] parameterizedTypes() default {};
        boolean isVarArgs() default false;
    }

    @Target({})
    @interface ParameterizedInfo {
        Class<? extends Type> value() default Class.class;
        String string() default "";
    }

    public class Foo {
        int thing;
        @ExpectedParameterInfo(parameterCount = 6,
                               parameterTypes =
                               {int.class, Foo.class,
                                List.class, List.class,
                                List.class, String[].class},
                               parameterizedTypes =
                                {@ParameterizedInfo(Class.class),
                                 @ParameterizedInfo(Class.class),
                                 @ParameterizedInfo(value=ParameterizedType.class, string="java.util.List<?>"),
                                 @ParameterizedInfo(value=ParameterizedType.class, string="java.util.List<WithoutParameters$Foo>"),
                                 @ParameterizedInfo(value=ParameterizedType.class, string="java.util.List<? extends WithoutParameters$Foo>"),
                                 @ParameterizedInfo(Class.class)},
                               isVarArgs = true)
        public void qux(int quux, Foo quuux,
                        List<?> l, List<Foo> l2,
                        List<? extends Foo> l3,
                        String... rest) {}
        public class Inner {
            int thang;
            @ExpectedParameterInfo(parameterCount = 2,
                                   parameterTypes = {Foo.class, int.class})
            public Inner(int theng) {
                thang = theng + thing;
            }
        }
    }
}
