/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 7088020
 * @summary SEGV in JNIHandleBlock::release_block
 *
 * @run main compiler.runtime.Test7088020
 */

package compiler.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;

public class Test7088020 {
    public static boolean test() {
        return false;
    }

    public static void main(String... args) throws Throwable {
        MethodHandle test = MethodHandles.lookup().findStatic(Test7088020.class, "test",  MethodType.methodType(Boolean.TYPE));

        // Exercise WMT with different argument alignments
        int thrown = 0;
        try {
            test.invokeExact(0);
        } catch (WrongMethodTypeException wmt) {
            thrown++;
            if (wmt.getStackTrace().length < 1) throw new InternalError("missing stack frames");
        }
        try {
            test.invokeExact(0, 1);
        } catch (WrongMethodTypeException wmt) {
            thrown++;
            if (wmt.getStackTrace().length < 1) throw new InternalError("missing stack frames");
        }
        try {
            test.invokeExact(0, 1, 2);
        } catch (WrongMethodTypeException wmt) {
            thrown++;
            if (wmt.getStackTrace().length < 1) throw new InternalError("missing stack frames");
        }
        try {
            test.invokeExact(0, 1, 2, 3);
        } catch (WrongMethodTypeException wmt) {
            thrown++;
            if (wmt.getStackTrace().length < 1) throw new InternalError("missing stack frames");
        }
        try {
            thrown++;
            test.invokeExact(0, 1, 2, 3, 4);
        } catch (WrongMethodTypeException wmt) {
            if (wmt.getStackTrace().length < 1) throw new InternalError("missing stack frames");
        }
        if (thrown != 5) {
            throw new InternalError("not enough throws");
        }
    }
}
