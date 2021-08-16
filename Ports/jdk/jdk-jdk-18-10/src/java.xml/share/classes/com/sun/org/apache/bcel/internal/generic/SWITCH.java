/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.bcel.internal.generic;

/**
 * SWITCH - Branch depending on int value, generates either LOOKUPSWITCH or
 * TABLESWITCH instruction, depending on whether the match values (int[]) can be
 * sorted with no gaps between the numbers.
 *
 */
public final class SWITCH implements CompoundInstruction {

    private int[] match;
    private InstructionHandle[] targets;
    private Select instruction;
    private int matchLength;


    /**
     * Template for switch() constructs. If the match array can be
     * sorted in ascending order with gaps no larger than max_gap
     * between the numbers, a TABLESWITCH instruction is generated, and
     * a LOOKUPSWITCH otherwise. The former may be more efficient, but
     * needs more space.
     *
     * Note, that the key array always will be sorted, though we leave
     * the original arrays unaltered.
     *
     * @param match array of match values (case 2: ... case 7: ..., etc.)
     * @param targets the instructions to be branched to for each case
     * @param target the default target
     * @param max_gap maximum gap that may between case branches
     */
    public SWITCH(final int[] match, final InstructionHandle[] targets, final InstructionHandle target, final int max_gap) {
        this.match = match.clone();
        this.targets = targets.clone();
        if ((matchLength = match.length) < 2) {
            instruction = new TABLESWITCH(match, targets, target);
        } else {
            sort(0, matchLength - 1);
            if (matchIsOrdered(max_gap)) {
                fillup(max_gap, target);
                instruction = new TABLESWITCH(this.match, this.targets, target);
            } else {
                instruction = new LOOKUPSWITCH(this.match, this.targets, target);
            }
        }
    }


    public SWITCH(final int[] match, final InstructionHandle[] targets, final InstructionHandle target) {
        this(match, targets, target, 1);
    }


    private void fillup( final int max_gap, final InstructionHandle target ) {
        final int max_size = matchLength + matchLength * max_gap;
        final int[] m_vec = new int[max_size];
        final InstructionHandle[] t_vec = new InstructionHandle[max_size];
        int count = 1;
        m_vec[0] = match[0];
        t_vec[0] = targets[0];
        for (int i = 1; i < matchLength; i++) {
            final int prev = match[i - 1];
            final int gap = match[i] - prev;
            for (int j = 1; j < gap; j++) {
                m_vec[count] = prev + j;
                t_vec[count] = target;
                count++;
            }
            m_vec[count] = match[i];
            t_vec[count] = targets[i];
            count++;
        }
        match = new int[count];
        targets = new InstructionHandle[count];
        System.arraycopy(m_vec, 0, match, 0, count);
        System.arraycopy(t_vec, 0, targets, 0, count);
    }


    /**
     * Sort match and targets array with QuickSort.
     */
    private void sort( final int l, final int r ) {
        int i = l;
        int j = r;
        int h;
        final int m = match[(l + r) >>> 1];
        InstructionHandle h2;
        do {
            while (match[i] < m) {
                i++;
            }
            while (m < match[j]) {
                j--;
            }
            if (i <= j) {
                h = match[i];
                match[i] = match[j];
                match[j] = h; // Swap elements
                h2 = targets[i];
                targets[i] = targets[j];
                targets[j] = h2; // Swap instructions, too
                i++;
                j--;
            }
        } while (i <= j);
        if (l < j) {
            sort(l, j);
        }
        if (i < r) {
            sort(i, r);
        }
    }


    /**
     * @return match is sorted in ascending order with no gap bigger than max_gap?
     */
    private boolean matchIsOrdered( final int max_gap ) {
        for (int i = 1; i < matchLength; i++) {
            if (match[i] - match[i - 1] > max_gap) {
                return false;
            }
        }
        return true;
    }


    @Override
    public InstructionList getInstructionList() {
        return new InstructionList(instruction);
    }


    public Instruction getInstruction() {
        return instruction;
    }
}
