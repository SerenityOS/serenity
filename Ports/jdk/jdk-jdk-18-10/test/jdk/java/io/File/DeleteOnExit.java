/*
 * Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug  4614121 4809375 6437591
   @summary Basic test for deleteOnExit method
   @author kladko
 */


import java.io.File;

public class DeleteOnExit  {

    static String tmpdir = System.getProperty("java.io.tmpdir");
    static String java = System.getProperty("java.home") + File.separator +
                         "bin" + File.separator + "java";
    static File file1 = new File(tmpdir + "deletedOnExit1");
    static File file2 = new File(tmpdir + "deletedOnExit2");
    static File file3 = new File(tmpdir + "deletedOnExit3");

    // used to verify deletion order
    static File dir = new File(tmpdir + "deletedOnExitDir");
    static File file4 = new File(dir + File.separator + "deletedOnExit4");
    static File file5 = new File(dir + File.separator + "dxnsdnguidfgejngognrogn");
    static File file6 = new File(dir + File.separator + "mmmmmmsdmfgmdsmfgmdsfgm");
    static File file7 = new File(dir + File.separator + "12345566777");

    public static void main (String args[]) throws Exception{
        if (args.length == 0) {
            String cmd = java + " -classpath " + System.getProperty("test.classes")
                + " DeleteOnExit -test";
            Runtime.getRuntime().exec(cmd).waitFor();
            if (file1.exists() || file2.exists() || file3.exists() ||
                dir.exists()   || file4.exists() || file5.exists() ||
                file6.exists() || file7.exists())  {

                System.out.println(file1 + ", exists = " + file1.exists());
                System.out.println(file2 + ", exists = " + file2.exists());
                System.out.println(file3 + ", exists = " + file3.exists());
                System.out.println(dir + ", exists = " + dir.exists());
                System.out.println(file4 + ", exists = " + file4.exists());
                System.out.println(file5 + ", exists = " + file5.exists());
                System.out.println(file6 + ", exists = " + file6.exists());
                System.out.println(file7 + ", exists = " + file7.exists());

                // cleanup undeleted dir if test fails
                dir.delete();

                throw new Exception("File exists");
            }
        } else {
            file1.createNewFile();
            file2.createNewFile();
            file3.createNewFile();
            file1.deleteOnExit();
            file2.deleteOnExit();
            file3.deleteOnExit();

            // verify that deleting a File marked deleteOnExit will not cause a problem
            // during shutdown.
            file3.delete();

            // verify that calling deleteOnExit multiple times on a File does not cause
            // a problem during shutdown.
            file2.deleteOnExit();
            file2.deleteOnExit();
            file2.deleteOnExit();

            // Verify DeleteOnExit Internal implementation deletion order.
            if (dir.mkdir()) {
                dir.deleteOnExit();

                file4.createNewFile();
                file5.createNewFile();
                file6.createNewFile();
                file7.createNewFile();

                file4.deleteOnExit();
                file5.deleteOnExit();
                file6.deleteOnExit();
                file7.deleteOnExit();
            }
        }
    }
}
