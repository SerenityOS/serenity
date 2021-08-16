/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

abstract class AbstractNamedNode extends Node {

    NameNode nameNode = null;
    String name;

    public String name() {
        return name;
    }

    void prune() {
        Iterator<Node> it = components.iterator();

        if (it.hasNext()) {
            Node nameNode = it.next();

            if (nameNode instanceof NameNode) {
                this.nameNode = (NameNode)nameNode;
                this.name = this.nameNode.text();
                it.remove();
            } else {
                error("Bad name: " + name);
            }
        } else {
            error("empty");
        }
        super.prune();
    }

    void constrain(Context ctx) {
        nameNode.constrain(ctx);
        super.constrain(ctx.subcontext(name));
    }

    void document(PrintWriter writer) {
        writer.println("<h2 id=\"" + name + "\">" + name +
                       " Command Set</h2>");
        for (Node node : components) {
            node.document(writer);
        }
    }

    String javaClassName() {
        return name();
    }

    void genJavaClassSpecifics(PrintWriter writer, int depth) {
    }

    String javaClassImplements() {
        return ""; // does not implement anything, by default
    }

    void genJavaClass(PrintWriter writer, int depth) {
        writer.println();
        genJavaComment(writer, depth);
        indent(writer, depth);
        if (depth != 0) {
            writer.print("static ");
        }
        writer.print("class " + javaClassName());
        writer.println(javaClassImplements() + " {");
        genJavaClassSpecifics(writer, depth+1);
        for (Node node : components) {
            node.genJava(writer, depth+1);
        }
        indent(writer, depth);
        writer.println("}");
    }

    void genCInclude(PrintWriter writer) {
        if (nameNode instanceof NameValueNode) {
            writer.println("#define " + context.whereC +
                           " " + nameNode.value());
        }
        super.genCInclude(writer);
    }
}
