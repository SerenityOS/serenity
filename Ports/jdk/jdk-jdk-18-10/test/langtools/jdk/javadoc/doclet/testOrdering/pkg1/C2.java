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

package pkg1;

public class C2 {

    /**
     * Field in C2.
     */
    public UsedClass fieldInC2;

    /**
     * another field
     */
    public C1 field = null;

    /**
     * A duplicated field
     */
    public UsedClass zfield;

    /**
     * Method in C2.
     * @return C1
     */
    public C1 methodInC2() {return null;}

    /**
     * @param c1 a param
     */
    public void method(pkg1.C1 c1) {}

    /**
     * Method in C2.
     * @param p a param
     * @return UsedClass
     */
    public UsedClass methodInC2(UsedClass p) {return p;}

    /**
     * A duplicated method to test ordering
     * @param p a param
     * @return UsedClass
     */
    public UsedClass zmethod(UsedClass p) {
        return p;
    }
}
