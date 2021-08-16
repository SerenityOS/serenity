/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.utils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jdk.test.lib.jittester.BinaryOperator;
import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.CatchBlock;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.LocalVariable;
import jdk.test.lib.jittester.NonStaticMemberVariable;
import jdk.test.lib.jittester.Nothing;
import jdk.test.lib.jittester.Operator;
import jdk.test.lib.jittester.OperatorKind;
import jdk.test.lib.jittester.PrintVariables;
import jdk.test.lib.jittester.Statement;
import jdk.test.lib.jittester.StaticMemberVariable;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.TryCatchBlock;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.VariableInitialization;
import jdk.test.lib.jittester.functions.ArgumentDeclaration;
import jdk.test.lib.jittester.functions.Function;
import jdk.test.lib.jittester.functions.FunctionDefinition;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.functions.Return;
import jdk.test.lib.jittester.jtreg.Printer;
import jdk.test.lib.jittester.loops.CounterInitializer;
import jdk.test.lib.jittester.loops.CounterManipulator;
import jdk.test.lib.jittester.loops.For;
import jdk.test.lib.jittester.loops.Loop;
import jdk.test.lib.jittester.loops.LoopingCondition;
import jdk.test.lib.jittester.types.TypeArray;
import jdk.test.lib.jittester.types.TypeKlass;

public class FixedTrees {
    private static final Literal EOL = new Literal("\n", TypeList.STRING);

    public static FunctionDefinition printVariablesAsFunction(PrintVariables node) {
        TypeKlass owner = node.getOwner();

        ArrayList<IRNode> nodes = new ArrayList<>();

        VariableInfo resultInfo = new VariableInfo("result", node.getOwner(), TypeList.STRING, VariableInfo.LOCAL);
        nodes.add(new Statement(new VariableInitialization(resultInfo, new Literal("[", TypeList.STRING)), true));
        LocalVariable resultVar = new LocalVariable(resultInfo);

        List<Symbol> vars = node.getVars();

        TypeKlass printerKlass = new TypeKlass(Printer.class.getName());
        VariableInfo thisInfo = new VariableInfo("this", node.getOwner(),
                node.getOwner(), VariableInfo.LOCAL | VariableInfo.INITIALIZED);

        LocalVariable thisVar = new LocalVariable(thisInfo);

        for (int i = 0; i < vars.size(); i++) {
            Symbol v = vars.get(i);
            nodes.add(new Statement(new BinaryOperator(OperatorKind.COMPOUND_ADD, TypeList.STRING, resultVar,
                    new Literal(v.owner.getName() + "." + v.name + " = ", TypeList.STRING)), true));
            VariableInfo argInfo = new VariableInfo("arg", printerKlass,
                    v.type instanceof TypeKlass ? TypeList.OBJECT : v.type,
                    VariableInfo.LOCAL | VariableInfo.INITIALIZED);
            FunctionInfo printInfo = new FunctionInfo("print", printerKlass,
                    TypeList.STRING, 0,  FunctionInfo.PUBLIC | FunctionInfo.STATIC, argInfo);
            Function call = new Function(owner, printInfo, null);
            VariableInfo varInfo = new VariableInfo(v.name, v.owner, v.type, v.flags);
            if (v.isStatic()) {
                call.addChild(new StaticMemberVariable(v.owner, varInfo));
            } else {
                call.addChild(new NonStaticMemberVariable(thisVar, varInfo));
            }
            nodes.add(new Statement(new BinaryOperator(OperatorKind.COMPOUND_ADD, TypeList.STRING, resultVar,
                    call), true));
            if (i < vars.size() - 1) {
                nodes.add(new Statement(new BinaryOperator(OperatorKind.COMPOUND_ADD, TypeList.STRING, resultVar,
                        EOL), true));
            }
        }
        nodes.add(new Statement(
                new BinaryOperator(OperatorKind.COMPOUND_ADD, TypeList.STRING, resultVar, new Literal("]\n", TypeList.STRING)),
                true));

        Block block = new Block(node.getOwner(), TypeList.STRING, nodes, 1);
        FunctionInfo toStringInfo = new FunctionInfo("toString", owner, TypeList.STRING, 0L, FunctionInfo.PUBLIC, thisInfo);
        return new FunctionDefinition(toStringInfo, new ArrayList<>(), block, new Return(resultVar));
    }

