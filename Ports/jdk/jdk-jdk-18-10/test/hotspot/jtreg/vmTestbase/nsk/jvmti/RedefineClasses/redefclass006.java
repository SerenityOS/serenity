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
 * properly returns the error <code>JVMTI_ERROR_NULL_POINTER</code>:<br>
 * <i>Invalid pointer: classDefs or one of class_bytes is NULL</i><br>
 * The test creates a dummy instance of tested class <i>redefclass006r</i>.
 * Then the test tries twice to redefine the class <i>redefclass006r</i> by
 * sequencely invoking the function <code>RedefineClasses()</code> with:
 * <li>NULL pointer to the field <code>class_bytes</code> of the
 * structure <code>JVMTI_class_definition</code>
 * <li>NULL pointer to the structure <code>JVMTI_class_definition</code>
 * by itself.
 */
public class redefclass006 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static boolean DEBUG_MODE = false;

    private PrintStream out;

    static {
        try {
            System.loadLibrary("redefclass006");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load redefclass006 library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int makeRedefinition(int vrb, Class redefClass,
        byte[] classBytes);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new redefclass006().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        File newRedefClassFile = null;
        byte[] redefClassBytes;
        int totRes = PASSED;
        int retValue = PASSED;

        this.out = out;
        for (int i = 0; i < argv.length; i++) {
            String token = argv[i];

            if (token.equals("-v")) // verbose mode
                DEBUG_MODE = true;
        }

        redefclass006r redefClsObj = new redefclass006r();
        if (redefClsObj.checkIt(out, DEBUG_MODE) == 19) {
            if (DEBUG_MODE)
                out.println("Successfully check the class redefclass006r");
        } else {
            out.println("TEST: failed to check the class redefclass006r");
            return FAILED;
        }

// try to redefine class redefclass006r
        String fileName =
            redefclass006r.class.getName().replace('.', File.separatorChar) +
            ".class";
        if (DEBUG_MODE)
            out.println("Trying to redefine class from the file: " + fileName);
        try {
            ClassLoader cl = redefclass006.class.getClassLoader();
            InputStream in = cl.getSystemResourceAsStream(fileName);
            if (in == null) {
                out.println("# Class file \"" + fileName + "\" not found");
                return FAILED;
            }
            redefClassBytes = new byte[in.available()];
            in.read(redefClassBytes);
            in.close();
        } catch (Exception ex) {
            out.println("# Unexpected exception while reading class file:");
            out.println("# " + ex);
            return FAILED;
        }

/* check that if RedefineClasses() is invoked with NULL pointer to
   the field JVMTI_class_definition.class_bytes, it will return
   the error JVMTI_ERROR_NULL_POINTER */
        if (DEBUG_MODE)
            totRes = retValue = makeRedefinition(3,
                redefClsObj.getClass(), redefClassBytes);
        else
            totRes = retValue = makeRedefinition(2,
                redefClsObj.getClass(), redefClassBytes);
        if (DEBUG_MODE && retValue == PASSED)
            out.println("Check #1 PASSED:\n" +
                "\tRedefineClasses() being invoked with NULL pointer " +
                "to the JVMTI_class_definition.class_bytes,\n" +
                "\treturned the appropriate error JVMTI_ERROR_NULL_POINTER");

/* check that RedefineClasses() is invoked with NULL pointer to
   the JVMTI structure JVMTI_class_definition, it will return
   the error JVMTI_ERROR_NULL_POINTER */
        if (DEBUG_MODE)
            retValue = makeRedefinition(1,
                redefClsObj.getClass(), redefClassBytes);
        else
            retValue = makeRedefinition(0,
                redefClsObj.getClass(), redefClassBytes);
        if (retValue == FAILED)
            totRes = FAILED;
        else {
            if (DEBUG_MODE)
                out.println("\nCheck #2 PASSED:\n" +
                    "\tRedefineClasses() being invoked with NULL pointer " +
                    "to the JVMTI structure JVMTI_class_definition,\n" +
                    "\treturned the appropriate error JVMTI_ERROR_NULL_POINTER");
        }

        return totRes;
    }
}
