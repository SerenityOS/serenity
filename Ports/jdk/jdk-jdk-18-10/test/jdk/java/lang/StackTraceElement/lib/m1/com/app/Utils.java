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

package com.app;

import java.lang.StackWalker.StackFrame;
import java.lang.module.ModuleDescriptor;
import java.util.Objects;

public class Utils {
    public static void verify(Class<?> caller, String loaderName,
                              String methodname, String filename) {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        StackWalker.StackFrame[] frames = new StackFrame[] {
            makeStackFrame(Utils.class, "verify", "Utils.java"),
            makeStackFrame(caller, methodname, filename)
        };

        checkFrame("app", frames[0], stes[1]);
        checkFrame(loaderName, frames[1], stes[2]);
    }

    public static StackFrame makeStackFrame(Class<?> c, String methodname, String filename) {
        return new StackFrame() {
            @Override
            public String getClassName() {
                return c.getName();
            }
            @Override
            public String getMethodName() {
                return methodname;
            }
            @Override
            public Class<?> getDeclaringClass() {
                return c;
            }
            @Override
            public int getByteCodeIndex() {
                return 0;
            }
            @Override
            public String getFileName() {
                return filename;
            }

            @Override
            public int getLineNumber() {
                return 0;
            }
            @Override
            public boolean isNativeMethod() {
                return false;
            }
            @Override
            public StackTraceElement toStackTraceElement() {
                return null;
            }

            private String getClassLoaderName(Class<?> c) {
                ClassLoader loader = c.getClassLoader();
                String name = "";
                if (loader == null) {
                    name = "boot";
                } else if (loader.getName() != null) {
                    name = loader.getName();
                }
                return name;
            }

            @Override
            public String toString() {
                String mid = getClassLoaderName(c);
                Module module = c.getModule();
                if (module.isNamed()) {
                    ModuleDescriptor md = module.getDescriptor();
                    mid = md.name();
                    if (md.version().isPresent())
                        mid += "@" + md.version().get().toString();
                    mid += "/";
                }
                String fileName = getFileName();
                int lineNumber = getLineNumber();
                String sourceinfo = "Unknown Source";
                if (isNativeMethod()) {
                    sourceinfo = "Native Method";
                } else if (fileName != null && lineNumber >= 0) {
                    sourceinfo = fileName + ":" + lineNumber;
                }
                return String.format("%s/%s.%s(%s)", mid, getClassName(), getMethodName(),
                                     sourceinfo);

            }
        };
    }

    public static void checkFrame(String loaderName, StackFrame frame,
                                  StackTraceElement ste) {
        System.err.println("checking " + ste.toString() + " expected: " + frame.toString());
        Class<?> c = frame.getDeclaringClass();
        Module module = c.getModule();
        assertEquals(ste.getModuleName(), module.getName(), "module name");
        assertEquals(ste.getClassLoaderName(), loaderName, "class loader name");
        assertEquals(ste.getClassLoaderName(), c.getClassLoader().getName(),
                     "class loader name");
        assertEquals(ste.getClassName(), c.getName(), "class name");
        assertEquals(ste.getMethodName(), frame.getMethodName(), "method name");
        assertEquals(ste.getFileName(), frame.getFileName(), "file name");

    }
    private static void assertEquals(String actual, String expected, String msg) {
        if (!Objects.equals(actual, expected))
            throw new AssertionError("Actual: " + actual + " Excepted: " +
                expected + " mismatched " + msg);
    }
}
