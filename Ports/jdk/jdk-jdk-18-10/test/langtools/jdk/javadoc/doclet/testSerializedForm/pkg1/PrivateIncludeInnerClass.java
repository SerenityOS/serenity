/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package pkg1;

import java.io.*;

/**
 * A test class where the outer class is package private and inner class
 * is private which is included using the tag.
 */

class PrivateIncludeInnerClass {

    /**
     * @serial include
     */
    private static class PriInnerClass implements java.io.Serializable {

        public final int SERIALIZABLE_CONSTANT = 1;

        /**
         * @param s ObjectInputStream.
         * @throws IOException when there is an I/O error.
         * @serial
         */
        private void readObject(ObjectInputStream s) throws IOException {
        }

        /**
         * @param s ObjectOutputStream.
         * @throws IOException when there is an I/O error.
         * @serial
         */
        private void writeObject(ObjectOutputStream s) throws IOException {
        }
    }
}
