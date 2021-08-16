/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.SetNativeMethodPrefix;

public class Binder {
    static { System.loadLibrary("SetNativeMethodPrefix001"); }

    // Function index in the table from SetNativeMethodPrefix1.c
    public final static int FUNCTION_FOO = 0;
    public final static int FUNCTION_WRAPPED_FOO = 1;

    // Return values from corresponding native functions
    static public final int FOO_RETURN = 1;
    static public final int WRAPPED_FOO_RETURN = 2;

    native static public boolean setMethodPrefix (String prefix);
    native static public boolean setMultiplePrefixes (String prefix);
    native static public boolean registerMethod (Class klass, String methodName, String methodSig, int i);
}
