/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetClassLoaderClasses;

import java.io.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class clsldrclss002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new clsldrclss002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // tested objects
    static ClassLoader testedClassLoader;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        log.display("Debugee started");

        // check on default classloader
        testedClassLoader = clsldrclss002.class.getClassLoader();
        status = checkStatus(status);

        // check on custom classloader
        try {
            testedClassLoader = new TestClassLoader();
            Class c = testedClassLoader.loadClass("nsk.jvmti.GetClassLoaderClasses.clsldrclss002a");
            status = checkStatus(status);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(out);
            return Consts.TEST_FAILED;
        }

        log.display("Debugee finished");
        return status;
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
