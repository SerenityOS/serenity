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
 * This test checks that the JVMTI function <code>RedefineClasses()</code>
 * clears all breakpoints in the redefined class.
 * The test creates a dummy instance of tested class <i>redefclass008r</i>.
 * Then the test sets several breakpoints in the tested class and
 * redefines this class.<br>
 * Bytes of new version of the class <code>redefclass008r</code>
 * are taken from the directory <i>./newclass</i>.<br>
 * Finally, the test checks that all breakpoints were cleared after
 * the redefinition.<br><br>
 * The test was updated due to the bug 4441348.
 */
public class redefclass008 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static boolean DEBUG_MODE = false;
    static String fileDir = ".";

    private PrintStream out;

    static {
        try {
            System.loadLibrary("redefclass008");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load redefclass008 library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int makeRedefinition(int vrb, Class redefClass,
        byte[] classBytes);
    native static int setBreakpoints(int vrb, redefclass008r redefClsObj);
    native static int getResult(int vrb, redefclass008r redefClsObj);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new redefclass008().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        File newRedefClassFile = null;
        byte[] redefClassBytes;
        int retValue = PASSED;
        int totRes = PASSED;

        this.out = out;
        for (int i = 0; i < argv.length; i++) {
            String token = argv[i];

            if (token.equals("-v")) // verbose mode
                DEBUG_MODE = true;
            else
                fileDir = token;
        }

        redefclass008r redefClsObj = new redefclass008r();
        if (redefClsObj.checkIt(out, DEBUG_MODE) == 19) {
            if (DEBUG_MODE)
                out.println("Successfully check the class redefclass008r");
        } else {
            out.println("TEST: failed to check the class redefclass008r");
            return FAILED;
        }

// set several breakpoints in the redefined class redefclass008r
        if (DEBUG_MODE)
            retValue = setBreakpoints(1, redefClsObj);
        else
            retValue = setBreakpoints(0, redefClsObj);
        if (retValue == FAILED)
            return FAILED;

// try to redefine the class redefclass008r
        String fileName = fileDir + File.separator + "newclass" + File.separator +
            redefclass008r.class.getName().replace('.', File.separatorChar) +
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

        if (DEBUG_MODE)
            retValue = makeRedefinition(1,
                redefClsObj.getClass(), redefClassBytes);
        else
            retValue = makeRedefinition(0,
                redefClsObj.getClass(), redefClassBytes);
        if (retValue == FAILED)
            return FAILED;

        if ((retValue = redefClsObj.checkIt(out, DEBUG_MODE)) == 73) {
            if (DEBUG_MODE)
                out.println("Successfully check new version of the redefined class");
        } else {
            out.println("TEST FAILED:\n" +
                "\tthe class redefclass008r was not redefined: checkIt() returned " +
                retValue);
            return FAILED;
        }

/* check that all breakpoints in the redefined class redefclass008r
   were cleared */
        if (DEBUG_MODE)
            return getResult(1, redefClsObj);
        else return getResult(0, redefClsObj);
    }
}
