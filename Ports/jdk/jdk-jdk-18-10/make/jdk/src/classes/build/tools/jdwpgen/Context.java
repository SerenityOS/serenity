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

class Context {

    static final int outer = 0;
    static final int readingReply = 1;
    static final int writingCommand = 2;

    final String whereJava;
    final String whereC;

    int state = outer;
    private boolean inEvent = false;

    Context() {
        whereJava = "";
        whereC = "";
    }

    private Context(String whereJava, String whereC) {
        this.whereJava = whereJava;
        this.whereC = whereC;
    }

    Context subcontext(String level) {
        Context ctx;
        if (whereC.length() == 0) {
            ctx = new Context(level, level);
        } else {
            ctx = new Context(whereJava + "." + level, whereC + "_" + level);
        }
        ctx.state = state;
        ctx.inEvent = inEvent;
        return ctx;
    }

    private Context cloneContext() {
        Context ctx = new Context(whereJava, whereC);
        ctx.state = state;
        ctx.inEvent = inEvent;
        return ctx;
    }

    Context replyReadingSubcontext() {
        Context ctx = cloneContext();
        ctx.state = readingReply;
        return ctx;
    }

    Context commandWritingSubcontext() {
        Context ctx = cloneContext();
        ctx.state = writingCommand;
        return ctx;
    }

    Context inEventSubcontext() {
        Context ctx = cloneContext();
        ctx.inEvent = true;
        return ctx;
    }

    boolean inEvent() {
        return inEvent;
    }

    boolean isWritingCommand() {
        return state == writingCommand;
    }

    boolean isReadingReply() {
        return state == readingReply;
    }
}
