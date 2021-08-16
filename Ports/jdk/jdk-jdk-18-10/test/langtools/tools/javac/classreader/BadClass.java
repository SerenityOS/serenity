/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6898851
 * @summary Compiling against this corrupt class file causes a stacktrace from javac
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassWriter;
import com.sun.tools.javac.Main;

public class BadClass {
    // Create and compile file containing body; return compiler output
    static String makeClass(String dir, String filename, String body) throws IOException {
        File file = new File(dir, filename);
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(body);
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String args[] = { "-cp", dir, "-d", dir, "-XDrawDiagnostics", file.getPath() };
        Main.compile(args, pw);
        pw.close();
        return sw.toString();
    }

    public static void main(String... args) throws Exception {
        new File("d1").mkdir();
        new File("d2").mkdir();

        // Step 1. build an empty class with an interface
        makeClass("d1", "Empty.java", "abstract class Empty implements Readable {}");

        // Step 2. Modify classfile to have invalid constant pool index
        ClassFile cf = ClassFile.read(new File("d1","Empty.class"));
        cf.interfaces[0] = cf.constant_pool.size() + 10;
        ClassWriter cw = new ClassWriter();
        cw.write(cf, new File("d2","Empty.class"));

        // Step 3. Compile use of invalid class
        String result = makeClass("d2", "EmptyUse.java", "class EmptyUse { Empty e; }");
        if (!result.contains("compiler.misc.bad.class.file")) {
            System.out.println(result);
            throw new Exception("test failed");
        }
    }
}
