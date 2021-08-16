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

package jdk.test.lib.jittester.visitors;

import jdk.test.lib.jittester.BinaryOperator;
import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.Break;
import jdk.test.lib.jittester.CastOperator;
import jdk.test.lib.jittester.CatchBlock;
import jdk.test.lib.jittester.Continue;
import jdk.test.lib.jittester.Declaration;
import jdk.test.lib.jittester.If;
import jdk.test.lib.jittester.Initialization;
import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.LocalVariable;
import jdk.test.lib.jittester.NonStaticMemberVariable;
import jdk.test.lib.jittester.Nothing;
import jdk.test.lib.jittester.PrintVariables;
import jdk.test.lib.jittester.Statement;
import jdk.test.lib.jittester.StaticMemberVariable;
import jdk.test.lib.jittester.Switch;
import jdk.test.lib.jittester.TernaryOperator;
import jdk.test.lib.jittester.Throw;
import jdk.test.lib.jittester.TryCatchBlock;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.UnaryOperator;
import jdk.test.lib.jittester.VariableDeclaration;
import jdk.test.lib.jittester.VariableDeclarationBlock;
import jdk.test.lib.jittester.arrays.ArrayCreation;
import jdk.test.lib.jittester.arrays.ArrayElement;
import jdk.test.lib.jittester.arrays.ArrayExtraction;
import jdk.test.lib.jittester.classes.ClassDefinitionBlock;
import jdk.test.lib.jittester.classes.Interface;
import jdk.test.lib.jittester.classes.Klass;
import jdk.test.lib.jittester.classes.MainKlass;
import jdk.test.lib.jittester.functions.ArgumentDeclaration;
import jdk.test.lib.jittester.functions.ConstructorDefinition;
import jdk.test.lib.jittester.functions.ConstructorDefinitionBlock;
import jdk.test.lib.jittester.functions.Function;
import jdk.test.lib.jittester.functions.FunctionDeclaration;
import jdk.test.lib.jittester.functions.FunctionDeclarationBlock;
import jdk.test.lib.jittester.functions.FunctionDefinition;
import jdk.test.lib.jittester.functions.FunctionDefinitionBlock;
import jdk.test.lib.jittester.functions.FunctionRedefinition;
import jdk.test.lib.jittester.functions.FunctionRedefinitionBlock;
import jdk.test.lib.jittester.functions.Return;
import jdk.test.lib.jittester.functions.StaticConstructorDefinition;
import jdk.test.lib.jittester.loops.CounterInitializer;
import jdk.test.lib.jittester.loops.CounterManipulator;
import jdk.test.lib.jittester.loops.DoWhile;
import jdk.test.lib.jittester.loops.For;
import jdk.test.lib.jittester.loops.LoopingCondition;
import jdk.test.lib.jittester.loops.While;
import jdk.test.lib.jittester.types.TypeArray;

public interface Visitor<T> {
    T visit(ArgumentDeclaration node);
    T visit(ArrayCreation node);
    T visit(ArrayElement node);
    T visit(ArrayExtraction node);
    T visit(BinaryOperator node);
    T visit(Block node);
    T visit(Break node);
    T visit(CastOperator node);
    T visit(ClassDefinitionBlock node);
    T visit(ConstructorDefinition node);
    T visit(ConstructorDefinitionBlock node);
    T visit(Continue node);
    T visit(CounterInitializer node);
    T visit(CounterManipulator node);
    T visit(Declaration node);
    T visit(DoWhile node);
    T visit(For node);
    T visit(Function node);
    T visit(FunctionDeclaration node);
    T visit(FunctionDeclarationBlock node);
    T visit(FunctionDefinition node);
    T visit(FunctionDefinitionBlock node);
    T visit(FunctionRedefinition node);
    T visit(FunctionRedefinitionBlock node);
    T visit(If node);
    T visit(Initialization node);
    T visit(Interface node);
    T visit(Klass node);
    T visit(Literal node);
    T visit(LocalVariable node);
    T visit(LoopingCondition node);
    T visit(MainKlass node);
    T visit(NonStaticMemberVariable node);
    T visit(Nothing node);
    T visit(PrintVariables node);
    T visit(Return node);
    T visit(Throw node);
    T visit(Statement node);
    T visit(StaticConstructorDefinition node);
    T visit(StaticMemberVariable node);
    T visit(Switch node);
    T visit(TernaryOperator node);
    T visit(Type node);
    T visit(TypeArray node);
    T visit(UnaryOperator node);
    T visit(VariableDeclaration node);
    T visit(VariableDeclarationBlock node);
    T visit(While node);
    T visit(CatchBlock node);
    T visit(TryCatchBlock node);
}
