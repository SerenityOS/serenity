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

class Border {
    enum BorderType {
        EMPTY,
        PAINTER
    }

    private BorderType type;

    private String painter;

    private int top;

    private int left;

    private int bottom;

    private int right;

    Border(XMLStreamReader reader) {
        switch (reader.getAttributeValue(null, "type")) {
            case "empty":
                type = BorderType.EMPTY;
                break;
            case "painter":
                type =BorderType.PAINTER;
                break;
        }
        painter = reader.getAttributeValue(null, "painter");
        top = Integer.parseInt(reader.getAttributeValue(null, "top"));
        left = Integer.parseInt(reader.getAttributeValue(null, "left"));
        bottom = Integer.parseInt(reader.getAttributeValue(null, "bottom"));
        right = Integer.parseInt(reader.getAttributeValue(null, "right"));
    }

    public String write() {
        switch (type) {
            case PAINTER:
                return String.format("new PainterBorder(\"%s\", new Insets(%d, %d, %d, %d))",
                                     painter, top, left, bottom, right);
            case EMPTY:
                return String.format("BorderFactory.createEmptyBorder(%d, %d, %d, %d)",
                                     top, left, bottom, right);
            default:
                return "### Look, here's an unknown border! $$$";
        }
    }
}
