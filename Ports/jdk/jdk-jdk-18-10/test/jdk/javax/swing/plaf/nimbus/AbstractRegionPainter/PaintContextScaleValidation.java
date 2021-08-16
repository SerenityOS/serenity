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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.plaf.nimbus.AbstractRegionPainter;

/**
 * @test
 * @bug 8134256
 */
public final class PaintContextScaleValidation extends AbstractRegionPainter {

    public static void main(final String[] args) {
        final PaintContextScaleValidation t = new PaintContextScaleValidation();
        t.test(0, 0);
        t.test(0, 1);
        t.test(1, 0);
    }

    private void test(final double maxH, final double maxV) {
        try {
            new PaintContext(new Insets(1, 1, 1, 1), new Dimension(1, 1), false,
                             null, maxH, maxV);
        } catch (final IllegalArgumentException ignored) {
            return; // expected exception
        }
        throw new RuntimeException("IllegalArgumentException was not thrown");
    }

    @Override
    protected PaintContext getPaintContext() {
        return null;
    }

    @Override
    protected void doPaint(final Graphics2D g, final JComponent c,
                           final int width, final int height,
                           final Object[] extendedCacheKeys) {
    }
}
