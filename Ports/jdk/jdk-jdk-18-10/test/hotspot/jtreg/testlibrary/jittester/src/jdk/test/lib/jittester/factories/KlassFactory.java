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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.classes.Klass;
import jdk.test.lib.jittester.functions.FunctionDeclarationBlock;
import jdk.test.lib.jittester.functions.FunctionDefinition;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class KlassFactory extends Factory<Klass> {
    private final String name;
    private final long complexityLimit;
    private final int statementsInFunctionLimit;
    private final int operatorLimit;
    private final int memberFunctionsArgLimit;
    private final int level;
    private final ArrayList<TypeKlass> interfaces;
    private TypeKlass thisKlass;
    private TypeKlass parent;
    private int memberFunctionsLimit;

    KlassFactory(String name, long complexityLimit,
            int memberFunctionsLimit, int memberFunctionsArgLimit, int statementsInFunctionLimit,
            int operatorLimit, int level) {
        this.name = name;
        this.complexityLimit = complexityLimit;
        this.memberFunctionsLimit = memberFunctionsLimit;
        this.memberFunctionsArgLimit = memberFunctionsArgLimit;
        this.statementsInFunctionLimit = statementsInFunctionLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
        interfaces = new ArrayList<>();
    }

    @Override
    public Klass produce() throws ProductionFailedException {
        HashSet<Symbol> abstractSet = new HashSet<>();
        HashSet<Symbol> overrideSet = new HashSet<>();
        thisKlass = new TypeKlass(name);
        // Do we want to inherit something?
        if (!ProductionParams.disableInheritance.value()) {
            inheritClass();
            inheritInterfaces();
            // Now, we should carefully construct a set of all methods with are still abstract.
            // In order to do that, we will make two sets of methods: abstract and non-abstract.
            // Then by substracting non-abstract from abstract we'll get what we want.
            HashSet<Symbol> nonAbstractSet = new HashSet<>();
            for (Symbol symbol : SymbolTable.getAllCombined(thisKlass, FunctionInfo.class)) {
                FunctionInfo functionInfo = (FunctionInfo) symbol;
                // There could be multiple definitions or declarations encountered,
                // but all we interested in are signatures.
                if ((functionInfo.flags & FunctionInfo.ABSTRACT) > 0) {
                    abstractSet.add(functionInfo);
                } else {
                    nonAbstractSet.add(functionInfo);
                }
            }
            abstractSet.removeAll(nonAbstractSet);
            // We may randomly remove some elements from the abstract set in order to force generation
            // of an abstract class.
            if (PseudoRandom.randomBoolean(0.2)) {
                // so, we want to be abstract..
                for (Iterator<Symbol> i = abstractSet.iterator(); i.hasNext();) {
                    i.next();
                    if (PseudoRandom.randomBoolean(0.2)) {
                        thisKlass.setAbstract();
                        i.remove();
                    }
                }
            }
            if (PseudoRandom.randomBoolean(0.2)) {
                int redefineLimit = (int) (memberFunctionsLimit * PseudoRandom.random());
                if (redefineLimit > 0) {
                    // We may also select some functions from the hierarchy that we want
                    // to redefine..
                    int i = 0;
                    ArrayList<Symbol> shuffledNonAbstractSet = new ArrayList<>(nonAbstractSet);
                    PseudoRandom.shuffle(shuffledNonAbstractSet);
                    for (Symbol symbol : shuffledNonAbstractSet) {
                        if (++i > redefineLimit) {
                            break;
                        }
                        FunctionInfo functionInfo = (FunctionInfo) symbol;
                        if ((functionInfo.flags & FunctionInfo.FINAL) > 0) {
                            continue;
                        }
                        overrideSet.add(functionInfo);
                    }
                }
            }
            memberFunctionsLimit -= abstractSet.size() + overrideSet.size();
            // Ok, remove the symbols from the table which are going to be overrided.
            // Because the redefiner would probably modify them and put them back into table.
            for (Symbol symbol : abstractSet) {
                SymbolTable.remove(symbol);
            }
            for (Symbol symbol : overrideSet) {
                SymbolTable.remove(symbol);
            }
        } else {
            parent = TypeList.OBJECT;
            thisKlass.addParent(parent.getName());
            thisKlass.setParent(parent);
            parent.addChild(name);
        }
        SymbolTable.add(new VariableInfo("this", thisKlass, thisKlass,
                VariableInfo.FINAL | VariableInfo.LOCAL | VariableInfo.INITIALIZED));
        IRNode variableDeclarations = null;
        IRNode constructorDefinitions = null;
        IRNode functionDefinitions = null;
        IRNode functionDeclarations = null;
        IRNode abstractFunctionsRedefinitions = null;
        IRNode overridenFunctionsRedefinitions = null;
        IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass(thisKlass)
                .setExceptionSafe(true);
        try {
            builder.setLevel(level + 1)
                    .setOperatorLimit(operatorLimit)
                    .setStatementLimit(statementsInFunctionLimit)
                    .setMemberFunctionsArgLimit(memberFunctionsArgLimit);
            variableDeclarations = builder.setComplexityLimit((long) (complexityLimit * 0.001 * PseudoRandom.random()))
                    .getVariableDeclarationBlockFactory().produce();
            if (!ProductionParams.disableFunctions.value()) {
                // Try to implement all methods.
                abstractFunctionsRedefinitions = builder.setComplexityLimit((long) (complexityLimit * 0.3 * PseudoRandom.random()))
                        .setLevel(level + 1)
                        .getFunctionRedefinitionBlockFactory(abstractSet)
                        .produce();
                overridenFunctionsRedefinitions = builder.setComplexityLimit((long) (complexityLimit * 0.3 * PseudoRandom.random()))
                        .getFunctionRedefinitionBlockFactory(overrideSet)
                        .produce();
                if (PseudoRandom.randomBoolean(0.2)) { // wanna be abstract ?
                    functionDeclarations = builder.setMemberFunctionsLimit((int) (memberFunctionsLimit * 0.2
                                    * PseudoRandom.random()))
                            .getFunctionDeclarationBlockFactory()
                            .produce();
                    if (((FunctionDeclarationBlock) functionDeclarations).size() > 0) {
                        thisKlass.setAbstract();
                    }
                }
                functionDefinitions = builder.setComplexityLimit((long) (complexityLimit * 0.5 * PseudoRandom.random()))
                        .setMemberFunctionsLimit((int) (memberFunctionsLimit * 0.6
                                * PseudoRandom.random()))
                        .setFlags(FunctionInfo.NONE)
                        .getFunctionDefinitionBlockFactory()
                        .produce();
                constructorDefinitions = builder.setComplexityLimit((long) (complexityLimit * 0.2 * PseudoRandom.random()))
                        .setMemberFunctionsLimit((int) (memberFunctionsLimit * 0.2
                                * PseudoRandom.random()))
                        .setStatementLimit(statementsInFunctionLimit)
                        .setOperatorLimit(operatorLimit)
                        .setLevel(level + 1)
                        .getConstructorDefinitionBlockFactory()
                        .produce();
            }
        } catch (ProductionFailedException e) {
            System.out.println("Exception during klass production process:");
            e.printStackTrace(System.out);
            throw e;
        } finally {
            SymbolTable.remove(new Symbol("this", thisKlass, thisKlass, VariableInfo.NONE));
        }
        // a non-abstract class can be final, so we should allow this to happen.
        if (!ProductionParams.disableFinalClasses.value() && !thisKlass.isAbstract()
                && PseudoRandom.randomBoolean()) {
            thisKlass.setFinal();
        }
        TypeList.add(thisKlass);
        IRNode printVariables = builder.setLevel(2).getPrintVariablesFactory().produce();
        return new Klass(thisKlass, parent, interfaces, name, level,
                variableDeclarations, constructorDefinitions, functionDefinitions,
                abstractFunctionsRedefinitions, overridenFunctionsRedefinitions,
                functionDeclarations, printVariables);
    }

   private void inheritClass() {
        // Grab all Klasses from the TypeList and select one to be a parent
        LinkedList<Type> probableParents = new LinkedList<>(TypeList.getAll());
        for (Iterator<Type> i = probableParents.iterator(); i.hasNext();) {
            Type klass = i.next();
            if (!(klass instanceof TypeKlass) || ((TypeKlass) klass).isFinal()
                    || ((TypeKlass) klass).isInterface()) {
                // we can not derive from finals and interfaces
                i.remove();
            }
        }
        if (probableParents.isEmpty()) {
            parent = TypeList.OBJECT;
        } else {
            parent = (TypeKlass) PseudoRandom.randomElement(probableParents);
        }
        thisKlass.addParent(parent.getName());
        thisKlass.setParent(parent);
        parent.addChild(name);
        for (Symbol symbol : SymbolTable.getAllCombined(parent)) {
            if ((symbol.flags & Symbol.PRIVATE) == 0) {
                Symbol symbolCopy = symbol.deepCopy();
                if (symbolCopy instanceof FunctionInfo) {
                    FunctionInfo functionInfo = (FunctionInfo) symbolCopy;
                    if (functionInfo.isConstructor()) {
                        continue;
                    }
                    if ((functionInfo.flags & FunctionInfo.STATIC) == 0) {
                        functionInfo.argTypes.get(0).type = thisKlass;
                    }
                }
                symbolCopy.owner = thisKlass;
                SymbolTable.add(symbolCopy);
            }
        }
    }

    private void inheritInterfaces() {
        // Select interfaces that we'll implement.
        LinkedList<Type> probableInterfaces = new LinkedList<>(TypeList.getAll());
        for (Iterator<Type> i = probableInterfaces.iterator(); i.hasNext();) {
            Type klass = i.next();
            if (!(klass instanceof TypeKlass) || !((TypeKlass) klass).isInterface()) {
                i.remove();
            }
        }
        PseudoRandom.shuffle(probableInterfaces);
        int implLimit = (int) (ProductionParams.implementationLimit.value() * PseudoRandom.random());
        // Mulitiple inheritance compatibility check
        compatibility_check:
        for (Iterator<Type> i = probableInterfaces.iterator(); i.hasNext() && implLimit > 0; implLimit--) {
            TypeKlass iface = (TypeKlass) i.next();
            ArrayList<Symbol> ifaceFuncSet = SymbolTable.getAllCombined(iface, FunctionInfo.class);
            for (Symbol symbol : SymbolTable.getAllCombined(thisKlass, FunctionInfo.class)) {
                if (FunctionDefinition.isInvalidOverride((FunctionInfo) symbol, ifaceFuncSet)) {
                    continue compatibility_check;
                }
            }
            interfaces.add(iface);
            iface.addChild(name);
            thisKlass.addParent(iface.getName());
            for (Symbol symbol : SymbolTable.getAllCombined(iface, FunctionInfo.class)) {
                FunctionInfo functionInfo = (FunctionInfo) symbol.deepCopy();
                functionInfo.owner = thisKlass;
                functionInfo.argTypes.get(0).type = thisKlass;
                SymbolTable.add(functionInfo);
            }
        }
    }
}
