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
 * @summary test equals method
 * @bug 7092764
 */

import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;

public class TransformEqualityTest {

    public static void main(String[] args) {
        AffineTransform tx1 = new AffineTransform(1, 0, 1, 1, 0, 0);
        AffineTransform tx2 = new AffineTransform(1, 0, 1, 1, 0, 0);
        AffineTransform tx3 = new AffineTransform(2, 0, 1, 1, 0, 0);
        TransformAttribute ta1a = new TransformAttribute(tx1);
        TransformAttribute ta1b = new TransformAttribute(tx1);
        TransformAttribute ta2 = new TransformAttribute(tx2);
        TransformAttribute ta3 = new TransformAttribute(tx3);
        if (ta1a.equals(null)) {
            throw new RuntimeException("should not be equal to null.");
        }
        if (!ta1a.equals(ta1a)) {
            throw new RuntimeException("(1) should be equal.");
        }
        if (!ta1a.equals(ta1b)) {
            throw new RuntimeException("(2) should be equal.");
        }
        if (!ta1a.equals(ta2)) {
            throw new RuntimeException("(3) should be equal.");
        }
        if (ta2.equals(ta3)) {
            throw new RuntimeException("should be different.");
        }
    }
}

