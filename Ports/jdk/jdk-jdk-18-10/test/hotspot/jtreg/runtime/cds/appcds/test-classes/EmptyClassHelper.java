/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.*;

import java.lang.reflect.Method;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

class EmptyClassHelper {
    static final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
    static final String USE_APP = "useAppLoader";
    public static void main(String[] args) throws Exception {
        Class cls = null;
        Method m = null;
        ClassLoader appLoader = ClassLoader.getSystemClassLoader();
        String className = "com.sun.tools.javac.Main";
        if (args[0].equals(USE_APP)) {
            cls = appLoader.loadClass(className);
            System.out.println("appLoader loaded class");
            try {
                m = cls.getMethod("main", String[].class);
                System.out.println("appLoader found method main");
            } catch(NoSuchMethodException ex) {
                System.out.println(ex.toString());
            }
        } else {
            cls = jla.findBootstrapClassOrNull(className);
            System.out.println("bootLoader loaded class");
            System.out.println("cls = " + cls);
            try {
                m = cls.getMethod("main", String[].class);
                System.out.println("bootLoader found method main");
            } catch(NoSuchMethodException ex) {
                System.out.println(ex.toString());
            }
        }
    }
}
