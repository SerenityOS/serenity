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

import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.visitors.Visitor;


public class Klass extends IRNode {

    public TypeKlass getThisKlass() {
        return thisKlass;
    }

    public List<TypeKlass> getInterfaces() {
        return interfaces;
    }

    public TypeKlass getParentKlass() {
        return parentKlass;
    }

    @Override
    public String getName() {
        return name;
    }

    public enum KlassPart {
        DATA_MEMBERS,
        CONSTRUCTORS,
        REDEFINED_FUNCTIONS,
        OVERRIDEN_FUNCTIONS,
        MEMBER_FUNCTIONS,
        MEMBER_FUNCTIONS_DECLARATIONS,
        PRINT_VARIABLES,
    }

    protected final String name;
    protected final TypeKlass thisKlass;
    private final TypeKlass parentKlass;
    private final ArrayList<TypeKlass> interfaces;

    public Klass(TypeKlass thisKlass, TypeKlass parent,
            ArrayList<TypeKlass> interfaces, String name, int level,
            IRNode variableDeclarations, IRNode constructorDefinitions,
            IRNode functionDefinitions, IRNode abstractFunctionRedefinitions,
            IRNode overridenFunctionRedefitions, IRNode functionDeclarations,
            IRNode printVariablesBlock) {
        super(thisKlass);
        this.thisKlass = thisKlass;
        owner = thisKlass;
        this.parentKlass = parent;
        this.interfaces = interfaces;
        this.name = name;
        this.level = level;
        resizeUpChildren(KlassPart.values().length);
        setChild(KlassPart.DATA_MEMBERS.ordinal(), variableDeclarations);
        setChild(KlassPart.CONSTRUCTORS.ordinal(), constructorDefinitions);
        setChild(KlassPart.REDEFINED_FUNCTIONS.ordinal(), abstractFunctionRedefinitions);
        setChild(KlassPart.OVERRIDEN_FUNCTIONS.ordinal(), overridenFunctionRedefitions);
        setChild(KlassPart.MEMBER_FUNCTIONS.ordinal(), functionDefinitions);
        setChild(KlassPart.MEMBER_FUNCTIONS_DECLARATIONS.ordinal(), functionDeclarations);
        setChild(KlassPart.PRINT_VARIABLES.ordinal(), printVariablesBlock);
    }

    @Override
    public long complexity() {
        return 0;
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }
}
