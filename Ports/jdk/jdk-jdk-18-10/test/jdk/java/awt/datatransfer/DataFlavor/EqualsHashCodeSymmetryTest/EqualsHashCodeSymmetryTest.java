/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.DataFlavor;

/**
 * @test
 * @bug 8038999
 * @summary DataFlavor.equals is not symmetric
 * @author Petr Pchelko <petr.pchelko@oracle.com>
 * @modules java.datatransfer
 */
public class EqualsHashCodeSymmetryTest {

    private static final DataFlavor[] dataFlavors = {
            DataFlavor.stringFlavor,
            DataFlavor.imageFlavor,
            DataFlavor.javaFileListFlavor,
            DataFlavor.allHtmlFlavor,
            DataFlavor.selectionHtmlFlavor,
            DataFlavor.fragmentHtmlFlavor,
            createFlavor("text/html; class=java.lang.String"),
            new DataFlavor(String.class, "My test flavor number 1"),
            new DataFlavor(String.class, "My test flavor number 2"),
            new DataFlavor(StringBuilder.class, "My test flavor number 1")
    };

    public static void main(String[] args) {
        testEqualsSymmetry();
        testEqualsHashCodeConsistency();
        testSimpleCollision();
    }

    private static void testEqualsSymmetry() {
        for (DataFlavor flavor1 : dataFlavors) {
            for (DataFlavor flavor2 : dataFlavors) {
                if (flavor1.equals(flavor2) != flavor2.equals(flavor1)) {
                    throw new RuntimeException(
                            String.format("Equals is not symmetric for %s and %s", flavor1, flavor2));
                }
            }
        }
    }

    private static void testEqualsHashCodeConsistency() {
        for (DataFlavor flavor1 : dataFlavors) {
            for (DataFlavor flavor2 : dataFlavors) {
                if ((flavor1.equals(flavor2) && flavor1.hashCode() != flavor2.hashCode())) {
                    throw new RuntimeException(
                            String.format("Equals and hash code not consistent for %s and %s", flavor1, flavor2));
                }
            }
        }
    }

    private static void testSimpleCollision() {
        if (createFlavor("text/html; class=java.lang.String").hashCode() == DataFlavor.allHtmlFlavor.hashCode()) {
            throw new RuntimeException("HashCode collision because the document parameter is not used");
        }
    }

    private static DataFlavor createFlavor(String mime) {
        try {
            return new DataFlavor(mime);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
