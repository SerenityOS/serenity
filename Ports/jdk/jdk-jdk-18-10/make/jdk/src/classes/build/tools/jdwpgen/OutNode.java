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

class OutNode extends AbstractTypeListNode {

    String cmdName;

    void set(String kind, List<Node> components, int lineno) {
        super.set(kind, components, lineno);
        components.add(0, new NameNode("Out"));
    }

    void constrain(Context ctx) {
        super.constrain(ctx.commandWritingSubcontext());
        CommandNode cmd = (CommandNode)parent;
        cmdName = cmd.name;
    }

    void genProcessMethod(PrintWriter writer, int depth) {
        writer.println();
        indent(writer, depth);
        writer.print(
            "static " + cmdName + " process(VirtualMachineImpl vm");
        for (Node node : components) {
            TypeNode tn = (TypeNode)node;
            writer.println(", ");
            indent(writer, depth+5);
            writer.print(tn.javaParam());
        }
        writer.println(")");
        indent(writer, depth+6);
        writer.println("throws JDWPException {");
        indent(writer, depth+1);
        writer.print("PacketStream ps = enqueueCommand(vm");
        for (Node node : components) {
            TypeNode tn = (TypeNode)node;
            writer.print(", ");
            writer.print(tn.name());
        }
        writer.println(");");
        indent(writer, depth+1);
        writer.println("return waitForReply(vm, ps);");
        indent(writer, depth);
        writer.println("}");
    }

    void genEnqueueMethod(PrintWriter writer, int depth) {
        writer.println();
        indent(writer, depth);
        writer.print(
            "static PacketStream enqueueCommand(VirtualMachineImpl vm");
        for (Node node : components) {
            TypeNode tn = (TypeNode)node;
            writer.println(", ");
            indent(writer, depth+5);
            writer.print(tn.javaParam());
        }
        writer.println(") {");
        indent(writer, depth+1);
        writer.println(
            "PacketStream ps = new PacketStream(vm, COMMAND_SET, COMMAND);");
        if (Main.genDebug) {
            indent(writer, depth+1);
            writer.println(
                "if ((vm.traceFlags & VirtualMachineImpl.TRACE_SENDS) != 0) {");
            indent(writer, depth+2);
            writer.print(
                "vm.printTrace(\"Sending Command(id=\" + ps.pkt.id + \") ");
            writer.print(parent.context.whereJava);
            writer.println(
                "\"+(ps.pkt.flags!=0?\", FLAGS=\" + ps.pkt.flags:\"\"));");
            indent(writer, depth+1);
            writer.println("}");
        }
        genJavaWrites(writer, depth+1);
        indent(writer, depth+1);
        writer.println("ps.send();");
        indent(writer, depth+1);
        writer.println("return ps;");
        indent(writer, depth);
        writer.println("}");
    }

    void genWaitMethod(PrintWriter writer, int depth) {
        writer.println();
        indent(writer, depth);
        writer.println(
            "static " + cmdName + " waitForReply(VirtualMachineImpl vm, " +
                                  "PacketStream ps)");
        indent(writer, depth+6);
        writer.println("throws JDWPException {");
        indent(writer, depth+1);
        writer.println("ps.waitForReply();");
        indent(writer, depth+1);
        writer.println("return new " + cmdName + "(vm, ps);");
        indent(writer, depth);
        writer.println("}");
    }

    void genJava(PrintWriter writer, int depth) {
        genJavaPreDef(writer, depth);
        super.genJava(writer, depth);
        genProcessMethod(writer, depth);
        genEnqueueMethod(writer, depth);
        genWaitMethod(writer, depth);
    }
}
