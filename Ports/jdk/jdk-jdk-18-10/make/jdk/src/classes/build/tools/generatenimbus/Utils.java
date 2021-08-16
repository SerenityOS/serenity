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

import java.util.ArrayList;
import java.util.List;
import javax.swing.plaf.synth.Region;

public class Utils {

    public static String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    public static String normalize(String s) {
        char[] src = s.toCharArray();
        StringBuilder buf = new StringBuilder();
        List<String> parts = new ArrayList<String>();
        boolean capitalize = false;

        for (int i = 0; i < src.length; i++) {
            switch (src[i]) {
                case '\\':
                case '"':
                    break;
                case '.':
                    capitalize = true;
                    break;
                case ':':
                    parts.add(buf.toString());
                    buf.delete(0, buf.length());
                    capitalize = true;
                    break;
                default:
                    buf.append(capitalize ? Character.toUpperCase(src[i]) : src[i]);
                    capitalize = false;
                    break;
            }
        }
        parts.add(buf.toString());

        // Try to optimize long class names by omitting repeating prefixes, e.g.
        // SliderTrackPainter.java instead of SliderSliderTrackPainter.java
        String result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part.startsWith(result)) {
                result = part;
            } else {
                result += part;
            }
        }
        return result;
    }

    public static String regionNameToCaps(String regionName) {
        if (Region.ARROW_BUTTON.getName().equals(regionName)) {
            return "ARROW_BUTTON";
        } else if (Region.BUTTON.getName().equals(regionName)) {
            return "BUTTON";
        } else if (Region.CHECK_BOX.getName().equals(regionName)) {
            return "CHECK_BOX";
        } else if (Region.CHECK_BOX_MENU_ITEM.getName().equals(regionName)) {
            return "CHECK_BOX_MENU_ITEM";
        } else if (Region.COLOR_CHOOSER.getName().equals(regionName)) {
            return "COLOR_CHOOSER";
        } else if (Region.COMBO_BOX.getName().equals(regionName)) {
            return "COMBO_BOX";
        } else if (Region.DESKTOP_ICON.getName().equals(regionName)) {
            return "DESKTOP_ICON";
        } else if (Region.DESKTOP_PANE.getName().equals(regionName)) {
            return "DESKTOP_PANE";
        } else if (Region.EDITOR_PANE.getName().equals(regionName)) {
            return "EDITOR_PANE";
        } else if (Region.FILE_CHOOSER.getName().equals(regionName)) {
            return "FILE_CHOOSER";
        } else if (Region.FORMATTED_TEXT_FIELD.getName().equals(regionName)) {
            return "FORMATTED_TEXT_FIELD";
        } else if (Region.INTERNAL_FRAME.getName().equals(regionName)) {
            return "INTERNAL_FRAME";
        } else if (Region.INTERNAL_FRAME_TITLE_PANE.getName().equals(regionName)) {
            return "INTERNAL_FRAME_TITLE_PANE";
        } else if (Region.LABEL.getName().equals(regionName)) {
            return "LABEL";
        } else if (Region.LIST.getName().equals(regionName)) {
            return "LIST";
        } else if (Region.MENU.getName().equals(regionName)) {
            return "MENU";
        } else if (Region.MENU_BAR.getName().equals(regionName)) {
            return "MENU_BAR";
        } else if (Region.MENU_ITEM.getName().equals(regionName)) {
            return "MENU_ITEM";
        } else if (Region.MENU_ITEM_ACCELERATOR.getName().equals(regionName)) {
            return "MENU_ITEM_ACCELERATOR";
        } else if (Region.OPTION_PANE.getName().equals(regionName)) {
            return "OPTION_PANE";
        } else if (Region.PANEL.getName().equals(regionName)) {
            return "PANEL";
        } else if (Region.PASSWORD_FIELD.getName().equals(regionName)) {
            return "PASSWORD_FIELD";
        } else if (Region.POPUP_MENU.getName().equals(regionName)) {
            return "POPUP_MENU";
        } else if (Region.POPUP_MENU_SEPARATOR.getName().equals(regionName)) {
            return "POPUP_MENU_SEPARATOR";
        } else if (Region.PROGRESS_BAR.getName().equals(regionName)) {
            return "PROGRESS_BAR";
        } else if (Region.RADIO_BUTTON.getName().equals(regionName)) {
            return "RADIO_BUTTON";
        } else if (Region.RADIO_BUTTON_MENU_ITEM.getName().equals(regionName)) {
            return "RADIO_BUTTON_MENU_ITEM";
        } else if (Region.ROOT_PANE.getName().equals(regionName)) {
            return "ROOT_PANE";
        } else if (Region.SCROLL_BAR.getName().equals(regionName)) {
            return "SCROLL_BAR";
        } else if (Region.SCROLL_BAR_THUMB.getName().equals(regionName)) {
            return "SCROLL_BAR_THUMB";
        } else if (Region.SCROLL_BAR_TRACK.getName().equals(regionName)) {
            return "SCROLL_BAR_TRACK";
        } else if (Region.SCROLL_PANE.getName().equals(regionName)) {
            return "SCROLL_PANE";
        } else if (Region.SEPARATOR.getName().equals(regionName)) {
            return "SEPARATOR";
        } else if (Region.SLIDER.getName().equals(regionName)) {
            return "SLIDER";
        } else if (Region.SLIDER_THUMB.getName().equals(regionName)) {
            return "SLIDER_THUMB";
        } else if (Region.SLIDER_TRACK.getName().equals(regionName)) {
            return "SLIDER_TRACK";
        } else if (Region.SPINNER.getName().equals(regionName)) {
            return "SPINNER";
        } else if (Region.SPLIT_PANE.getName().equals(regionName)) {
            return "SPLIT_PANE";
        } else if (Region.SPLIT_PANE_DIVIDER.getName().equals(regionName)) {
            return "SPLIT_PANE_DIVIDER";
        } else if (Region.TABBED_PANE.getName().equals(regionName)) {
            return "TABBED_PANE";
        } else if (Region.TABBED_PANE_CONTENT.getName().equals(regionName)) {
            return "TABBED_PANE_CONTENT";
        } else if (Region.TABBED_PANE_TAB.getName().equals(regionName)) {
            return "TABBED_PANE_TAB";
        } else if (Region.TABBED_PANE_TAB_AREA.getName().equals(regionName)) {
            return "TABBED_PANE_TAB_AREA";
        } else if (Region.TABLE.getName().equals(regionName)) {
            return "TABLE";
        } else if (Region.TABLE_HEADER.getName().equals(regionName)) {
            return "TABLE_HEADER";
        } else if (Region.TEXT_AREA.getName().equals(regionName)) {
            return "TEXT_AREA";
        } else if (Region.TEXT_FIELD.getName().equals(regionName)) {
            return "TEXT_FIELD";
        } else if (Region.TEXT_PANE.getName().equals(regionName)) {
            return "TEXT_PANE";
        } else if (Region.TOGGLE_BUTTON.getName().equals(regionName)) {
            return "TOGGLE_BUTTON";
        } else if (Region.TOOL_BAR.getName().equals(regionName)) {
            return "TOOL_BAR";
        } else if (Region.TOOL_BAR_CONTENT.getName().equals(regionName)) {
            return "TOOL_BAR_CONTENT";
        } else if (Region.TOOL_BAR_DRAG_WINDOW.getName().equals(regionName)) {
            return "TOOL_BAR_DRAG_WINDOW";
        } else if (Region.TOOL_BAR_SEPARATOR.getName().equals(regionName)) {
            return "TOOL_BAR_SEPARATOR";
        } else if (Region.TOOL_TIP.getName().equals(regionName)) {
            return "TOOL_TIP";
        } else if (Region.TREE.getName().equals(regionName)) {
            return "TREE";
        } else if (Region.TREE_CELL.getName().equals(regionName)) {
            return "TREE_CELL";
        } else if (Region.VIEWPORT.getName().equals(regionName)) {
            return "VIEWPORT";
        }
        throw new RuntimeException("Bad Region name " + regionName);
    }

    public static String statesToConstantName(String states) {
        String s = states.replace(" ", "");
        s = states.replace("+", "_");
        return s.toUpperCase();
    }

    //takes a states string of the form Enabled+Foo+Bar.
    //removes any whitespace. Replaces the + signs with And.
    public static String statesToClassName(String states) {
        String s = states.replace(" ", "");
        s = states.replace("+", "And");
        return s;
    }

    public static String formatDouble(String doubleValue) {
        return doubleValue.replace("INF", "Double.POSITIVE_INFINITY");
    }
}
