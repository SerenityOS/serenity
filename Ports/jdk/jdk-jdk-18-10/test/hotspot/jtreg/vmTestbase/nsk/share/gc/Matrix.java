/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc;

public class Matrix {
        private int cellSize;
        private int matrixSize;
        private Cell matrix[][];
        private int nKnockedOut; // number of cells "nulled out"

        public Matrix(int matrixSize, int cellSize) {
                this.matrixSize = matrixSize;
                this.cellSize = cellSize;
                matrix = new Cell[matrixSize][matrixSize];
                populate();
        }

        public void populate() {
                for (int i = 0; i < matrixSize ; i++) {
                        for( int j = 0 ; j < matrixSize ; j++) {
                                matrix[i][j] = new Cell(cellSize, i);
                        }
                }
        }

        public int returnArrayBound() {
                return matrixSize - 1;
        }

        public synchronized void clear(int i, int j) {
                matrix[i][j] = null;
                ++nKnockedOut;
        }

        public synchronized void repopulate(int i, int j) {
                matrix[i][j] = new Cell(cellSize, i + j);
                --nKnockedOut;
        }

        public synchronized void resetCellCount() {
                nKnockedOut = 0;
        }

        public synchronized int getCellCount() {
                return nKnockedOut;
        }
}
