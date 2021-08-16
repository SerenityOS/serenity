/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * A helper that is used for creating HTML elements.
 */
public class HtmlHelper {

    private static final String STYLE
            = "style=\"font-family: Courier New; "
            + "font-size: 12px; "
            + "white-space: pre-wrap\"";

    public static String htmlRow(String... values) {
        StringBuilder row = new StringBuilder();
        row.append(startTr());
        for (String value : values) {
            row.append(startTd());
            row.append(value);
            row.append(endTd());
        }
        row.append(endTr());
        return row.toString();
    }

    public static String startHtml() {
        return startTag("html");
    }

    public static String endHtml() {
        return endTag("html");
    }

    public static String startPre() {
        return startTag("pre " + STYLE);
    }

    public static String endPre() {
        return endTag("pre");
    }

    public static String startTable() {
        return startTag("table border=\"1\" padding=\"1\" cellspacing=\"0\" " + STYLE);
    }

    public static String endTable() {
        return endTag("table");
    }

    public static String startTr() {
        return "\t" + startTag("tr") + "\n";
    }

    public static String endTr() {
        return "\t" + endTag("tr") + "\n";
    }

    public static String startTd() {
        return "\t\t" + startTag("td");
    }

    public static String endTd() {
        return endTag("td") + "\n";
    }

    public static String startTag(String tag) {
        return "<" + tag + ">";
    }

    public static String endTag(String tag) {
        return "</" + tag + ">";
    }

    public static String anchorName(String name, String text) {
        return "<a name=" + name + "><hr/>" + text + "</a>";
    }

    public static String anchorLink(String file, String anchorName,
            String text) {
        return "<a href=" + file + "#" + anchorName + ">" + text + "</a>";
    }
}
