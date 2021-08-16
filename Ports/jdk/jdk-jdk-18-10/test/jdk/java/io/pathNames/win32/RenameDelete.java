/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4042592 4042593
 * @summary Test operation of rename and delete on win32
 */

import java.io.*;

/**
 * This class tests to see if java.io.file rename() method
 * operates properly with non canonical pathnames
 * and then tests delete() method with non canonical pathnames
 */

public class RenameDelete {

    public static void main(String[] args) throws Exception {
        boolean success = false;

        if (File.separatorChar != '\\') {
            System.err.println("Not a win32 platform -- test inapplicable");
            return;
        }

        //construct a test file in this location
        File f1 = new File(".");
        StringBuffer location = new StringBuffer("\\");
        location.append(f1.getCanonicalPath());

        StringBuffer fromLocation = new StringBuffer(location.toString()+"\\From");
        StringBuffer toLocation = new StringBuffer(location.toString()+"\\To");

        f1 = new File(fromLocation.toString());
        File f2 = new File(toLocation.toString());

        if(f1.exists() || f2.exists()) {
            System.err.println("Directories exist -- test not valid");
            return;
        }

        System.err.println("Create:"+f1.mkdir());
        System.err.println("Exist as directory:"+f1.exists()+" "+f1.isDirectory());
        success = f1.renameTo(f2);
        System.err.println("Rename:"+success);

        if (!success)
            throw new RuntimeException("File method rename did not function");

        success = f2.delete();
        System.err.println("Delete:"+success);

        if (!success)
            throw new RuntimeException("File method delete did not function");

    }

}
