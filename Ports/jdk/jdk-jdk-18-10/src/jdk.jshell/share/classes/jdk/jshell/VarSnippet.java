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

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import jdk.jshell.Key.VarKey;

/**
 * Snippet for a variable definition.
 * The Kind is {@link jdk.jshell.Snippet.Kind#VAR}.
 * <p>
 * <code>VarSnippet</code> is immutable: an access to
 * any of its methods will always return the same result.
 * and thus is thread-safe.
 *
 * @since 9
 * @jls 8.3 Field Declarations
 */
public class VarSnippet extends DeclarationSnippet {

    /**A human readable type of the variable. May include intersection types
     * and human readable description of anonymous classes.
     */
    final String typeName;

    /**The full type inferred for "var" variables. May include intersection types
     * and inaccessible types. {@literal null} if enhancing the type is not necessary.
     */
    final String fullTypeName;

    /**The anonymous class declared in the initializer of the "var" variable.
     * These are automatically statically imported when the field is imported.
     */
    final Set<String> anonymousClasses;

     VarSnippet(VarKey key, String userSource, Wrap guts,
            String name, SubKind subkind, String typeName, String fullTypeName,
            Set<String> anonymousClasses, Collection<String> declareReferences,
            DiagList syntheticDiags) {
        super(key, userSource, guts, name, subkind, null, declareReferences,
                null, syntheticDiags);
        this.typeName = typeName;
        this.fullTypeName = fullTypeName;
        this.anonymousClasses = anonymousClasses;
    }

    /**
     * A String representation of the type of the variable.
     * @return the variable type as a String.
     */
    public String typeName() {
        return typeName;
    }

    @Override
    String importLine(JShell state) {
        return "import static " + classFullName() + "." + name() + ";   " +
               anonymousClasses.stream()
                               .map(c -> "import static " + classFullName() + "." + c + ";   ")
                               .collect(Collectors.joining());
    }

}
