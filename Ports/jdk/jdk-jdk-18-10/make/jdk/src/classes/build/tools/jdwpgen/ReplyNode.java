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

class ReplyNode extends AbstractTypeListNode {

    String cmdName;

    void set(String kind, List<Node> components, int lineno) {
        super.set(kind, components, lineno);
        components.add(0, new NameNode(kind));
    }

    void constrain(Context ctx) {
        super.constrain(ctx.replyReadingSubcontext());
        CommandNode cmd = (CommandNode)parent;
        cmdName = cmd.name;
    }

    void genJava(PrintWriter writer, int depth) {
        genJavaPreDef(writer, depth);
        super.genJava(writer, depth);
        writer.println();
        genJavaReadingClassBody(writer, depth, cmdName);
    }

    void genJavaReads(PrintWriter writer, int depth) {
        if (Main.genDebug) {
            indent(writer, depth);
            writer.println(
                "if (vm.traceReceives) {");
            indent(writer, depth+1);
            writer.print(
                "vm.printTrace(\"Receiving Command(id=\" + ps.pkt.id + \") ");
            writer.print(parent.context.whereJava);
            writer.print("\"");
            writer.print(
                "+(ps.pkt.flags!=0?\", FLAGS=\" + ps.pkt.flags:\"\")");
            writer.print(
                "+(ps.pkt.errorCode!=0?\", ERROR CODE=\" + ps.pkt.errorCode:\"\")");
            writer.println(");");
            indent(writer, depth);
            writer.println("}");
        }
        super.genJavaReads(writer, depth);
    }
}
