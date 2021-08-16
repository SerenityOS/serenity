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
 * This test checks that preexisting instance fields of a redefined
 * class will retain their previous values at the completion of
 * the redefinition and any new fields will have their default values.
 */
public class redefclass004 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int NO_RESULTS = 3;
    static final int JCK_STATUS_BASE = 95;

    static boolean DEBUG_MODE = false;
    static String fileDir = ".";

    private PrintStream out;

    static {
        try {
            System.loadLibrary("redefclass004");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load redefclass004 library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int makeRedefinition(int vrb, Class redefCls,
        byte[] classBytes);
    native static int checkNewFields(int vrb, redefclass004r redefClsObj);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new redefclass004().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        File newRedefClassFile = null;
        byte[] redefClassBytes;
        int retValue = 0;
        boolean totalRes = true;

        this.out = out;
        for (int i = 0; i < argv.length; i++) {
            String token = argv[i];

            if (token.equals("-v")) // verbose mode
                DEBUG_MODE = true;
            else
                fileDir = token;
        }

        redefclass004r redefClsObj = new redefclass004r();
        if ((retValue=redefClsObj.checkIt(out, DEBUG_MODE)) == 19) {
            if (DEBUG_MODE) {
                out.println("Successfully check the OLD version of redefclass004r");
                out.println("Default values of instance field of the redefined class:");
                out.println("\tbyteFld = " + redefClsObj.byteFld);
                out.println("\tshortFld = " + redefClsObj.shortFld);
                out.println("\tintFld = " + redefClsObj.intFld);
                out.println("\tlongFld = " + redefClsObj.longFld);
                out.println("\tfloatFld = " + redefClsObj.floatFld);
                out.println("\tdoubleFld = " + redefClsObj.doubleFld);
                out.println("\tcharFld = '" + redefClsObj.charFld + "'");
                out.println("\tbooleanFld = " + redefClsObj.booleanFld);
                out.println("\tstringFld = \"" + redefClsObj.stringFld + "\"\n");
            }
        } else {
            out.println("TEST: failed to check the OLD version of redefclass004r");
            out.println("      check CLASSPATH to the file oldclass/redefclass004r.class");
            out.println("      java.class.path: " +
                System.getProperty("java.class.path"));
            return FAILED;
        }
// change the values of instance (nonstatic) fields of a redefined class
        redefClsObj.byteFld = 1;
        redefClsObj.shortFld = 2;
        redefClsObj.intFld = 3;
        redefClsObj.longFld = 4L;
        redefClsObj.floatFld = 5.1F;
        redefClsObj.doubleFld = 6.0D;
        redefClsObj.charFld = 'a';
        redefClsObj.booleanFld = false;
        redefClsObj.stringFld = "redefclass004r";

// try to redefine class redefclass004r
        String fileName = fileDir + File.separator + "newclass" + File.separator +
            redefclass004r.class.getName().replace('.', File.separatorChar) +
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
            retValue=makeRedefinition(1, redefClsObj.getClass(),
                redefClassBytes);
        else
            retValue=makeRedefinition(0, redefClsObj.getClass(),
                redefClassBytes);
        switch (retValue) {
            case FAILED:
                out.println("TEST: failed to redefine class");
                return FAILED;
            case NO_RESULTS:
                return PASSED;
            case PASSED:
                break;
        }

        if ((retValue=redefClsObj.checkIt(out, DEBUG_MODE)) == 73) {
            if (DEBUG_MODE)
                out.println("Successfully check the NEW version of redefclass004r\n");

/* Check that preexisting instance (nonstatic) fields of the redefined class
   retain their previous values after the redefinition */
            if (redefClsObj.byteFld == 1 && redefClsObj.shortFld == 2 &&
                redefClsObj.intFld == 3 && redefClsObj.longFld == 4L &&
                redefClsObj.floatFld == 5.1F && redefClsObj.doubleFld == 6.0D &&
                redefClsObj.charFld == 'a' && redefClsObj.booleanFld == false &&
                redefClsObj.stringFld.equals("redefclass004r")) {
                if (DEBUG_MODE)
                    out.println("Check #1 PASSED: Preexisting instance fields " +
                        "of the redefined class retain their previous values\n");
            } else {
                out.println("TEST FAILED: Preexisting instance fields of the " +
                    "redefined class do not retain their previous values");
                out.println("Current values of instance field of the redefined class:");
                out.println("\tbyteFld = " + redefClsObj.byteFld +
                    ",\texpected 1");
                out.println("\tshortFld = " + redefClsObj.shortFld +
                    ",\texpected 2");
                out.println("\tintFld = " + redefClsObj.intFld +
                    ",\texpected 3");
                out.println("\tlongFld = " + redefClsObj.longFld +
                    ",\texpected 4");
                out.println("\tfloatFld = " + redefClsObj.floatFld +
                    ",\texpected 5.1");
                out.println("\tdoubleFld = " + redefClsObj.doubleFld +
                    ",\texpected 6.0");
                out.println("\tcharFld = '" + redefClsObj.charFld +
                    "',\texpected 'a'");
                out.println("\tbooleanFld = " + redefClsObj.booleanFld +
                    ",\texpected false");
                out.println("\tstringFld = \"" + redefClsObj.stringFld +
                    "\",\texpected \"redefclass004r\"\n");
                totalRes = false;
            }

/* Check that all new instance (nonstatic) fields of the redefined class
   will have their default values after redefinition */
            if (DEBUG_MODE)
                retValue=checkNewFields(1, redefClsObj);
            else
                retValue=checkNewFields(0, redefClsObj);
            if (retValue == PASSED) {
                if (DEBUG_MODE)
                    out.println("Check #2 PASSED: All new instance " +
                        "fields of the redefined class\n" +
                        "\thave their default values\n");
            } else {
                out.println("TEST FAILED: New instance fields " +
                    "of the redefined class were not assigned " +
                    "their default value\n");
                totalRes = false;
            }

        } else {
            if (retValue == 19)
                out.println("TEST FAILED: the method redefclass004r.checkIt() is still old");
            else
                out.println("TEST: failed to call method redefclass004r.checkIt()");

            return FAILED;
        }

        if (totalRes) return PASSED;
        else return FAILED;
    }
}
