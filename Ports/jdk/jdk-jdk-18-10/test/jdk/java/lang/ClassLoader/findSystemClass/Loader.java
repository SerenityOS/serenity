/*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * This test runs in othervm mode as it tests ClassLoader.findSystemClass
 * and getSystemResource methods.
 */

/* @test
   @bug 4147599 4478150
   @summary In 1.2beta4-I ClassLoader loaded classes can not link
            against application classes.
   @run main/othervm Loader
*/

/*
 * We are trying to test that certain methods of ClassLoader look at the same
 * paths as they did in 1.1.  To run this test on 1.1, you will have to pass
 * "-1.1" as option on the command line.
 *
 * The required files are:
 *
 *      - Loader.java            (a 1.1 style class loader)
 *      - Loadee.java            (source for a class that refers to Loader)
 *      - Loadee.classfile       (to test findSystemClass)
 *      - Loadee.resource        (to test getSystemResource)
 *
 * The extension ".classfile" is so the class file is not seen by any loader
 * other than Loader.  If you need to make any changes you will have to
 * compile Loadee.java and rename Loadee.class to Loadee.classfile.
 */

import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;


/**
 * A 1.1-style ClassLoader.  The only class it can really load is "Loadee".
 * For other classes it might be asked to load, it relies on loaders set up by
 * the launcher.
 */
public class Loader extends ClassLoader {

    public Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
        Class c = null;
        try {
            c = findSystemClass(name);
        } catch (ClassNotFoundException cnfe) {
        }
        if (c == null) {
            if (!name.equals("Loadee"))
                throw new Error("java.lang.ClassLoader.findSystemClass() " +
                                "did not find class " + name);
            byte[] b = locateBytes();
            c = defineClass(name, b, 0, b.length);
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    private byte[] locateBytes() {
        try {
            File f   = new File(System.getProperty("test.src", "."),
                                "Loadee.classfile");
            long l   = f.length();
            byte[] b = new byte[(int)l];
            DataInputStream in =
                new DataInputStream(new FileInputStream(f));
            in.readFully(b);
            return b;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Error("Test failed due to IOException!");
        }
    }

    private static final int FIND      = 0x1;
    private static final int RESOURCE  = 0x2;
    private static final int RESOURCES = 0x4;

    public static void main(String[] args) throws Exception {
        int tests = FIND | RESOURCE | RESOURCES;

        if (args.length == 1 && args[0].equals("-1.1")) {
            tests &= ~RESOURCES; /* Do not run getResources test. */
        }

        if ((tests & FIND) == FIND) {
            report("findSystemClass()");
            ClassLoader l = new Loader();
            Class       c = l.loadClass("Loadee");
            Object      o = c.newInstance();
        }

        if ((tests & RESOURCE) == RESOURCE) {
            report("getSystemResource()");
            URL u = getSystemResource("Loadee.resource");
            if (u == null)
                throw new Exception
                    ("java.lang.ClassLoader.getSystemResource() test failed!");
        }
    }

    private static void report(String s) {
        System.out.println("Testing java.lang.ClassLoader." + s + " ...");
    }
}
