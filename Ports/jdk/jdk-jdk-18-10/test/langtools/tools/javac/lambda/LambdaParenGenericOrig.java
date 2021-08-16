/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8029558
 * @summary VerifyError lambda body is parenthesized generic value (originally submitted test)
 * @author  Dmitrii Afanasyev
 * @run main LambdaParenGenericOrig
 */

public class LambdaParenGenericOrig {

    @FunctionalInterface
    public static interface Function1<R, A> {
        R apply(A input);
    }

    @FunctionalInterface
    public static interface Function2<R, A1, A2> {
        R apply(A1 input1, A2 input2);
    }

    public static void main(String[] args) {
        final Function2<Integer, Integer, Integer> add = (x, y) -> x + y;
        final Function1<Integer, Integer> inc = x -> (add.apply(x, 1));
        System.out.println(inc.apply(0));
    }
}
