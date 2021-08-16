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

package nsk.jvmti.RedefineClasses;

import java.io.*;

/**
 * This test makes simple check that class can be redefined. It creates
 * an instance of tested class <code>redefclass001r</code>. Then the test
 * invokes native function <code>makeRedefinition()</code>. This native
 * function makes class file redifinition of loaded class
 * <code>redefclass001r</code>.<br>
 * Bytes of new version of the class <code>redefclass001r</code> are taken
 * from the <i>./newclass</i> directory.<br>
 * Finally, the test checks that the class <code>redefclass001r</code> was
 * redefined by invoking new version of its method.
 */
public class redefclass001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static boolean DEBUG_MODE = false;
    static String fileDir = ".";

    private PrintStream out;

    static {
        try {
            System.loadLibrary("redefclass001");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load redefclass001 library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int makeRedefinition(int verbose, Class redefClass,
        byte[] classBytes);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new redefclass001().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        File newRedefClassFile = null;
        byte[] redefClassBytes;
        int retValue = 0;

        this.out = out;
        for (int i = 0; i < argv.length; i++) {
            String token = argv[i];

            if (token.equals("-v")) // verbose mode
                DEBUG_MODE = true;
            else
                fileDir = token;
        }

        redefclass001r redefClsObj = new redefclass001r();
        if ((retValue=redefClsObj.checkIt(DEBUG_MODE, out)) == 19) {
            if (DEBUG_MODE)
                out.println("Successfully invoke method checkIt() of OLD redefclass001r");
        } else {
            out.println("TEST: failed to invoke method redefclass001r.checkIt()");
            return FAILED;
        }

// try to redefine class redefclass001r
        String fileName = fileDir + File.separator + "newclass" + File.separator +
            redefclass001r.class.getName().replace('.', File.separatorChar) +
            ".class";
        if (DEBUG_MODE)
            out.println("Trying to redefine class from the file: " + fileName);
        try {
            FileInputStream in = new FileInputStream(fileName);
            redefClassBytes = new byte[in.available()];
            in.read(redefClassBytes);
            in.close();
        } catch (Exception ex) {
            out.println("# Unexpected exception while reading class file:");
            out.println("# " + ex);
            return FAILED;
        }

// make real redefinition
        if (DEBUG_MODE)
            retValue=makeRedefinition(2, redefClsObj.getClass(),
                redefClassBytes);
        else
            retValue=makeRedefinition(1, redefClsObj.getClass(),
                redefClassBytes);
        if (retValue != PASSED) {
            out.println("TEST: failed to redefine class");
            return FAILED;
        }

        if ((retValue=redefClsObj.checkIt(DEBUG_MODE, out)) == 73) {
            if (DEBUG_MODE)
                out.println("Successfully invoke method checkIt() of NEW redefclass001r");
            return PASSED;
        } else {
            if (retValue == 19)
                out.println("TEST: the method redefclass001r.checkIt() is still old");
            else
                out.println("TEST: failed to call method redefclass001r.checkIt()");
            return FAILED;
        }
    }
}
