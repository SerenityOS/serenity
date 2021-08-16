/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules java.management java.base/java.io:+open java.base/java.net:+open
 *          java.base/sun.net java.base/sun.nio.ch:+open
 * @run main/othervm UnreferencedDatagramSockets
 * @run main/othervm -Djava.net.preferIPv4Stack=true UnreferencedDatagramSockets
 * @summary Check that unreferenced datagram sockets are closed
 */

import java.io.FileDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import com.sun.management.UnixOperatingSystemMXBean;

import jdk.test.lib.net.IPSupport;
import sun.net.NetProperties;

public class UnreferencedDatagramSockets {

    /**
     * The set of sockets we have to check up on.
     */
    final static ArrayDeque<NamedWeak> pendingSockets = new ArrayDeque<>(5);

    /**
     * Queued objects when they are unreferenced.
     */
    final static ReferenceQueue<Object> pendingQueue = new ReferenceQueue<>();

    // Server to echo a datagram packet
    static class Server implements Runnable {

        DatagramSocket ss;
        CountDownLatch latch = new CountDownLatch(1);

        Server() throws IOException {
            ss = new DatagramSocket(0, getHost());
            System.out.printf("  DatagramServer addr: %s: %d%n",
                    this.getHost(), this.getPort());
            pendingSockets.add(new NamedWeak(ss, pendingQueue, "serverDatagramSocket"));
            extractRefs(ss, "serverDatagramSocket");
        }

        InetAddress getHost() throws UnknownHostException {
            InetAddress localhost = lookupLocalHost();
            return localhost;
        }

        int getPort() {
            return ss.getLocalPort();
        }

        // Receive a byte and send back a byte
        public void run() {
            try {
                byte[] buffer = new byte[50];
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                ss.receive(p);
                buffer[0] += 1;
                ss.send(p);         // send back +1
                latch.await();      // wait for the client to receive the packet
                // do NOT close but 'forget' the datagram socket reference
                ss = null;
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
    }

    static InetAddress lookupLocalHost() throws UnknownHostException {
        return InetAddress.getByName("localhost"); //.getLocalHost();
    }

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        // Create and close a DatagramSocket to warm up the FD count for side effects.
        try (DatagramSocket s = new DatagramSocket(0, lookupLocalHost())) {
            // no-op; close immediately
            s.getLocalPort();   // no-op
        }

        long fdCount0 = getFdCount();
        listProcFD();

        // start a server
        Server svr = new Server();
        Thread thr = new Thread(svr);
        thr.start();

        DatagramSocket client = new DatagramSocket(0, lookupLocalHost());
        client.connect(svr.getHost(), svr.getPort());
        pendingSockets.add(new NamedWeak(client, pendingQueue, "clientDatagramSocket"));
        extractRefs(client, "clientDatagramSocket");

        byte[] msg = new byte[1];
        msg[0] = 1;
        DatagramPacket p = new DatagramPacket(msg, msg.length, svr.getHost(), svr.getPort());
        client.send(p);

        p = new DatagramPacket(msg, msg.length);
        client.receive(p);
        svr.latch.countDown(); // unblock the server


        System.out.printf("echo received from: %s%n", p.getSocketAddress());
        if (msg[0] != 2) {
            throw new AssertionError("incorrect data received: expected: 2, actual: " + msg[0]);
        }

        // Do NOT close the DatagramSocket; forget it

        Object ref;
        int loops = 20;
        while (!pendingSockets.isEmpty() && loops-- > 0) {
            ref = pendingQueue.remove(1000L);
            if (ref != null) {
                pendingSockets.remove(ref);
                System.out.printf("  ref freed: %s, remaining: %d%n", ref, pendingSockets.size());
            } else {
                client = null;
                p = null;
                msg = null;
                System.gc();
            }
        }

        thr.join();

        // List the open file descriptors
        long fdCount = getFdCount();
        System.out.printf("Initial fdCount: %d, final fdCount: %d%n", fdCount0, fdCount);
        listProcFD();

        if (loops == 0) {
            throw new AssertionError("Not all references reclaimed");
        }
    }

    // Get the count of open file descriptors, or -1 if not available
    private static long getFdCount() {
        OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
        return (mxBean instanceof UnixOperatingSystemMXBean)
                ? ((UnixOperatingSystemMXBean) mxBean).getOpenFileDescriptorCount()
                : -1L;
    }

    private static boolean usePlainDatagramSocketImpl() {
        PrivilegedAction<String> pa = () -> NetProperties.get("jdk.net.usePlainDatagramSocketImpl");
        String s = AccessController.doPrivileged(pa);
        return (s != null) && (s.isEmpty() || s.equalsIgnoreCase("true"));
    }

