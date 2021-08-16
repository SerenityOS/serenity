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

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public class LambdaReturn {

    interface I {
        void m();
    }

    static void hereV() {}
    static String hereS() { return "hi"; }
    static MethodHandles.Lookup l;
    private static MethodType mt(Class<?> k) { return MethodType.methodType(k); }
    private static MethodType mt(Class<?> k, Class<?> k2) { return MethodType.methodType(k, k2); }
    private static void amf(List<String> errs, MethodHandle h, MethodType mts, MethodType mtf, MethodType mtb, boolean shouldWork) {
        MethodType mti = mt(I.class);
        try {
            LambdaMetafactory.altMetafactory(l, "m", mti, mts,h,mtf,
                                          LambdaMetafactory.FLAG_BRIDGES, 1, mtb);
        } catch(LambdaConversionException e) {
            if (shouldWork)  errs.add("Error: Should work h=" + h + " s=" + mts + " -- f=" + mtf + " / b=" + mtb + " got: " + e);
            return;
        }
        if (!shouldWork)  errs.add("Error: Should fail h=" + h + " s=" + mts + " -- f=" + mtf + " / b=" + mtb);
    }

    public static void main(String[] args) throws Throwable {
        l = MethodHandles.lookup();
        MethodHandle hV = l.findStatic(LambdaReturn.class, "hereV", mt(void.class));
        MethodHandle hS = l.findStatic(LambdaReturn.class, "hereS", mt(String.class));
        List<String> errs = new ArrayList<>();
        MethodType V = mt(void.class);
        MethodType S = mt(String.class);
        MethodType O = mt(Object.class);
        MethodType I = mt(int.class);
        amf(errs, hS, S, S, O, true);
        amf(errs, hS, S, S, V, false);
        amf(errs, hS, S, S, I, false);
        amf(errs, hS, O, S, S, true);
        amf(errs, hS, V, S, S, false);
        amf(errs, hS, I, S, S, false);
        amf(errs, hS, O, O, S, false);
        amf(errs, hS, S, O, O, false);
        amf(errs, hV, V, V, O, false);
        amf(errs, hV, V, V, I, false);
        amf(errs, hV, V, V, S, false);
        amf(errs, hV, O, V, V, false);
        amf(errs, hV, I, V, V, false);
        amf(errs, hV, S, V, V, false);

        if (errs.size() > 0) {
            for (String err : errs) {
                System.err.println(err);
            }
            throw new AssertionError("Errors: " + errs.size());
        }
    }
}
