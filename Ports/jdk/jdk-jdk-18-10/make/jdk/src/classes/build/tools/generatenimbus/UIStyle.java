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
import java.util.ArrayList;
import java.util.List;

class UIStyle {

    public enum CacheMode {
        NO_CACHING, FIXED_SIZES, NINE_SQUARE_SCALE
    }

    private UIColor textForeground = null;
    private boolean textForegroundInherited = true;

    private UIColor textBackground = null;
    private boolean textBackgroundInherited = true;

    private UIColor background = null;
    private boolean backgroundInherited = true;

    private boolean cacheSettingsInherited = true;
    CacheMode cacheMode = CacheMode.FIXED_SIZES;
    String maxHozCachedImgScaling = "1.0";
    String maxVertCachedImgScaling = "1.0";

    private List<UIProperty> uiProperties = new ArrayList<>();

    UIStyle() {
    }

    UIStyle(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "textForeground":
                            textForeground = new UIColor(reader);
                            break;
                        case "textBackground":
                            textBackground = new UIColor(reader);
                            break;
                        case "background":
                            background = new UIColor(reader);
                            break;
                        case "uiProperty":
                            uiProperties.add(new UIProperty(reader));
                            break;
                        case "inherit-textForeground":
                            textForegroundInherited = Boolean.parseBoolean(reader.getElementText());
                            break;
                        case "inherit-textBackground":
                            textBackgroundInherited = Boolean.parseBoolean(reader.getElementText());
                            break;
                        case "cacheSettingsInherited":
                            cacheSettingsInherited = Boolean.parseBoolean(reader.getElementText());
                            break;
                        case "inherit-background":
                            backgroundInherited = Boolean.parseBoolean(reader.getElementText());
                            break;
                        case "cacheMode":
                            cacheMode = CacheMode.valueOf(reader.getElementText());
                            break;
                        case "maxHozCachedImgScaling":
                            maxHozCachedImgScaling = reader.getElementText();
                            break;
                        case "maxVertCachedImgScaling":
                            maxVertCachedImgScaling = reader.getElementText();
                            break;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "style":
                            return;
                    }
                    break;
            }
        }
    }

    private UIStyle parentStyle = null;
    public void setParentStyle(UIStyle parentStyle) {
        this.parentStyle = parentStyle;
    }

    public CacheMode getCacheMode() {
        if (cacheSettingsInherited) {
            return (parentStyle == null ?
                CacheMode.FIXED_SIZES : parentStyle.getCacheMode());
        } else {
            return cacheMode;
        }
    }

    public String getMaxHozCachedImgScaling() {
        if (cacheSettingsInherited) {
            return (parentStyle == null ?
                "1.0" : parentStyle.getMaxHozCachedImgScaling());
        } else {
            return maxHozCachedImgScaling;
        }
    }

    public String getMaxVertCachedImgScaling() {
        if (cacheSettingsInherited) {
            return (parentStyle == null ?
                "1.0" : parentStyle.getMaxVertCachedImgScaling());
        } else {
            return maxVertCachedImgScaling;
        }
    }

    public String write(String prefix) {
        StringBuilder sb = new StringBuilder();
        if (! textForegroundInherited) {
            sb.append(String.format("        addColor(d, \"%s%s\", %s);\n",
                    prefix, "textForeground", textForeground.getValue().write()));
        }
        if (! textBackgroundInherited) {
            sb.append(String.format("        addColor(d, \"%s%s\", %s);\n",
                    prefix, "textBackground", textBackground.getValue().write()));
        }
        if (! backgroundInherited) {
            sb.append(String.format("        addColor(d, \"%s%s\", %s);\n",
                    prefix, "background", background.getValue().write()));
        }
        for (UIProperty property : uiProperties) {
            sb.append(property.write(prefix));
        }
        return sb.toString();
    }
}
