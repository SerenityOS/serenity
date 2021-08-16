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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is used in RuntimeTest.java.
 */
public class UseByReflection {

    /**
     * Method for the test execution.
     *
     * @param args - no args needed
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, IOException {
        Class mainClass = Class.forName("testpackage.Main");
        Method getMainVersion = mainClass.getMethod("getMainVersion");
        int mainVersionActual = (int) getMainVersion.invoke(null);
        Method getHelperVersion = mainClass.getMethod("getHelperVersion");
        int helperVersionActual = (int) getHelperVersion.invoke(null);
        ClassLoader cl = UseByReflection.class.getClassLoader();
        int resourceVersionActual;
        try (InputStream ris = cl.getResourceAsStream("versionResource");
                BufferedReader br = new BufferedReader(new InputStreamReader(ris))) {
            resourceVersionActual = Integer.parseInt(br.readLine());
        }
        System.out.println("Main version: " + mainVersionActual);
        System.out.println("Helpers version: " + helperVersionActual);
        System.out.println("Resource version: " + resourceVersionActual);
    }
}
