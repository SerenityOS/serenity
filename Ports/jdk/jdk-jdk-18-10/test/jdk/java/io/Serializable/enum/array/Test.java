/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4838379
 * @summary Verify that serialization of enum constant arrays functions
 *          properly.
 */

import java.io.*;
import java.util.Arrays;

enum Foo { klaatu, barada { int i = 1; }, nikto }

public class Test {
    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        Foo[] fa = Foo.values();
        Object[] oa = Arrays.asList(fa).toArray();
        oout.writeObject(oa);
        oout.writeObject(fa);
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        Object[] oa2 = (Object[]) oin.readObject();
        if (!elementsIdentical(oa, oa2)) {
            throw new Error("arrays differ: " +
                Arrays.asList(oa) + ", " + Arrays.asList(oa2));
        }
        Foo[] fa2 = (Foo[]) oin.readObject();
        if (!elementsIdentical(fa, fa2)) {
            throw new Error("arrays differ: " +
                Arrays.asList(fa) + ", " + Arrays.asList(fa2));
        }
    }

    static boolean elementsIdentical(Object[] a1, Object[] a2) {
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        return true;
    }
}
