/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8035776
 * @summary Consistent Lambda construction
 */

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import LambdaReceiver_anotherpkg.LambdaReceiver_A;

public class LambdaReceiver extends LambdaReceiver_A {

    interface IA {
        int m(LambdaReceiver_A x);
    }

    static MethodHandles.Lookup l;
    static MethodHandle h;
    private static MethodType mt(Class<?> k) { return MethodType.methodType(k); }
    private static MethodType mt(Class<?> k, Class<?> k2) { return MethodType.methodType(k, k2); }
    private static void mf(List<String> errs, MethodType mts, MethodType mtf, boolean shouldWork) {
    }

    public static void main(String[] args) throws Throwable {
        l = MethodHandles.lookup();
        h = l.findVirtual(LambdaReceiver_A.class, "f", mt(int.class));
        MethodType X = mt(int.class, LambdaReceiver.class);
        MethodType A = mt(int.class, LambdaReceiver_A.class);
        MethodType mti = mt(IA.class);
        CallSite cs = LambdaMetafactory.metafactory(l, "m", mti,A,h,X);
        IA p = (IA)cs.dynamicInvoker().invoke();
        LambdaReceiver_A lra = new LambdaReceiver_A();
        try {
            p.m(lra);
        } catch (ClassCastException cce) {
            return;
        }
        throw new AssertionError("CCE expected");
    }
}
