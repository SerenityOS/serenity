/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell;

import java.util.Locale;
import javax.tools.Diagnostic;

/**
 * Diagnostic information for a Snippet.
 *
 * @since 9
 * @see jdk.jshell.JShell#diagnostics(jdk.jshell.Snippet)
 */
public abstract class Diag {
    // Simplified view on compiler Diagnostic.

    /**
     * In-package creation only.
     */
    Diag() {
    }

    /**
     * Used to signal that no position is available.
     */
    public static final long NOPOS = Diagnostic.NOPOS;

    /**
     * Indicates whether this diagnostic is an error (as opposed to a warning or
     * note).
     *
     * @return {@code true} if this diagnostic is an error; otherwise {@code false}
     */
    public abstract boolean isError();

    /**
     * Returns a character offset from the beginning of the source object
     * associated with this diagnostic that indicates the location of
     * the problem.  In addition, the following must be true:
     *
     * <p>{@code getStartPostion() <= getPosition()}
     * <p>{@code getPosition() <= getEndPosition()}
     *
     * @return character offset from beginning of source; {@link
     * #NOPOS} if the position is not available.
     */
    public abstract long getPosition();

    /**
     * Returns the character offset from the beginning of the file
     * associated with this diagnostic that indicates the start of the
     * problem.
     *
     * @return offset from beginning of file; {@link #NOPOS} if and
     * only if {@link #getPosition()} returns {@link #NOPOS}
     */
    public abstract long getStartPosition();

    /**
     * Returns the character offset from the beginning of the file
     * associated with this diagnostic that indicates the end of the
     * problem.
     *
     * @return offset from beginning of file; {@link #NOPOS} if and
     * only if {@link #getPosition()} returns {@link #NOPOS}
     */
    public abstract long getEndPosition();

    /**
     * Returns a diagnostic code indicating the type of diagnostic.  The
     * code is implementation-dependent and might be {@code null}.
     *
     * @return a diagnostic code
     */
    public abstract String getCode();

    /**
     * Returns a localized message for the given locale.  The actual
     * message is implementation-dependent.  If the locale is {@code
     * null} use the default locale.
     *
     * @param locale a locale; might be {@code null}
     * @return a localized message
     */
    public abstract String getMessage(Locale locale);

    // *** Internal support ***

    /**
     * Internal: If this is from a compile/analyze wrapped in an outer class, extract the snippet.
     * Otherwise null.
     */
    Snippet snippetOrNull() {
        return null;
    }

    /**
     * This is an unreachable-statement error
     */
    boolean isUnreachableError() {
        return getCode().equals("compiler.err.unreachable.stmt");
    }

    /**
     * This is a not-a-statement error
     */
    boolean isNotAStatementError() {
        return getCode().equals("compiler.err.not.stmt");
    }

    /**
     * This is a resolution error.
     */
    boolean isResolutionError() {
        //TODO: try javac RESOLVE_ERROR flag
        return getCode().startsWith("compiler.err.cant.resolve")
                || getCode().equals("compiler.err.cant.apply.symbol");
    }
}
