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

import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.If;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Switch;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.utils.TypeUtil;
import jdk.test.lib.jittester.loops.DoWhile;
import jdk.test.lib.jittester.loops.For;
import jdk.test.lib.jittester.loops.While;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

import java.util.ArrayList;
import java.util.List;

class BlockFactory extends Factory<Block> {
    private final Type returnType;
    private final long complexityLimit;
    private final int statementLimit;
    private final int operatorLimit;
    private final boolean subBlock;
    private final boolean canHaveBreaks;
    private final boolean canHaveContinues;
    private final boolean canHaveReturn;
    private final boolean canHaveThrow;
    private final int level;
    private final TypeKlass ownerClass;

    BlockFactory(TypeKlass klass, Type returnType, long complexityLimit, int statementLimit,
                 int operatorLimit, int level, boolean subBlock, boolean canHaveBreaks,
                 boolean canHaveContinues, boolean canHaveReturn, boolean canHaveThrows) {
        this.ownerClass = klass;
        this.returnType = returnType;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
        this.subBlock = subBlock;
        this.canHaveBreaks = canHaveBreaks;
        this.canHaveContinues = canHaveContinues;
        this.canHaveReturn = canHaveReturn;
        this.canHaveThrow = canHaveThrows;
    }

    @Override
    public Block produce() throws ProductionFailedException {
        if (statementLimit > 0 && complexityLimit > 0) {
            List<IRNode> content = new ArrayList<>();
            int slimit = PseudoRandom.randomNotZero(statementLimit);
            long climit = complexityLimit;
            IRNodeBuilder builder = new IRNodeBuilder()
                    .setOperatorLimit(operatorLimit)
                    .setOwnerKlass(ownerClass)
                    .setResultType(returnType)
                    .setCanHaveReturn(canHaveReturn)
                    .setCanHaveThrow(canHaveThrow)
                    .setCanHaveBreaks(canHaveBreaks)
                    .setCanHaveContinues(canHaveContinues)
                    .setExceptionSafe(false)
                    .setNoConsts(false);
            Rule<IRNode> rule;
            SymbolTable.push();
            for (int i = 0; i < slimit && climit > 0; ) {
                int subLimit = (int) (PseudoRandom.random() * (slimit - i - 1));
                builder.setComplexityLimit((long) (PseudoRandom.random() * climit));
                rule = new Rule<>("block");
                rule.add("statement", builder.getStatementFactory(), 5);
                if (!ProductionParams.disableVarsInBlock.value()) {
                    rule.add("decl", builder.setIsLocal(true).getDeclarationFactory());
                }
                if (subLimit > 0) {
                    builder.setStatementLimit(subLimit).setLevel(level + 1);
                    if (!ProductionParams.disableNestedBlocks.value()) {
                        rule.add("block", builder.setCanHaveReturn(false)
                                .setCanHaveThrow(false)
                                .setCanHaveBreaks(false)
                                .setCanHaveContinues(false)
                                .getBlockFactory());
                        rule.add("try-catch", builder.getTryCatchBlockFactory(), 0.3);
                        builder.setCanHaveReturn(canHaveReturn)
                                .setCanHaveThrow(canHaveThrow)
                                .setCanHaveBreaks(canHaveBreaks)
                                .setCanHaveContinues(canHaveContinues);
                    }
                    addControlFlowDeviation(rule, builder);
                }
                try {
                    IRNode choiceResult = rule.produce();
                    if (choiceResult instanceof If || choiceResult instanceof While || choiceResult instanceof DoWhile
                            || choiceResult instanceof For || choiceResult instanceof Switch) {
                        i += subLimit;
                    } else {
                        i++;
                    }
                    //climit -= subBlockComplLimit; // very approximate. to obnain a precise value, change to p.complexity()
                    climit -= choiceResult.complexity();
                    content.add(choiceResult);
                } catch (ProductionFailedException e) {
                    i++;
                }
            }
            // Ok, if the block can end with break and continue. Generate the appropriate productions.
            rule = new Rule<>("block_ending");
            if (canHaveBreaks && !subBlock) {
                rule.add("break", builder.getBreakFactory());
            }
            if (canHaveContinues && !subBlock) {
                rule.add("continue", builder.getContinueFactory());
            }
            if (canHaveReturn && !subBlock && !returnType.equals(TypeList.VOID)) {
                rule.add("return", builder.setComplexityLimit(climit).getReturnFactory());
            }
            if (canHaveThrow && !subBlock) {
                Type rtException = TypeList.find("java.lang.RuntimeException");
                rtException = PseudoRandom.randomElement(TypeUtil.getImplicitlyCastable(TypeList.getAll(), rtException));
                rule.add("throw", builder.setResultType(rtException)
                        .setComplexityLimit(Math.max(climit, 5))
                        .setOperatorLimit(Math.max(operatorLimit, 5))
                        .getThrowFactory());
            }

            try {
                if (rule.size() > 0) {
                    content.add(rule.produce());
                }
            } catch (ProductionFailedException e) {
            }
            if (!subBlock) {
                SymbolTable.pop();
            } else {
                SymbolTable.merge();
            }
            return new Block(ownerClass, returnType, content, level);
        }
        throw new ProductionFailedException();
    }

    private void addControlFlowDeviation(Rule<IRNode> rule, IRNodeBuilder builder) {
        if (!ProductionParams.disableIf.value()) {
            rule.add("if", builder.getIfFactory());
        }
        if (!ProductionParams.disableWhile.value()) {
            rule.add("while", builder.getWhileFactory());
        }
        if (!ProductionParams.disableDoWhile.value()) {
            rule.add("do_while", builder.getDoWhileFactory());
        }
        if (!ProductionParams.disableFor.value()) {
            rule.add("for", builder.getForFactory());
        }
        if (!ProductionParams.disableSwitch.value()) {
            rule.add("switch", builder.getSwitchFactory(), 0.1);
        }
    }
}
