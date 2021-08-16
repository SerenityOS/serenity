/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.java2d.marlin;

final class PathSimplifier implements DPathConsumer2D {

    // distance threshold in pixels (device)
    private static final double PIX_THRESHOLD = MarlinProperties.getPathSimplifierPixelTolerance();

    private static final double SQUARE_TOLERANCE = PIX_THRESHOLD * PIX_THRESHOLD;

    // members:
    private DPathConsumer2D delegate;
    private double cx, cy;

    PathSimplifier() {
    }

    PathSimplifier init(final DPathConsumer2D delegate) {
        this.delegate = delegate;
        return this; // fluent API
    }

    @Override
    public void pathDone() {
        delegate.pathDone();
    }

    @Override
    public void closePath() {
        delegate.closePath();
    }

    @Override
    public long getNativeConsumer() {
        return 0;
    }

    @Override
    public void quadTo(final double x1, final double y1,
                       final double xe, final double ye)
    {
        // Test if curve is too small:
        double dx = (xe - cx);
        double dy = (ye - cy);

        if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
            // check control points P1:
            dx = (x1 - cx);
            dy = (y1 - cy);

            if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
                return;
            }
        }
        delegate.quadTo(x1, y1, xe, ye);
        // final end point:
        cx = xe;
        cy = ye;
    }

    @Override
    public void curveTo(final double x1, final double y1,
                        final double x2, final double y2,
                        final double xe, final double ye)
    {
        // Test if curve is too small:
        double dx = (xe - cx);
        double dy = (ye - cy);

        if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
            // check control points P1:
            dx = (x1 - cx);
            dy = (y1 - cy);

            if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
                // check control points P2:
                dx = (x2 - cx);
                dy = (y2 - cy);

                if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
                    return;
                }
            }
        }
        delegate.curveTo(x1, y1, x2, y2, xe, ye);
        // final end point:
        cx = xe;
        cy = ye;
    }

    @Override
    public void moveTo(final double xe, final double ye) {
        delegate.moveTo(xe, ye);
        // starting point:
        cx = xe;
        cy = ye;
    }

    @Override
    public void lineTo(final double xe, final double ye) {
        // Test if segment is too small:
        double dx = (xe - cx);
        double dy = (ye - cy);

        if ((dx * dx + dy * dy) <= SQUARE_TOLERANCE) {
            return;
        }
        delegate.lineTo(xe, ye);
        // final end point:
        cx = xe;
        cy = ye;
    }
}
