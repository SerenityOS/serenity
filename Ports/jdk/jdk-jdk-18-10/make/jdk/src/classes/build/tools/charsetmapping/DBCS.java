/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Formatter;
import java.util.regex.Pattern;
import static build.tools.charsetmapping.Utils.*;

public class DBCS {
    // pattern used by this class to read in mapping table
    static Pattern mPattern = Pattern.compile("(?:0x)?(\\p{XDigit}++)\\s++(?:0x)?(\\p{XDigit}++)(?:\\s++#.*)?");

    public static void genClass(String type, Charset cs,
                                String srcDir, String dstDir, String template)
        throws Exception
    {
        String clzName = cs.clzName;
        String csName  = cs.csName;
        String hisName = cs.hisName;
        String pkgName = cs.pkgName;
        boolean isASCII = cs.isASCII;
        int b1Min = cs.b1Min;
        int b1Max = cs.b1Max;
        int b2Min = cs.b2Min;
        int b2Max = cs.b2Max;

        StringBuilder b2cSB = new StringBuilder();
        StringBuilder b2cNRSB = new StringBuilder();
        StringBuilder c2bNRSB = new StringBuilder();

        char[] db = new char[0x10000];
        char[] c2bIndex = new char[0x100];
        int c2bOff = 0x100;    // first 0x100 for unmappable segs

        Arrays.fill(db, UNMAPPABLE_DECODING);
        Arrays.fill(c2bIndex, UNMAPPABLE_DECODING);

        char[] b2cIndex = new char[0x100];
        Arrays.fill(b2cIndex, UNMAPPABLE_DECODING);

        // (1)read in .map to parse all b->c entries
        FileInputStream in = new FileInputStream(new File(srcDir, clzName + ".map"));
        Parser p = new Parser(in, mPattern);
        Entry  e = null;
        while ((e = p.next()) != null) {
            db[e.bs] = (char)e.cp;

            if (e.bs > 0x100 &&    // db
                b2cIndex[e.bs>>8] == UNMAPPABLE_DECODING) {
                b2cIndex[e.bs>>8] = 1;
            }

            if (c2bIndex[e.cp>>8] == UNMAPPABLE_DECODING) {
                c2bOff += 0x100;
                c2bIndex[e.cp>>8] = 1;
            }
        }
        Output out = new Output(new Formatter(b2cSB));
        out.format("%n    static final String b2cSBStr =%n");
        out.format(db, 0x00, 0x100,  ";");

        out.format("%n        static final String[] b2cStr = {%n");
        for (int i = 0; i < 0x100; i++) {
            if (b2cIndex[i] == UNMAPPABLE_DECODING) {
                out.format("            null,%n");  //unmappable segments
            } else {
                out.format(db, i, b2Min, b2Max, ",");
            }
        }

        out.format("        };%n");
        out.close();

        // (2)now parse the .nr file which includes "b->c" non-roundtrip entries
        File f = new File(srcDir, clzName + ".nr");
        if (f.exists()) {
            StringBuilder sb = new StringBuilder();
            in = new FileInputStream(f);
            p = new Parser(in, mPattern);
            e = null;
            while ((e = p.next()) != null) {
                // A <b,c> pair
                sb.append((char)e.bs);
                sb.append((char)e.cp);
            }
            char[] nr = sb.toString().toCharArray();
            out = new Output(new Formatter(b2cNRSB));
            out.format("String b2cNR =%n");
            out.format(nr, 0, nr.length,  ";");
            out.close();
        } else {
            b2cNRSB.append("String b2cNR = null;");
        }

        // (3)finally the .c2b file which includes c->b non-roundtrip entries
        f = new File(srcDir, clzName + ".c2b");
        if (f.exists()) {
            StringBuilder sb = new StringBuilder();
            in = new FileInputStream(f);
            p = new Parser(in, mPattern);
            e = null;
            while ((e = p.next()) != null) {
                // A <b,c> pair
                if (c2bIndex[e.cp>>8] == UNMAPPABLE_DECODING) {
                    c2bOff += 0x100;
                    c2bIndex[e.cp>>8] = 1;
                }
                sb.append((char)e.bs);
                sb.append((char)e.cp);
            }
            char[] nr = sb.toString().toCharArray();
            out = new Output(new Formatter(c2bNRSB));
            out.format("String c2bNR =%n");
            out.format(nr, 0, nr.length,  ";");
            out.close();
        } else {
            c2bNRSB.append("String c2bNR = null;");
        }

        // (4)it's time to generate the source file
        String b2c = b2cSB.toString();
        String b2cNR = b2cNRSB.toString();
        String c2bNR = c2bNRSB.toString();

        Scanner s = new Scanner(new File(srcDir, template));
        PrintStream ops = new PrintStream(new FileOutputStream(
                             new File(dstDir, clzName + ".java")));
        if (hisName == null)
            hisName = "";

        // (5) c2b replacement, only used for JIs0208/0212, which
        // are two pure db charsets so default '3f' does not work
        // TBD: move this into configuration file
        String c2bRepl = "";
        if (clzName.startsWith("JIS_X_0208")) {
            c2bRepl = "new byte[]{ (byte)0x21, (byte)0x29 },";
        } else if (clzName.startsWith("JIS_X_0212")) {
            c2bRepl = "new byte[]{ (byte)0x22, (byte)0x44 },";
        } else if (clzName.startsWith("IBM300")) {
            c2bRepl = "new byte[]{ (byte)0x42, (byte)0x6f },";
        }

        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.indexOf("$") == -1) {
                ops.println(line);
                continue;
            }
            line = line.replace("$PACKAGE$" , pkgName)
                       .replace("$IMPLEMENTS$", (hisName == null)?
                                "" : "implements HistoricallyNamedCharset")
                       .replace("$NAME_CLZ$", clzName)
                       .replace("$NAME_ALIASES$",
                                "sun.nio.cs".equals(pkgName) ?
                                "StandardCharsets.aliases_" + clzName + "()" :
                                "ExtendedCharsets.aliasesFor(\"" + csName + "\")")
                       .replace("$NAME_CS$" , csName)
                       .replace("$CONTAINS$",
                                "MS932".equals(clzName)?
                                "return ((cs.name().equals(\"US-ASCII\")) || (cs instanceof JIS_X_0201) || (cs instanceof " + clzName + "));":
                                (isASCII ?
                                 "return ((cs.name().equals(\"US-ASCII\")) || (cs instanceof " + clzName + "));":
                                 "return (cs instanceof " + clzName + ");"))
                       .replace("$HISTORICALNAME$",
                                (hisName == null)? "" :
                                "    public String historicalName() { return \"" + hisName + "\"; }")
                       .replace("$DECTYPE$", type)
                       .replace("$ENCTYPE$", type)
                       .replace("$B1MIN$"   , "0x" + Integer.toString(b1Min, 16))
                       .replace("$B1MAX$"   , "0x" + Integer.toString(b1Max, 16))
                       .replace("$B2MIN$"   , "0x" + Integer.toString(b2Min, 16))
                       .replace("$B2MAX$"   , "0x" + Integer.toString(b2Max, 16))
                       .replace("$ASCIICOMPATIBLE$", isASCII ? "true" : "false")
                       .replace("$B2C$", b2c)
                       .replace("$C2BLENGTH$", "0x" + Integer.toString(c2bOff, 16))
                       .replace("$NONROUNDTRIP_B2C$", b2cNR)
                       .replace("$NONROUNDTRIP_C2B$", c2bNR)
                       .replace("$ENC_REPLACEMENT$", c2bRepl);

            ops.println(line);
        }
        ops.close();
    }
}
