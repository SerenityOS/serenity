/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This is a CallSite, which constructor can be used as bootstrap method (via REF_newInvokeSpecial reference kind).
 *
 * This call site always calls MethodHandle set with {@link #setMH(MethodHandle)} method (no lookup by method name/type is performed!)
 * <p>Since we can't extend the java.lang.invoke.CallSite from package other than java.lang.invoke, we use system package name
 * for this class.
 */
public final class NewInvokeSpecialCallSite extends CallSite {

    private static MethodHandle mh;

    /**
     * Sets method handle, which will be used for CallSite target
     */
    public static void setMH(MethodHandle newMH) {
        mh = newMH;
    }

    /**
     * Constructs a CallSite. This constructor has special signature, which can be used for bootstrap method target
     * of REF_newInvokeSpecial reference kind.
     * @param lookup Ignored.
     * @param name Ignored.
     * @param type Ignored.
     */
    public NewInvokeSpecialCallSite(MethodHandles.Lookup lookup, String name, MethodType type) {
        super(mh);
    }

    /**
     * This method is no-op. Use {@link #setMH(MethodHandle)} for setting the target
     */
    public final void setTarget(MethodHandle newMH) {
        // No-op
    }

    /**
     * Always returns method handle set with {@link #setMH(MethodHandle)} method
     */
    public final MethodHandle getTarget() {
        return mh;
    }

    public final MethodHandle dynamicInvoker() {
        return makeDynamicInvoker();
    }
}
