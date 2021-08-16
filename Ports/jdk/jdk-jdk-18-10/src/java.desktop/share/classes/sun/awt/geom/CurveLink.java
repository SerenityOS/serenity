/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

final class CurveLink {
    Curve curve;
    double ytop;
    double ybot;
    int etag;

    CurveLink next;

    public CurveLink(Curve curve, double ystart, double yend, int etag) {
        this.curve = curve;
        this.ytop = ystart;
        this.ybot = yend;
        this.etag = etag;
        if (ytop < curve.getYTop() || ybot > curve.getYBot()) {
            throw new InternalError("bad curvelink ["+ytop+"=>"+ybot+"] for "+curve);
        }
    }

    public boolean absorb(CurveLink link) {
        return absorb(link.curve, link.ytop, link.ybot, link.etag);
    }

    public boolean absorb(Curve curve, double ystart, double yend, int etag) {
        if (this.curve != curve || this.etag != etag ||
            ybot < ystart || ytop > yend)
        {
            return false;
        }
        if (ystart < curve.getYTop() || yend > curve.getYBot()) {
            throw new InternalError("bad curvelink ["+ystart+"=>"+yend+"] for "+curve);
        }
        this.ytop = Math.min(ytop, ystart);
        this.ybot = Math.max(ybot, yend);
        return true;
    }

    public boolean isEmpty() {
        return (ytop == ybot);
    }

    public Curve getCurve() {
        return curve;
    }

    public Curve getSubCurve() {
        if (ytop == curve.getYTop() && ybot == curve.getYBot()) {
            return curve.getWithDirection(etag);
        }
        return curve.getSubCurve(ytop, ybot, etag);
    }

    public Curve getMoveto() {
        return new Order0(getXTop(), getYTop());
    }

    public double getXTop() {
        return curve.XforY(ytop);
    }

    public double getYTop() {
        return ytop;
    }

    public double getXBot() {
        return curve.XforY(ybot);
    }

    public double getYBot() {
        return ybot;
    }

    public double getX() {
        return curve.XforY(ytop);
    }

    public int getEdgeTag() {
        return etag;
    }

    public void setNext(CurveLink link) {
        this.next = link;
    }

    public CurveLink getNext() {
        return next;
    }
}
