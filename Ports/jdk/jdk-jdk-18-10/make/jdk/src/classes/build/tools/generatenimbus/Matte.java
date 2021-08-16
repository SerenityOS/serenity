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
import java.util.Optional;

class Matte extends Paint {
    private int red;
    private int green;
    private int blue;
    private int alpha;

    private String uiDefaultParentName = null;
    private float hueOffset = 0;
    private float saturationOffset = 0;
    private float brightnessOffset = 0;
    private int alphaOffset = 0;

    private String componentPropertyName = null;

    public String getComponentPropertyName() { return componentPropertyName; }

    private boolean uiResource = true;

    Matte(XMLStreamReader reader) {
        red = Integer.parseInt(reader.getAttributeValue(null, "red"));
        green = Integer.parseInt(reader.getAttributeValue(null, "green"));
        blue = Integer.parseInt(reader.getAttributeValue(null, "blue"));
        alpha = Integer.parseInt(reader.getAttributeValue(null, "alpha"));
        uiDefaultParentName = reader.getAttributeValue(null,
                "uiDefaultParentName");
        hueOffset = Float.parseFloat(reader.getAttributeValue(null,
                "hueOffset"));
        saturationOffset = Float.parseFloat(reader.getAttributeValue(null,
                "saturationOffset"));
        brightnessOffset = Float.parseFloat(reader.getAttributeValue(null,
                "brightnessOffset"));
        alphaOffset = Integer.parseInt(reader.getAttributeValue(null,
                "alphaOffset"));
        componentPropertyName = reader.getAttributeValue(null,
                "componentPropertyName");
        uiResource = Boolean.parseBoolean(Optional.ofNullable(
                reader.getAttributeValue(null, "uiResource")).orElse("true"));
    }

    public boolean isAbsolute() {
        return uiDefaultParentName == null;
    }

    public String getDeclaration() {
        if (isAbsolute()) {
            return String.format("new Color(%d, %d, %d, %d)",
                                 red, green, blue, alpha);
        } else {
            return String.format("decodeColor(\"%s\", %sf, %sf, %sf, %d)",
                    uiDefaultParentName, String.valueOf(hueOffset),
                    String.valueOf(saturationOffset),
                    String.valueOf(brightnessOffset), alphaOffset);
        }
    }

    public String write() {
        if (isAbsolute()) {
            return String.format("%s, %s, %s, %s", red, green, blue, alpha);
        } else {
            String s = String.format("\"%s\", %sf, %sf, %sf, %d",
                    uiDefaultParentName, String.valueOf(hueOffset),
                    String.valueOf(saturationOffset),
                    String.valueOf(brightnessOffset), alphaOffset);
            if (! uiResource) {
                s += ", false";
            }
            return s;
        }
    }

    public ComponentColor createComponentColor(String variableName) {
        return new ComponentColor(componentPropertyName, variableName,
                saturationOffset, brightnessOffset, alphaOffset);
    }
}
