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

public class redefclass023 {

    final static int FAILED = 2;
    final static int JCK_STATUS_BASE = 95;

    static String fileDir = ".";

    static {
        try {
            System.loadLibrary("redefclass023");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load redefclass023 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int check(Class cls, byte bytes[]);

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        if (args.length > 0) {
            fileDir = args[0];
        }

        // Read data from class
        byte[] bytes;
        String fileName = fileDir + File.separator + "newclass" + File.separator +
            redefclass023a.class.getName().replace('.', File.separatorChar) +
            ".class";
        try {
            FileInputStream in = new FileInputStream(fileName);
            bytes = new byte[in.available()];
            in.read(bytes);
            in.close();
        } catch (Exception ex) {
            out.println("# Unexpected exception while reading class file:");
            out.println("# " + ex);
            return FAILED;
        }

        redefclass023a tc = new redefclass023a();
        return check(redefclass023a.class, bytes);
    }
}

interface redefclass023i {
    public void setValue(int i);
}

interface redefclass023j {
    public int getValue();
}

class redefclass023a implements redefclass023i {
    int intValue;

    public redefclass023a() {
        intValue = 0;
    }

    public void setValue(int i) {
        intValue = i;
    }

    public int getValue() {
        return intValue;
    }
}
