/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.jfc.model;

import java.io.PrintWriter;
import java.util.Map;

final class PrettyPrinter {
    private final PrintWriter out;

    PrettyPrinter(PrintWriter out) {
        this.out = out;
    }

    void print(XmlConfiguration configuration) {
        printHeader();
        prettyPrint("", configuration);
    }

    private void printHeader() {
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    private void prettyPrint(String indent, XmlElement element) {
        printComment(indent, element);
        String elementName = element.getElementName();
        out.print(indent + '<' + elementName);
        printAttributes(element.getAttributes());
        if (element.getChildren().isEmpty() && !element.hasContent()) {
            out.println("/>");
            return;
        }
        out.print('>');
        out.print(Utilities.escapeAll(element.getContent().trim()));
        if (element.getChildren().isEmpty()) {
            out.println("</" + elementName + '>');
            return;
        }
        out.println();
        boolean first = true;
        for (XmlElement child : element.getChildren()) {
            if (first && child.isEntity()) {
                out.println();
            }
            prettyPrint(indent + "  ", child);
            if (child.isEntity()) {
                out.println();
            }
            first = false;
        }
        out.println(indent + "</" + elementName + '>');
    }

    private void printComment(String indent, XmlElement element) {
        String comment = element.comment();
        if (!comment.isEmpty()) {
            String text = comment.indent(indent.length());
            out.println(indent + "<!--");
            out.println(text.replace("\n", System.lineSeparator()));
            out.println(indent + "-->");
        }
    }

    private void printAttributes(Map<String, String> attributes) {
        for (var entry : attributes.entrySet()) {
            out.print(' ');
            out.print(entry.getKey());
            out.print("=\"");
            out.print(Utilities.escapeAll(entry.getValue()));
            out.print('\"');
        }
    }
}
