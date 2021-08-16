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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.classes.ClassDefinitionBlock;
import jdk.test.lib.jittester.classes.Klass;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class ClassDefinitionBlockFactory extends Factory<ClassDefinitionBlock> {
    private final String prefix;
    private final long complexityLimit;
    private final int classesLimit;
    private final int statementLimit;
    private final int operatorLimit;
    private final int memberFunctionsLimit;
    private final int memberFunctionsArgLimit;
    private final int level;

    ClassDefinitionBlockFactory(String prefix, int classesLimit, int memberFunctionsLimit,
            int memberFunctionsArgLimit, long complexityLimit, int statementLimit,
            int operatorLimit, int level) {
        this.prefix = prefix;
        this.classesLimit = classesLimit;
        this.memberFunctionsLimit = memberFunctionsLimit;
        this.memberFunctionsArgLimit = memberFunctionsArgLimit;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
    }

    @Override
    public ClassDefinitionBlock produce() throws ProductionFailedException {
        ArrayList<IRNode> content = new ArrayList<>();
        int limit = (int) Math.ceil(PseudoRandom.random() * classesLimit);
        if (limit > 0) {
            long classCompl = complexityLimit / limit;
            IRNodeBuilder builder = new IRNodeBuilder().setLevel(level)
                    .setMemberFunctionsArgLimit(memberFunctionsArgLimit)
                    .setStatementLimit(statementLimit)
                    .setOperatorLimit(operatorLimit)
                    .setComplexityLimit(classCompl);
            for (int i = 0; i < limit; i++) {
                try {
                    Rule<IRNode> rule = new Rule<>("class");
                    rule.add("basic_class", builder.setName(prefix + "_Class_" + i)
                            .setMemberFunctionsLimit(memberFunctionsLimit)
                            .getKlassFactory());
                    if (!ProductionParams.disableInterfaces.value()) {
                        rule.add("interface", builder.setName(prefix + "_Interface_" + i)
                                .setMemberFunctionsLimit((int) (memberFunctionsLimit * 0.2))
                                .getInterfaceFactory(), 0.1);
                    }
                    // TODO: Add enums
                    content.add(rule.produce());
                } catch (ProductionFailedException e) {
                }
            }
        }
        ensureMinDepth(content);
        ensureMaxDepth(content);
        return new ClassDefinitionBlock(content, level);
    }

    private void ensureMinDepth(Collection<IRNode> content) throws ProductionFailedException {
        int minDepth = ProductionParams.minCfgDepth.value();
        List<IRNode> childs = content.stream()
                .filter(c -> c instanceof Klass)
                .collect(Collectors.toList());
        addMoreChildren(childs, content, minDepth);
    }

    private void addMoreChildren(List<IRNode> children, Collection<IRNode> content, int minDepth)
            throws ProductionFailedException {
        /* check situation when no stackable leaves available in all children */
        if (IRNode.getModifiableNodesCount(children) == 0L) {
            return;
        }
        /* now let's try to add children */
        while (!children.isEmpty() && IRNode.countDepth(content) < minDepth) {
            PseudoRandom.shuffle(children);
            IRNode randomChild = children.get(0);
            List<IRNode> leaves = randomChild.getStackableLeaves();
            if (!leaves.isEmpty()) {
                Block randomLeaf = (Block) leaves.get(PseudoRandom.randomNotNegative(leaves.size()));
                TypeKlass owner = randomChild.getOwner();
                int newLevel = randomLeaf.getLevel() + 1;
                Type retType = randomLeaf.getResultType();
                IRNodeBuilder b = new IRNodeBuilder()
                        .setOwnerKlass(owner)
                        .setResultType(retType)
                        .setComplexityLimit(complexityLimit)
                        .setStatementLimit(statementLimit)
                        .setOperatorLimit(operatorLimit)
                        .setLevel(newLevel);
                IRNode newBlock = b.getBlockFactory().produce();
                List<IRNode> siblings = randomLeaf.getChildren();
                // to avoid break;
                int index = PseudoRandom.randomNotZero(siblings.size() - 1);
                siblings.add(index, newBlock);
            }
        }
    }

    private void ensureMaxDepth(Collection<IRNode> content) {
        int maxDepth = ProductionParams.maxCfgDepth.value();
        List<IRNode> childrenClasses = content.stream()
                .filter(c -> c instanceof Klass && c.countDepth() > maxDepth)
                .collect(Collectors.toList());
        /* now attempt to reduce depth by removing optional parts of control deviation
           blocks in case IRTree has oversized depth */
        IRNode.tryToReduceNodesDepth(childrenClasses, maxDepth);
    }
}
