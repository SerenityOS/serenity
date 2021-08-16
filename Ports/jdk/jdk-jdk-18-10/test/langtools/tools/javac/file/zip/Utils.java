/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * This utils class is been used by test T8003512 which is compiled with Java 6
 * only features. So if this class is modified, it should be so using Java 6
 * features only.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class Utils {

    static final sun.tools.jar.Main jarTool =
            new sun.tools.jar.Main(System.out, System.err, "jar-tool");

    static final com.sun.tools.javac.Main javac =
            new com.sun.tools.javac.Main();

    private Utils(){}

    public static boolean compile(String... args) {
        return javac.compile(args) == 0;
    }

    public static void createClassFile(File javaFile, File superClass,
            boolean delete) throws IOException {
        createJavaFile(javaFile, superClass);
        if (!compile(javaFile.getName())) {
            throw new RuntimeException("compile failed unexpectedly");
        }
        if (delete) javaFile.delete();
    }

    public static void createJavaFile(File outFile) throws IOException {
        createJavaFile(outFile, null);
    }

    public static void createJavaFile(File outFile, File superClass) throws IOException {
        PrintStream ps = null;
        String srcStr = "public class " + getSimpleName(outFile) + " ";
        if (superClass != null) {
            srcStr = srcStr.concat("extends " + getSimpleName(superClass) + " ");
        }
        srcStr = srcStr.concat("{}");
        try {
            FileOutputStream fos = new FileOutputStream(outFile);
            ps = new PrintStream(fos);
            ps.println(srcStr);
        } finally {
            close(ps);
        }
    }

    static String getClassFileName(File javaFile) {
        return javaFile.getName().endsWith(".java")
                ? javaFile.getName().replace(".java", ".class")
                : null;
    }

    static String getSimpleName(File inFile) {
        String fname = inFile.getName();
        return fname.substring(0, fname.indexOf("."));
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n > 0) {
            out.write(buf, 0, n);
            n = in.read(buf);
        }
    }

    public static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignore) {}
        }
    }

    public static void deleteFile(File f) {
        if (!f.delete()) {
            throw new RuntimeException("could not delete file: " + f.getAbsolutePath());
        }
    }

    public static void cat(File output, File... files) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(output);
            bos = new BufferedOutputStream(fos);
            for (File x : files) {
                FileInputStream fis = new FileInputStream(x);
                bis = new BufferedInputStream(fis);
                copyStream(bis, bos);
                Utils.close(bis);
            }
        } finally {
            Utils.close(bis);
            Utils.close(bos);
            Utils.close(fos);
        }
    }
}
