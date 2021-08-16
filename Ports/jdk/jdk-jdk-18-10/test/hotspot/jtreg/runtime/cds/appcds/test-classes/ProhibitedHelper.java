/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

public class ProhibitedHelper {
    public static void main(String[] args) throws Throwable {

        String className = "java.lang.Prohibited";
        ClassLoader sysLoader = ClassLoader.getSystemClassLoader();
        try {
            Module unnamedModule = sysLoader.getUnnamedModule();
            Class cls = Class.forName(unnamedModule, className);
            System.out.println("cls " + cls);
            throw new java.lang.RuntimeException(className +
                "in a prohibited package shouldn't be loaded");
        } catch (Exception e) {
            e.printStackTrace();
            if (!(e instanceof java.lang.SecurityException)) {
                throw new java.lang.RuntimeException(
                    "SecurityException is expected to be thrown while loading " + className);
            }
        }

        try {
            Class cls = Class.forName(className, false, sysLoader);
            System.out.println("cls " + cls);
            throw new java.lang.RuntimeException(className +
                "in a prohibited package shouldn't be loaded");
        } catch (Exception e) {
            e.printStackTrace();
            if (!(e instanceof java.lang.ClassNotFoundException)) {
                throw new java.lang.RuntimeException(
                    "ClassNotFoundException is expected to be thrown while loading " + className);
            }
        }
    }
}
