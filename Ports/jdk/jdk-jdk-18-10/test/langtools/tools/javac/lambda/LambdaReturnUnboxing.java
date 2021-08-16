/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8203338
 * @summary Unboxing in return from lambda miscompiled to throw ClassCastException
 */

import java.util.*;

public class LambdaReturnUnboxing {
    interface C {
        Character get(Character c);
    }
    static <T> T c(T t, int... i) { return t; }

    static Character d() { return List.of('d').get(0); }

    public static void main(String... args) {
        List.of('x', 'y').stream().max((a, b) -> List.of(a).get(0));
        List.of('x', 'y').stream().max((a, b) -> { return List.of(a).get(0); });

        C c = LambdaReturnUnboxing::c;
        int i = c.get('c');

        i = d();
    }
}
