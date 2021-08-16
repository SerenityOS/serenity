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
 *
 */

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class TestMHApp {
    public static void main(String args[]) throws Exception {
        try {
            Class<?> testClass = Class.forName(args[0]);
            System.out.println(testClass);
            Object obj = testClass.newInstance();
            final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(testClass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                //System.out.println(method.toString());
                method.setAccessible(true);
                Annotation[] annotations = null;
                try {
                    annotations = method.getDeclaredAnnotations();
                } catch (Throwable th) {
                    System.out.println("skipping method");
                    continue;
                }
                boolean isTest = false;
                for (Annotation annotation : annotations) {
                    String annotationString = annotation.toString();
                    System.out.println("     annotation: " + annotationString);
                    if (annotationString.startsWith("@org.junit.Test")) {
                        isTest = true;
                    }
                }
                if (isTest) {
                    System.out.println("    invoking method: " + method.getName());
                    try {
                        method.invoke(obj);
                    } catch (IllegalAccessException iae) {
                        System.out.println("Got IllegalAccessException!!!");
                        System.out.println(iae.getCause());
                    } catch (InvocationTargetException ite) {
                        System.out.println("Got InvocationTargetException!!!");
                        //System.out.println(ite.getCause());
                        throw ite;
                    }
               }
            }
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Class not found: " + args[0]);
        } catch (java.lang.IllegalAccessError iae) {
            System.out.println("Skipping test: " + args[0]);
        }
    }
}
