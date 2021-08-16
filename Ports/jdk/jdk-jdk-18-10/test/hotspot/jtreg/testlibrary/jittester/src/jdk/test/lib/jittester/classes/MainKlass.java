/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.classes;

import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.visitors.Visitor;

public class MainKlass extends IRNode {
    public enum MainKlassPart {
        DATA_MEMBERS,
        MEMBER_FUNCTIONS,
        TEST_FUNCTION,
        PRINT_VARIABLES,
    }

    private final String name;
    private final TypeKlass thisKlass;

    public MainKlass(String name, TypeKlass thisKlass, IRNode variableDeclarations,
            IRNode functionDefinitions, IRNode testFunction, IRNode printVariables) {
        super(thisKlass);
        addChild(variableDeclarations);
        addChild(functionDefinitions);
        addChild(testFunction);
        addChild(printVariables);
        this.name = name;
        this.thisKlass = thisKlass;
    }

    @Override
    public long complexity() {
        IRNode dataMembers = getChild(MainKlassPart.DATA_MEMBERS.ordinal());
        IRNode testFunction = getChild(MainKlassPart.TEST_FUNCTION.ordinal());
        return dataMembers.complexity() + testFunction.complexity();
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    @Override
    public String getName() {
        return name;
    }

    public TypeKlass getThisKlass() {
        return thisKlass;
    }
}
