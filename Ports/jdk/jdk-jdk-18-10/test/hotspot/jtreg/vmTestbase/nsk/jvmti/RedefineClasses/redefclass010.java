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
 * updates the attribute <i>LineNumberTable</i> of the redefined class.
 * Note, that all tested classes should be compiled with debugging info.
 * The test creates a dummy instance of tested class <code>redefclass010r</code>.
 * Then the test redefines this class. Bytes of new version of the class
 * <code>redefclass010r</code> are taken from the directory <i>./newclass</i>.
 * Finally, the test checks that <i>LineNumberTable</i>, one of the two
 * attributes, accessible in JVMTI (<i>LineNumberTable</i> and
 * <i>LocalVariableTable</i>), was updated after the redefinition.
 */
public class redefclass010 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static boolean DEBUG_MODE = false;
    static String fileDir = ".";

    private PrintStream out;

    static {
        try {
            System.loadLibrary("redefclass010");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load redefclass010 library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int makeRedefinition(int vrb, Class redefClass,
        byte[] classBytes);
    native static int checkOrigAttr(redefclass010r redefClsObj);
    native static int getResult(int vrb, redefclass010r redefClsObj);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new redefclass010().runIt(argv, out);
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

        redefclass010r redefClsObj = new redefclass010r();
        if (redefClsObj.checkIt(out, DEBUG_MODE) == 19) {
            if (DEBUG_MODE)
                out.println("Successfully check the class redefclass010r");
        } else {
            out.println("TEST: failed to check the class redefclass010r");
            return FAILED;
        }

// check the original attributes of the redefined class redefclass010r
        if ((retValue = checkOrigAttr(redefClsObj)) == FAILED)
            return FAILED;

// try to redefine the class redefclass010r
        String fileName = fileDir + File.separator + "newclass" + File.separator +
            redefclass010r.class.getName().replace('.', File.separatorChar) +
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
                "\tthe class redefclass010r was not redefined: checkIt() returned " +
                retValue);
            return FAILED;
        }

/* check that LineNumberTable in the redefined class redefclass010r
   was updated */
        if (DEBUG_MODE)
            return getResult(1, redefClsObj);
        else return getResult(0, redefClsObj);
    }
}
