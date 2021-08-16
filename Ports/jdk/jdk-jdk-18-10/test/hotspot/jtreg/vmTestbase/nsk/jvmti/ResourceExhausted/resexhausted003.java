/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.ResourceExhausted;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import nsk.share.Consts;
import nsk.share.test.Stresser;
import jtreg.SkippedException;

public class resexhausted003 {

    static final long MAX_ITERATIONS = Long.MAX_VALUE;

    static class MyClassLoader extends ClassLoader {
        Class<?> loadUm(String name, byte[] bytes, ProtectionDomain pd) throws ClassFormatError {
            Class k = defineClass(name, bytes, 0, bytes.length, pd);
            resolveClass(k);
            return k;
        }

        Class loadClass(String name, byte[] bytes) {
            try {
                Class k = loadUm(name, bytes, getClass().getProtectionDomain());
                k.newInstance(); // prepare class
                return k;
            } catch (Exception exc) {
                throw new RuntimeException("Exception in loadClass: " + name, exc);
            }
        }
    };

    static class Node {
        MyClassLoader loader = new MyClassLoader();
        Node next;
    };


    static byte[] fileBytes(String fileName) {
        try {
            File f = new File(fileName);
            FileInputStream fi = new FileInputStream(f);
            byte[] bytes;
            try {
                bytes = new byte[(int) f.length()];
                fi.read(bytes);
            } finally {
                fi.close();
            }
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Exception when reading file '" + fileName + "'", e);
        }
    }


    public static int run(String args[], PrintStream out) {
        String testclasspath = System.getProperty("test.class.path");
        String [] testpaths = testclasspath.split(System.getProperty("path.separator"));
        String classesDir = "";

        Pattern pattern = Pattern.compile("^(.*)classes(.*)vmTestbase(.*)$");
        for (int i = 0 ; i < testpaths.length; i++) {
            if (pattern.matcher(testpaths[i]).matches()) {
                classesDir = testpaths[i];
            }
        }
        if (classesDir.equals("")) {
            System.err.println("TEST BUG: Classes directory not found in test,class.path.");
            return Consts.TEST_FAILED;
        }
        Stresser stress = new Stresser(args);

        String className = Helper.class.getName();
        byte[] bloatBytes = fileBytes(classesDir + File.separator + className.replace('.', '/') + ".class");

        int count = 0;
        Helper.resetExhaustedEvent();

        out.println("Loading classes...");
        stress.start(MAX_ITERATIONS);
        try {
            Node list = null;

            while ( stress.iteration() ) {
                Node n = new Node();
                n.next = list;
                list = n;
                n.loader.loadClass(className, bloatBytes);
                ++count;
            }

            System.out.println("Can't reproduce OOME due to a limit on iterations/execution time. Test was useless.");
            throw new SkippedException("Test did not get an OutOfMemory error");

        } catch (OutOfMemoryError e) {
            // that is what we are waiting for
        } finally {
            stress.finish();
        }

        System.gc();
        if (!Helper.checkResult(Helper.JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR,
                                "loading " + count + " classes of " + bloatBytes.length + " bytes")) {
            return Consts.TEST_FAILED;
        }

        return Consts.TEST_PASSED;
    }

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        int result = run(args, System.out);
        System.out.println(result == Consts.TEST_PASSED ? "TEST PASSED" : "TEST FAILED");
        System.exit(result + Consts.JCK_STATUS_BASE);
    }
}
