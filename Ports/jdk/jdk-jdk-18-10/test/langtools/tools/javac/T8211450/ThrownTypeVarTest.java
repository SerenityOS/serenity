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
 * @bug 8211450
 * @summary UndetVar::dup is not copying the kind field to the duplicated instance
 * @compile ThrownTypeVarTest.java
 */

import java.io.*;

public class ThrownTypeVarTest {
    void repro() throws IOException {
        when(f(any()));
    }

    interface MyInt<T1, E1 extends Exception> {}

    <T2, E2 extends Exception> T2 f(MyInt<T2, E2> g) throws IOException, E2 {
        return null;
    }

    static <T3> T3 any() {
        return null;
    }

    static <T4> void when(T4 methodCall) {}
}
