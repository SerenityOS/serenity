/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4843136 4763384 8044841
   @summary Various race conditions caused exec'ed processes to have
   extra unused file descriptors, which caused hard-to-reproduce hangs.
   @author Martin Buchholz
*/

import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;

public class SleepyCat {

    private static void destroy (Process[] deathRow) {
        for (int i = 0; i < deathRow.length; ++i)
            if (deathRow[i] != null)
                deathRow[i].destroy();
    }

    static class TimeoutTask extends TimerTask {
        private Process[] deathRow;
        private boolean timedOut;

        TimeoutTask (Process[] deathRow) {
            this.deathRow = deathRow;
            this.timedOut = false;
        }

        public void run() {
            dumpState(deathRow);        // before killing the processes dump all the state

            timedOut = true;
            destroy(deathRow);
        }

        public boolean timedOut() {
            return timedOut;
        }
    }

    /**
     * Temporary debugging code for intermittent failures.
     * @param pids the processes to dump status for
     */
    static void dumpState(Process... pids) {
        if (!System.getProperty("os.name").contains("SunOS")) {
            return;
        }
        try {
            String[] psArgs = {"ps", "-elf"};
            Process ps = new ProcessBuilder(psArgs).inheritIO().start();
            ps.waitFor();
            String[] sfiles = {"pfiles", "self"};
            Process fds = new ProcessBuilder(sfiles).inheritIO().start();
            fds.waitFor();

            for (Process p : pids) {
                if (p == null)
                    continue;
                String[] pfiles = {"pfiles", Long.toString(p.pid())};
                fds = new ProcessBuilder(pfiles).inheritIO().start();
                fds.waitFor();
                String[] pstack = {"pstack", Long.toString(p.pid())};
                fds = new ProcessBuilder(pstack).inheritIO().start();
                fds.waitFor();
            }
        } catch (IOException | InterruptedException i) {
            i.printStackTrace();
        }
    }

    private static boolean hang1() throws IOException, InterruptedException {
        // Time out was reproducible on Solaris 50% of the time;
        // on Linux 80% of the time.
        //
        // Scenario: After fork(), parent executes and closes write end of child's stdin.
        // This causes child to retain a write end of the same pipe.
        // Thus the child will never see an EOF on its stdin, and will hang.
        Runtime rt = Runtime.getRuntime();
        // Increasing the iteration count makes the bug more
        // reproducible not only for the obvious reason, but also for
        // the subtle reason that it makes reading /proc/getppid()/fd
        // slower, making the child more likely to win the race!
        int iterations = 20;
        int timeout = 30;
        String[] catArgs   = new String[] {UnixCommands.cat()};
        String[] sleepArgs = new String[] {UnixCommands.sleep(),
                                            String.valueOf(timeout+1)};
        Process[] cats   = new Process[iterations];
        Process[] sleeps = new Process[iterations];
        Timer timer = new Timer(true);
        TimeoutTask catExecutioner = new TimeoutTask(cats);
        timer.schedule(catExecutioner, timeout * 1000);

        for (int i = 0; i < cats.length; ++i) {
            cats[i] = rt.exec(catArgs);
            java.io.OutputStream s = cats[i].getOutputStream();
            Process sleep = rt.exec(sleepArgs);
            s.close(); // race condition here
            sleeps[i] = sleep;
        }

        for (int i = 0; i < cats.length; ++i)
            cats[i].waitFor(); // hangs?

        timer.cancel();

        destroy(sleeps);

        if (catExecutioner.timedOut())
            System.out.println("Child process has a hidden writable pipe fd for its stdin.");
        return catExecutioner.timedOut();
    }

    private static boolean hang2() throws Exception {
        // Inspired by the imaginative test case for
        // 4850368 (process) getInputStream() attaches to forked background processes (Linux)

        // Time out was reproducible on Linux 80% of the time;
        // never on Solaris because of explicit close in Solaris-specific code.

        // Scenario: After fork(), the parent naturally closes the
        // child's stdout write end.  The child dup2's the write end
        // of its stdout onto fd 1.  On Linux, it fails to explicitly
        // close the original fd, and because of the parent's close()
        // of the fd, the child retains it.  The child thus ends up
        // with two copies of its stdout.  Thus closing one of those
        // write fds does not have the desired effect of causing an
        // EOF on the parent's read end of that pipe.
        Runtime rt = Runtime.getRuntime();
        int iterations = 10;
        Timer timer = new Timer(true);
        int timeout = 30;
        Process[] backgroundSleepers = new Process[iterations];
        TimeoutTask sleeperExecutioner = new TimeoutTask(backgroundSleepers);
        timer.schedule(sleeperExecutioner, timeout * 1000);
        byte[] buffer = new byte[10];
        String[] args =
            new String[] {UnixCommands.sh(), "-c",
                          "exec " + UnixCommands.sleep() + " "
                                  + (timeout+1) + " >/dev/null"};

        for (int i = 0;
             i < backgroundSleepers.length && !sleeperExecutioner.timedOut();
             ++i) {
            backgroundSleepers[i] = rt.exec(args); // race condition here
            try {
                // should get immediate EOF, but might hang
                if (backgroundSleepers[i].getInputStream().read() != -1)
                    throw new Exception("Expected EOF, got a byte");
            } catch (IOException e) {
                // Stream closed by sleeperExecutioner
                break;
            }
        }

        timer.cancel();

        destroy(backgroundSleepers);

        if (sleeperExecutioner.timedOut())
            System.out.println("Child process has two (should be one) writable pipe fds for its stdout.");
        return sleeperExecutioner.timedOut();
    }

    public static void main (String[] args) throws Exception {
        if (! UnixCommands.isUnix) {
            System.out.println("For UNIX only");
            return;
        }
        UnixCommands.ensureCommandsAvailable("sh", "cat", "sleep");

        if (hang1() | hang2())
            throw new Exception("Read from closed pipe hangs");
    }
}
