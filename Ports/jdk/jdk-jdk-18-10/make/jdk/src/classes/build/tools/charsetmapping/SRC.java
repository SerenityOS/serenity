/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Scanner;

public class SRC {

    public static void genClass(Charset cs, String srcDir, String dstDir)
        throws Exception
    {
        String clzName = cs.clzName;
        String csName  = cs.csName;
        String pkgName = cs.pkgName;

        try (Scanner s = new Scanner(new File(srcDir, clzName + ".java.template"));
             PrintStream out = new PrintStream(new FileOutputStream(
                                  new File(dstDir, clzName + ".java")));) {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.indexOf("$") < 0) {
                    out.println(line);
                    continue;
                }
                if (line.indexOf("$PACKAGE$") != -1) {
                    out.println(line.replace("$PACKAGE$", pkgName));
                } else if (line.indexOf("$ALIASES$") != -1) {
                    if ("sun.nio.cs".equals(pkgName))
                        out.println(line.replace("$ALIASES$",
                                                 "StandardCharsets.aliases_" + clzName + "()"));
                    else
                        out.println(line.replace("$ALIASES$",
                                                 "ExtendedCharsets.aliasesFor(\"" + csName + "\")"));
                } else {
                    out.println(line);
                }
            }
        }
    }
}
