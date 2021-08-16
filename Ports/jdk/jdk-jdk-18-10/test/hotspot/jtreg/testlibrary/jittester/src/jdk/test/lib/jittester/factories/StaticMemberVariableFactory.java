/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.test.lib.jittester.factories;

import java.util.ArrayList;

import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.StaticMemberVariable;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class StaticMemberVariableFactory extends Factory<StaticMemberVariable> {
    private final Type type;
    private final int flags;
    private final Type ownerClass;

    StaticMemberVariableFactory(TypeKlass ownerClass, Type type, int flags) {
        this.ownerClass = ownerClass;
        this.type = type;
        this.flags = flags;
    }

    @Override
    public StaticMemberVariable produce() throws ProductionFailedException {
        // Get the variables of the requested type from SymbolTable
        ArrayList<Symbol> variables = new ArrayList<>(SymbolTable.get(type, VariableInfo.class));
        if (!variables.isEmpty()) {
            PseudoRandom.shuffle(variables);
            for (Symbol symbol : variables) {
                VariableInfo varInfo = (VariableInfo) symbol;
                if ((varInfo.flags & VariableInfo.FINAL) == (flags & VariableInfo.FINAL)
                        && (varInfo.flags & VariableInfo.INITIALIZED) == (flags & VariableInfo.INITIALIZED)
                        && (varInfo.flags & VariableInfo.STATIC) > 0) {
                    return new StaticMemberVariable((TypeKlass) ownerClass, varInfo);
                }
            }
        }
        throw new ProductionFailedException();
    }
}
