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

public class VictimClassLoader extends ClassLoader {
    public static long counter = 0;

    private int which = (int) ++counter;

    protected VictimClassLoader() {
        super(VictimClassLoader.class.getClassLoader());
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c;
        if (!name.endsWith("Victim")) {
            c = super.loadClass(name, resolve);
            return c;
        }

        c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        byte[] buf = readClassFile(name);
        c = defineClass(name, buf, 0, buf.length);
        resolveClass(c);

        if (c.getClassLoader() != this) {
            throw new AssertionError();
        }

        Test8003720.println("loaded " + c + "#" + System.identityHashCode(c) + " in " + c.getClassLoader());
        return c;
    }

    static byte[] readClassFile(String name) {
        try {
            String rname = name.substring(name.lastIndexOf('.') + 1) + ".class";
            java.net.URL url = VictimClassLoader.class.getResource(rname);
            Test8003720.println("found " + rname + " = " + url);

            java.net.URLConnection connection = url.openConnection();
            int contentLength = connection.getContentLength();
            byte[] buf = readFully(connection.getInputStream(), contentLength);

            return Asmator.fixup(buf);
        } catch (java.io.IOException ex) {
            throw new Error(ex);
        }
    }

    static byte[] readFully(java.io.InputStream in, int len) throws java.io.IOException {
        byte[] b = in.readAllBytes();
        if (len != -1 && b.length != len)
            throw new java.io.IOException("Expected:" + len + ", actual:" + b.length);
        return b;
    }

    public void finalize() {
        Test8003720.println("Goodbye from " + this);
    }

    public String toString() {
        return "VictimClassLoader#" + which;
    }
}
