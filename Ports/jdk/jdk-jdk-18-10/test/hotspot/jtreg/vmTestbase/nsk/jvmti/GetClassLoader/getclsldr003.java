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

package nsk.jvmti.GetClassLoader;

import java.io.*;
import java.lang.reflect.Array;

public class getclsldr003 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("getclsldr003");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getclsldr003 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void check(Class cls, ClassLoader cl);
    native static int getRes();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        ClassLoader cl = getclsldr003.class.getClassLoader();
        check(getclsldr003.class, cl);
        check(new getclsldr003[1].getClass(), cl);

        try {
            TestClassLoader tcl = new TestClassLoader();
            Class c = tcl.loadClass("nsk.jvmti.GetClassLoader.getclsldr003a");
            Object a = Array.newInstance(c, 1);
            check(c, tcl);
            check(a.getClass(), tcl);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(out);
            return 2;
        }

        return getRes();
    }

    private static class TestClassLoader extends ClassLoader {
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
