/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester;

// all unary and binary operator kinds
public enum OperatorKind {
    /** a += b */
    COMPOUND_ADD(1),
    /** a -= b */
    COMPOUND_SUB(1),
    /** a *= b */
    COMPOUND_MUL(1),
    /** a /= b */
    COMPOUND_DIV(1),
    /** a %= b */
    COMPOUND_MOD(1),
    /** a &= b */
    COMPOUND_AND(1),
    /** a |= b */
    COMPOUND_OR (1),
    /** a ^= b */
    COMPOUND_XOR(1),
    /** a >>= b */
    COMPOUND_SHR(1),
    /** a <<= b */
    COMPOUND_SHL(1),
    /** a >>>= b */
    COMPOUND_SAR(1),
    /** a = b */
    ASSIGN      (1),
    /**  a || b */
    OR          (3),
    /** a | b */
    BIT_OR      (5),
    /** a ^ b */
    BIT_XOR     (6),
    /** a && b */
    AND         (7),
    /** a & b */
    BIT_AND     (7),
    /** a == b */
    EQ          (8),
    /** a != b */
    NE          (8),
    /** a > b */
    GT          (9),
    /** a < b */
    LT          (9),
    /** a >= b */
    GE          (9),
    /** a <= b */
    LE          (9),
    /** a >> b */
    SHR         (10),
    /** a << b */
    SHL         (10),
    /** a >>> b */
    SAR         (10),
    /** a + b */
    ADD         (11),
    /** a.toString() + b */
    STRADD      (11),
    /** a - b */
    SUB         (11),
    /** a * b */
    MUL         (12),
    /** a / b */
    DIV         (12),
    /** a % b */
    MOD         (12),
    /** !a */
    NOT         (14),
    /** ~a */
    BIT_NOT     (14),
    /** +a */
    UNARY_PLUS  (14),
    /** -a */
    UNARY_MINUS (14),
    /** --a */
    PRE_DEC     (15, true),
    /** a-- */
    POST_DEC    (15, false),
    /** ++a */
    PRE_INC     (16, true),
    /** a++ */
    POST_INC    (16, false),
    ;

    public final int priority;
    public final boolean isPrefix; // used for unary operators

    private OperatorKind(int priority) {
        this(priority, true);
    }

    private OperatorKind(int priority, boolean isPrefix) {
        this.priority = priority;
        this.isPrefix = isPrefix;
    }
}
