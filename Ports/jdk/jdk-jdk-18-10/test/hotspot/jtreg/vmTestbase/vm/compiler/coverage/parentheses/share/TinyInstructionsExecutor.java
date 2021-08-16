/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.coverage.parentheses.share;

import java.util.List;

/**
 * Tiny stack InstructionsExecutor. This executor verifies HotspotInstructionsExecutor
 */
public class TinyInstructionsExecutor implements InstructionsExecutor {

    private int[] stack;
    private int stackHead = -1;

    public TinyInstructionsExecutor(int stackSize) {
        stack = new int[stackSize];
    }

    private void putOnStack(int v) {
        stack[++stackHead] = v;
    }

    private void exec(Instruction instruction) {
        switch (instruction) {
            case ICONST_M1:
                putOnStack(-1);
                break;
            case ICONST_0:
                putOnStack(0);
                break;
            case ICONST_1:
                putOnStack(1);
                break;
            case ICONST_2:
                putOnStack(2);
                break;
            case ICONST_3:
                putOnStack(3);
                break;
            case ICONST_4:
                putOnStack(4);
                break;
            case ICONST_5:
                putOnStack(5);
                break;
            case DUP:
                stack[stackHead + 1] = stack[stackHead];
                stackHead++;
                break;

            case IADD:
                stack[stackHead - 1] += stack[stackHead];
                stackHead--;
                break;
            case ISUB:
                stack[stackHead - 1] -= stack[stackHead];
                stackHead--;
                break;
            case IMUL:
                stack[stackHead - 1] *= stack[stackHead];
                stackHead--;
                break;
            case IOR:
                stack[stackHead - 1] |= stack[stackHead];
                stackHead--;
                break;
            case IAND:
                stack[stackHead - 1] &= stack[stackHead];
                stackHead--;
                break;
            case IXOR:
                stack[stackHead - 1] ^= stack[stackHead];
                stackHead--;
                break;
            case ISHL:
                stack[stackHead - 1] <<= stack[stackHead];
                stackHead--;
                break;
            case ISHR:
                stack[stackHead - 1] >>= stack[stackHead];
                stackHead--;
                break;

            case SWAP: {
                int t = stack[stackHead];
                stack[stackHead] = stack[stackHead - 1];
                stack[stackHead - 1] = t;
                break;
            }
            case NOP:
                break;
            case INEG:
                stack[stackHead] = -stack[stackHead];
                break;
        }
    }

    private int top() {
        return stack[stackHead];
    }

    @Override
    public int execute(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            exec(instruction);
        }
        return top();
    }
}
