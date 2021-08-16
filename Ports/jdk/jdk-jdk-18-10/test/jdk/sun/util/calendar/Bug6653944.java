/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/*
 *@test
 *@bug 6653944
 *@summary Deserialization tests for YEAR calculcations
 */

import java.io.*;
import java.util.*;

public class Bug6653944 {
    private static int errorCount = 0;

    public static void main(String[] args) throws Exception {
        Calendar buddhist = Calendar.getInstance(new Locale("th", "TH"));
        int expectedYear = buddhist.get(Calendar.YEAR);

        Calendar deserialized = (Calendar) deserialize(serialize(buddhist));
        compare(deserialized, buddhist);

        int deserializedYear = deserialized.get(Calendar.YEAR);
        compare(deserializedYear, expectedYear);

        // test add(YEAR, n).
        buddhist.add(Calendar.YEAR, 12);
        expectedYear = buddhist.get(Calendar.YEAR);
        deserialized.add(Calendar.YEAR, 12);
        deserializedYear = deserialized.get(Calendar.YEAR);
        compare(deserialized, buddhist);
        compare(deserializedYear, expectedYear);

        if (errorCount > 0) {
            throw new RuntimeException("Bug6653944: failed");
        }
    }

    private static void compare(int got, int expected) {
        if (got != expected) {
            System.err.println("got " + got + ", expected " + expected);
            errorCount++;
        }
    }

    private static void compare(Calendar got, Calendar expected) {
        if (!got.equals(expected)) {
            System.err.println("got " + got + ", expected " + expected);
            errorCount++;
        }
    }

    private static byte[] serialize(Serializable obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    private static Object deserialize(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }
}
