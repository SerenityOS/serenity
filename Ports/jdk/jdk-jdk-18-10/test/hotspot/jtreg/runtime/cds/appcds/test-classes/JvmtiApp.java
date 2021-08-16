/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import sun.hotspot.WhiteBox;

public class JvmtiApp {
    static Class forname(String cn) {
        try {
            return Class.forName(cn);
        } catch (Throwable t) {
            return null;
        }
    }

    static void failed(String msg) {
        System.out.println("TEST FAILED: " + msg);
        System.exit(1);
    }

    // See ../JvmtiAddPath.java for how the classpaths are configured.
    public static void main(String args[]) {
        String cn = "Hello";
        if (args.length >= 3) {
            cn = args[args.length - 1];
        }

        if (args[0].equals("noadd")) {
            if (forname(cn) != null) {
                failed(cn + " class was loaded unexpectedly");
            }
            // We use -verbose:class to verify that Extra.class IS loaded by AppCDS if
            // the boot classpath HAS NOT been appended.
            ExtraClass.doit();
            System.exit(0);
        }

        WhiteBox wb = WhiteBox.getWhiteBox();

        if (args[0].equals("bootonly")) {
            wb.addToBootstrapClassLoaderSearch(args[1]);
            Class cls = forname(cn);
            if (cls == null) {
                failed("Cannot find " + cn + " class");
            }
            if (cls.getClassLoader() != null) {
                failed("Hello class not loaded by boot classloader");
            }
        } else if (args[0].equals("apponly")) {
            wb.addToSystemClassLoaderSearch(args[1]);
            Class cls = forname(cn);
            if (cls == null) {
                failed("Cannot find " + cn + " class");
            }
            if (cls.getClassLoader() != JvmtiApp.class.getClassLoader()) {
                failed(cn + " class not loaded by app classloader");
            }
        } else if (args[0].equals("noadd-appcds")) {
            cn = (args.length == 1) ? "Hello" : args[1];
            Class cls = forname(cn);
            if (cls == null) {
                failed("Cannot find " + cn + " class");
            }
            if (cls.getClassLoader() != JvmtiApp.class.getClassLoader()) {
                failed(cn + " class not loaded by app classloader");
            }
        } else if (args[0].equals("appandboot")) {
            wb.addToBootstrapClassLoaderSearch(args[1]);
            wb.addToSystemClassLoaderSearch(args[2]);
            cn = (args.length == 3) ? "Hello" : args[3];
            Class cls = forname(cn);
            if (cls == null) {
                failed("Cannot find " + cn + " class");
            }
            if (cls.getClassLoader() != null) {
                failed(cn + " class not loaded by boot classloader");
            }
        } else {
            failed("unknown option " + args[0]);
        }

        // We use -verbose:class to verify that Extra.class IS NOT loaded by AppCDS if
        // the boot classpath HAS been appended.
        ExtraClass.doit();

        System.out.println("Test passed: " + args[0]);
    }
}

class ExtraClass {
    static void doit() {}
}
