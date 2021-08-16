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

abstract class Node {

    String kind;
    List<Node> components;
    int lineno;
    List<String> commentList = new ArrayList<>();
    Node parent = null;
    Context context = null;

    static final int maxStructIndent = 5;
    static int structIndent = 0; // horrible hack

    abstract void document(PrintWriter writer);

    void set(String kind, List<Node> components, int lineno) {
        this.kind = kind;
        this.components = components;
        this.lineno = lineno;
    }

    void parentAndExtractComments() {
        for (Iterator<Node> it = components.iterator(); it.hasNext();) {
            Node node = it.next();
            if (node instanceof CommentNode) {
                it.remove();
                commentList.add(((CommentNode)node).text());
            } else {
                node.parent = this;
                node.parentAndExtractComments();
            }
        }
    }

    void prune() {
        for (Node node : components) {
            node.prune();
        }
    }

    void constrain(Context ctx) {
        context = ctx;
        for (Node node : components) {
            constrainComponent(ctx, node);
        }
    }

    void constrainComponent(Context ctx, Node node) {
        node.constrain(ctx);
    }

    void indent(PrintWriter writer, int depth) {
        for (int i = 0; i < depth; i++) {
            writer.print("    ");
        }
    }

    void documentIndex(PrintWriter writer) {
    }

    String indentElement(int depth, String content) {
        return depth > 0
                ? "<div class=\"indent" + depth + "\">" + content + "</div>"
                : content;
    }

    String comment() {
        StringBuffer comment = new StringBuffer();
        for (String st : commentList) {
            comment.append(st);
        }
        return comment.toString();
    }

    void genJavaComment(PrintWriter writer, int depth) {
        if (commentList.size() > 0) {
            indent(writer, depth);
            writer.println("/**");
            for (String comment : commentList) {
                indent(writer, depth);
                writer.println(" * " + comment);
            }
            indent(writer, depth);
            writer.println(" */");
        }
    }

    String javaType() {
        return "-- WRONG ---";
    }

    void genJava(PrintWriter writer, int depth) {
        for (Node node : components) {
            node.genJava(writer, depth);
        }
    }

    void genCInclude(PrintWriter writer) {
        for (Node node : components) {
            node.genCInclude(writer);
        }
    }

    String debugValue(String label) {
        return label;
    }

    void genJavaDebugWrite(PrintWriter writer, int depth,
                           String writeLabel) {
        genJavaDebugWrite(writer, depth, writeLabel, debugValue(writeLabel));
    }

    void genJavaDebugWrite(PrintWriter writer, int depth,
                           String writeLabel, String displayValue) {
        if (!Main.genDebug) {
            return;
        }
        indent(writer, depth);
        writer.println(
          "if ((ps.vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {");
        indent(writer, depth+1);
        writer.print("ps.vm.printTrace(\"Sending: ");
        indent(writer, depth);  // this is inside the quotes
        writer.print(writeLabel + "(" + javaType() + "): \" + ");
        writer.println(displayValue + ");");
        indent(writer, depth);
        writer.println("}");
    }

    public void genJavaRead(PrintWriter writer, int depth,
                            String readLabel) {
        error("Internal - Should not call Node.genJavaRead()");
    }

    void genJavaDebugRead(PrintWriter writer, int depth,
                          String readLabel, String displayValue) {
        if (!Main.genDebug) {
            return;
        }
        indent(writer, depth);
        writer.println(
          "if (vm.traceReceives) {");
        indent(writer, depth+1);
        writer.print("vm.printReceiveTrace(" + depth + ", \"");
        writer.print(readLabel + "(" + javaType() + "): \" + ");
        writer.println(displayValue + ");");
        indent(writer, depth);
        writer.println("}");
    }

    void genJavaPreDef(PrintWriter writer, int depth) {
        for (Node node : components) {
            node.genJavaPreDef(writer, depth);
        }
    }

    void error(String errmsg) {
        System.err.println();
        System.err.println(Main.specSource + ":" + lineno + ": " +
                           kind + " - " + errmsg);
        System.err.println();
        throw new RuntimeException("Error: " + errmsg);
    }
}
