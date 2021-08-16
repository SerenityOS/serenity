/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * A test for
 * 6469606: (process) Process.destroy() can kill wrong process (Unix)
 * that is not suitable for use as a regression test, because it must
 * create 32768 processes, and depends on perl.
 */
import java.io.*;

public class FeelingLucky {

    public static void main(String[] args) throws Throwable {
        final Runtime rt = Runtime.getRuntime();
        final String[] pidPrinter = {"/bin/sh", "-c", "echo $$"};
        final Process minedProcess = rt.exec(pidPrinter);
        int minedPid = 0;
        final InputStream is = minedProcess.getInputStream();
        int c;
        while ((c = is.read()) >= '0' && c <= '9')
            minedPid = 10 * minedPid + (c - '0');
        System.out.printf("minedPid=%d%n", minedPid);
        minedProcess.waitFor();

        final String[] magnum = {
            "perl", "-e",
            "my $punk = getppid();" +
            "open TTY, '> /dev/tty';" +
            "print TTY \"punk=$punk\\n\";" +
            "for (my $i = 0; $i < 32768; $i++) {" +
            "  my $pid = fork();" +
            "  if ($pid == 0) {" +
            "    if ($$ == " + minedPid + ") {" +
            "      print TTY \"MATCH $$ $punk\\n\";" +
            "      $SIG{TERM} = sub {" +
            "        print TTY \"Got TERM $$ $punk\\n\";" +
            "        kill 9, $punk;" +
            "        exit; };" +
            "      $| = 1; print 'Go ahead.  Make my day.';" +
            "      sleep 100;" +
            "    }" +
            "    exit;" +
            "  } else { " +
            "    waitpid($pid,0);" +
            "    break if $pid == " + minedPid + ";" +
            "  }" +
            "}"
        };

        Process p = rt.exec(magnum);
        if (p.getInputStream().read() != -1) {
            System.out.println("got a char");
            minedProcess.destroy();
            Thread.sleep(1000);
        }
    }
}
