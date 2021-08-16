/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.charsetmapping;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Formatter;
import java.util.regex.*;
import java.nio.charset.*;
import static build.tools.charsetmapping.Utils.*;

public class EUC_TW {

    static char[] toCharArray(int[] db,
                              int b1Min, int b1Max,
                              int b2Min, int b2Max)
    {
        char[] ca = new char[(b1Max - b1Min + 1) * (b2Max - b2Min + 1)];
        int off = 0;
        for (int b1 = b1Min; b1 <= b1Max; b1++) {
            for (int b2 = b2Min; b2 <= b2Max; b2++) {
                ca[off++] = (char)(db[b1 * 256 + b2] & 0xffff);
            }
        }
        return ca;
    }

    static char[] toCharArray(byte[] ba,
                              int b1Min, int b1Max,
                              int b2Min, int b2Max)
    {
        char[] ca = new char[(b1Max - b1Min + 1) * (b2Max - b2Min + 1)];
        int off = 0;
        for (int b1 = b1Min; b1 <= b1Max; b1++) {
            int b2 = b2Min;
            while (b2 <= b2Max) {
                ca[off++] = (char)(((ba[b1 * 256 + b2++] & 0xff) << 8) |
                                   (ba[b1 * 256 + b2++] & 0xff));
            }
        }
        return ca;
    }

    private static int initC2BIndex(char[] index) {
        int off = 0;
        for (int i = 0; i < index.length; i++) {
            if (index[i] != 0) {
                index[i] = (char)off;
                off += 0x100;
            } else {
                index[i] = UNMAPPABLE_ENCODING;
            }
        }
        return off;
    }

    private static Pattern euctw = Pattern.compile("(?:8ea)?(\\p{XDigit}++)\\s++(\\p{XDigit}++)?\\s*+.*");

    static void genClass(String pkg, String args[]) throws Exception
    {
        InputStream is = new FileInputStream(new File(args[0], "EUC_TW.map"));
        PrintStream ps = new PrintStream(new File(args[1], "EUC_TWMapping.java"),
                                         "ISO-8859-1");
        String copyright = getCopyright(new File(args[7], "EUC_TW.java"));


        // ranges of byte1 and byte2, something should come from a "config" file
        int b1Min = 0xa1;
        int b1Max = 0xfe;
        int b2Min = 0xa1;
        int b2Max = 0xfe;

        try {
            int[][] db = new int[8][0x10000];        // doublebyte
            byte[]  suppFlag = new byte[0x10000];    // doublebyte
            char[]  indexC2B = new char[256];
            char[]  indexC2BSupp = new char[256];

            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 0x10000; j++)
                    db[i][j] = UNMAPPABLE_DECODING;

            Parser p = new Parser(is, euctw);
            Entry  e = null;
            while ((e = p.next()) != null) {
                int plane = 0;
                if (e.bs >= 0x10000) {
                    plane = ((e.bs >> 16) & 0xff) - 1;
                    if (plane >= 14)
                        plane = 7;
                    e.bs = e.bs & 0xffff;
                }
                db[plane][e.bs] = e.cp;
                if (e.cp < 0x10000) {
                    indexC2B[e.cp>>8] = 1;
                } else {
                    indexC2BSupp[(e.cp&0xffff)>>8] = 1;
                    suppFlag[e.bs] |= (1 << plane);
                }
            }

            StringBuilder sb = new StringBuilder();
            Output out = new Output(new Formatter(sb));

            out.format(copyright);
            out.format("%n// -- This file was mechanically generated: Do not edit! -- //%n");
            out.format("package %s;%n%n", pkg);
            out.format("class EUC_TWMapping {%n%n");

            // boundaries
            out.format("    final static int b1Min = 0x%x;%n", b1Min);
            out.format("    final static int b1Max = 0x%x;%n", b1Max);
            out.format("    final static int b2Min = 0x%x;%n", b2Min);
            out.format("    final static int b2Max = 0x%x;%n", b2Max);

            // b2c tables
            out.format("%n    final static String[] b2c = {%n");
            for (int plane = 0; plane < 8; plane++) {
                out.format("        // Plane %d%n", plane);
                out.format(toCharArray(db[plane], b1Min, b1Max, b2Min, b2Max),
                           ",");
                out.format("%n");
            }
            out.format("    };%n");

            // c2bIndex
            out.format("%n    static final int C2BSIZE = 0x%x;%n",
                       initC2BIndex(indexC2B));
            out.format("%n    static char[] c2bIndex = new char[] {%n");
            out.format(indexC2B);
            out.format("    };%n");

            // c2bIndexSupp
            out.format("%n    static final int C2BSUPPSIZE = 0x%x;%n",
                       initC2BIndex(indexC2BSupp));
            out.format("%n    static char[] c2bSuppIndex = new char[] {%n");
            out.format(indexC2BSupp);
            out.format("    };%n");

            // suppFlags
            out.format("%n    static String b2cIsSuppStr =%n");
            out.format(toCharArray(suppFlag, b1Min, b1Max, b2Min, b2Max),
                       ";");
            out.format("}");
            out.close();

            ps.println(sb.toString());
            ps.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
