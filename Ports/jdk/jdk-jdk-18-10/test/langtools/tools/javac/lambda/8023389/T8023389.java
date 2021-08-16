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
 * @bug 8023389
 * @summary Javac fails to infer type for lambda used with intersection type and wildcards
 * @compile T8023389.java
 */
public class T8023389 {

    static class U1 {}
    static class X1 extends U1 {}

    interface I { }

    interface SAM<T> {
        void m(T t);
    }

    /* Strictly speaking only the second of the following declarations provokes the bug.
     * But the first line is also a useful test case.
     */
    SAM<? extends U1> sam1 = (SAM<? extends U1>) (X1 x) -> { };
    SAM<? extends U1> sam2 = (SAM<? extends U1> & I) (X1 x) -> { };
}
