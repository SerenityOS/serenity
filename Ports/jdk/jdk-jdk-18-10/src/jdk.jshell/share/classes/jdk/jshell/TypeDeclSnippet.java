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
import jdk.jshell.Key.TypeDeclKey;

/**
 * Snippet for a type definition (a class, interface, enum, or annotation
 * interface definition).
 * The Kind is {@link jdk.jshell.Snippet.Kind#TYPE_DECL}.
 * <p>
 * <code>TypeDeclSnippet</code> is immutable: an access to
 * any of its methods will always return the same result.
 * and thus is thread-safe.
 *
 * @since 9
 */
public class TypeDeclSnippet extends DeclarationSnippet {

    TypeDeclSnippet(TypeDeclKey key, String userSource, Wrap guts,
            String unitName, SubKind subkind, Wrap corralled,
            Collection<String> declareReferences,
            Collection<String> bodyReferences,
            DiagList syntheticDiags) {
        super(key, userSource, guts, unitName, subkind, corralled,
                declareReferences, bodyReferences, syntheticDiags);
    }

    /**** internal access ****/

    @Override
    TypeDeclKey key() {
        return (TypeDeclKey) super.key();
    }
}
