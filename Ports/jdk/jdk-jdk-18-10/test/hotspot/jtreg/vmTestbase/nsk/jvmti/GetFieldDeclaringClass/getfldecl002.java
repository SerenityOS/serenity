/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetFieldDeclaringClass;

import java.io.*;

public class getfldecl002 {
    final static int exit_delta = 95;

    static {
        try {
            System.loadLibrary("getfldecl002");
        } catch (UnsatisfiedLinkError err) {
            System.err.println("Could not load getfldecl002 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw err;
        }
    }

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv,System.out) + exit_delta);
    }

    native static void check(int i, Class cls1, Class cls2);
    native static int getResult();

    public static int run(String argv[], PrintStream out) {
        try {
            KlassLoader kl = new KlassLoader();
            Class cls_i =
                kl.loadClass("nsk.jvmti.GetFieldDeclaringClass.getfldecl002i");
            Class cls_e =
                kl.loadClass("nsk.jvmti.GetFieldDeclaringClass.getfldecl002e");
            Class cls_a =
                kl.loadClass("nsk.jvmti.GetFieldDeclaringClass.getfldecl002a");
            check(0, cls_a, cls_i);
            check(1, cls_a, cls_e);
            check(2, cls_a, cls_a);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(out);
            return 2;
        }

        return getResult();
    }

    private static class KlassLoader extends ClassLoader {
        protected Class findClass(String name) throws ClassNotFoundException {
            byte[] buf;
            try {
                InputStream in = getSystemResourceAsStream(
                    name.replace('.', File.separatorChar) + ".klass");
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                buf = new byte[in.available()];
                in.read(buf);
                in.close();
            } catch (Exception ex) {
                throw new ClassNotFoundException(name, ex);
            }

            return defineClass(name, buf, 0, buf.length);
        }
    }
}
