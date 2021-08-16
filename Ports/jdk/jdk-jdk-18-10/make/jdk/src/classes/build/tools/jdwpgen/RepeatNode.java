/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.jdwpgen;

import java.util.*;
import java.io.*;

class RepeatNode extends AbstractTypeNode {

    Node member = null;

    void constrain(Context ctx) {
        super.constrain(ctx);
        if (components.size() != 1) {
            error("Repeat must have exactly one member, use Group for more");
        }
        member = components.get(0);
        if (!(member instanceof TypeNode)) {
            error("Repeat member must be type specifier");
        }
    }

    void document(PrintWriter writer) {
        writer.println("<tr>");
        writer.println("<td>" + indentElement(structIndent, "int"));
        writer.println("<th scope=\"row\"><i>" + name() + "</i>");
        writer.println("<td>" + comment() + "&nbsp;");
        writer.println("</tr>");

        writer.println("<tr>");
        writer.println("<th colspan=\"3\" scope=\"rowgroup\">"
                + indentElement(structIndent, "Repeated <i>" + name() + "</i> times:"));
        writer.println("</tr>");

        ++structIndent;
        member.document(writer);
        --structIndent;
    }

    String docType() {
        return "-BOGUS-"; // should never call this
    }

    String javaType() {
        return member.javaType() + "[]";
    }

    public void genJavaWrite(PrintWriter writer, int depth,
                             String writeLabel) {
        genJavaDebugWrite(writer, depth, writeLabel, "\"\"");
        indent(writer, depth);
        writer.println("ps.writeInt(" + writeLabel + ".length);");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + writeLabel + ".length; i++) {;");
        ((TypeNode)member).genJavaWrite(writer, depth+1, writeLabel + "[i]");
        indent(writer, depth);
        writer.println("}");
    }

    String javaRead() {
        error("Internal - Should not call RepeatNode.javaRead()");
        return "";
    }

    public void genJavaRead(PrintWriter writer, int depth,
                            String readLabel) {
        genJavaDebugRead(writer, depth, readLabel, "\"\"");
        String cntLbl = readLabel + "Count";
        indent(writer, depth);
        writer.println("int " + cntLbl + " = ps.readInt();");
        indent(writer, depth);
        writer.println(readLabel + " = new " + member.javaType() +
                       "[" + cntLbl + "];");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + cntLbl + "; i++) {;");
        member.genJavaRead(writer, depth+1, readLabel + "[i]");
        indent(writer, depth);
        writer.println("}");
    }
}
