/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

/**
 * Here are two relative links in a class:
 * <a href="relative-class-link.html">relative class link</a>,
 * <a href="#class-fragment">fragment class link</a>.
 *
 * <a id="class-fragment">Class fragment</a>.
 */
public class C {

    /**
     * Here is a relative link in a field:\u0130
     * <a href="relative-field-link.html">relative field link</a>.
     */
    public C field = null;

    /**
     * Here are two relative links in a method:
     * <a href="relative-method-link.html">relative method link</a>,
     * <a href="#method-fragment">fragment method link</a>.
     */
    public C method() { return null;}

    /**
     * Here is a relative link in a method:
     * <a
     * href="relative-multi-line-link.html">relative-multi-line-link</a>.
     *
     * <a id="method-fragment">Method fragment</a>.
     */
    public C multipleLineTest() { return null;}

    /**
     * <a id="masters"></a>
     * Something that goes holy cow. Second line.
     */
    public static class WithAnAnchor{}

}
