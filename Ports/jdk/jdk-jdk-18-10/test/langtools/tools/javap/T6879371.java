/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6879371
 * @summary javap does not close internal default file manager
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.zip.*;

public class T6879371 {
    public static void main(String[] args) throws Exception {
        new T6879371().run();
    }

    public void run() throws Exception {
        // create a simple test class which we can put into
        // a test zip file and know that it will be used by
        // javap.
        File classDir = new File("classes");
        classDir.mkdir();

        String className = "Test";
        File javaFile = writeTestFile(className);
        compileTestFile(classDir, javaFile);

        test(classDir, className, false);
        test(classDir, className, true);
    }

    void test(File classDir, String className, boolean useJavaUtilZip) throws Exception {
        // javac should really not be using system properties like this
        // -- it should really be using (hidden) options -- but until then
        // take care to leave system properties as we find them, so as not
        // to adversely affect other tests that might follow.
        String prev = System.getProperty("useJavaUtilZip");
        setProperty("useJavaUtilZip", (useJavaUtilZip ? "true" : null));
        try {
            File zipFile = zip(classDir, new File(classDir + ".zip"));
            javap("-classpath", zipFile.getPath(), className);

            if (!zipFile.delete())
                throw new Exception("failed to delete " + zipFile);
        } finally {
            setProperty("useJavaUtilZip", prev);
        }
    }

    File writeTestFile(String name) throws IOException {
        File f = new File(name + ".java");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        out.println("class " + name + " { }");
        out.close();
        return f;
    }

    void compileTestFile(File classDir, File file) {
        int rc = com.sun.tools.javac.Main.compile(
           new String[] { "-d", classDir.getPath(), file.getPath() });
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
    }

    File zip(File dir, File zipFile) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
        for (File file: dir.listFiles()) {
            if (file.isFile()) {
                byte[] data = new byte[(int) file.length()];
                DataInputStream in = new DataInputStream(new FileInputStream(file));
                in.readFully(data);
                in.close();
                zipOut.putNextEntry(new ZipEntry(file.getName()));
                zipOut.write(data, 0, data.length);
                zipOut.closeEntry();
            }
        }
        zipOut.close();
        return zipFile;
    }

    String javap(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, out);
        if (rc != 0)
            throw new Error("javap failed. rc=" + rc);
        out.close();
        return sw.toString();
    }

    void setProperty(String key, String value) {
        if (value != null)
            System.setProperty(key, value);
        else
            System.getProperties().remove(key);
    }
}
