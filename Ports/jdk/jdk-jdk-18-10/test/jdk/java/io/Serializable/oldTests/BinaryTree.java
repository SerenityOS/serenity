/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary it is new version of old test which was
 *          /src/share/test/serialization/piotest.java
 *          Test of serialization/deserialization of
 *          objects with BinaryTree types
 */

import java.io.*;

public class BinaryTree {
    public static void main (String argv[]) {
        System.err.println("\nRegression test for testing of " +
            "serialization/deserialization of objects " +
            "with BinaryTree types \n");

        try {
            BinaryTreeTest base = new BinaryTreeTest(2);
            FileOutputStream ostream = new FileOutputStream("piotest3.tmp");
            try {
                ObjectOutputStream p = new ObjectOutputStream(ostream);
                p.writeObject(null);
                p.writeObject(base);
                p.flush();
            } finally {
                ostream.close();
            }

            FileInputStream istream = new FileInputStream("piotest3.tmp");
            try {
                ObjectInputStream q = new ObjectInputStream(istream);
                Object n = q.readObject();
                if (n != null) {
                    System.err.println("\nnull read as " + n);
                }
                BinaryTreeTest nbase = (BinaryTreeTest)q.readObject();
                if (!base.equals(nbase)) {
                    System.err.println("\nTEST FAILED: BinaryTree read " +
                        "incorrectly.");
                    throw new Error();
                }
            } finally {
                istream.close();
            }

            System.err.println("\nTEST PASSED");
        } catch (Exception e) {
            System.err.print("TEST FAILED: ");
            e.printStackTrace();
            throw new Error();
        }
    }
}

class BinaryTreeTest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public BinaryTreeTest left;
    public BinaryTreeTest right;
    public int id;
    public int level;

    private static int count = 0;

    public BinaryTreeTest(int l) {
        id = count++;
        level = l;
        if (l > 0) {
            left = new BinaryTreeTest(l-1);
            right = new BinaryTreeTest(l-1);
        }
    }

    public void print(int levels) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
        System.err.println("node " + id);

        if (level <= levels && left != null) {
            left.print(levels);
        }

        if (level <= levels && right != null) {
            right.print(levels);
        }
    }

    public boolean equals(BinaryTreeTest other) {
        if (other == null) {
            return false;
        }

        if (id != other.id) {
            return false;
        }

        if (left != null && !left.equals(other.left)) {
            return false;
        }

        if (right != null && !right.equals(other.right)) {
            return false;
        }

        return true;
    }
}
