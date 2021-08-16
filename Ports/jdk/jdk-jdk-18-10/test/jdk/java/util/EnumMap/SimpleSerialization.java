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

/*
 * Portions Copyright (c) 2011 IBM Corporation
 */

/*
 * @test
 * @bug 6312706
 * @summary A serialized EnumMap can be successfully de-serialized.
 * @author Neil Richards <neil.richards@ngmr.net>, <neil_richards@uk.ibm.com>
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;

public class SimpleSerialization {
    private enum TestEnum { e00, e01, e02, e03, e04, e05, e06, e07 }
    public static void main(final String[] args) throws Exception {
        final EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);

        enumMap.put(TestEnum.e01, TestEnum.e01.name());
        enumMap.put(TestEnum.e04, TestEnum.e04.name());
        enumMap.put(TestEnum.e05, TestEnum.e05.name());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(enumMap);
        oos.close();

        final byte[] data = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final ObjectInputStream ois = new ObjectInputStream(bais);

        final Object deserializedObject = ois.readObject();
        ois.close();

        if (false == enumMap.equals(deserializedObject)) {
            throw new RuntimeException(getFailureText(enumMap, deserializedObject));
        }
    }

    private static String getFailureText(final Object orig, final Object copy) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        pw.println("Test FAILED: Deserialized object is not equal to the original object");
        pw.print("\tOriginal: ");
        printObject(pw, orig).println();
        pw.print("\tCopy:     ");
        printObject(pw, copy).println();

        pw.close();
        return sw.toString();
    }

    private static PrintWriter printObject(final PrintWriter pw, final Object o) {
        pw.printf("%s@%08x", o.getClass().getName(), System.identityHashCode(o));
        return pw;
    }
}
