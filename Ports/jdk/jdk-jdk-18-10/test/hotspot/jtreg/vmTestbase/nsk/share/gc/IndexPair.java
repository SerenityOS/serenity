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

public final class IndexPair {
        private int i, j;

        public IndexPair(int i, int j) {
                setI(i);
                setJ(j);
        }

        public int getI() {
                return i;
        }

        public void setI(int i) {
                this.i = i;
        }

        public int getJ() {
                return j;
        }

        public void setJ(int j) {
                this.j = j;
        }

        public boolean equals(IndexPair pair) {
                return (this.i == pair.i && this.j == pair.j);
        }

        public boolean equals(Object o) {
                return o instanceof IndexPair && equals((IndexPair) o);
        }

        public int hashCode() {
                return i << 16 + j;
        }
}
