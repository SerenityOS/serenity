/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2010, 2011 IBM Corporation
 */

/*
 * @test
 * @bug 6934356
 * @summary A serialized Vector can be successfully de-serialized.
 * @author Neil Richards <neil.richards@ngmr.net>, <neil_richards@uk.ibm.com>
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;

public class SimpleSerialization {
    public static void main(final String[] args) throws Exception {
        final Vector<String> v1 = new Vector<>();

        v1.add("entry1");
        v1.add("entry2");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(v1);
        oos.close();

        final byte[] data = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final ObjectInputStream ois = new ObjectInputStream(bais);

        final Object deserializedObject = ois.readObject();
        ois.close();

        if (false == v1.equals(deserializedObject)) {
            throw new RuntimeException(getFailureText(v1, deserializedObject));
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
