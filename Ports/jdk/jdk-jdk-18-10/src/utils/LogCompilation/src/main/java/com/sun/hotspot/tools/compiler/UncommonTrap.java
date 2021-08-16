/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.tools.compiler;

import java.io.PrintStream;

/**
 * An instance of this class represents an uncommon trap associated with a
 * given bytecode instruction. An uncommon trap is described in terms of its
 * reason and action to be taken. An instance of this class is always relative
 * to a specific method and only contains the relevant bytecode instruction
 * index.
 */
class UncommonTrap {

    private int bci;
    private String reason;
    private String action;
    private String bytecode;

    public UncommonTrap(int b, String r, String a, String bc) {
        bci = b;
        reason = r;
        action = a;
        bytecode = bc;
    }

    public int getBCI() {
        return bci;
    }

    public String getReason() {
        return reason;
    }

    public String getAction() {
        return action;
    }

    public String getBytecode() {
        return bytecode;
    }

    void emit(PrintStream stream, int indent) {
        for (int i = 0; i < indent; i++) {
            stream.print(' ');
        }
    }

    public void print(PrintStream stream, int indent) {
        emit(stream, indent);
        stream.println(this);
    }

    public String toString() {
        return "@ " + bci  + " " + getBytecode() + " uncommon trap " + getReason() + " " + getAction();
    }
}
