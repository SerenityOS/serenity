/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.generatenimbus;

import javax.xml.stream.XMLStreamReader;

public abstract class Shape {
    PaintPoints paintPoints;

    public double getPaintX1() { return paintPoints.x1; }
    public double getPaintX2() { return paintPoints.x2; }
    public double getPaintY1() { return paintPoints.y1; }
    public double getPaintY2() { return paintPoints.y2; }

    Paint paint;
    public Paint getPaint() { return paint; }

    static class PaintPoints {
        double x1;
        double y1;
        double x2;
        double y2;

        PaintPoints(XMLStreamReader reader) {
            x1 = Double.parseDouble(reader.getAttributeValue(null, "x1"));
            x2 = Double.parseDouble(reader.getAttributeValue(null, "x2"));
            y1 = Double.parseDouble(reader.getAttributeValue(null, "y1"));
            y2 = Double.parseDouble(reader.getAttributeValue(null, "y2"));
        }
    }
}
