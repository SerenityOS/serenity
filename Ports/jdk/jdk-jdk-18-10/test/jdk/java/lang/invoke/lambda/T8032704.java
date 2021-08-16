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
 * @bug 8032704
 * @summary Issues with lib perm in Lambda
 */

import java.io.Closeable;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class T8032704 {

    public static void here() {}
    static MethodHandle h;
    private static MethodType mt(Class<?> k) { return MethodType.methodType(k); }
    private static boolean mf(MethodHandles.Lookup l) {
        try {
            LambdaMetafactory.metafactory(l, "close",
                mt(Closeable.class),mt(void.class),h,mt(void.class));
        } catch(LambdaConversionException e) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup ll = MethodHandles.lookup();
        h = ll.findStatic(T8032704.class, "here", mt(void.class));
        if (mf(ll)) throw new AssertionError("Error: Should work");
        if (!mf(MethodHandles.publicLookup())) throw new AssertionError("Error: Should fail - public");
        if (!mf(ll.in(T8032704other.class))) throw new AssertionError("Error: Should fail - other");
        if (!mf(ll.in(Thread.class))) throw new AssertionError("Error: Should fail - Thread");
    }
}

class T8032704other {}
