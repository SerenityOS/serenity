/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.geom.GeneralPath;

import static java.awt.geom.Path2D.WIND_EVEN_ODD;
import static java.awt.geom.Path2D.WIND_NON_ZERO;

/*
 * @test
 * @bug 6606673
 * @summary The test checks if correct exceptions are thrown by the constructors
 */
public final class GeneralPathExceptions {

    public static void main(String[] args) {
        try {
            new GeneralPath(null);
            throw new RuntimeException("NullPointerException is expected");
        } catch (NullPointerException ignore) {
            // expected
        }

        try {
            new GeneralPath(-1);
            throw new RuntimeException("IllegalArgumentException is expected");
        } catch (IllegalArgumentException ignore) {
            // expected
        }
        try {
            new GeneralPath(-1, 0);
            throw new RuntimeException("IllegalArgumentException is expected");
        } catch (IllegalArgumentException ignore) {
            // expected
        }

        try {
            new GeneralPath(WIND_EVEN_ODD, -1);
            throw new RuntimeException("NegativeArraySizeException is expected");
        } catch (NegativeArraySizeException ignore) {
            // expected
        }
        try {
            new GeneralPath(WIND_NON_ZERO, -1);
            throw new RuntimeException("NegativeArraySizeException is expected");
        } catch (NegativeArraySizeException ignore) {
            // expected
        }
    }
}
