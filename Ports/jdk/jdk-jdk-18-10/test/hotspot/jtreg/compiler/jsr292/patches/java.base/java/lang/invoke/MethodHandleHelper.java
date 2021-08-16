/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

import java.lang.invoke.MethodHandles.Lookup;
import jdk.internal.vm.annotation.DontInline;
import jdk.internal.vm.annotation.ForceInline;

/**
 * Helper class to inject into java.lang.invoke that provides access to
 * package-private methods in this package.
 */
public class MethodHandleHelper {

    private MethodHandleHelper() { }

    public static final Lookup IMPL_LOOKUP = Lookup.IMPL_LOOKUP;
    public static final Class<?> MHN_CALL_SITE_CONTEXT_CLASS
            = MethodHandleNatives.CallSiteContext.class;

    public static void customize(MethodHandle mh) {
        mh.customize();
    }

    @ForceInline
    public static Object internalMemberName(MethodHandle mh) throws Throwable {
        return mh.internalMemberName();
    }

    @ForceInline
    public static void linkToStatic(float arg, Object name) throws Throwable {
        MethodHandle.linkToStatic(arg, name);
    }

    @ForceInline
    public static void invokeBasicV(MethodHandle mh, float arg) throws Throwable {
        mh.invokeBasic(arg);
    }

    @ForceInline
    public static Object invokeBasicL(MethodHandle mh) throws Throwable {
        return mh.invokeBasic();
    }

    @ForceInline
    public static int invokeBasicI(MethodHandle mh) throws Throwable {
        return (int) mh.invokeBasic();
    }

    public static MethodHandle varargsArray(int nargs) {
        return MethodHandleImpl.varargsArray(nargs);
    }

    public static MethodHandle varargsArray(Class<?> arrayType, int nargs) {
        return MethodHandleImpl.varargsArray(arrayType, nargs);
    }

    public static LambdaForm getLambdaForm(MethodHandle mh) {
        return mh.form;
    }
}
