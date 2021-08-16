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

package test;

/**
 * @test
 * @bug 8021112
 * @summary Verify that \\@SuppressWarnings work even if the value is defined
 *          inside the suppressed class itself, and verify that "unnecessary cast"
 *          lint can be properly suppressed.
 * @compile -Xlint:cast -Werror T8021112a.java
 */

import static test.T8021112a.D;

@SuppressWarnings(D)
public class T8021112a {
    public static final String D = (String) "cast";
}

class Other {
    public static final String D = "cast";
    @SuppressWarnings(D)
    public static final String D2 = (String) "cast";
}
