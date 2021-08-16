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

class CommandNode extends AbstractCommandNode {

    void constrain(Context ctx) {
        if (components.size() == 3) {
            Node out   = components.get(0);
            Node reply = components.get(1);
            Node error = components.get(2);
            if (!(out instanceof OutNode)) {
                error("Expected 'Out' item, got: " + out);
            }
            if (!(reply instanceof ReplyNode)) {
                error("Expected 'Reply' item, got: " + reply);
            }
            if (!(error instanceof ErrorSetNode)) {
                error("Expected 'ErrorSet' item, got: " + error);
            }
        } else if (components.size() == 1) {
            Node evt = components.get(0);
            if (!(evt instanceof EventNode)) {
                error("Expected 'Event' item, got: " + evt);
            }
        } else {
            error("Command must have Out and Reply items or ErrorSet item");
        }
        super.constrain(ctx);
    }

    void genJavaClassSpecifics(PrintWriter writer, int depth) {
        indent(writer, depth);
        writer.println("static final int COMMAND = " +
                       nameNode.value() + ";");
    }

    void genJava(PrintWriter writer, int depth) {
        genJavaClass(writer, depth);
    }
}
