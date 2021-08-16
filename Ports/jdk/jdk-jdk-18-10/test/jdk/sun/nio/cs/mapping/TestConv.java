/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4179153 4652234 6529796
   @summary Read code mapping table and check code conversion
   @modules jdk.charsets
 */

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashSet;

public class TestConv {
    static int errorNum = 0;
    private static final int maxBytesPerChar = 10;

    public static void main(String args[])
        throws Exception
    {
        File d = new File(System.getProperty("test.src", "."));
        if (args.length == 0) {
            String[] files = d.list();
            String encoding;
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".b2c")) {
                    encoding = files[i].substring(0, files[i].length() - 4 );
                    check(d, encoding);
                }
            }
        } else {
            for (int i = 0; i < args.length; i++)
                check(d, args[i]);
        }
    }

    static class Parser2 extends CoderTest.Parser {
        int warnOff;
        String regwarnCP;
        Parser2 (InputStream is) throws IOException {
            super(is);
        }
        protected boolean isDirective(String line) {
            if ((warnOff = line.indexOf("REGWARN")) != -1)
                regwarnCP = line.substring(warnOff+7);
            else
                regwarnCP = null;
            return false;
        }
    }

    private static void check(File dir, String encoding) throws Exception
    {
        byte[] inByte;
        byte[] outByte;
        char[] inChar;
        String inStr;
        String outStr;

        System.out.println("\nChecking " + encoding + "...");
        errorNum = 0;

        if (!Charset.isSupported(encoding)) {
            System.out.println("Not supported: " + encoding);
            return;
        }

        Parser2 p = null;
        try {
            p = new Parser2(new FileInputStream(new File(dir, encoding + ".b2c")));
        } catch (Exception e) {
            throw new Exception("Can't open file " + encoding + ".b2c");
        }
        CoderTest.Entry e = new CoderTest.Entry();

        while ((e = (CoderTest.Entry)p.next(e)) != null) {
            if (e.cp2 != 0)
                continue;  // skip composite (base+cc) for now
            inByte = e.bb;
            inChar = Character.toChars(e.cp);
            inStr = new String(inChar);
            outStr = new String(inByte, encoding);
            outByte = inStr.getBytes(encoding);
            int r = compareInOut(inStr, outStr, inByte, outByte);
            if (r == 1) {
                if (p.warnOff == -1)
                    errorNum++;
                else {
                   System.out.println ("Regression Warning code point " +
                                       p.regwarnCP);
                }
                System.out.println("Warning " + errorNum
                                   + ": " + byteString(inByte)
                                   + " -> \\u" + toHex(outStr)
                                   + "  multi-mapping? \\u" + toHex(inStr));
            } else if (r == 2) {
                if (p.warnOff == -1)
                    errorNum++;
            }
        }

        if (errorNum == 0) {
            System.out.println("OK.");
        } else {
            throw new RuntimeException(errorNum + " Warning(s).");
        }
    }

    private static int compareInOut(String inStr, String outStr,
                                byte[] inByte, byte[] outByte)
    {
        if (inStr.compareTo(outStr) != 0)
            return 1;

        if (inByte.length != outByte.length) {
            return 2;
        }

        for (int i = 0; i < inByte.length; i++) {
            if (inByte[i] != outByte[i])
                return 2;
        }

        return 0;
    }

    private static String toHex(String str)
    {
        if (str.length() == 0)
            return "";
        String s = Integer.toHexString(str.charAt(0)).toUpperCase();
        if (s.length() == 1 || s.length() == 3)
            return "0" + s;
        return s;
    }

    private static String byteString(byte[] b)
    {
        String s = "0x";
        for (int i = 0; i < b.length; i++)
            s += Integer.toHexString(b[i] & 0xff).toUpperCase();
        return s;
    }

}
