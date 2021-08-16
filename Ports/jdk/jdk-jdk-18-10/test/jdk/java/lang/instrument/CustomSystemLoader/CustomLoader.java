/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class CustomLoader extends ClassLoader {
    private static PrintStream out = System.out;
    public  static ClassLoader myself;
    public  static ClassLoader agentClassLoader;
    public  static boolean failed = true;

    public CustomLoader(ClassLoader classLoader) {
        super(classLoader);
        myself = this;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        out.println("CustomLoader: loading class: " + name);
        if (name.equals("Agent")) {
            Class c = null;
            try {
                byte[] buf = locateBytes();
                c = defineClass(name, buf, 0, buf.length);
            } catch (IOException ex) {
                throw new ClassNotFoundException(ex.getMessage());
            }
            resolveClass(c);
            out.println("CustomLoader.loadClass after resolveClass: " + name +
                        "; Class: " + c + "; ClassLoader: " + c.getClassLoader());
            return c;
        }
        return super.loadClass(name);
    }

    private byte[] locateBytes() throws IOException {
        try {
            JarFile jar = new JarFile("Agent.jar");
            InputStream is = jar.getInputStream(jar.getEntry("Agent.class"));
            int len = is.available();
            byte[] buf = new byte[len];
            DataInputStream in = new DataInputStream(is);
            in.readFully(buf);
            return buf;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IOException("Test failed due to IOException!");
        }
    }

    void appendToClassPathForInstrumentation(String path) {
        out.println("CustomLoader.appendToClassPathForInstrumentation: " +
                    this + ", jar: " + path);
        failed = false;
    }
}
