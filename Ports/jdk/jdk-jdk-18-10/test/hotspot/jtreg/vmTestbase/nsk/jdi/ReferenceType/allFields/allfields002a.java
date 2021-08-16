/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.allFields;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;
import java.io.*;


/**
 * This class is used as debugee application for the allfields002 JDI test.
 */

public class allfields002a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation

    private final static String package_prefix = "nsk.jdi.ReferenceType.allFields.";
    private final static String checked_class_name = package_prefix + "allfields002b";

    private static void print_log_on_verbose(String message) {
        if ( verbose_mode ) {
            System.err.println(message);
        }
    }

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
                verbose_mode = true;
                break;
            }
        }

        print_log_on_verbose("**> allfields002a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        String checked_class_dir = (argHandler.getArguments())[0] + File.separator + "loadclass";

        allfields002aClassLoader customClassLoader = new allfields002aClassLoader(checked_class_dir, checked_class_name);
        try {
            customClassLoader.preloadClass(checked_class_name);
            print_log_on_verbose
                ("--> allfields002a: checked class loaded but not prepared: " + checked_class_name);
        } catch (Throwable e) {  // ClassNotFoundException
            print_log_on_verbose
                ("--> allfields002a: checked class NOT loaded: " + e);
        }

        print_log_on_verbose("**> allfields002a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> allfields002a: \"quit\" signal recieved!");
            print_log_on_verbose("**> allfields002a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> allfields002a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> allfields002a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

/**
 * Custom class loader to load class without preparation.
 */
class allfields002aClassLoader extends ClassLoader {

    private String classPath;
    public static Class loadedClass;

    public allfields002aClassLoader(String classPath, String className) {
        super(allfields002aClassLoader.class.getClassLoader());
        this.classPath = classPath;
    }

    public void preloadClass (String className) throws ClassNotFoundException {
        loadedClass = findClass(className);
    }

    protected synchronized Class findClass(String className) throws ClassNotFoundException {
        String classFileName = classPath + "/" + className.replace('.', '/') + ".class";

        FileInputStream in;
        try {
            in = new FileInputStream(classFileName);
            if (in == null) {
                throw new ClassNotFoundException(classFileName);
            }
        } catch (FileNotFoundException e) {
            throw new ClassNotFoundException(classFileName, e);
        }

        int len;
        byte data[];
        try {
            len = in.available();
            data = new byte[len];
            for (int total = 0; total < data.length; ) {
                total += in.read(data, total, data.length - total);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(classFileName, e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new ClassNotFoundException(classFileName, e);
            }
        }

        return defineClass(className, data, 0, data.length);
    }
}
