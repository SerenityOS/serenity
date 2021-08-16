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

import java.io.*;

abstract class AbstractGroupNode extends AbstractTypeListNode {

    void document(PrintWriter writer) {
        for (Node node : components) {
            node.document(writer);
        }
    }

    String javaType() {
        return name();
    }

    void genJava(PrintWriter writer, int depth) {
        genJavaClass(writer, depth);
    }

    void genJavaWriteMethod(PrintWriter writer, int depth) {
        genJavaWriteMethod(writer, depth, "private ");
    }

    void genJavaWriteMethod(PrintWriter writer, int depth, String modifier) {
        writer.println();
        indent(writer, depth);
        writer.print(modifier);
        writer.println("void write(PacketStream ps) {");
        genJavaWrites(writer, depth+1);
        indent(writer, depth);
        writer.println("}");
    }

    void genJavaClassSpecifics(PrintWriter writer, int depth) {
        switch (context.state) {
            case Context.readingReply:
                genJavaReadingClassBody(writer, depth, name());
                break;

            case Context.writingCommand:
                genJavaWritingClassBody(writer, depth, name());
                genJavaWriteMethod(writer, depth);
                break;

            default:
                error("Group in outer");
                break;
        }
    }

    public void genJavaDeclaration(PrintWriter writer, int depth) {
        writer.println();
        genJavaComment(writer, depth);
        indent(writer, depth);
        writer.print("final ");
        writer.print(name());
        writer.print(" a" + name());
        writer.println(";");
    }

    public String javaParam() {
        return name() + " a" + name();
    }

    public void genJavaWrite(PrintWriter writer, int depth,
                             String writeLabel) {
        genJavaDebugWrite(writer, depth, writeLabel, "\"\"");
        indent(writer, depth);
        writer.println(writeLabel + ".write(ps);");
    }

    String javaRead() {
        error("Internal - Should not call AbstractGroupNode.javaRead()");
        return "";
    }

    public void genJavaRead(PrintWriter writer, int depth,
                            String readLabel) {
        genJavaDebugRead(writer, depth, readLabel, "\"\"");
        indent(writer, depth);
        writer.print(readLabel);
        writer.print(" = new ");
        writer.print(name());
        writer.println("(vm, ps);");
    }
}
