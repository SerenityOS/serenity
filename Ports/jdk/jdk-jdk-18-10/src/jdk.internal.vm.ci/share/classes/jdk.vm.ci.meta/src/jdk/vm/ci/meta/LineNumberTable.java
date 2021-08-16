/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;

/**
 * Maps bytecode indexes to source line numbers.
 *
 * @see "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.12"
 */
public class LineNumberTable {

    private final int[] lineNumbers;
    private final int[] bcis;

    /**
     *
     * @param lineNumbers an array of source line numbers. This array is now owned by this object
     *            and should not be mutated by the caller.
     * @param bcis an array of bytecode indexes the same length at {@code lineNumbers} whose entries
     *            are sorted in ascending order. This array is now owned by this object and must not
     *            be mutated by the caller.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "caller transfers ownership of `lineNumbers` and `bcis`")
    public LineNumberTable(int[] lineNumbers, int[] bcis) {
        assert bcis.length == lineNumbers.length;
        this.lineNumbers = lineNumbers;
        this.bcis = bcis;
    }

    /**
     * Gets a source line number for bytecode index {@code atBci}.
     */
    public int getLineNumber(int atBci) {
        for (int i = 0; i < this.bcis.length - 1; i++) {
            if (this.bcis[i] <= atBci && atBci < this.bcis[i + 1]) {
                return lineNumbers[i];
            }
        }
        return lineNumbers[lineNumbers.length - 1];
    }

    /**
     * Gets a copy of the array of line numbers that was passed to this object's constructor.
     */
    public int[] getLineNumbers() {
        return lineNumbers.clone();
    }

    /**
     * Gets a copy of the array of bytecode indexes that was passed to this object's constructor.
     */
    public int[] getBcis() {
        return bcis.clone();
    }
}
