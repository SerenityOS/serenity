/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @requires (os.family == "linux" | os.family == "mac")
 * @run testng AsyncShutdown
 * @summary Test shutdownInput/shutdownOutput with threads blocked in read/write
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class AsyncShutdown {

    public void testShutdownInput1() throws IOException {
        withConnection((s1, s2) -> {
            scheduleShutdownInput(s1, 2000);
            int n = s1.getInputStream().read();
            assertTrue(n == -1);
        });
    }

    public void testShutdownInput2() throws IOException {
        withConnection((s1, s2) -> {
            scheduleShutdownInput(s1, 2000);
            s1.setSoTimeout(30*1000);
            int n = s1.getInputStream().read();
            assertTrue(n == -1);
        });
    }

    public void testShutdownOutput1() throws IOException {
        withConnection((s1, s2) -> {
            scheduleShutdownOutput(s1, 2000);
            byte[] data = new byte[128*1024];
            try {
                while (true) {
                    s1.getOutputStream().write(data);
                }
            } catch (IOException expected) { }
        });
    }

    public void testShutdownOutput2() throws IOException {
        withConnection((s1, s2) -> {
            s1.setSoTimeout(100);
            try {
                s1.getInputStream().read();
                assertTrue(false);
            } catch (SocketTimeoutException e) { }

            scheduleShutdownOutput(s1, 2000);
            byte[] data = new byte[128*1024];
            try {
                while (true) {
                    s1.getOutputStream().write(data);
                }
            } catch (IOException expected) { }
        });
    }

    static void scheduleShutdownInput(Socket s, long delay) {
        schedule(() -> {
            try {
                s.shutdownInput();
            } catch (IOException ioe) { }
        }, delay);
    }

    static void scheduleShutdownOutput(Socket s, long delay) {
        schedule(() -> {
            try {
                s.shutdownOutput();
            } catch (IOException ioe) { }
        }, delay);
    }

    static void schedule(Runnable task, long delay) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            executor.schedule(task, delay, TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdown();
        }
    }

    interface ThrowingBiConsumer<T, U> {
        void accept(T t, U u) throws IOException;
    }

    static void withConnection(ThrowingBiConsumer<Socket, Socket> consumer)
        throws IOException
    {
        Socket s1 = null;
        Socket s2 = null;
        try (ServerSocket ss = createBoundServer()) {
            s1 = new Socket();
            s1.connect(ss.getLocalSocketAddress());
            s2 = ss.accept();
            consumer.accept(s1, s2);
        } finally {
            if (s1 != null) s1.close();
            if (s2 != null) s2.close();
        }
    }

    static ServerSocket createBoundServer() throws IOException {
        ServerSocket ss = new ServerSocket();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress address = new InetSocketAddress(loopback, 0);
        ss.bind(address);
        return ss;
    }

}
