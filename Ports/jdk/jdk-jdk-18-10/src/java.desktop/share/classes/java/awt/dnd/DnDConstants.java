/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.awt.dnd;

import java.lang.annotation.Native;

/**
 * This class contains constant values representing
 * the type of action(s) to be performed by a Drag and Drop operation.
 * @since 1.2
 */
public final class DnDConstants {

    private DnDConstants() {} // define null private constructor.

    /**
     * An {@code int} representing no action.
     */
    @Native public static final int ACTION_NONE         = 0x0;

    /**
     * An {@code int} representing a "copy" action.
     */
    @Native public static final int ACTION_COPY         = 0x1;

    /**
     * An {@code int} representing a "move" action.
     */
    @Native public static final int ACTION_MOVE         = 0x2;

    /**
     * An {@code int} representing a "copy" or
     * "move" action.
     */
    @Native public static final int ACTION_COPY_OR_MOVE = ACTION_COPY | ACTION_MOVE;

    /**
     * An {@code int} representing a "link" action.
     *
     * The link verb is found in many, if not all native DnD platforms, and the
     * actual interpretation of LINK semantics is both platform
     * and application dependent. Broadly speaking, the
     * semantic is "do not copy, or move the operand, but create a reference
     * to it". Defining the meaning of "reference" is where ambiguity is
     * introduced.
     *
     * The verb is provided for completeness, but its use is not recommended
     * for DnD operations between logically distinct applications where
     * misinterpretation of the operations semantics could lead to confusing
     * results for the user.
     */

    @Native public static final int ACTION_LINK         = 0x40000000;

    /**
     * An {@code int} representing a "reference"
     * action (synonym for ACTION_LINK).
     */
    @Native public static final int ACTION_REFERENCE    = ACTION_LINK;

}
