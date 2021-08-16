/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.1 03/10/27
 * @bug 4769840
 * @library /java/text/testlib
 * @build Bug4769840 HexDumpReader
 * @run main Bug4769840
 * @summary Confirm serialization compatibility
 */

import java.io.*;
import java.text.*;

public class Bug4769840 {

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("-ser")) {
            serialize();
        } else {
            deserialize();
        }
    }

    /* Serialization */
    private static void serialize() throws Exception {
        /* Serialize with JDK 1.1 */
        serialize("ChoiceFormat.ser", new ChoiceFormat("0# foo|1# bar"));

        /*
         * Serialize with JDK1.4.0 because the Field class was added in the
         * version.
         */
        serialize("DateFormat.Field.ser", DateFormat.Field.TIME_ZONE);
        serialize("MessageFormat.Field.ser", MessageFormat.Field.ARGUMENT);
        serialize("NumberFormat.Field.ser", NumberFormat.Field.INTEGER);
    }

    private static void serialize(String filename, Object o) throws Exception {
        FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(o);
        out.close();
    }

    /* Deserialization */
    private static void deserialize() throws Exception {
        deserialize("ChoiceFormat.ser");
        deserialize("DateFormat.Field.ser");
        deserialize("MessageFormat.Field.ser");
        deserialize("NumberFormat.Field.ser");
    }

    private static void deserialize(String filename) throws Exception {
        InputStream is = HexDumpReader.getStreamFromHexDump(filename + ".txt");
        ObjectInputStream in = new ObjectInputStream(is);
        Object obj = in.readObject();
        in.close();
        System.out.println("Deserialization of <" + filename + "> succeeded.");
    }
}
