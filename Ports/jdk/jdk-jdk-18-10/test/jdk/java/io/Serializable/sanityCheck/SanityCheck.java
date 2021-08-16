/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic sanity check to test if default (de)serialization is
 *          transmitting values properly.
 * @key randomness
 */

import java.io.*;
import java.util.*;

class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    static final int ARRAYLEN = 1000;
    static final int STRLEN = 1000;
    static Random rand = new Random(System.currentTimeMillis());

    boolean z;
    byte b;
    char c;
    short s;
    int i;
    float f;
    long j;
    double d;

    boolean[] zary;
    byte[] bary;
    char[] cary;
    short[] sary;
    int[] iary;
    float[] fary;
    long[] jary;
    double[] dary;

    String str;
    @SuppressWarnings("serial")  /* Incorrect declarations are being tested */
    Object[] oary;

    Item() {
        z = rand.nextBoolean();
        b = (byte) rand.nextInt();
        c = (char) rand.nextInt();
        s = (short) rand.nextInt();
        i = rand.nextInt();
        f = rand.nextFloat();
        j = rand.nextLong();
        d = rand.nextDouble();

        zary = new boolean[ARRAYLEN];
        bary = new byte[ARRAYLEN];
        cary = new char[ARRAYLEN];
        sary = new short[ARRAYLEN];
        iary = new int[ARRAYLEN];
        fary = new float[ARRAYLEN];
        jary = new long[ARRAYLEN];
        dary = new double[ARRAYLEN];
        oary = new Object[ARRAYLEN];

        for (int i = 0; i < ARRAYLEN; i++) {
            zary[i] = rand.nextBoolean();
            bary[i] = (byte) rand.nextInt();
            cary[i] = (char) rand.nextInt();
            sary[i] = (short) rand.nextInt();
            iary[i] = rand.nextInt();
            fary[i] = rand.nextFloat();
            jary[i] = rand.nextLong();
            dary[i] = rand.nextDouble();
            oary[i] = rand.nextInt();
        }

        char[] strChars = new char[STRLEN];
        for (int i = 0; i < STRLEN; i++) {
            strChars[i] = (char) rand.nextInt();
        }
        str = new String(strChars);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Item)) {
            return false;
        }
        Item other = (Item) obj;

        if ((z != other.z) || (b != other.b) || (c != other.c) ||
            (s != other.s) || (i != other.i) || (f != other.f) ||
            (j != other.j) || (d != other.d))
        {
            return false;
        }

        for (int i = 0; i < ARRAYLEN; i++) {
            if ((zary[i] != other.zary[i]) || (bary[i] != other.bary[i]) ||
                (cary[i] != other.cary[i]) || (sary[i] != other.sary[i]) ||
                (iary[i] != other.iary[i]) || (fary[i] != other.fary[i]) ||
                (jary[i] != other.jary[i]) || (dary[i] != other.dary[i]) ||
                !oary[i].equals(other.oary[i]))
            {
                return false;
            }
        }

        if (!str.equals(other.str)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j);
    }
}

public class SanityCheck {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20; i++) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            Item item = new Item();
            oout.writeObject(item);
            oout.close();

            ObjectInputStream oin = new ObjectInputStream(
                new ByteArrayInputStream(bout.toByteArray()));
            Item itemcopy = (Item) oin.readObject();

            if (! item.equals(itemcopy)) {
                throw new Error();
            }
        }
    }
}
