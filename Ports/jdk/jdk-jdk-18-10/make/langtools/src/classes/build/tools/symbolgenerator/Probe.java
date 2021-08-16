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

package build.tools.symbolgenerator;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**A tool to dump the content of the default javac's bootclasspath. This tool should not use any
 * features not available on the oldest supported target JDK, which is currently JDK 6.
 *
 * For more information on use of this site, please see CreateSymbols.
 */
public class Probe {

    public static void main(String... args) throws IOException {
        if (args.length != 1) {
            System.err.println("Not enough arguments.");
            System.err.println("Usage:");
            System.err.println("    java " + Probe.class.getName() + " <output-file>");
            return ;
        }

        File outFile = new File(args[0]);
        Charset cs = Charset.forName("UTF-8");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);
        OutputStream out = new FileOutputStream(outFile);

        try {
            Iterable<JavaFileObject> bcpFiles =
                    fm.list(StandardLocation.PLATFORM_CLASS_PATH, "", EnumSet.of(Kind.CLASS), true);

            for (JavaFileObject jfo : bcpFiles) {
                InputStream in = new BufferedInputStream(jfo.openInputStream());
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    StringBuilder textual = new StringBuilder();
                    int read;

                    while ((read = in.read()) != (-1)) {
                        baos.write(read);
                        textual.append(String.format("%02x", read));
                    }

                    textual.append("\n");
                    out.write(textual.toString().getBytes(cs));
                } finally {
                    in.close();
                }
            }
        } finally {
            out.close();
        }
    }

}
