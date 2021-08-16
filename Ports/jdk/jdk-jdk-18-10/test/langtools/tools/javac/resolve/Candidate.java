/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@interface Candidate {
    /**
     * the candidate position (line/col of the method call for which this candidate
     * is a potential overload candidate)
     */
    Pos pos() default @Pos(userDefined=false);
    /**
     * resolution phases for which this candidate is applicable
     */
    Phase[] applicable() default { };
    /**
     * is this candidate the most specific (in the resolution phases for which it
     * is also applicable)
     */
    boolean mostSpecific() default false;
    /**
     * this candidate inferred signature (in the resolution phases for which it
     * is also applicable, in case it corresponds to a generic method)
     */
    String sig() default "";
}

enum Phase {
    BASIC("BASIC"),
    BOX("BOX"),
    VARARGS("VARARITY");

    final String javacString;

    private Phase(String javacString) {
        this.javacString = javacString;
    }

    static Phase fromString(String s) {
        for (Phase phase : Phase.values()) {
            if (phase.javacString.equals(s)) {
                return phase;
            }
        }
        throw new AssertionError("Invalid resolution phase string " + s);
    }
}
