/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import java.util.HashMap;

import com.sun.tools.javac.util.Context;

/** Partial map to record which compiler phases have been executed
 *  for each compilation unit. Used for ATTR and FLOW phases.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CompileStates extends HashMap<Env<AttrContext>, CompileStates.CompileState> {
    /** The context key for the compile states. */
    protected static final Context.Key<CompileStates> compileStatesKey = new Context.Key<>();

    /** Get the CompileStates instance for this context. */
    public static CompileStates instance(Context context) {
        CompileStates instance = context.get(compileStatesKey);
        if (instance == null) {
            instance = new CompileStates(context);
        }
        return instance;
    }

    /** Ordered list of compiler phases for each compilation unit. */
    public enum CompileState {
        INIT(0),
        PARSE(1),
        ENTER(2),
        PROCESS(3),
        ATTR(4),
        FLOW(5),
        TRANSTYPES(6),
        TRANSPATTERNS(7),
        UNLAMBDA(8),
        LOWER(9),
        GENERATE(10);

        CompileState(int value) {
            this.value = value;
        }
        public boolean isAfter(CompileState other) {
            return value > other.value;
        }
        public static CompileState max(CompileState a, CompileState b) {
            return a.value > b.value ? a : b;
        }
        private final int value;
    }

    private static final long serialVersionUID = 1812267524140424433L;

    protected transient Context context;

    public CompileStates(Context context) {
        this.context = context;
        context.put(compileStatesKey, this);
    }

    public boolean isDone(Env<AttrContext> env, CompileState cs) {
        CompileState ecs = get(env);
        return (ecs != null) && !cs.isAfter(ecs);
    }
}
