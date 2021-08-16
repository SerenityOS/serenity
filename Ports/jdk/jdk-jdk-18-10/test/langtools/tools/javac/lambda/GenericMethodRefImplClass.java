/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8009582
 * @summary Method reference generic constructor gives: IllegalArgumentException: Invalid lambda deserialization
 * @author  Robert Field
 * @run main GenericMethodRefImplClass
 */

import java.io.*;
import java.util.*;

public class GenericMethodRefImplClass {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        try {
            // Write lambdas out
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);

            write(out, HashMap::new );
            out.flush();
            out.close();

            // Read them back
            ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bais);
            readIt(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        assertTrue(assertionCount == 1);
    }

    static void write(ObjectOutput out, GenericMethodRefImplClassLSI lamb) throws IOException {
        out.writeObject(lamb);
    }

    static void readIt(ObjectInputStream in)  throws IOException, ClassNotFoundException {
        GenericMethodRefImplClassLSI ls = (GenericMethodRefImplClassLSI) in.readObject();
        Map result = ls.convert();
        assertTrue(result.getClass().getName().equals("java.util.HashMap"));
    }
}

interface GenericMethodRefImplClassLSI extends Serializable {
    Map convert();
}
