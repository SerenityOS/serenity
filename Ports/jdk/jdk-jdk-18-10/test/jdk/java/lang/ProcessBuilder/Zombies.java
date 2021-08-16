/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @run main/othervm Zombies
 * @bug 6474073 6180151
 * @summary Make sure zombies don't get created on Unix
 * @author Martin Buchholz
 */

import java.io.*;

public class Zombies {

    static final String os = System.getProperty("os.name");

    static final String TrueCommand = os.contains("OS X")?
        "/usr/bin/true" : "/bin/true";

    public static void main(String[] args) throws Throwable {
        if (! new File("/usr/bin/perl").canExecute() ||
            ! new File("/bin/ps").canExecute())
            return;
        System.out.println("Looks like a Unix system.");
        long mypid = ProcessHandle.current().pid();
        System.out.printf("mypid: %d%n", mypid);

        final Runtime rt = Runtime.getRuntime();

        try {
            rt.exec("no-such-file");
            throw new Error("expected IOException not thrown");
        } catch (IOException expected) {/* OK */}

        try {
            rt.exec(".");
            throw new Error("expected IOException not thrown");
        } catch (IOException expected) {/* OK */}

        try {
            rt.exec(TrueCommand, null, new File("no-such-dir"));
            throw new Error("expected IOException not thrown");
        } catch (IOException expected) {/* OK */}

        Process p = rt.exec(TrueCommand);
        ProcessHandle pp = p.toHandle().parent().orElse(null);
        System.out.printf("%s pid: %d, parent: %s%n", TrueCommand, p.pid(), pp);
        p.waitFor();

        // Count all the zombies that are children of this Java process
        final String[] zombieCounter = {
            "/usr/bin/perl", "-e",
                "$a=`/bin/ps -eo ppid,pid,s,command`;" +
                        "print @b=$a=~/^ *@{[getppid]} +[0-9]+ +Z.*$/mog;" +
                        "exit @b"
        };

        ProcessBuilder pb = new ProcessBuilder(zombieCounter);
        pb.inheritIO();
        int zombies = pb.start().waitFor();
        if (zombies != 0) {
            throw new Error(zombies + " zombies!");
        }
    }
}
