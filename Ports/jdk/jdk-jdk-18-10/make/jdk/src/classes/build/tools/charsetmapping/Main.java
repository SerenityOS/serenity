/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedHashMap;
import java.util.Locale;

public class Main {

    public static void main(String args[]) throws Throwable {
        int SRC_DIR  = 0;
        int DST_DIR  = 1;
        int TYPE     = 2;
        int CHARSETS = 3;
        int OS       = 4;
        int TEMPLATE = 5;
        int EXT_SRC  = 6;
        int COPYRIGHT_SRC  = 7;

        if (args.length < 3 ) {
            System.out.println("Usage: java -jar charsetmapping.jar src dst spiType charsets os [template]");
            System.exit(1);
        }
        boolean isStandard = "stdcs".equals(args[TYPE]);
        boolean isExtended = "extcs".equals(args[TYPE]);
        if (isStandard || isExtended) {
            LinkedHashMap<String, Charset> charsets = getCharsets(
                new File(args[SRC_DIR], args[CHARSETS]));
            String[] osStdcs = getOSStdCSList(new File(args[SRC_DIR], args[OS]));
            boolean hasBig5_HKSCS = false;
            boolean hasMS950_HKSCS = false;
            boolean hasMS950_HKSCS_XP = false;
            boolean hasEUC_TW = false;
            for (String name : osStdcs) {
                Charset cs = charsets.get(name);
                if (cs != null) {
                    cs.pkgName = "sun.nio.cs";
                }
                if (name.equals("Big5_HKSCS")) {
                    hasBig5_HKSCS = true;
                } else if (name.equals("MS950_HKSCS")) {
                    hasMS950_HKSCS = true;
                } else if (name.equals("MS950_HKSCS_XP")) {
                    hasMS950_HKSCS_XP = true;
                } else if (name.equals("EUC_TW")) {
                    hasEUC_TW = true;
                }
            }
            for (Charset cs : charsets.values()) {
                if (isStandard && cs.pkgName.equals("sun.nio.cs.ext") ||
                    isExtended && cs.pkgName.equals("sun.nio.cs")) {
                    continue;
                }
                verbose(cs);
                switch (cs.type) {
                case "template":
                    SRC.genClass(cs, args[EXT_SRC], args[DST_DIR]);
                    break;
                case "sbcs":
                    SBCS.genClass(cs, args[SRC_DIR], args[DST_DIR],
                                  "SingleByte-X.java.template");
                    break;
                case "source":
                    break;                   // source file, do nothing
                default:                     // dbcs
                    DBCS.genClass("dbcs".equals(cs.type) ?
                                      "" :  "_" + cs.type.toUpperCase(Locale.ENGLISH),
                                  cs, args[SRC_DIR], args[DST_DIR],
                                  "DoubleByte-X.java.template");
                }
            }
            // provider StandardCharsets.java / ExtendedCharsets.java
            SPI.genClass(args[TYPE], charsets,
                         args[SRC_DIR], args[DST_DIR],
                         args[TEMPLATE],
                         args[OS].endsWith("windows") ? "windows" : "unix");

            // HKSCSMapping(2008).java goes std if one of Big5_HKSCS MS950_HKSCS
            // is in std
            if (isStandard && (hasBig5_HKSCS || hasMS950_HKSCS) ||
                isExtended && !(hasBig5_HKSCS  || hasMS950_HKSCS)) {
                HKSCS.genClass2008(args[SRC_DIR], args[DST_DIR],
                                   isStandard ? "sun.nio.cs" : "sun.nio.cs.ext",
                                   new File(args[COPYRIGHT_SRC], "HKSCS.java"));
            }
            // HKSCS_XPMapping.java goes together with MS950XP_HKSCS
            if (isStandard && hasMS950_HKSCS_XP || isExtended && !hasMS950_HKSCS_XP) {
                HKSCS.genClassXP(args[SRC_DIR], args[DST_DIR],
                                 isStandard ? "sun.nio.cs" : "sun.nio.cs.ext",
                                 new File(args[COPYRIGHT_SRC], "HKSCS.java"));
            }
            if (isStandard && hasEUC_TW) {
                EUC_TW.genClass("sun.nio.cs", args);
            }
            if (!isStandard && !hasEUC_TW) {
                EUC_TW.genClass("sun.nio.cs.ext", args);
            }
        } else if ("sjis0213".equals(args[TYPE])) {
            JIS0213.genClass(args);
        } else if ("hkscs".equals(args[TYPE])) {
            HKSCS.genClass2001(args);
        }
    }

