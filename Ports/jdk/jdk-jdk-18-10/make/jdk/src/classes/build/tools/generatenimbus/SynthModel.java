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

public class SynthModel {
    private UIStyle style;

    private ArrayList<UIColor> colors;

    private ArrayList<UIFont> fonts;

    private ArrayList<UIComponent> components;

    SynthModel(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int eventType = reader.next();
            switch (eventType) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "style":
                            style = new UIStyle(reader);
                            break;
                        case "colors":
                            colors = new ArrayList<>();
                            break;
                        case "fonts":
                            fonts = new ArrayList<>();
                            break;
                        case "components":
                            components = new ArrayList<>();
                            break;
                        case "uiColor":
                            colors.add(new UIColor(reader));
                            break;
                        case "uiFont":
                            fonts.add(new UIFont(reader));
                            break;
                        case "uiComponent":
                            components.add(new UIComponent(reader));
                            break;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    break;
            }
        }
    }

    public void initStyles() {
        for (UIComponent c: components) {
            c.initStyles(this.style);
        }
    }

    public void write(StringBuilder defBuffer, StringBuilder styleBuffer, String packageName) {
        defBuffer.append("        //Color palette\n");
        for (UIColor c: colors) defBuffer.append(c.write());
        defBuffer.append('\n');

        defBuffer.append("        //Font palette\n");
        defBuffer.append("        d.put(\"defaultFont\", new FontUIResource(defaultFont));\n");
        for (UIFont f: fonts) defBuffer.append(f.write());
        defBuffer.append('\n');

        defBuffer.append("        //Border palette\n");
        defBuffer.append('\n');

        defBuffer.append("        //The global style definition\n");
        defBuffer.append(style.write(""));
        defBuffer.append('\n');

        for (UIComponent c: components) {
            String prefix = Utils.escape(c.getKey());
            defBuffer.append("        //Initialize ").append(prefix).append("\n");
            c.write(defBuffer, styleBuffer, c, prefix, packageName);
            defBuffer.append('\n');
        }
    }
}
