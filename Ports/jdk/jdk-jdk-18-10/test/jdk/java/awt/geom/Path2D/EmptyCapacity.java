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

import java.awt.geom.Path2D;

/**
 * @test
 * @bug 8042103
 * @summary Path2D.moveTo() should work if empty initial capacity was set.
 * @author Sergey Bylokhov
 */
public final class EmptyCapacity {

    public static void main(final String[] args) {
        final Path2D path1 = new Path2D.Double(Path2D.WIND_EVEN_ODD, 0);
        path1.moveTo(10, 10);
        path1.lineTo(20, 20);
        final Path2D path2 = new Path2D.Float(Path2D.WIND_EVEN_ODD, 0);
        path2.moveTo(10, 10);
        path2.lineTo(20, 20);
    }
}
