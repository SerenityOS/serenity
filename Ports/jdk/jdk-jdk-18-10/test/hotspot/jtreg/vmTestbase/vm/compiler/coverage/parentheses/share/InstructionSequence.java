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

import java.io.*;
import java.util.List;

public class InstructionSequence implements Serializable {

    private List<Instruction> instructions;
    private int maxStackDepth;

    public InstructionSequence(List<Instruction> instructions, int maxStackDepth) {
        this.instructions = instructions;
        this.maxStackDepth = maxStackDepth;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public int getMaxStackDepth() {
        return maxStackDepth;
    }

    public void saveToFile(String fileName) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));
        oos.writeObject(this);
        oos.close();
    }

    public static InstructionSequence fromFile(String fileName) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(fileName));
        return (InstructionSequence) objectInputStream.readObject();
    }

    @Override
    public String toString() {
        return instructions + "\n" + "maxStackDepth=" + maxStackDepth + "\n";
    }
}
