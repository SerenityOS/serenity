/*
 * Copyright (c) 1999, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4217676
 * @summary Ensure that object streams support serialization of long strings
 *          (strings whose UTF representation > 64k in length)
 * @key randomness
 */

import java.io.*;
import java.util.*;

public class LongString {
    public static void main(String[] args) throws Exception {
        Random rand = new Random(System.currentTimeMillis());
        ByteArrayOutputStream bout;
        ByteArrayInputStream bin;
        ObjectOutputStream oout;
        ObjectInputStream oin;
        FileInputStream fin;
        File mesgf;

        // generate a long random string
        StringBuffer sbuf = new StringBuffer();
        for (int i = 0; i < 100000; i++)
            sbuf.append((char) rand.nextInt(Character.MAX_VALUE + 1));
        String str = sbuf.toString();

        // write and read long string
        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        oout.writeObject(str);
        oout.flush();
        bin = new ByteArrayInputStream(bout.toByteArray());
        oin = new ObjectInputStream(bin);
        String strcopy = (String) oin.readObject();
        if (! str.equals(strcopy))
            throw new Error("deserialized long string not equal to original");

        // test backwards compatibility
        String mesg = "Message in golden file";
        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        oout.writeObject(mesg);
        oout.flush();
        byte[] buf1 = bout.toByteArray();

        mesgf = new File(System.getProperty("test.src", "."), "mesg.ser");
        fin = new FileInputStream(mesgf);
        bout = new ByteArrayOutputStream();
        try {
            while (fin.available() > 0)
                bout.write(fin.read());
        } finally {
            fin.close();
        }
        byte[] buf2 = bout.toByteArray();

        if (! Arrays.equals(buf1, buf2))
            throw new Error("incompatible string format (write)");

        fin = new FileInputStream(mesgf);
        try {
            oin = new ObjectInputStream(fin);
            String mesgcopy = (String) oin.readObject();
            if (! mesg.equals(mesgcopy))
                throw new Error("incompatible string format (read)");
        } finally {
            fin.close();
        }
    }
}
