/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4680945 4873419
 * @summary Check process exit code
 * @author kladko, Martin Buchholz
 */

import java.io.File;

public class ExitValue
{

    public static String join(String separator, String[] elts) {
        String result = elts[0];
        for (int i = 1; i < elts.length; ++i)
            result = result + separator + elts[i];
        return result;
    }

    public static void checkExitValue(String[] commandArgs,
                                      int expectedExitValue)
        throws Exception
    {
        if (! (new File(commandArgs[0]).exists()))
            return;

        System.out.println("Running command: " + join(" ", commandArgs));
        Process proc = Runtime.getRuntime().exec(commandArgs);
        int val;
        byte[] buf = new byte[4096];
        int n = proc.getErrorStream().read(buf);
        if (n > 0)
            throw new Exception
                ("Unexpected stderr: "
                 + new String(buf, 0, n, "ASCII"));
        if ((val = proc.waitFor()) != expectedExitValue)
            throw new Exception
                ("waitFor() returned unexpected value " + val);
        if ((val = proc.exitValue()) != expectedExitValue)
            throw new Exception
                ("exitValue() returned unexpected value " + val);
    }

    public static void checkPosixShellExitValue(String posixShellProgram,
                                                int expectedExitValue)
        throws Exception
    {
        checkExitValue(new String[] { UnixCommands.sh(), "-c", posixShellProgram },
                       expectedExitValue);
    }

    static final int EXIT_CODE = 5;

    public static void main(String[] args) throws Exception {
        if (! UnixCommands.isUnix) {
            System.out.println("For UNIX only");
            return;
        }
        UnixCommands.ensureCommandsAvailable("sh", "true", "kill");

        String java = join(File.separator, new String []
            { System.getProperty("java.home"), "bin", "java" });

        checkExitValue(new String[]
            { java,
              "-classpath", System.getProperty("test.classes", "."),
              "ExitValue$Run", String.valueOf(EXIT_CODE)
            }, EXIT_CODE);

        checkExitValue(new String[] { UnixCommands.findCommand("true") }, 0);

        checkPosixShellExitValue("exit", 0);

        checkPosixShellExitValue("exit 7", 7);

        int sigoffset = 128;
        checkPosixShellExitValue(UnixCommands.kill() + " -9 $$", sigoffset+9);
    }

    public static class Run {
        public static void main (String[] argv) {
            System.exit(Integer.parseInt(argv[0]));
        }
    }
}