    public static FunctionDefinition generateMainOrExecuteMethod(TypeKlass owner, boolean isMain) {
        Nothing nothing = new Nothing();
        ArrayList<IRNode> testCallNodeContent = new ArrayList<>();
        VariableInfo tInfo = new VariableInfo("t", owner, owner, VariableInfo.LOCAL);
        LocalVariable tVar = new LocalVariable(tInfo);
        Function testCallNode = new Function(owner, new FunctionInfo("test", owner, TypeList.VOID,
                0L, FunctionInfo.PRIVATE, tInfo), null);
        testCallNode.addChild(tVar);
        testCallNodeContent.add(new Statement(testCallNode, true));
        // { t.test(); }
        Block testCallNodeBlock = new Block(owner, TypeList.VOID, testCallNodeContent, 4);

        IRNode tryNode = testCallNodeBlock;
        if (isMain) {
            VariableInfo iInfo = new VariableInfo("i", owner, TypeList.INT, VariableInfo.LOCAL);
            LocalVariable iVar = new LocalVariable(iInfo);
            Operator increaseCounter = new BinaryOperator(OperatorKind.ASSIGN, TypeList.INT,
                    iVar,
                    new BinaryOperator(OperatorKind.ADD, TypeList.INT,
                            iVar, new Literal(1, TypeList.INT)));
            Loop loop = new Loop();
            Block emptyBlock = new Block(owner, TypeList.VOID, new LinkedList<>(), 3);
            loop.initialization = new CounterInitializer(iInfo, new Literal(0, TypeList.INT));
            loop.manipulator = new CounterManipulator(new Statement(increaseCounter, false));
            loop.condition = new LoopingCondition(new BinaryOperator(OperatorKind.LT, TypeList.BOOLEAN, iVar,
                    new Literal(150000, TypeList.INT)));
            For forNode = new For(4, loop, 150000, emptyBlock, new Statement(nothing, false),
                    new Statement(nothing, false), testCallNodeBlock, emptyBlock, emptyBlock);
            tryNode = forNode;
        }

        FunctionInfo constrInfo = new FunctionInfo(owner.getName(), owner, owner, 0, FunctionInfo.PUBLIC);
        Function testConstructor = new Function(owner, constrInfo, null);
        // Test t = new Test()
        VariableInitialization testInit = new VariableInitialization(tInfo, testConstructor);

        TypeKlass throwableKlass = new TypeKlass("java.lang.Throwable");
        List<Type> throwables = new ArrayList<>();
        throwables.add(throwableKlass);

        TypeKlass printStreamKlass = new TypeKlass("java.io.PrintStream");
        FunctionInfo printInfo = new FunctionInfo("print", printStreamKlass,
                TypeList.VOID, 0, FunctionInfo.PUBLIC,
                new VariableInfo("this", owner, printStreamKlass, VariableInfo.LOCAL | VariableInfo.INITIALIZED),
                new VariableInfo("t", owner, TypeList.OBJECT,
                        VariableInfo.LOCAL  | VariableInfo.INITIALIZED));
        TypeKlass systemKlass = new TypeKlass("java.lang.System");
        StaticMemberVariable systemErrVar = new StaticMemberVariable(owner,
                new VariableInfo("err", systemKlass, printStreamKlass, VariableInfo.STATIC | VariableInfo.PUBLIC));

        LocalVariable exVar = new LocalVariable(
                new VariableInfo("ex", owner, throwableKlass, VariableInfo.LOCAL | VariableInfo.INITIALIZED));
        TypeKlass classKlass = new TypeKlass("java.lang.Class");
        FunctionInfo getClassInfo = new FunctionInfo("getClass", TypeList.OBJECT,
                classKlass, 0, FunctionInfo.PUBLIC,
                new VariableInfo("this", owner, TypeList.OBJECT, VariableInfo.LOCAL | VariableInfo.INITIALIZED));
        Function getClass = new Function(TypeList.OBJECT, getClassInfo, Arrays.asList(exVar));
        FunctionInfo getNameInfo = new FunctionInfo("getName", classKlass,
                TypeList.STRING, 0, FunctionInfo.PUBLIC,
                new VariableInfo("this", owner, TypeList.OBJECT, VariableInfo.LOCAL | VariableInfo.INITIALIZED));
        Function getName = new Function(classKlass, getNameInfo, Arrays.asList(getClass));
        ArrayList<IRNode> printExceptionBlockContent = new ArrayList<>();
        // { System.err.print(ex.getClass().getName()); System.err.print("\n"); }
        printExceptionBlockContent.add(new Statement(
            new Function(printStreamKlass, printInfo, Arrays.asList(systemErrVar, getName)), true));
        printExceptionBlockContent.add(new Statement(
            new Function(printStreamKlass, printInfo, Arrays.asList(systemErrVar, EOL)), true));

        Block printExceptionBlock = new Block(owner, TypeList.VOID, printExceptionBlockContent, 3);
        List<CatchBlock> catchBlocks1 = new ArrayList<>();
        catchBlocks1.add(new CatchBlock(printExceptionBlock, throwables, 3));
        List<CatchBlock> catchBlocks2 = new ArrayList<>();
        catchBlocks2.add(new CatchBlock(printExceptionBlock, throwables, 3));
        List<CatchBlock> catchBlocks3 = new ArrayList<>();
        catchBlocks3.add(new CatchBlock(printExceptionBlock, throwables, 2));

        TryCatchBlock tryCatch1 = new TryCatchBlock(tryNode, nothing, catchBlocks1, 3);
        List<IRNode> printArgs = new ArrayList<>();
        VariableInfo systemOutInfo = new VariableInfo("out", systemKlass, printStreamKlass,
                VariableInfo.STATIC | VariableInfo.PUBLIC);
        StaticMemberVariable systemOutVar = new StaticMemberVariable(owner, systemOutInfo);
        printArgs.add(systemOutVar);
        printArgs.add(tVar);
        Function print = new Function(printStreamKlass, printInfo, printArgs);
        ArrayList<IRNode> printBlockContent = new ArrayList<>();
        printBlockContent.add(new Statement(print, true));
        Block printBlock = new Block(owner, TypeList.VOID, printBlockContent, 3);
        TryCatchBlock tryCatch2 = new TryCatchBlock(printBlock, nothing, catchBlocks2, 3);

        List<IRNode> mainTryCatchBlockContent = new ArrayList<>();
        mainTryCatchBlockContent.add(new Statement(testInit, true));
        mainTryCatchBlockContent.add(tryCatch1);
        mainTryCatchBlockContent.add(tryCatch2);
        Block mainTryCatchBlock = new Block(owner, TypeList.VOID, mainTryCatchBlockContent, 2);
        TryCatchBlock mainTryCatch = new TryCatchBlock(mainTryCatchBlock, nothing, catchBlocks3, 2);
        ArrayList<IRNode> bodyContent = new ArrayList<>();
        bodyContent.add(mainTryCatch);
        Block funcBody = new Block(owner, TypeList.VOID, bodyContent, 1);

        // static main(String[] args)V or static execute()V
        VariableInfo mainArgs = new VariableInfo("args", owner,
                new TypeArray(TypeList.STRING, 1), VariableInfo.LOCAL);
        FunctionInfo fInfo = isMain
                ? new FunctionInfo("main", owner, TypeList.VOID, 0, FunctionInfo.PUBLIC | FunctionInfo.STATIC, mainArgs)
                : new FunctionInfo("execute", owner, TypeList.VOID, 0, FunctionInfo.PUBLIC | FunctionInfo.STATIC);
        ArrayList<ArgumentDeclaration> argDecl = new ArrayList<>();
        if (isMain) {
            argDecl.add(new ArgumentDeclaration(mainArgs));
        }
        return new FunctionDefinition(fInfo, argDecl, funcBody, new Return(nothing));
    }
}
