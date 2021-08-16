/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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


final class CollinearSimplifier implements DPathConsumer2D {

    enum SimplifierState {

        Empty, PreviousPoint, PreviousLine
    };
    // slope precision threshold
    static final double EPS = 1e-4d; // aaime proposed 1e-3d

    DPathConsumer2D delegate;
    SimplifierState state;
    double px1, py1, px2, py2;
    double pslope;

    CollinearSimplifier() {
    }

    public CollinearSimplifier init(DPathConsumer2D delegate) {
        this.delegate = delegate;
        this.state = SimplifierState.Empty;

        return this; // fluent API
    }

    @Override
    public void pathDone() {
        emitStashedLine();
        state = SimplifierState.Empty;
        delegate.pathDone();
    }

    @Override
    public void closePath() {
        emitStashedLine();
        state = SimplifierState.Empty;
        delegate.closePath();
    }

    @Override
    public long getNativeConsumer() {
        return 0;
    }

    @Override
    public void quadTo(double x1, double y1, double x2, double y2) {
        emitStashedLine();
        delegate.quadTo(x1, y1, x2, y2);
        // final end point:
        state = SimplifierState.PreviousPoint;
        px1 = x2;
        py1 = y2;
    }

    @Override
    public void curveTo(double x1, double y1, double x2, double y2,
                        double x3, double y3) {
        emitStashedLine();
        delegate.curveTo(x1, y1, x2, y2, x3, y3);
        // final end point:
        state = SimplifierState.PreviousPoint;
        px1 = x3;
        py1 = y3;
    }

    @Override
    public void moveTo(double x, double y) {
        emitStashedLine();
        delegate.moveTo(x, y);
        state = SimplifierState.PreviousPoint;
        px1 = x;
        py1 = y;
    }

    @Override
    public void lineTo(final double x, final double y) {
        switch (state) {
            case Empty:
                delegate.lineTo(x, y);
                state = SimplifierState.PreviousPoint;
                px1 = x;
                py1 = y;
                return;

            case PreviousPoint:
                state = SimplifierState.PreviousLine;
                px2 = x;
                py2 = y;
                pslope = getSlope(px1, py1, x, y);
                return;

            case PreviousLine:
                final double slope = getSlope(px2, py2, x, y);
                // test for collinearity
                if ((slope == pslope) || (Math.abs(pslope - slope) < EPS)) {
                    // merge segments
                    px2 = x;
                    py2 = y;
                    return;
                }
                // emit previous segment
                delegate.lineTo(px2, py2);
                px1 = px2;
                py1 = py2;
                px2 = x;
                py2 = y;
                pslope = slope;
                return;
            default:
        }
    }

    private void emitStashedLine() {
        if (state == SimplifierState.PreviousLine) {
            delegate.lineTo(px2, py2);
        }
    }

    private static double getSlope(double x1, double y1, double x2, double y2) {
        double dy = y2 - y1;
        if (dy == 0.0d) {
            return (x2 > x1) ? Double.POSITIVE_INFINITY
                   : Double.NEGATIVE_INFINITY;
        }
        return (x2 - x1) / dy;
    }
}
