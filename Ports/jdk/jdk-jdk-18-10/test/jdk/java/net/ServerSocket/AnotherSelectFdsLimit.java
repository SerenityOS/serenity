/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035897
 * @summary FD_SETSIZE should be set on macosx
 * @run main/othervm AnotherSelectFdsLimit 1023
 * @run main/othervm AnotherSelectFdsLimit 1024
 * @run main/othervm AnotherSelectFdsLimit 1025
 * @run main/othervm AnotherSelectFdsLimit 1600
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class AnotherSelectFdsLimit {
    static final int DEFAULT_FDS_TO_USE = 1600;

    public static void main(String [] args) throws Exception {
        if (!System.getProperty("os.name").contains("OS X")) {
            System.out.println("Test only run on MAC. Exiting.");
            return;
        }

        int fdsToUse = DEFAULT_FDS_TO_USE;
        if (args.length == 1)
            fdsToUse = Integer.parseInt(args[0]);

        System.out.println("Using " + fdsToUse + " fds.");

        List<Thread> threads = new ArrayList<>();
        for (int i=0; i<fdsToUse; i++)
            threads.add(new WorkerThread());

        for (Thread t : threads)
            t.start();

        for (Thread t : threads)
            t.join();
    }

    static class WorkerThread extends Thread {
        public void run() {
            try (ServerSocket ss = new ServerSocket(0)) {
                ss.setSoTimeout(2000);
                ss.accept();
            } catch (SocketTimeoutException x) {
                // expected
            } catch (IOException x) {
                throw new java.io.UncheckedIOException(x);
            }
        }
    }
}
