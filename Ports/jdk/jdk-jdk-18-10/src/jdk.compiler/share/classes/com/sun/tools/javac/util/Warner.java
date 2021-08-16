/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.EnumSet;

/**
 * An interface to support optional warnings, needed for support of
 * unchecked conversions and unchecked casts.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class Warner {

    private DiagnosticPosition pos = null;
    protected boolean warned = false;
    private EnumSet<LintCategory> nonSilentLintSet = EnumSet.noneOf(LintCategory.class);
    private EnumSet<LintCategory> silentLintSet = EnumSet.noneOf(LintCategory.class);

    public DiagnosticPosition pos() {
        return pos;
    }

    public void warn(LintCategory lint) {
        nonSilentLintSet.add(lint);
    }

    public void silentWarn(LintCategory lint) {
        silentLintSet.add(lint);
    }

    public Warner(DiagnosticPosition pos) {
        this.pos = pos;
    }

    public boolean hasSilentLint(LintCategory lint) {
        return silentLintSet.contains(lint);
    }

    public boolean hasNonSilentLint(LintCategory lint) {
        return nonSilentLintSet.contains(lint);
    }

    public boolean hasLint(LintCategory lint) {
        return hasSilentLint(lint) ||
                hasNonSilentLint(lint);
    }

    public void clear() {
        nonSilentLintSet.clear();
        silentLintSet.clear();
        this.warned = false;
    }

    public Warner() {
        this(null);
    }
}
