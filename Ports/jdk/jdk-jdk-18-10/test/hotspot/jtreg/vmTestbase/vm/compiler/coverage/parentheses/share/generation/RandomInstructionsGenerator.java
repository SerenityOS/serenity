/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.coverage.parentheses.share.generation;

import vm.compiler.coverage.parentheses.share.Instruction;
import vm.compiler.coverage.parentheses.share.InstructionSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.test.lib.Utils;

/**
 * Generates random but correct list of JVM instructions
 */
public class RandomInstructionsGenerator {
    private static Random random = Utils.getRandomInstance();

    private static <E> E choseRandomElement(List<E> list) {
        return list.get(random.nextInt(list.size()));
    }

    private int maxStackDepth;

    public RandomInstructionsGenerator(int maxStackDepth) {
        this.maxStackDepth = maxStackDepth;
    }

    public InstructionSequence generate() {
        List<Instruction> instructions = new ArrayList<Instruction>();

        //this head with constants is necessary to avoid stack underflow
        instructions.add(Instruction.ICONST_1);
        instructions.add(Instruction.ICONST_2);


        String parenthesis = ParenthesesGenerator.generate(maxStackDepth - 2);

        for (char c : parenthesis.toCharArray()) {
            if (c == '(') {
                //+1: add op that increase stack
                instructions.add(choseRandomElement(Instruction.stackUp));
            } else {
                //-1
                instructions.add(choseRandomElement(Instruction.stackDown));
            }

            //+0: add element that doesn't change stack
            if (random.nextBoolean()) {
                instructions.add(choseRandomElement(Instruction.neutral));
            }

        }
        return new InstructionSequence(instructions, maxStackDepth);
    }
}
