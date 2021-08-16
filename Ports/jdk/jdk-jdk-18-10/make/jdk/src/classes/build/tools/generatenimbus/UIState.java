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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class UIState {
    private String stateKeys;

    public String getStateKeys() { return stateKeys; }

    /** Indicates whether to invert the meaning of the 9-square stretching insets */
    private boolean inverted;

    /** A cached string representing the list of stateKeys deliminated with "+" */
    private String cachedName = null;

    private Canvas canvas;
    public Canvas getCanvas() { return canvas; }

    private UIStyle style;
    public UIStyle getStyle() { return style; }

    UIState(XMLStreamReader reader) throws XMLStreamException {
        stateKeys = reader.getAttributeValue(null, "stateKeys");
        inverted = Boolean.parseBoolean(reader.getAttributeValue(null, "inverted"));
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "canvas":
                            canvas = new Canvas(reader);
                            break;
                        case "style":
                            style = new UIStyle(reader);
                            break;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "state":
                            return;
                    }
                    break;
            }
        }
    }

    public boolean hasCanvas() {
        return ! canvas.isBlank();
    }

    public static List<String> stringToKeys(String keysString) {
        return Arrays.asList(keysString.split("\\+"));
    }

    public String getName() {
        if (cachedName == null) {
            StringBuilder buf = new StringBuilder();
            List<String> keys = stringToKeys(stateKeys);
            Collections.sort(keys);
            for (Iterator<String> iter = keys.iterator(); iter.hasNext();) {
                buf.append(iter.next());
                if (iter.hasNext()) {
                    buf.append('+');
                }
            }
            cachedName = buf.toString();
        }
        return cachedName;
    }

    public void write(StringBuilder sb, String prefix, String pkg, String fileNamePrefix, String painterPrefix) {
        String statePrefix = prefix + "[" + getName() + "]";
        // write state style
        sb.append(style.write(statePrefix + '.'));
        // write painter
        if (hasCanvas()) {
            writeLazyPainter(sb, statePrefix, pkg, fileNamePrefix, painterPrefix);
        }
    }

    private void writeLazyPainter(StringBuilder sb, String statePrefix, String packageNamePrefix, String fileNamePrefix, String painterPrefix) {
        String cacheModeString = "AbstractRegionPainter.PaintContext.CacheMode." + style.getCacheMode();
        String stateConstant = Utils.statesToConstantName(painterPrefix + "_" + stateKeys);
        sb.append(String.format(
                "        d.put(\"%s.%sPainter\", new LazyPainter(\"%s.%s\", %s.%s, %s, %s, %b, %s, %s, %s));\n",
                statePrefix, painterPrefix, packageNamePrefix, fileNamePrefix,
                fileNamePrefix, stateConstant, canvas.getStretchingInsets().write(false),
                canvas.getSize().write(false), inverted, cacheModeString,
                Utils.formatDouble(style.getMaxHozCachedImgScaling()),
                Utils.formatDouble(style.getMaxVertCachedImgScaling())));
    }
}
