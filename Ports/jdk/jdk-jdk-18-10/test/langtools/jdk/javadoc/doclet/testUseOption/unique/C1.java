/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package unique;

/**
 * A class to ensure we get unique entries in class-use for each of these,
 * so make sure each method is uniquely named to avoid false positives
 */

public class C1 {

    /**
     * Ctor C1 to test uniqueness
     * @param a param
     * @param b param
     */
    public C1(UseMe a, UseMe b){}

    /**
     * umethod1 to test uniqueness
     * @param one param
     * @param uarray param
     */
    public void umethod1(UseMe<?> one, UseMe<?>[] uarray){}

    /**
     * umethod1 to test uniqueness
     * @param one param
     * @param two param
     */
    public void umethod2(UseMe<?> one, UseMe<?> two){}

    /**
     * umethod1 to test uniqueness
     * @param a param
     * @param b param
     */
    public void umethod3(UseMe a, UseMe b){}
}
