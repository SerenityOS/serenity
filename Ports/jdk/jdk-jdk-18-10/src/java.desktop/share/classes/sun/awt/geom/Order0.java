/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.geom;

import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.util.Vector;

final class Order0 extends Curve {
    private double x;
    private double y;

    public Order0(double x, double y) {
        super(INCREASING);
        this.x = x;
        this.y = y;
    }

    public int getOrder() {
        return 0;
    }

    public double getXTop() {
        return x;
    }

    public double getYTop() {
        return y;
    }

    public double getXBot() {
        return x;
    }

    public double getYBot() {
        return y;
    }

    public double getXMin() {
        return x;
    }

    public double getXMax() {
        return x;
    }

    public double getX0() {
        return x;
    }

    public double getY0() {
        return y;
    }

    public double getX1() {
        return x;
    }

    public double getY1() {
        return y;
    }

    public double XforY(double y) {
        return y;
    }

    public double TforY(double y) {
        return 0;
    }

    public double XforT(double t) {
        return x;
    }

    public double YforT(double t) {
        return y;
    }

    public double dXforT(double t, int deriv) {
        return 0;
    }

    public double dYforT(double t, int deriv) {
        return 0;
    }

    public double nextVertical(double t0, double t1) {
        return t1;
    }

    public int crossingsFor(double x, double y) {
        return 0;
    }

    public boolean accumulateCrossings(Crossings c) {
        return (x > c.getXLo() &&
                x < c.getXHi() &&
                y > c.getYLo() &&
                y < c.getYHi());
    }

    public void enlarge(Rectangle2D r) {
        r.add(x, y);
    }

    public Curve getSubCurve(double ystart, double yend, int dir) {
        return this;
    }

    public Curve getReversedCurve() {
        return this;
    }

    public int getSegment(double[] coords) {
        coords[0] = x;
        coords[1] = y;
        return PathIterator.SEG_MOVETO;
    }
}
