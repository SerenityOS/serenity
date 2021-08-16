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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.instrument.ClassDefinition;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

public class RedefineMethodWithAnnotationsApp {
    public static void main(String args[]) throws Exception {
        System.out.println("Hello from RedefineMethodWithAnnotationsApp!");

        new RedefineMethodWithAnnotationsApp().doTest();

        System.exit(0);
    }

    private void doTest() throws Exception {
        doMethodParameterAnnotationsTest();
    }

    private void doMethodParameterAnnotationsTest() throws Exception {
        verifyMethodParameterAnnotationsValue(
                ParameterAnnotation.STRING_VALUE_1);

        doRedefine(RedefineMethodWithAnnotationsTarget.class);

        verifyMethodParameterAnnotationsValue(
                ParameterAnnotation.STRING_VALUE_2);
    }

    private static void verifyMethodParameterAnnotationsValue(
            String expectedValue) throws Exception {
        Class<RedefineMethodWithAnnotationsTarget> c =
                RedefineMethodWithAnnotationsTarget.class;
        Method method = c.getMethod("annotatedMethod", String.class);

        Annotation [][] parametersAnnotations =
                method.getParameterAnnotations();
        if (parametersAnnotations.length != 1) {
            throw new Exception("Incorrect number of parameters to method: " +
                    method.getName() + "." +
                    " Expected: 1," +
                    " got: " + parametersAnnotations.length);
        }

        Annotation[] parameterAnnotations = parametersAnnotations[0];
        if (parameterAnnotations.length != 1) {
            throw new Exception("Incorrect number of annotations." +
                    " Expected: 1" +
                    ", got " + parameterAnnotations.length);
        }

        Annotation parameterAnnotation = parameterAnnotations[0];
        if (!(parameterAnnotation instanceof ParameterAnnotation)) {
            throw new Exception("Incorrect Annotation class." +
                    " Expected: " + ParameterAnnotation.class.getName() +
                    ", got: " + parameterAnnotation.getClass().getName());
        }

        ParameterAnnotation pa = (ParameterAnnotation)parameterAnnotation;
        String annotationValue = pa.value();
        if (!expectedValue.equals(annotationValue)) {
            throw new Exception("Incorrect parameter annotation value." +
                    " Expected: " + expectedValue +
                    ", got: " + annotationValue);
        }
    }

    private static void doRedefine(Class<?> clazz) throws Exception {
        // Load the second version of this class.
        File f = new File(clazz.getName() + ".class");
        System.out.println("Reading test class from " + f);
        InputStream redefineStream = new FileInputStream(f);

        byte[] redefineBuffer = NamedBuffer.loadBufferFromStream(redefineStream);

        ClassDefinition redefineParamBlock = new ClassDefinition(
                clazz, redefineBuffer);

        RedefineMethodWithAnnotationsAgent.getInstrumentation().redefineClasses(
                new ClassDefinition[] {redefineParamBlock});
    }
}
