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

package nsk.jdi.ClassLoaderReference.definedClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.io.*;

/**
 * The debugged applcation of the test.
 */
public class definedclasses005a {

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

    private final static String prefix = "nsk.jdi.ClassLoaderReference.definedClasses.";
    private final static String checkedClassName = prefix + "definedclasses005b";
    static definedclasses005aClassLoader customClassLoader;
    static Class loadedClass;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) throws ClassNotFoundException {

        exitStatus = Consts.TEST_PASSED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        try {
            String checkedClassDir = (argHandler.getArguments())[0] + File.separator + "loadclass";
            customClassLoader = new definedclasses005aClassLoader(checkedClassDir);

            loadedClass = customClassLoader.preloadClass(checkedClassName);

            // ensure that class was loaded by custom class loader
            if (!(loadedClass.getClassLoader() instanceof definedclasses005aClassLoader)) {
                throw new Failure("Default system loader was used to load class " + checkedClassName);
            }

            display ("Checked class loaded but not prepared: " + checkedClassName);

            pipe.println(definedclasses005.SIGNAL_READY);

            receiveSignal(definedclasses005.SIGNAL_QUIT);
            display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        } catch (Failure e) {
            log.complain(e.getMessage());
            System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
        }
    }

    //--------------------------------------------------------- test specific methods

}

//--------------------------------------------------------- test specific classes

/**
 * Custom class loader.
 */
class definedclasses005aClassLoader extends ClassLoader {

    private String classPath;

    public definedclasses005aClassLoader(String classPath) {
        super(definedclasses005aClassLoader.class.getClassLoader());
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
