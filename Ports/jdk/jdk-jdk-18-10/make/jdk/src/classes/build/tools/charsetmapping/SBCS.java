/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

public class SBCS {

    static Pattern sbmap = Pattern.compile("0x(\\p{XDigit}++)\\s++(?:U\\+|0x)?(\\p{XDigit}++)(?:\\s++#.*)?");

    public static void genClass(Charset cs,
                                String srcDir, String dstDir, String template)
        throws Exception
    {
        String clzName = cs.clzName;
        String csName  = cs.csName;
        String hisName = cs.hisName;
        String pkgName = cs.pkgName;
        boolean isASCII = cs.isASCII;
        boolean isLatin1Decodable = true;

        StringBuilder b2cSB = new StringBuilder();
        StringBuilder b2cNRSB = new StringBuilder();
        StringBuilder c2bNRSB = new StringBuilder();

        char[] sb = new char[0x100];
        char[] c2bIndex = new char[0x100];
        int    c2bOff = 0;
        Arrays.fill(sb, UNMAPPABLE_DECODING);
        Arrays.fill(c2bIndex, UNMAPPABLE_DECODING);

        // (1)read in .map to parse all b->c entries
        FileInputStream in = new FileInputStream(
                                 new File(srcDir, clzName + ".map"));
        Parser p = new Parser(in, sbmap);
        Entry  e = null;

        while ((e = p.next()) != null) {
            sb[e.bs] = (char)e.cp;
            if (c2bIndex[e.cp>>8] == UNMAPPABLE_DECODING) {
                c2bOff += 0x100;
                c2bIndex[e.cp>>8] = 1;
            }
            if (e.cp > 0xFF) {
                isLatin1Decodable = false;
            }
        }

        Formatter fm = new Formatter(b2cSB);
        fm.format("%n");

        // vm -server shows cc[byte + 128] access is much faster than
        // cc[byte&0xff] so we output the upper segment first
        toString(sb, 0x80, 0x100, fm, "+", true);
        toString(sb, 0x00, 0x80,  fm, ";", true);
        fm.close();

        // (2)now the .nr file which includes "b->c" non-roundtrip entries
        File f = new File(srcDir, clzName + ".nr");
        if (f.exists()) {
            in = new FileInputStream(f);
            fm = new Formatter(b2cNRSB);
            p = new Parser(in, sbmap);
            e = null;

            fm.format("// remove non-roundtrip entries%n");
            fm.format("        b2cMap = b2cTable.toCharArray();%n");
            while ((e = p.next()) != null) {
                fm.format("        b2cMap[%d] = UNMAPPABLE_DECODING;%n",
                          (e.bs>=0x80)?(e.bs-0x80):(e.bs+0x80));
            }
            fm.close();
        }

        // (3)finally the .c2b file which includes c->b non-roundtrip entries
        f = new File(srcDir, clzName + ".c2b");
        if (f.exists()) {
            in = new FileInputStream(f);
            fm = new Formatter(c2bNRSB);
            p = new Parser(in, sbmap);
            e = null;
            ArrayList<Entry> es = new ArrayList<Entry>();
            while ((e = p.next()) != null) {
                if (c2bIndex[e.cp>>8] == UNMAPPABLE_DECODING) {
                    c2bOff += 0x100;
                    c2bIndex[e.cp>>8] = 1;
                }
                es.add(e);
            }
            fm.format("// non-roundtrip c2b only entries%n");
            if (es.size() < 100) {
                fm.format("        c2bNR = new char[%d];%n", es.size() * 2);
                int i = 0;
                for (Entry entry: es) {
                    fm.format("        c2bNR[%d] = 0x%x; c2bNR[%d] = 0x%x;%n",
                              i++, entry.bs, i++, entry.cp);
                }
            } else {
                char[] cc = new char[es.size() * 2];
                int i = 0;
                for (Entry entry: es) {
                    cc[i++] = (char)entry.bs;
                    cc[i++] = (char)entry.cp;
                }
                fm.format("        c2bNR = (%n");
                toString(cc, 0, i,  fm, ").toCharArray();", false);
            }
            fm.close();
        }

        // (4)it's time to generate the source file
        String b2c = b2cSB.toString();
        String b2cNR = b2cNRSB.toString();
        String c2bNR = c2bNRSB.toString();

        Scanner s = new Scanner(new File(srcDir, template));
        PrintStream out = new PrintStream(new FileOutputStream(
                              new File(dstDir, clzName + ".java")));

        while (s.hasNextLine()) {
            String line = s.nextLine();
            int i = line.indexOf("$");
            if (i == -1) {
                out.println(line);
                continue;
            }
            if (line.indexOf("$PACKAGE$", i) != -1) {
                line = line.replace("$PACKAGE$", pkgName);
            }
            if (line.indexOf("$NAME_CLZ$", i) != -1) {
                line = line.replace("$NAME_CLZ$", clzName);
            }
            if (line.indexOf("$NAME_CS$", i) != -1) {
                line = line.replace("$NAME_CS$", csName);
            }
            if (line.indexOf("$NAME_ALIASES$", i) != -1) {
                if ("sun.nio.cs".equals(pkgName))
                    line = line.replace("$NAME_ALIASES$",
                                        "StandardCharsets.aliases_" + clzName + "()");
                else
                    line = line.replace("$NAME_ALIASES$",
                                        "ExtendedCharsets.aliasesFor(\"" + csName + "\")");
            }
            if (line.indexOf("$NAME_HIS$", i) != -1) {
                line = line.replace("$NAME_HIS$", hisName);
            }
            if (line.indexOf("$CONTAINS$", i) != -1) {
                if (isASCII)
                    line = "        return ((cs.name().equals(\"US-ASCII\")) || (cs instanceof " + clzName + "));";
                else
                    line = "        return (cs instanceof " + clzName + ");";
            }
            if (line.indexOf("$ASCIICOMPATIBLE$") != -1) {
                line = line.replace("$ASCIICOMPATIBLE$", isASCII ? "true" : "false");
            }
            if (line.indexOf("$LATIN1DECODABLE$") != -1) {
                line = line.replace("$LATIN1DECODABLE$", isLatin1Decodable ? "true" : "false");
            }
            if (line.indexOf("$B2CTABLE$") != -1) {
                line = line.replace("$B2CTABLE$", b2c);
            }
            if (line.indexOf("$C2BLENGTH$") != -1) {
                line = line.replace("$C2BLENGTH$", "0x" + Integer.toString(c2bOff, 16));
            }
            if (line.indexOf("$NONROUNDTRIP_B2C$") != -1) {
                if (b2cNR.length() == 0)
                    continue;
                line = line.replace("$NONROUNDTRIP_B2C$", b2cNR);
            }

            if (line.indexOf("$NONROUNDTRIP_C2B$") != -1) {
                if (c2bNR.length() == 0)
                    continue;
                line = line.replace("$NONROUNDTRIP_C2B$", c2bNR);
            }
            out.println(line);
        }
        out.close();
    }

    private static void toString(char[] sb, int off, int end,
                                 Formatter out, String closure, boolean comment)
    {
        while (off < end) {
            out.format("        \"");
            for (int j = 0; j < 8; j++) {
                if (off == end)
                    break;
                char c = sb[off++];
                switch (c) {
                case '\b':
                    out.format("\\b"); break;
                case '\t':
                    out.format("\\t"); break;
                case '\n':
                    out.format("\\n"); break;
                case '\f':
                    out.format("\\f"); break;
                case '\r':
                    out.format("\\r"); break;
                case '\"':
                    out.format("\\\""); break;
                case '\'':
                    out.format("\\'"); break;
                case '\\':
                    out.format("\\\\"); break;
                default:
                    out.format("\\u%04X", c & 0xffff);
                }
            }
            if (comment) {
                if (off == end)
                    out.format("\" %s      // 0x%02x - 0x%02x%n",
                               closure, off-8, off-1);
                else
                    out.format("\" +      // 0x%02x - 0x%02x%n",
                               off-8, off-1);
            } else {
                if (off == end)
                    out.format("\"%s%n", closure);
                else
                    out.format("\" +%n");
            }
        }
    }
}
