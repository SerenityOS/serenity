/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5006520
 * @summary Check many different ways to run Windows programs
 * @author Martin Buchholz
 * @key randomness
 */

import java.io.*;
import java.util.*;
import static java.lang.System.*;

class StreamDrainer extends Thread {
    private final InputStream is;
    private final ByteArrayOutputStream os = new ByteArrayOutputStream();
    public StreamDrainer(InputStream is) { this.is = is; }
    public void run() {
        try {
            int i;
            while ((i = is.read()) >= 0)
                os.write(i);
        } catch (Exception e) {}
    }
    public String toString() { return os.toString(); }
}

class CommandRunner {
    private static Random generator = new Random();
    public final int exitValue;
    public final String out;
    public final String err;
    CommandRunner(String... args) throws Exception {
        Process p = (generator.nextInt(2) == 0)
            ? new ProcessBuilder(args).start()
            : Runtime.getRuntime().exec(args);
        StreamDrainer d1 = new StreamDrainer(p.getInputStream());
        StreamDrainer d2 = new StreamDrainer(p.getErrorStream());
        d1.start();
        d2.start();
        p.waitFor();
        d1.join();
        d2.join();
        this.exitValue = p.exitValue();
        this.out = d1.toString();
        this.err = d2.toString();
    }
}

public class WinCommand {
    private static int failed = 0;

    private static void fail(String msg) {
        err.printf("FAIL: %s%n", msg);
        failed++;
    }

    private static String outputOf(String... args) {
        try {
            CommandRunner cr = new CommandRunner(args);
            if (cr.exitValue != 0)
                fail("exitValue != 0");
            if (! cr.err.equals(""))
                fail("stderr: " + cr.err);
            return cr.out.replaceFirst("[\r\n]+$", "");
        } catch (Exception e) {
            fail(e.toString());
            return "";
        }
    }

    private static void checkCD(String... filespecs) {
        String firstCD = null;
        for (String filespec : filespecs) {
            String CD = outputOf(filespec, "/C", "CD");
            out.printf("%s CD ==> %s%n", filespec, CD);
            if (firstCD == null) {
                firstCD = CD;
                checkDir(CD);
            }
            if (! CD.equals(firstCD)) {
                fail("Inconsistent result from CD subcommand");
                checkDir(CD);
            }
        }
    }

    private static void checkDir(String dirname) {
        if (! new File(dirname).isDirectory())
            fail(String.format("Not a directory: %s%n", dirname));
    }

    private static void writeFile(String filename, String contents) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(contents.getBytes());
            fos.close();
        } catch (Exception e) {
            fail("Unexpected exception" + e.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        File systemRoot =
            getenv("SystemRoot") != null ? new File(getenv("SystemRoot")) :
            getenv("WINDIR")     != null ? new File(getenv ("WINDIR")) :
            null;
        if (systemRoot == null || ! systemRoot.isDirectory())
            return; // Not Windows as we know it

        String systemDirW = new File(systemRoot, "System32").getPath();
        String systemDirM = systemDirW.replace('\\', '/');
        out.printf("systemDirW=%s%n", systemDirW);
        out.printf("systemDirM=%s%n", systemDirM);

        // Win9x systems don't have a cmd.exe
        if (new File(systemDirW, "cmd.exe").exists()) {
            try {
                out.println("Running cmd.exe tests...");
                writeFile("cdcmd.cmd", "@echo off\r\nCD\r\n");
                writeFile("cdbat.bat", "@echo off\r\nCD\r\n");
                checkCD("cmd",
                        "cmd.exe",
                        systemDirW + "\\cmd.exe",
                        // Only the ".exe" extension can be omitted
                        systemDirW + "\\cmd",
                        systemDirM + "/cmd.exe",
                        systemDirM + "/cmd",
                        "/" + systemDirM + "/cmd",
                        "cdcmd.cmd", "./cdcmd.cmd", ".\\cdcmd.cmd",
                        "cdbat.bat", "./cdbat.bat", ".\\cdbat.bat");
            } finally {
                new File("cdcmd.cmd").delete();
                new File("cdbat.bat").delete();
            }
        }

        // 16-bit apps like command.com must have a console;
        // fix this someday...

//      // Win64 systems don't have a command.com
//      if (new File(systemDirW, "command.com").exists()
//          // no output if running without a console;
//          // fix this in Mustang
//          && ! outputOf("command.com", "/C", "CD").equals("")) {
//          out.println("Running command.com tests...");
//          checkCD("command.com",
//                  systemDirM + "/command.com",
//                  systemDirW + "\\command.com");
//      }

        // Win9x systems have a %SYSTEMDRIVE%\command.com
//      if (new File("C:\\COMMAND.COM").exists()
//          && ! outputOf("COMMAND.COM", "/C", "CD").equals("")) {
//          out.println("Running COMMAND.COM tests...");
//          checkCD("C:/command.com",
//                  "C:\\command.com");
//      }

        if (failed > 0)
            throw new Exception(failed + " tests failed");
    }
}
