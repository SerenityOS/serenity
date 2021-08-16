/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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


/**
 * @test
 * @bug 8012453 8016046
 * @run main/othervm -Djava.security.manager=allow ExecCommand
 * @summary workaround for legacy applications with Runtime.getRuntime().exec(String command)
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.AccessControlException;

public class ExecCommand {
    static class SecurityMan extends SecurityManager {
        public static String unquote(String str)
        {
            int length = (str == null)
                ? 0
                : str.length();

            if (length > 1
                && str.charAt(0) == '\"'
                && str.charAt(length - 1) == '\"')
            {
               return str.substring(1, length - 1);
            }
            return str;
        }

        @Override public void checkExec(String cmd) {
            String ncmd = (new File(unquote(cmd))).getPath();
            if ( ncmd.equals(".\\Program")
              || ncmd.equals("\".\\Program")
              || ncmd.equals(".\\Program Files\\do.cmd")
              || ncmd.equals(".\\Program.cmd")
              || ncmd.equals("cmd"))
            {
                return;
            }
            super.checkExec(cmd);
        }

        @Override public void checkDelete(String file) {}
        @Override public void checkRead(String file) {}
    }

    // Parameters for the Runtime.exec calls
    private static final String TEST_RTE_ARG[] = {
        "cmd /C dir > dirOut.txt",
        "cmd /C dir > \".\\Program Files\\dirOut.txt\"",
        ".\\Program Files\\do.cmd",
        "\".\\Program Files\\doNot.cmd\" arg",
        "\".\\Program Files\\do.cmd\" arg",
        // compatibility
        "\".\\Program.cmd\" arg",
        ".\\Program.cmd arg",
    };

    private static final String doCmdCopy[] = {
        ".\\Program.cmd",
        ".\\Program Files\\doNot.cmd",
        ".\\Program Files\\do.cmd",
    };

    // Golden image for results
    private static final String TEST_RTE_GI[][] = {
                    //Def Legacy mode, Enforced mode, Set Legacy mode, Set Legacy mode & SM
        // [cmd /C dir > dirOut.txt]
        new String[]{"Success",
                     "IOException",  // [cmd /C dir ">" dirOut.txt] no redirection
                     "Success",
                     "IOException"}, //SM - no legacy mode, bad command

        // [cmd /C dir > ".\Program Files\dirOut.txt"]
        new String[]{"Success",
                     "IOException",  // [cmd /C dir ">" ".\Program Files\dirOut.txt"] no redirection
                     "Success",
                     "IOException"}, //SM - no legacy mode, bad command

        // [.\Program File\do.cmd]
        new String[]{"Success",
                     "IOException",  // [.\Program] not found
                     "Success",
                     "IOException"}, //SM - no legacy mode [.\Program] - OK

        // [".\Program File\doNot.cmd" arg]
        new String[]{"Success",
                     "Success",
                     "Success",
                     "AccessControlException"}, //SM   - [".\Program] - OK,
                                 //     [.\\Program Files\\doNot.cmd] - Fail

        // [".\Program File\do.cmd" arg]
        // AccessControlException
        new String[]{"Success",
                     "Success",
                     "Success",
                     "Success"}, //SM - [".\Program] - OK,
                                 //     [.\\Program Files\\do.cmd] - OK

        // compatibility
        new String[]{"Success", "Success", "Success", "Success"}, //[".\Program.cmd"]
        new String[]{"Success", "Success", "Success", "Success"}  //[.\Program.cmd]
    };

    private static void deleteOut(String path) {
        try {
            Files.delete(FileSystems.getDefault().getPath(path));
        } catch (IOException ex) {
            //that is OK
        }
    }
    private static void checkOut(String path) throws FileNotFoundException {
        if (Files.notExists(FileSystems.getDefault().getPath(path)))
            throw new FileNotFoundException(path);
    }

    public static void main(String[] _args) throws Exception {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            return;
        }

        // tear up
        try {
            new File(".\\Program Files").mkdirs();
            for (int i = 0; i < doCmdCopy.length; ++i) {
                try (BufferedWriter outCmd = new BufferedWriter(
                             new FileWriter(doCmdCopy[i]))) {
                    outCmd.write("@echo %1");
                }
            }
        } catch (IOException e) {
            throw new Error(e.getMessage());
        }

        // action
        for (int k = 0; k < 4; ++k) {
            switch (k) {
            case 0:
                // the "jdk.lang.Process.allowAmbiguousCommands" is undefined
                // "true" by default with the legacy verification procedure
                break;
            case 1:
                System.setProperty("jdk.lang.Process.allowAmbiguousCommands", "false");
                break;
            case 2:
                System.setProperty("jdk.lang.Process.allowAmbiguousCommands", "");
                break;
            case 3:
                System.setSecurityManager( new SecurityMan() );
                break;
            }
            for (int i = 0; i < TEST_RTE_ARG.length; ++i) {
                String outRes;
                try {
                    // tear up
                    switch (i) {
                    case 0:
                        // [cmd /C dir > dirOut.txt]
                        deleteOut(".\\dirOut.txt");
                        break;
                    case 1:
                        // [cmd /C dir > ".\Program Files\dirOut.txt"]
                        deleteOut(".\\Program Files\\dirOut.txt");
                        break;
                    }

                    Process exec = Runtime.getRuntime().exec(TEST_RTE_ARG[i]);
                    exec.waitFor();

                    //exteded check
                    switch (i) {
                    case 0:
                        // [cmd /C dir > dirOut.txt]
                        checkOut(".\\dirOut.txt");
                        break;
                    case 1:
                        // [cmd /C dir > ".\Program Files\dirOut.txt"]
                        checkOut(".\\Program Files\\dirOut.txt");
                        break;
                    }
                    outRes = "Success";
                } catch (IOException ioe) {
                    outRes = "IOException: " + ioe.getMessage();
                } catch (IllegalArgumentException iae) {
                    outRes = "IllegalArgumentException: " + iae.getMessage();
                } catch (AccessControlException se) {
                    outRes = "AccessControlException: " + se.getMessage();
                }

                if (!outRes.startsWith(TEST_RTE_GI[i][k])) {
                    throw new Error("Unexpected output! Step" + k + ":" + i
                                + "\nArgument: " + TEST_RTE_ARG[i]
                                + "\nExpected: " + TEST_RTE_GI[i][k]
                                + "\n  Output: " + outRes);
                } else {
                    System.out.println("RTE OK:" + TEST_RTE_ARG[i]);
                }
            }
        }
    }
}
