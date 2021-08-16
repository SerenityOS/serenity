/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.calls.common;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A test class checking InvokeDynamic instruction.
 * This is not quite "ready-to-use" class, since javac can't generate indy
 * directly(only as part of lambda init) so, this class bytecode should be
 * patched with method "caller" which uses indy. Other methods can be written in
 * java for easier support and readability.
 */

public class InvokeDynamic extends CallsBase {
    private static final Object LOCK = new Object();

    public static void main(String args[]) {
        new InvokeDynamic().runTest(args);
    }

    /**
     * Caller method to call "callee" method. Must be overwritten with InvokeDynamicPatcher
     */
    @Override
    public void caller() {
    }

    /**
     * A bootstrap method for invokedynamic
     * @param lookup a lookup object
     * @param methodName methodName
     * @param type method type
     * @return CallSite for method
     */
    public static CallSite bootstrapMethod(MethodHandles.Lookup lookup,
            String methodName, MethodType type) throws IllegalAccessException,
            NoSuchMethodException {
        MethodType mtype = MethodType.methodType(boolean.class,
                new Class<?>[]{int.class, long.class, float.class,
                    double.class, String.class});
        return new ConstantCallSite(lookup.findVirtual(lookup.lookupClass(),
                methodName, mtype));
    }

    /**
     * A callee method, assumed to be called by "caller"
     */
    public boolean callee(int param1, long param2, float param3, double param4,
            String param5) {
        calleeVisited = true;
        CallsBase.checkValues(param1, param2, param3, param4, param5);
        return true;
    }

    /**
     * A native callee method, assumed to be called by "caller"
     */
    public native boolean calleeNative(int param1, long param2, float param3,
            double param4, String param5);

    /**
     * Returns object to lock execution on
     * @return lock object
     */
    @Override
    protected Object getLockObject() {
        return LOCK;
    }

    @Override
    protected void callerNative() {
        throw new Error("No native call for invokedynamic");
    }
}
