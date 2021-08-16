/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.code;

import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.ListBuffer;

/**
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class DeferredLintHandler {
    protected static final Context.Key<DeferredLintHandler> deferredLintHandlerKey = new Context.Key<>();

    public static DeferredLintHandler instance(Context context) {
        DeferredLintHandler instance = context.get(deferredLintHandlerKey);
        if (instance == null)
            instance = new DeferredLintHandler(context);
        return instance;
    }

    protected DeferredLintHandler(Context context) {
        context.put(deferredLintHandlerKey, this);
        this.currentPos = IMMEDIATE_POSITION;
    }

    /**An interface for deferred lint reporting - loggers passed to
     * {@link #report(LintLogger) } will be called when
     * {@link #flush(DiagnosticPosition) } is invoked.
     */
    public interface LintLogger {
        void report();
    }

    private DiagnosticPosition currentPos;
    private Map<DiagnosticPosition, ListBuffer<LintLogger>> loggersQueue = new HashMap<>();

    /**Associate the given logger with the current position as set by {@link #setPos(DiagnosticPosition) }.
     * Will be invoked when {@link #flush(DiagnosticPosition) } will be invoked with the same position.
     * <br>
     * Will invoke the logger synchronously if {@link #immediate() } was called
     * instead of {@link #setPos(DiagnosticPosition) }.
     */
    public void report(LintLogger logger) {
        if (currentPos == IMMEDIATE_POSITION) {
            logger.report();
        } else {
            ListBuffer<LintLogger> loggers = loggersQueue.get(currentPos);
            if (loggers == null) {
                loggersQueue.put(currentPos, loggers = new ListBuffer<>());
            }
            loggers.append(logger);
        }
    }

    /**Invoke all {@link LintLogger}s that were associated with the provided {@code pos}.
     */
    public void flush(DiagnosticPosition pos) {
        ListBuffer<LintLogger> loggers = loggersQueue.get(pos);
        if (loggers != null) {
            for (LintLogger lintLogger : loggers) {
                lintLogger.report();
            }
            loggersQueue.remove(pos);
        }
    }

    /**Sets the current position to the provided {@code currentPos}. {@link LintLogger}s
     * passed to subsequent invocations of {@link #report(LintLogger) } will be associated
     * with the given position.
     */
    public DiagnosticPosition setPos(DiagnosticPosition currentPos) {
        DiagnosticPosition prevPosition = this.currentPos;
        this.currentPos = currentPos;
        return prevPosition;
    }

    /**{@link LintLogger}s passed to subsequent invocations of
     * {@link #report(LintLogger) } will be invoked immediately.
     */
    public DiagnosticPosition immediate() {
        return setPos(IMMEDIATE_POSITION);
    }

    private static final DiagnosticPosition IMMEDIATE_POSITION = new DiagnosticPosition() {
        @Override
        public JCTree getTree() {
            Assert.error();
            return null;
        }

        @Override
        public int getStartPosition() {
            Assert.error();
            return -1;
        }

        @Override
        public int getPreferredPosition() {
            Assert.error();
            return -1;
        }

        @Override
        public int getEndPosition(EndPosTable endPosTable) {
            Assert.error();
            return -1;
        }
    };
}
