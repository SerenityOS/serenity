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
 * @compile -parameters WithParameters.java
 * @run main WithParameters
 * @summary javac should generate method parameters correctly.
 */

import java.lang.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.List;

public class WithParameters {

    private static final Class<?>[] qux_types = {
        int.class, Foo.class, List.class, List.class, List.class, String[].class
    };

    private static final String[] qux_names = {
        "quux", "quuux", "l", "l2", "l3", "rest"
    };

    public static void main(String argv[]) throws Exception {
        int error = 0;
        Method[] methods = Foo.class.getMethods();
        for(Method m : methods) {
            System.err.println("Inspecting method " + m.getName());
            Parameter[] parameters = m.getParameters();
            if(parameters == null)
                throw new Exception("getParameters should never be null");
            for(int i = 0; i < parameters.length; i++) {
                    Parameter p = parameters[i];
                    if(!p.getDeclaringExecutable().equals(m)) {
                        System.err.println(p + ".getDeclaringExecutable != " + m);
                        error++;
                    }
                    if(null == p.getType()) {
                        System.err.println(p + ".getType() == null");
                        error++;
                    }
                    if(null == p.getParameterizedType()) {
                        System.err.println(p + ".getParameterizedType == null");
                        error++;
                    }
            }
            if(m.getName().equals("qux")) {
                if(6 != parameters.length) {
                    System.err.println("Wrong number of parameters for qux");
                    error++;
                }
                for(int i = 0; i < parameters.length; i++) {
                    Parameter p = parameters[i];
                    if(!p.isNamePresent()) {
                        System.err.println(p + ".isNamePresent == false");
                        error++;
                    }
                    if(!parameters[i].getName().equals(qux_names[i])) {
                        System.err.println("Wrong parameter name for " + parameters[i]);
                        error++;
                    }
                    // The getType family work with or without
                    // parameter attributes compiled in.
                    if(!parameters[i].getType().equals(qux_types[i])) {
                        System.err.println("Wrong parameter type for " + parameters[0] + ": expected " + qux_types[i] + ", but got " + parameters[i].getType());
                        error++;
                    }
                }
                if(!parameters[0].toString().equals("final int quux")) {
                    System.err.println("toString for quux is wrong, expected \"final int quux\", got \"" + parameters[0] + "\"");
                    error++;
                }
                if(parameters[0].getModifiers() != Modifier.FINAL) {
                    System.err.println("quux is not final");
                    error++;
                }
                if(parameters[0].isVarArgs()) {
                    System.err.println("isVarArg for quux is wrong");
                    error++;
                }
                if(!parameters[0].getParameterizedType().equals(int.class)) {
                    System.err.println("getParameterizedType for quux is wrong");
                    error++;
                }
                if(!parameters[1].toString().equals("WithParameters$Foo quuux")) {
                    System.err.println("toString for quuux is wrong, expected \"WithParameters$Foo quuux\", got \"" + parameters[1] + "\"");
                    error++;
                }
                if(parameters[1].isVarArgs()) {
                    System.err.println("isVarArg for quuux is wrong");
                    error++;
                }
                if(!parameters[1].getParameterizedType().equals(Foo.class)) {
                    System.err.println("getParameterizedType for quuux is wrong");
                    error++;
                }
                Annotation[] anns = parameters[1].getAnnotations();
                if(1 != anns.length) {
                    System.err.println("getAnnotations missed an annotation");
                    error++;
                } else if(!anns[0].annotationType().equals(Thing.class)) {
                    System.err.println("getAnnotations has the wrong annotation");
                    error++;
                }
                if(!parameters[2].toString().equals("java.util.List<?> l")) {
                    System.err.println("toString for l is wrong, expected \"java.util.List<?> l\", got \"" + parameters[2] + "\"");
                    error++;
                }
                if(parameters[2].isVarArgs()) {
                    System.err.println("isVarArg for l is wrong");
                    error++;
                }
                if(!(parameters[2].getParameterizedType() instanceof
                     ParameterizedType)) {
                    System.err.println("getParameterizedType for l is wrong");
                    error++;
                } else {
                    ParameterizedType pt =
                        (ParameterizedType) parameters[2].getParameterizedType();
                    if(!pt.getRawType().equals(List.class)) {
                        System.err.println("Raw type for l is wrong");
                        error++;
                    }
                    if(1 != pt.getActualTypeArguments().length) {
                        System.err.println("Number of type parameters for l is wrong");
                        error++;
                    }
                    if(!(pt.getActualTypeArguments()[0] instanceof WildcardType)) {
                        System.err.println("Type parameter for l is wrong");
                        error++;
                    }
                }
                if(!parameters[3].toString().equals("java.util.List<WithParameters$Foo> l2")) {
                    System.err.println("toString for l2 is wrong, expected \"java.util.List<WithParameters$Foo> l2\", got \"" + parameters[3] + "\"");
                    error++;
                }
                if(parameters[3].isVarArgs()) {
                    System.err.println("isVarArg for l2 is wrong");
                    error++;
                }
                if(!(parameters[3].getParameterizedType() instanceof
                     ParameterizedType)) {
                    System.err.println("getParameterizedType for l2 is wrong");
                    error++;
                } else {
                    ParameterizedType pt =
                        (ParameterizedType) parameters[3].getParameterizedType();
                    if(!pt.getRawType().equals(List.class)) {
                        System.err.println("Raw type for l2 is wrong");
                        error++;
                    }
                    if(1 != pt.getActualTypeArguments().length) {
                        System.err.println("Number of type parameters for l2 is wrong");
                        error++;
                    }
                    if(!(pt.getActualTypeArguments()[0].equals(Foo.class))) {
                        System.err.println("Type parameter for l2 is wrong");
                        error++;
                    }
                }
                if(!parameters[4].toString().equals("java.util.List<? extends WithParameters$Foo> l3")) {
                    System.err.println("toString for l3 is wrong, expected \"java.util.List<? extends WithParameters$Foo> l3\", got \"" + parameters[3] + "\"");
                    error++;
                }
                if(parameters[4].isVarArgs()) {
                    System.err.println("isVarArg for l3 is wrong");
                    error++;
                }
                if(!(parameters[4].getParameterizedType() instanceof
                     ParameterizedType)) {
                    System.err.println("getParameterizedType for l3 is wrong");
                    error++;
                } else {
                    ParameterizedType pt =
                        (ParameterizedType) parameters[4].getParameterizedType();
                    if(!pt.getRawType().equals(List.class)) {
                        System.err.println("Raw type for l3 is wrong");
                        error++;
                    }
                    if(1 != pt.getActualTypeArguments().length) {
                        System.err.println("Number of type parameters for l3 is wrong");
                        error++;
                    }
                    if(!(pt.getActualTypeArguments()[0] instanceof WildcardType)) {
                        System.err.println("Type parameter for l3 is wrong");
                        error++;
                    } else {
                        WildcardType wt = (WildcardType)
                            pt.getActualTypeArguments()[0];
                        if(!wt.getUpperBounds()[0].equals(Foo.class)) {
                            System.err.println("Upper bounds on type parameter fol l3 is wrong");
                            error++;
                        }
                    }
                }
                if(!parameters[5].toString().equals("java.lang.String... rest")) {
                    System.err.println("toString for rest is wrong, expected \"java.lang.String... rest\", got \"" + parameters[5] + "\"");
                    error++;
                }
                if(!parameters[5].isVarArgs()) {
                    System.err.println("isVarArg for rest is wrong");
                    error++;
                }
                if(!(parameters[5].getParameterizedType().equals(String[].class))) {
                    System.err.println("getParameterizedType for rest is wrong");
                    error++;
                }
            }
        }
        if(0 != error)
            throw new Exception("Failed " + error + " tests");
    }

    void test(int test) {}

    public class Foo {
        int thing;
        public void qux(final int quux, @Thing Foo quuux,
                        List<?> l, List<Foo> l2,
                        List<? extends Foo> l3,
                        String... rest) {}
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Thing {}

}
