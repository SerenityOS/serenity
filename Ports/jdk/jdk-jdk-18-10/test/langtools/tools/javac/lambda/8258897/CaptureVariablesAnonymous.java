/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8258897
 * @summary Checks translation of capturing local classes inside nested lambdas
 * @run main CaptureVariablesAnonymous
 */

import java.util.function.Supplier;

public class CaptureVariablesAnonymous {
    static Supplier<Integer> supplier1 = () -> {
        boolean b0 = false;
        int i0 = 6;
        boolean b1 = false;
        String s0 = "hello";

        class Local {
            int i = s0.length() + i0;
        }

        return ((Supplier<Integer>) () -> new Local() {}.i).get();
    };

    static Supplier<Integer> supplier2 = () -> {
        boolean b0 = false;
        int i0 = 6;
        boolean b1 = false;
        String s0 = "hello";

        class Local {
            int i = s0.length() + i0;
        }

        return ((Supplier<Integer>) () -> ((Supplier<Integer>) () -> new Local() {}.i).get()).get();
    };

    Supplier<Integer> supplier3 = () -> {
        boolean b0 = false;
        int i0 = 6;
        boolean b1 = false;
        String s0 = "hello";

        class Local {
            int i = s0.length() + i0;
        }

        return ((Supplier<Integer>) () -> new Local() {}.i).get();
    };

    Supplier<Integer> supplier4 = () -> {
        boolean b0 = false;
        int i0 = 6;
        boolean b1 = false;
        String s0 = "hello";

        class Local {
            int i = s0.length() + i0;
        }

        return ((Supplier<Integer>) () -> ((Supplier<Integer>) () -> new Local() {}.i).get()).get();
    };

    public static void main(String[] args) {
        assert supplier1.get() == 11;
        assert supplier2.get() == 11;
        assert new CaptureVariablesAnonymous().supplier3.get() == 11;
        assert new CaptureVariablesAnonymous().supplier4.get() == 11;
    }
}
