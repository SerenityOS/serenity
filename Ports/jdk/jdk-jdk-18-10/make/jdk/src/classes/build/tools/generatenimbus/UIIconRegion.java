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

class UIIconRegion extends UIRegion {
    private String basicKey;

    UIIconRegion(XMLStreamReader reader) throws XMLStreamException {
        super(reader, false);
        basicKey = reader.getAttributeValue(null, "basicKey");
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    parse(reader);
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "uiIconRegion":
                            return;
                    }
                    break;
            }
        }

    }

    @Override public void write(StringBuilder sb, StringBuilder styleBuffer, UIComponent comp, String prefix, String pkg) {
        Dimension size = null;
        String fileNamePrefix = Utils.normalize(prefix) + "Painter";
        // write states ui defaults
        for (UIState state : backgroundStates) {
            Canvas canvas = state.getCanvas();
            if (!canvas.isBlank()) {
                state.write(sb, prefix, pkg, fileNamePrefix, getKey());
                size = canvas.getSize();
            }
        }

        if (size != null) {
            // Put SynthIconImpl wrapper in UiDefaults
            String k = (basicKey == null ? prefix + "." + getKey() : basicKey);
            sb.append(String.format(
                    "        d.put(\"%s\", new NimbusIcon(\"%s\", \"%sPainter\", %d, %d));\n",
                    k, prefix, getKey(), size.width, size.height));
        }
    }
}
