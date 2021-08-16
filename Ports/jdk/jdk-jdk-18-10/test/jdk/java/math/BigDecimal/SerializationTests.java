/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6177836
 * @summary Verify BigDecimal objects with collapsed values are serialized properly.
 * @author Joseph D. Darcy
 */

import java.math.*;
import java.io.*;

public class SerializationTests {

    static void checkSerialForm(BigDecimal bd) throws Exception  {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(bd);
        oos.flush();
        oos.close();
        ObjectInputStream ois = new
            ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        BigDecimal tmp = (BigDecimal)ois.readObject();

        if (!bd.equals(tmp) ||
            bd.hashCode() != tmp.hashCode()) {
            System.err.print("  original : " + bd);
            System.err.println(" (hash: 0x" + Integer.toHexString(bd.hashCode()) + ")");
            System.err.print("serialized : " + tmp);
            System.err.println(" (hash: 0x" + Integer.toHexString(tmp.hashCode()) + ")");
            throw new RuntimeException("Bad serial roundtrip");
        }
    }

    public static void main(String[] args) throws Exception {
        BigDecimal values[] = {
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.TEN,
            new BigDecimal(0),
            new BigDecimal(1),
            new BigDecimal(10),
            new BigDecimal(Integer.MAX_VALUE),
            new BigDecimal(Long.MAX_VALUE-1),
            new BigDecimal(BigInteger.valueOf(1), 1),
            new BigDecimal(BigInteger.valueOf(100), 50),
        };

        for(BigDecimal value : values) {
            checkSerialForm(value);
            checkSerialForm(value.negate());
        }

    }
}