    // Reflect to find references in the datagram implementation that will be gc'd
    private static void extractRefs(DatagramSocket s, String name) {
        try {
            Field datagramSocketField = DatagramSocket.class.getDeclaredField("delegate");
            datagramSocketField.setAccessible(true);

            if (!usePlainDatagramSocketImpl()) {
                // DatagramSocket using DatagramSocketAdaptor
                Object DatagramSocket = datagramSocketField.get(s);
                assert DatagramSocket.getClass() == Class.forName("sun.nio.ch.DatagramSocketAdaptor");

                Method m = DatagramSocket.class.getDeclaredMethod("getChannel");
                m.setAccessible(true);
                DatagramChannel datagramChannel = (DatagramChannel) m.invoke(DatagramSocket);

                assert datagramChannel.getClass() == Class.forName("sun.nio.ch.DatagramChannelImpl");

                Field fileDescriptorField = datagramChannel.getClass().getDeclaredField("fd");
                fileDescriptorField.setAccessible(true);
                FileDescriptor fileDescriptor = (FileDescriptor) fileDescriptorField.get(datagramChannel);
                extractRefs(fileDescriptor, name);

            } else {
                // DatagramSocket using PlainDatagramSocketImpl
                Object DatagramSocket = datagramSocketField.get(s);
                assert DatagramSocket.getClass() == Class.forName("java.net.NetMulticastSocket");

                Method m = DatagramSocket.getClass().getDeclaredMethod("getImpl");
                m.setAccessible(true);
                DatagramSocketImpl datagramSocketImpl = (DatagramSocketImpl) m.invoke(DatagramSocket);

                Field fileDescriptorField = DatagramSocketImpl.class.getDeclaredField("fd");
                fileDescriptorField.setAccessible(true);
                FileDescriptor fileDescriptor = (FileDescriptor) fileDescriptorField.get(datagramSocketImpl);
                extractRefs(fileDescriptor, name);

                Class<?> socketImplClass = datagramSocketImpl.getClass();
                System.out.printf("socketImplClass: %s%n", socketImplClass);
                if (socketImplClass.getName().equals("java.net.TwoStacksPlainDatagramSocketImpl")) {
                    Field fileDescriptor1Field = socketImplClass.getDeclaredField("fd1");
                    fileDescriptor1Field.setAccessible(true);
                    FileDescriptor fileDescriptor1 = (FileDescriptor) fileDescriptor1Field.get(datagramSocketImpl);
                    extractRefs(fileDescriptor1, name + "::twoStacksFd1");

                } else {
                    System.out.printf("socketImpl class name not matched: %s != %s%n",
                            socketImplClass.getName(), "java.net.TwoStacksPlainDatagramSocketImpl");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new AssertionError("missing field", ex);
        }
    }

    private static void extractRefs(FileDescriptor fileDescriptor, String name) {
        Object cleanup = null;
        int rawfd = -1;
        try {
            if (fileDescriptor != null) {
                Field fd1Field = FileDescriptor.class.getDeclaredField("fd");
                fd1Field.setAccessible(true);
                rawfd = fd1Field.getInt(fileDescriptor);

                Field cleanupfdField = FileDescriptor.class.getDeclaredField("cleanup");
                cleanupfdField.setAccessible(true);
                cleanup = cleanupfdField.get(fileDescriptor);
                pendingSockets.add(new NamedWeak(fileDescriptor, pendingQueue,
                        name + "::fileDescriptor: " + rawfd));
                pendingSockets.add(new NamedWeak(cleanup, pendingQueue, name + "::fdCleanup: " + rawfd));

            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
            throw new AssertionError("missing field", ex);
        } finally {
            System.out.print(String.format("  %s:: fd: %s, fd: %d, cleanup: %s%n",
                    name, fileDescriptor, rawfd, cleanup));
        }
    }

    /**
     * Method to list the open file descriptors (if supported by the 'lsof' command).
     */
    static void listProcFD() {
        List<String> lsofDirs = List.of("/usr/bin", "/usr/sbin");
        Optional<Path> lsof = lsofDirs.stream()
                .map(s -> Paths.get(s, "lsof"))
                .filter(f -> Files.isExecutable(f))
                .findFirst();
        lsof.ifPresent(exe -> {
            try {
                System.out.printf("Open File Descriptors:%n");
                long pid = ProcessHandle.current().pid();
                ProcessBuilder pb = new ProcessBuilder(exe.toString(), "-p", Integer.toString((int) pid));
                pb.inheritIO();
                Process p = pb.start();
                p.waitFor(10, TimeUnit.SECONDS);
            } catch (IOException | InterruptedException ie) {
                ie.printStackTrace();
            }
        });
    }

    // Simple class to identify which refs have been queued
    static class NamedWeak extends WeakReference<Object> {
        private final String name;

        NamedWeak(Object o, ReferenceQueue<Object> queue, String name) {
            super(o, queue);
            this.name = name;
        }

        public String toString() {
            return name + "; " + super.toString();
        }
    }
}