    private static LinkedHashMap<String, Charset> getCharsets(File cslist)
        throws Throwable
    {
        LinkedHashMap<String, Charset> charsets = new LinkedHashMap<>();
        try (Scanner s = new Scanner(cslist)) {
            Charset cs = null;
            ArrayList<String> names = new ArrayList<>();
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                String[] tokens = line.split("\\s+");
                if (tokens.length < 2) {
                    continue;
                }
                if ("charset".equals(tokens[0])) {
                    if (cs != null) {
                        cs.aliases = names.toArray(new String[names.size()]);
                        charsets.put(cs.clzName, cs);
                        cs = null;
                        names.clear();
                    }
                    if (tokens.length < 3) {
                        throw new RuntimeException("Error: incorrect charset line [" + line + "]");
                    }
                    if ((cs = charsets.get(tokens[2])) != null) {
                        throw new RuntimeException("Error: deplicate charset line [" + line + "]");
                    }
                    cs = new Charset();
                    cs.csName = tokens[1];
                    cs.clzName = tokens[2];
                } else {
                    String key = tokens[1];           // leading empty str
                    switch (key) {
                    case "alias":
                        if (tokens.length < 3) {
                            throw new RuntimeException("Error: incorrect alias line [" + line + "]");
                        } else if (names != null) {
                            names.add(tokens[2]);     // ALIAS_NAME
                        }
                        break;
                    case "package":
                        cs.pkgName = tokens[2];
                        break;
                    case "type":
                        cs.type = tokens[2];
                        break;
                    case "os":
                        cs.os = tokens[2];
                        break;
                    case "hisname":
                        cs.hisName = tokens[2];
                        break;
                    case "ascii":
                        cs.isASCII = Boolean.parseBoolean(tokens[2]);
                        break;
                    case "minmax":
                        cs.b1Min = toInteger(tokens[2]);
                        cs.b1Max = toInteger(tokens[3]);
                        cs.b2Min = toInteger(tokens[4]);
                        cs.b2Max = toInteger(tokens[5]);
                        break;
                    case "internal":
                        cs.isInternal = Boolean.parseBoolean(tokens[2]);
                        break;
                    default:  // ignore
                    }
                }
            }
            if (cs != null) {
                cs.aliases = names.toArray(new String[names.size()]);
                charsets.put(cs.clzName, cs);
            }
        }
        return charsets;
    }

    private static String[] getOSStdCSList(File stdcsos) throws Throwable
    {
        ArrayList<String> names = new ArrayList<>();
        if (stdcsos.exists()) {
            try (Scanner s = new Scanner(stdcsos)) {
                while (s.hasNextLine()) {
                    String line = s.nextLine();
                    int i = line.indexOf('#');
                    if (i != -1) {
                        line = line.substring(0, i);
                    }
                    line = line.trim();
                    if (line.length() != 0) {
                        names.add(line);
                    }
                }
            }
        }
        return names.toArray(new String[names.size()]);
    }

    static void verbose(Charset cs) {
         System.out.printf("%s, %s, %s, %s, %s  %b%n",
                           cs.clzName, cs.csName, cs.hisName, cs.pkgName, cs.type, cs.isASCII);
    }

    static int toInteger(String s) {
        return (s.startsWith("0x") || s.startsWith("0X"))
               ? Integer.valueOf(s.substring(2), 16)
               : Integer.valueOf(s);
    }
}
