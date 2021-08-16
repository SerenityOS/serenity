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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class Ellipse extends Shape {
    private double x1;

    public double getX1() { return x1; }

    private double x2;
    public double getX2() { return x2; }

    private double y1;
    public double getY1() { return y1; }

    private double y2;
    public double getY2() { return y2; }

    Ellipse(XMLStreamReader reader) throws XMLStreamException {
        x1 = Double.parseDouble(reader.getAttributeValue(null, "x1"));
        x2 = Double.parseDouble(reader.getAttributeValue(null, "x2"));
        y1 = Double.parseDouble(reader.getAttributeValue(null, "y1"));
        y2 = Double.parseDouble(reader.getAttributeValue(null, "y2"));
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "matte":
                            paint = new Matte(reader);
                            break;
                        case "gradient":
                            paint = new Gradient(reader);
                            break;
                        case "radialGradient":
                            paint = new RadialGradient(reader);
                            break;
                        case "paintPoints":
                            paintPoints = new PaintPoints(reader);
                            break;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "ellipse":
                            return;
                    }
                    break;
            }
        }
    }

}
