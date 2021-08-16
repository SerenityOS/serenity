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

import java.util.Collection;
import jdk.jshell.Key.DeclarationKey;

/**
 * Grouping for all declaration Snippets: variable declarations
 * ({@link jdk.jshell.VarSnippet}), method declarations
 * ({@link jdk.jshell.MethodSnippet}), and type declarations
 * ({@link jdk.jshell.TypeDeclSnippet}).
 * <p>
 * Declaration snippets are unique in that they can be active
 *  with unresolved references:
 * {@link jdk.jshell.Snippet.Status#RECOVERABLE_DEFINED RECOVERABLE_DEFINED} or
 * {@link jdk.jshell.Snippet.Status#RECOVERABLE_NOT_DEFINED RECOVERABLE_NOT_DEFINED}.
 * Unresolved references can be queried with
 * {@link jdk.jshell.JShell#unresolvedDependencies(jdk.jshell.DeclarationSnippet)
 * JShell.unresolvedDependencies(DeclarationSnippet)}.
 * <p>
 * <code>DeclarationSnippet</code> is immutable: an access to
 * any of its methods will always return the same result.
 * and thus is thread-safe.
 *
 * @since 9
 */
public abstract class DeclarationSnippet extends PersistentSnippet {

    private final Wrap corralled;
    private final Collection<String> declareReferences;
    private final Collection<String> bodyReferences;

    DeclarationSnippet(DeclarationKey key, String userSource, Wrap guts,
            String unitName, SubKind subkind, Wrap corralled,
            Collection<String> declareReferences,
            Collection<String> bodyReferences,
            DiagList syntheticDiags) {
        super(key, userSource, guts, unitName, subkind, syntheticDiags);
        this.corralled = corralled;
        this.declareReferences = declareReferences;
        this.bodyReferences = bodyReferences;
    }

    /**** internal access ****/

    /**
     * @return the corralled guts
     */
    @Override
    Wrap corralled() {
        return corralled;
    }

    @Override
    Collection<String> declareReferences() {
        return declareReferences;
    }

    @Override
    Collection<String> bodyReferences() {
        return bodyReferences;
    }

    @Override
    String importLine(JShell state) {
        return "import static " + classFullName() + "." + name() + ";  ";
    }
}
