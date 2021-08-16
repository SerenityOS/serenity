/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class SPI {

    public static void genClass(String type,
                                LinkedHashMap<String, Charset> charsets,
                                String srcDir, String dstDir, String template,
                                String os)
        throws Exception
    {
        try (Scanner s = new Scanner(new File(template));
             PrintStream out = new PrintStream(new FileOutputStream(
                 new File(dstDir, new File(
                     template.replace(".template", "")).getName()))); ) {
            if (type.startsWith("extcs")) {           // ExtendedCharsets.java
                while (s.hasNextLine()) {
                    String line = s.nextLine();
                    if (line.indexOf("_CHARSETS_DEF_LIST_") == -1) {
                        out.println(line);
                    } else {
                        charsets.values()
                                .stream()
                                .filter(cs -> cs.pkgName.equals("sun.nio.cs.ext") &&
                                              !cs.isInternal &&
                                              (cs.os == null || cs.os.equals(os)))
                                .forEach( cs -> {
                            out.printf("        charset(\"%s\", \"%s\",%n", cs.csName, cs.clzName);
                            out.printf("                new String[] {%n");
                            for (String alias : cs.aliases) {
                                out.printf("                    \"%s\",%n", alias);
                            }
                            out.printf("                });%n%n");
                        });
                    }
                }
            } else if (type.startsWith("stdcs")) {    // StandardCharsets.java
                 ArrayList<String> aliasKeys = new ArrayList<>();
                 ArrayList<String> aliasValues = new ArrayList<>();
                 ArrayList<String> clzKeys = new ArrayList<>();
                 ArrayList<String> clzValues = new ArrayList<>();
                 charsets.values()
                         .stream()
                         .filter(cs -> cs.pkgName.equals("sun.nio.cs") &&
                                       !cs.isInternal)
                         .forEach( cs -> {
                     String csname = cs.csName.toLowerCase(Locale.ENGLISH);
                     clzKeys.add(csname);
                     clzValues.add("\"" + cs.clzName + "\"");
                     if (cs.aliases != null) {
                         csname = "\"" + csname + "\"";
                         for (String alias : cs.aliases) {
                             aliasKeys.add(alias.toLowerCase(Locale.ENGLISH));
                             aliasValues.add(csname);
                         }
                     }
                 });
                 while (s.hasNextLine()) {
                     String line = s.nextLine();
                     if (line.indexOf("_INCLUDE_ALIASES_TABLES_") != -1) {
                         charsets.values()
                                 .stream()
                                 .filter(cs -> cs.pkgName.equals("sun.nio.cs"))
                                 .forEach( cs -> {
                             if (cs.aliases == null || cs.aliases.length == 0) {
                                 out.printf("    static String[] aliases_%s() { return null; }%n%n",
                                            cs.clzName);
                             } else {
                                 boolean methodEnd = true;
                                 // non-final for SJIS and MS932 to support sun.nio.cs.map
                                 if (cs.clzName.equals("SJIS") || cs.clzName.equals("MS932")) {
                                     out.printf("    static String[] aliases_%s() { return aliases_%s; }%n%n",
                                                cs.clzName, cs.clzName);
                                     out.printf("    static String[] aliases_%s = new String[] {%n",
                                                cs.clzName);
                                     methodEnd = false;
                                 } else {
                                     out.printf("    static String[] aliases_%s() { return new String[] {%n",
                                                cs.clzName);
                                 }
                                 for (String alias : cs.aliases) {
                                     out.printf("            \"%s\",%n", alias);
                                 }
                                 out.printf("        };%n%n");
                                 if (methodEnd) {
                                     out.printf("    }%n%n");
                                 }
                             }
                         });
                         Charset cs = charsets.get("SJIS");
                         if (cs == null || cs.pkgName.equals("sun.nio.cs.ext")) {
                              // StandardCharsets.java has explicit reference
                              // to aliases_SJIS/MS932. If we don't have these
                              // two in std, just put a pair of dummy fields to
                              // make the compiler happy.
                              out.printf("    static String[] aliases_SJIS = null;%n%n");
                              out.printf("    static String[] aliases_MS932 = null;%n%n");
                         }
                     } else if (line.indexOf("_INCLUDE_ALIASES_MAP_") != -1) {
                         Hasher.genClass(out, aliasKeys, aliasValues,
                                         null, "Aliases", "String",
                                         12, 3, true, false, false);
                     } else if (line.indexOf("_INCLUDE_CLASSES_MAP_") != -1) {
                         Hasher.genClass(out, clzKeys, clzValues,
                                         null, "Classes", "String",
                                         11, 3, true, false, false);
                     } else if (line.indexOf("_INCLUDE_CACHE_MAP_") != -1) {
                         Hasher.genClass(out, clzKeys, clzValues,
                                         null, "Cache", "Charset",
                                         11, 3, true, true, false);
                     } else {
                         out.println(line);
                     }
                }
            } else {
                throw new RuntimeException("Unknown type:" + type);
            }
        }

    }
}
