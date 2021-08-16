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

package nsk.jvmti.CompiledMethodUnload;

import java.io.*;
import java.math.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * This test exercises the JVMTI event <code>CompiledMethodUnload</code>.
 * <br>It creates an instance of tested class 'HotClass'. Then special
 * 'hot' methods are called in a loop in order to be compiled/inlined.
 * Then the class is provoked to be unloaded and, thus, CompiledMethodUnload
 * events generation for the compiled methods mentioned above.<br>
 * The CompiledMethodUnload events, if they occur, must be sent only
 * during the live phase of the VM execution.
 */
public class compmethunload001 {

    /**
     * class signature to be loaded and then unloaded
     */
    private final static String CLS_TO_BE_UNLOADED =
        "nsk.jvmti.CompiledMethodUnload.compmethunload001u";

    private final static int MAX_ITERATIONS = 5;

    static {
        try {
            System.loadLibrary("compmethunload001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"compmethunload001\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check();
    native int unloaded();

    public static void main(String[] argv) throws Exception {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) throws Exception {
        return new compmethunload001().runThis(argv, out);
    }

    public static void callHotClass(String location) throws Exception {
        String clsDir = location + File.separator + "loadclass";

        ClassUnloader clsUnLoader = new ClassUnloader();
        // load the class
        System.out.println("\nTrying to load class from "
                + clsDir + " ...");
        clsUnLoader.loadClass(CLS_TO_BE_UNLOADED, clsDir);

        Class<?> c = clsUnLoader.getLoadedClass();
        Object hotCls = c.newInstance();

        // Call hot methods that get compiled
        c.getMethod("entryMethod").invoke(hotCls);
        c.getMethod("entryNewMethod").invoke(hotCls);
        // provoke unloading of previously compiled methods
        hotCls = null;
        c = null;

        boolean clsUnloaded = clsUnLoader.unloadClass();
        clsUnLoader = null;
        System.gc();
    }

    private int runThis(String argv[], PrintStream out) throws Exception {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        String args[] = argHandler.getArguments();
        if (args.length < 1)
            throw new Failure("TEST BUG: path for class to be loaded is not specified");

        callHotClass(args[0]);

        // Wait until something is unloaded
        int num = unloaded();
        int iter = 0;
        while (num == 0) {
           System.gc();
           num = unloaded();
           iter++;
           if (iter > MAX_ITERATIONS) {
               throw new Failure("PRODUCT BUG: class was not unloaded in " + MAX_ITERATIONS);
           }
        }
        System.out.println("Number of unloaded events " + num + " number of iterations " + iter);
        return check();
    }
}
