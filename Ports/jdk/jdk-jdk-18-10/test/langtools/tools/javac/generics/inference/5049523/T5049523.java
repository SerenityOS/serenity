/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5049523
 * @summary Inference should compute glb of type arguments
 * @compile T5049523.java
 */

abstract public class T5049523 {
    abstract <T> T choose(boolean b, T t1, T t2);
    abstract <T> Class<? extends T> m(Class<T> c);

    <T> void test(Class<? extends T> clazz) {
        boolean b = clazz.isInterface();
        Class<? extends T> c1 = b ? m(clazz) : clazz;
        Class<? extends T> c2 = choose(b, m(clazz), clazz);
    }
}
