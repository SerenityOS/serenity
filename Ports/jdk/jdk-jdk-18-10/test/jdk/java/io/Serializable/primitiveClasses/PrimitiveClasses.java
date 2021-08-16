/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4171142 4519050
 * @summary Verify that primitive classes can be serialized and deserialized.
 */

import java.io.*;

public class PrimitiveClasses {
    public static void main(String[] args) throws Exception {
        Class<?>[] primClasses = {
            boolean.class, byte.class, char.class, short.class,
            int.class, long.class, float.class, double.class, void.class
        };

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        for (int i = 0; i < primClasses.length; i++) {
            oout.writeObject(primClasses[i]);
        }
        oout.close();

        ByteArrayInputStream bin =
            new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(bin);
        for (int i = 0; i < primClasses.length; i++) {
            Object obj = oin.readObject();
            if (obj != primClasses[i]) {
                throw new Error(
                    "expected " + primClasses[i] + " instead of " + obj);
            }
        }
    }
}
