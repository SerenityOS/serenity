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

class SelectNode extends AbstractGroupNode implements TypeNode {

    AbstractSimpleTypeNode typeNode = null;

    void prune() {
        super.prune();
        Iterator<Node> it = components.iterator();

        if (it.hasNext()) {
            Node typeNode = it.next();

            if (typeNode instanceof ByteTypeNode ||
                      typeNode instanceof IntTypeNode) {
                this.typeNode = (AbstractSimpleTypeNode)typeNode;
                it.remove();
            } else {
                error("Select must be based on 'int' or 'byte'");
            }
        } else {
            error("empty");
        }
    }

    void constrain(Context ctx) {
        super.constrain(ctx);
        if (components.size() < 2) {
            error("Select must have at least two options");
        }
    }

    void constrainComponent(Context ctx, Node node) {
        node.constrain(ctx);
        if (!(node instanceof AltNode)) {
            error("Select must consist of selector followed by Alt items");
        }
    }

    void document(PrintWriter writer) {
        typeNode.document(writer);
        super.document(writer);
    }

    String docType() {
        // should never call this
        error("Internal - called SelectNode.docType()");
        return null;
    }

    String commonBaseClass() {
        return name() + "Common";
    }

    private String commonVar() {
        return " a" + commonBaseClass();
    }

    void genJavaClassSpecifics(PrintWriter writer, int depth) {
        indent(writer, depth);
        writer.println("abstract static class " + commonBaseClass() + " {");
        if (context.isWritingCommand()) {
            indent(writer, depth+1);
            writer.println("abstract void write(PacketStream ps);");
        } else {
            indent(writer, depth+1);
            writer.println("abstract " + typeNode.javaParam() + "();");
        }
        indent(writer, depth);
        writer.println("}");
        typeNode.genJavaDeclaration(writer, depth);
        indent(writer, depth);
        writer.println(commonBaseClass() + commonVar() + ";");
        super.genJavaClassSpecifics(writer, depth);
    }

    void genJavaClassBodyComponents(PrintWriter writer, int depth) {
        // don't naively include alt components
    }

    void genJavaWritingClassBody(PrintWriter writer, int depth,
                                 String className) {
        writer.println();
        indent(writer, depth);
        writer.print(className + "(" + typeNode.javaParam() + ", ");
        writer.print(commonBaseClass() + commonVar());
        writer.println(") {");
        indent(writer, depth+1);
        writer.println("this." + typeNode.name() + " = " + typeNode.name() + ";");
        indent(writer, depth+1);
        writer.println("this." + commonVar() + " =" + commonVar() + ";");
        indent(writer, depth);
        writer.println("}");
    }

    void genJavaWrites(PrintWriter writer, int depth) {
        typeNode.genJavaWrite(writer, depth, typeNode.name());
        indent(writer, depth);
        writer.println(commonVar() + ".write(ps);");
    }

    void genJavaReads(PrintWriter writer, int depth) {
        typeNode.genJavaRead(writer, depth, typeNode.name());
        indent(writer, depth);
        writer.println("switch (" + typeNode.name() + ") {");
        for (Node node : components) {
            AltNode alt = (AltNode)node;
            alt.genJavaReadsSelectCase(writer, depth+1, commonVar());
        }
        indent(writer, depth);
        writer.println("}");
    }

    public void genJavaDeclaration(PrintWriter writer, int depth) {
        typeNode.genJavaDeclaration(writer, depth);
        super.genJavaDeclaration(writer, depth);
    }

    public String javaParam() {
        return typeNode.javaParam() + ", " + name() + " a" + name();
    }
}
