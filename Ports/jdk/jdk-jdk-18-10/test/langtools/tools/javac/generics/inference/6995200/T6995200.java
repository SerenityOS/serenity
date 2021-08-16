/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6995200
 *
 * @summary JDK 7 compiler crashes when type-variable is inferred from expected primitive type
 * @author mcimadamore
 * @compile T6995200.java
 *
 */

import java.util.List;

class T6995200 {
    static <T> T getValue() {
        return null;
    }

    <X> void test() {
        byte v1 = getValue();
        short v2 = getValue();
        int v3 = getValue();
        long v4 = getValue();
        float v5 = getValue();
        double v6 = getValue();
        String v7 = getValue();
        String[] v8 = getValue();
        List<String> v9 = getValue();
        List<String>[] v10 = getValue();
        List<? extends String> v11 = getValue();
        List<? extends String>[] v12 = getValue();
        List<? super String> v13 = getValue();
        List<? super String>[] v14 = getValue();
        List<?> v15 = getValue();
        List<?>[] v16 = getValue();
        X v17 = getValue();
        X[] v18 = getValue();
        List<X> v19 = getValue();
        List<X>[] v20 = getValue();
        List<? extends X> v21 = getValue();
        List<? extends X>[] v22 = getValue();
        List<? super X> v23 = getValue();
        List<? super X>[] v24 = getValue();
    }
}
