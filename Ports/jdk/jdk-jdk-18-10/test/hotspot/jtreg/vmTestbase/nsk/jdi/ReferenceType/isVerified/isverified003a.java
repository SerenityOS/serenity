/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.isVerified;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.io.*;

/**
 * The debugged applcation of the test.
 */
public class isverified003a {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static ArgumentHandler argHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    public static void receiveSignal(String signal) {
        String line = pipe.readln();

        if ( !line.equals(signal) )
            throw new Failure("UNEXPECTED debugger's signal " + line);

        display("debuger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    private final static String prefix = "nsk.jdi.ReferenceType.isVerified.";
    private final static String checkedClassName1 = prefix + "isverified003b";
    private final static String checkedClassName2 = prefix + "isverified003c";
    static isverified003aClassLoader customClassLoader;
    static Class loadedClass1;
    static Class loadedClass2;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) throws ClassNotFoundException {

        exitStatus = Consts.TEST_PASSED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        try {
            String checkedClassDir = (argHandler.getArguments())[0] + File.separator + "loadclass";
            customClassLoader = new isverified003aClassLoader(checkedClassDir);

            loadedClass1 = loadUntilPreparation (checkedClassName1);
            loadedClass2 = loadUntilPreparation (checkedClassName2);

            pipe.println(isverified003.SIGNAL_READY);

            receiveSignal(isverified003.SIGNAL_QUIT);
            display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        } catch (Failure e) {
            log.complain(e.getMessage());
            System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
        }
    }

    //--------------------------------------------------------- test specific methods

    private static Class loadUntilPreparation (String className) throws ClassNotFoundException {
        Class loadedClass = customClassLoader.preloadClass(className);
        // ensure that class was loaded by custom class loader
        if (!(loadedClass.getClassLoader() instanceof isverified003aClassLoader)) {
            throw new Failure("Default system loader was used to load class " + className);
        }
        display ("Checked class loaded but not prepared: " + className);
        return loadedClass;
    }
}

//--------------------------------------------------------- test specific classes

/**
 * Custom class loader.
 */
class isverified003aClassLoader extends ClassLoader {

    private String classPath;

    public isverified003aClassLoader(String classPath) {
        super(isverified003aClassLoader.class.getClassLoader());
        this.classPath = classPath;
    }

    public Class preloadClass (String className) throws ClassNotFoundException {
        return findClass(className);
    }

    protected synchronized Class findClass(String name) throws ClassNotFoundException {
        String classFileName = classPath + "/" + name.replace('.', '/') + ".class";

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

        return defineClass(name, data, 0, data.length);
    }
}
