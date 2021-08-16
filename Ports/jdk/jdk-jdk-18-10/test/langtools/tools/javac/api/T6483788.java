/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6483788
 * @summary DefaultFileManager.ZipFileObject.toUri() fails to escape space characters
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.jar.*;
import java.util.zip.*;
import javax.tools.*;

public class T6483788 {
    public static void main(String[] args) throws Exception {
        new T6483788().run();
    }

    void run() throws Exception {
        File jar = createJar();
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_PATH, Collections.singleton(jar));
            JavaFileObject fo = fm.getJavaFileForInput(StandardLocation.CLASS_PATH, "dummy", JavaFileObject.Kind.CLASS);
            System.err.println("file: " + fo);
            URI uri = fo.toUri();
            System.err.println("uri: " + uri);
            if (uri.toString().contains(" "))
                throw new Exception("unexpected space character found");
        }
    }

    File createJar() throws IOException {
        byte[] dummy_data = new byte[10];
        File f = new File("a b.jar");
        OutputStream out = new FileOutputStream(f);
        try {
            JarOutputStream jar = new JarOutputStream(out);
            jar.putNextEntry(new ZipEntry("dummy.class"));
            jar.write(dummy_data);
            jar.close();
        } finally {
            out.close();
        }
        return f;
    }
}

