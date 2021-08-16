/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.jshell.Key.ImportKey;

/**
 * Snippet for an import declaration.
 * The Kind is {@link jdk.jshell.Snippet.Kind#IMPORT}.
 * <p>
 * {@code ImportSnippet} is immutable: an access to
 * any of its methods will always return the same result.
 * and thus is thread-safe.
 *
 * @since 9
 * @jls 7.5 Import Declarations
 */
public class ImportSnippet extends PersistentSnippet {

    final String fullname;
    final String fullkey;
    final boolean isStatic;
    final boolean isStar;

    ImportSnippet(ImportKey key, String userSource, Wrap guts,
            String fullname, String name, SubKind subkind, String fullkey,
            boolean isStatic, boolean isStar) {
        super(key, userSource, guts, name, subkind, null);
        this.fullname = fullname;
        this.fullkey = fullkey;
        this.isStatic = isStatic;
        this.isStar = isStar;
    }

    /**
     * The identifying name of the import. For on-demand imports
     * ({@link jdk.jshell.Snippet.SubKind#TYPE_IMPORT_ON_DEMAND_SUBKIND} or
     * ({@link jdk.jshell.Snippet.SubKind#STATIC_IMPORT_ON_DEMAND_SUBKIND})
     * that is the full specifier including any
     * qualifiers and the asterisks. For single imports
     * ({@link jdk.jshell.Snippet.SubKind#SINGLE_TYPE_IMPORT_SUBKIND} or
     * ({@link jdk.jshell.Snippet.SubKind#SINGLE_STATIC_IMPORT_SUBKIND}),
     * it is the imported name. That is, the unqualified name.
     * @return the name of the import.
     */
    @Override
    public String name() {
        return key().name();
    }

    /**
     *
     * The qualified name of the import. For any imports
     * ({@link jdk.jshell.Snippet.SubKind#TYPE_IMPORT_ON_DEMAND_SUBKIND},
     * ({@link jdk.jshell.Snippet.SubKind#STATIC_IMPORT_ON_DEMAND_SUBKIND}),
     * ({@link jdk.jshell.Snippet.SubKind#SINGLE_TYPE_IMPORT_SUBKIND} or
     * ({@link jdk.jshell.Snippet.SubKind#SINGLE_STATIC_IMPORT_SUBKIND})
     * that is the full specifier including any
     * qualifiers and the asterisks.
     * @return the fullname of the import
     */
    public String fullname() {
        return fullname;
    }

    /**
     * Indicates whether this snippet represents a static import.
     *
     * @return {@code true} if this snippet represents a static import;
     * otherwise {@code false}
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**** internal access ****/

    @Override
    ImportKey key() {
        return (ImportKey) super.key();
    }

    @Override
    String importLine(JShell state) {
        return guts().wrapped();
    }
}
