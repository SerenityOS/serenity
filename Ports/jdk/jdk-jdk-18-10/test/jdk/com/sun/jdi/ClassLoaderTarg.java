/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 */
import java.util.*;
import java.io.*;

class ClassLoaderTarg extends ClassLoader {
    private Hashtable loaded = new Hashtable();
    String id;

    public ClassLoaderTarg(String id) {
        this.id = id;
    }

    private byte[] loadClassBytes(String cname)
                                         throws ClassNotFoundException {
        StringTokenizer stk = new StringTokenizer(System.getProperty("java.class.path"),
                                                  File.pathSeparator);
        /* search class path for the class file */
        while (stk.hasMoreTokens()) {
           File cfile = new File(stk.nextToken() + File.separator +
                                 cname + ".class");
           if (cfile.exists()) {
               System.out.println("loading from: " + cfile);
               return loadBytes(cfile, cname);
           } else {
               System.out.println("no file: " + cfile + " - trying next");
           }
        }
        throw new ClassNotFoundException(cname);
    }


    private byte[] loadBytes(File cfile, String cname)
                                         throws ClassNotFoundException {
        try {
            long fsize = cfile.length();
            if (fsize > 0) {
                FileInputStream in = new FileInputStream(cfile);
                byte[] cbytes = new byte[(int)fsize];
                in.read(cbytes);
                in.close();
                return cbytes;
            }
        } catch (IOException exc) {
            // drop down
        }
        throw new ClassNotFoundException(cname);
    }

    public synchronized Class findClass(String cname)
                                     throws ClassNotFoundException {
        Class klass = (Class)loaded.get(cname);
        if (klass == null) {
            byte[] cbytes = loadClassBytes(cname);
            klass = defineClass(cname, cbytes, 0, cbytes.length);
            loaded.put(cname, klass);
            System.err.println("ClassLoaderTarg (" + id +") loaded: " + cname);
        }
        return klass;
    }

    protected void finalize() {
        UnloadEventTarg.classLoaderFinalized(id);
        try {
            super.finalize();
        } catch (Throwable thrown) {
        }
    }
}
