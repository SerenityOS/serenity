/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6990094
 * @summary Verify ObjectInputStream.cloneArray works on many kinds of arrays
 * @author Stuart Marks, Joseph D. Darcy
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class CloneArray {
    static Object replacement;

    static class Resolver implements Serializable {
        private static final long serialVersionUID = 1L;

        private Object readResolve() throws ObjectStreamException {
            return replacement;
        }
    }

    private static void test(Object rep)
        throws IOException, ClassNotFoundException {

        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try(ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(new Resolver());
                oos.writeObject(new Resolver());
            }

            Object o1;
            Object o2;
            try(ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bais)) {
                replacement = rep;
                o1 = ois.readUnshared();
                o2 = ois.readUnshared();
            }

            if (o1 == o2)
                throw new AssertionError("o1 and o2 must not be identical");
        }
    }

    public static void main(String[] args)
        throws IOException, ClassNotFoundException {
        Object[] replacements = {
            new byte[]    {1},
            new char[]    {'2'},
            new short[]   {3},
            new int[]     {4},
            new long[]    {5},
            new float[]   {6.0f},
            new double[]  {7.0},
            new boolean[] {true},
            new Object[] {"A string."}
        };

        for(Object replacement : replacements) {
            test(replacement);
        }
    }
}
