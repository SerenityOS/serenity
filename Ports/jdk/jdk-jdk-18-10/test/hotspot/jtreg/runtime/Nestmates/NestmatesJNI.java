/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Utility class for invoking methods and constructors and accessing fields
 * via JNI.
 */
public class NestmatesJNI {

    static {
        System.loadLibrary("NestmatesJNI");
    }

    public static native void callVoidVoid(Object target, String definingClassName, String methodName, boolean virtual);

    public static native String callStringVoid(Object target, String definingClassName, String methodName, boolean virtual);

    public static native void callStaticVoidVoid(String definingClassName, String methodName);

    public static Object newInstance(String definingClassName, String sig, Object outerThis) {
        return newInstance0(definingClassName, "<init>", sig, outerThis);
    }

    private static native Object newInstance0(String definingClassName, String method_name, String sig, Object outerThis);

    public static native int getIntField(Object target, String definingClassName, String fieldName);

    public static native void setIntField(Object target, String definingClassName, String fieldName, int newVal);

    public static native int getStaticIntField(String definingClassName, String fieldName);

    public static native void setStaticIntField(String definingClassName, String fieldName, int newVal);

}
