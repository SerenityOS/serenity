/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Exception reported on attempting to execute a
 * {@link jdk.jshell.Snippet.Status#RECOVERABLE_DEFINED RECOVERABLE_DEFINED}
 * snippet.
 * <p>
 * The stack can be queried by methods on <code>Exception</code>.
 * Note that in stack trace frames representing JShell Snippets,
 * <code>StackTraceElement.getFileName()</code> will return "#" followed by
 * the Snippet id and for snippets without a method name (for example an
 * expression) <code>StackTraceElement.getName()</code> will be the
 * empty string.
 *
 * @since 9
 */
@SuppressWarnings("serial")             // serialVersionUID intentionally omitted
public class UnresolvedReferenceException extends JShellException {

    final DeclarationSnippet snippet;

    UnresolvedReferenceException(DeclarationSnippet snippet, StackTraceElement[] stackElements) {
        super("Attempt to use definition snippet with unresolved references in " + snippet);
        this.snippet = snippet;
        this.setStackTrace(stackElements);
    }

    /**
     * Return the Snippet which has the unresolved reference(s).
     * @return the <code>Snippet</code> of the
     * {@link jdk.jshell.Snippet.Status#RECOVERABLE_DEFINED RECOVERABLE_DEFINED}
     * definition snippet.
     */
    public DeclarationSnippet getSnippet() {
        return snippet;
    }
}
