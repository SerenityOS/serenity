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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import jdk.jshell.Key.TypeDeclKey;
import jdk.jshell.Key.ErroneousKey;
import jdk.jshell.Key.ExpressionKey;
import jdk.jshell.Key.ImportKey;
import jdk.jshell.Key.MethodKey;
import jdk.jshell.Key.StatementKey;
import jdk.jshell.Key.VarKey;
import jdk.jshell.Snippet.SubKind;
import static jdk.jshell.Snippet.SubKind.SINGLE_STATIC_IMPORT_SUBKIND;
import static jdk.jshell.Snippet.SubKind.SINGLE_TYPE_IMPORT_SUBKIND;

/**
 * Maps signature to Key by Kind.
 * @author Robert Field
 */
class KeyMap {
    private final JShell state;

    KeyMap(JShell state) {
        this.state = state;
    }

    private final Map<String, TypeDeclKey> classMap = new LinkedHashMap<>();
    private final Map<String, MethodKey> methodMap = new LinkedHashMap<>();
    private final Map<String, VarKey> varMap = new LinkedHashMap<>();
    private final Map<String, ImportKey> importMap = new LinkedHashMap<>();

    TypeDeclKey keyForClass(String name) {
        return classMap.computeIfAbsent(name, k -> new TypeDeclKey(state, name));
    }

    MethodKey keyForMethod(String name, String parameterTypes) {
        return methodMap.computeIfAbsent(name + ":" + parameterTypes,
                                         k -> new MethodKey(state, name, parameterTypes));
    }

    VarKey keyForVariable(String name) {
        return varMap.computeIfAbsent(name, k -> new VarKey(state, name));
    }

    ImportKey keyForImport(String name, SubKind snippetKind) {
        return importMap.computeIfAbsent(name + ":"
                + ((snippetKind == SINGLE_TYPE_IMPORT_SUBKIND || snippetKind == SINGLE_STATIC_IMPORT_SUBKIND)
                        ? "single"
                        : snippetKind.toString()),
                k -> new ImportKey(state, name, snippetKind));
    }

    StatementKey keyForStatement() {
        return new StatementKey(state);
    }

    ExpressionKey keyForExpression(String name, String type) {
        return new ExpressionKey(state, name, type);
    }

    ErroneousKey keyForErroneous() {
        return new ErroneousKey(state);
    }

    boolean doesVariableNameExist(String name) {
        return varMap.get(name) != null;
    }

    Stream<ImportKey> importKeys() {
        return importMap.values().stream();
    }
}
