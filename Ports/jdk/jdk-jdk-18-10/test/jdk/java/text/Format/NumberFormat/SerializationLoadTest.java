/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4101150
 * @library /java/text/testlib
 * @build SerializationLoadTest HexDumpReader
 * @run main SerializationLoadTest
 * @summary test serialization compatibility of DecimalFormat and DecimalFormatSymbols
 * @key randomness
 */

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Random;

public class SerializationLoadTest {

    public static void main(String[] args)
    {
        try {
            InputStream istream1 = HexDumpReader.getStreamFromHexDump("DecimalFormat.114.txt");
            ObjectInputStream p = new ObjectInputStream(istream1);
            CheckDecimalFormat it = (CheckDecimalFormat)p.readObject();
            System.out.println("1.1.4 DecimalFormat Loaded ok.");
            System.out.println(it.Update());
            System.out.println("Called Update successfully.");
            istream1.close();

            InputStream istream2 = HexDumpReader.getStreamFromHexDump("DecimalFormatSymbols.114.txt");
            ObjectInputStream p2 = new ObjectInputStream(istream2);
            CheckDecimalFormatSymbols it2 = (CheckDecimalFormatSymbols)p2.readObject();
            System.out.println("1.1.4 DecimalFormatSymbols Loaded ok.");
            System.out.println("getDigit : "  + it2.Update());
            System.out.println("Called Update successfully.");
            istream2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

@SuppressWarnings("serial")
class CheckDecimalFormat implements Serializable
{
    DecimalFormat _decFormat = (DecimalFormat)NumberFormat.getInstance();

    public String Update()
    {
        Random r = new Random();
        return _decFormat.format(r.nextDouble());
    }
}

@SuppressWarnings("serial")
class CheckDecimalFormatSymbols implements Serializable
{
    DecimalFormatSymbols _decFormatSymbols = new DecimalFormatSymbols();

    public char Update()
    {
        return  _decFormatSymbols.getDigit();
    }
}
