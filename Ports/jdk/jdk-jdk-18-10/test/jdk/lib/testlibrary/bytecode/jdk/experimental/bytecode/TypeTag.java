/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

public enum TypeTag implements Type {
    /**
     * byte
     */
    B("B", 0, 1, 8),
    /**
     * short
     */
    S("S", 0, 1, 9),
    /**
     * int
     */
    I("I", 0, 1, 10),
    /**
     * float
     */
    F("F", 2, 1, 6),
    /**
     * long
     */
    J("J", 1, 2, 11),
    /**
     * double
     */
    D("D", 3, 2, 7),
    /**
     * Reference type
     */
    A("A", 4, 1, -1),
    /**
     * char
     */
    C("C", 0, 1, 5),
    /**
     * boolean
     */
    Z("Z", 0, 1, 4),
    /**
     * void
     */
    V("V", -1, -1, -1),
    /**
     * Value type
     */
    Q("Q", -1, 1, -1);

    String typeStr;
    int offset;
    int width;
    int newarraycode;

    TypeTag(String typeStr, int offset, int width, int newarraycode) {
        this.typeStr = typeStr;
        this.offset = offset;
        this.width = width;
        this.newarraycode = newarraycode;
    }

    static TypeTag commonSupertype(TypeTag t1, TypeTag t2) {
        if (t1.isIntegral() && t2.isIntegral()) {
            int p1 = t1.ordinal();
            int p2 = t2.ordinal();
            return (p1 <= p2) ? t2 : t1;
        } else {
            return null;
        }
    }

    public int width() {
        return width;
    }

    boolean isIntegral() {
        switch (this) {
            case B:
            case S:
            case I:
                return true;
            default:
                return false;
        }
    }

    @Override
    public TypeTag getTag() {
        return this;
    }
}
