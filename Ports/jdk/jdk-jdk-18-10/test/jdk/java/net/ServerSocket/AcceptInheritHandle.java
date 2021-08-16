/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8067105
 * @library /test/lib
 * @summary Socket returned by ServerSocket.accept() is inherited by child process on Windows
 * @author Chris Hegarty
 */

import java.io.*;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import jdk.test.lib.net.IPSupport;

public class AcceptInheritHandle {

    enum ServerSocketProducer {
        JAVA_NET(() -> {
            try {
                return new ServerSocket();
            } catch(IOException x) {
                throw new UncheckedIOException(x);
            }
        }),
        NIO_CHANNELS(() -> {
            try {
                return ServerSocketChannel.open().socket();
            } catch (IOException x) {
                throw new UncheckedIOException(x);
            }
        });

        final Supplier<ServerSocket> supplier;
        ServerSocketProducer(Supplier<ServerSocket> supplier) {
            this.supplier = supplier;
        }
        Supplier<ServerSocket> supplier () { return supplier; }
    }

    static final String JAVA = System.getProperty("java.home")
        + File.separator + "bin" + File.separator + "java";

    static final String CLASSPATH = System.getProperty("java.class.path");

    public static void main(String[] args) throws Exception {
        if (args.length == 1)
            server(ServerSocketProducer.valueOf(args[0]));
        else
            mainEntry();
    }

    static void mainEntry() throws Exception {
        testJavaNetServerSocket();
        testNioServerSocketChannel();
    }

    static void testJavaNetServerSocket() throws Exception {
        test(ServerSocketProducer.JAVA_NET);
        if (IPSupport.hasIPv4()) {
            test(ServerSocketProducer.JAVA_NET, "-Djava.net.preferIPv4Stack=true");
        }
    }
    static void testNioServerSocketChannel() throws Exception {
        test(ServerSocketProducer.NIO_CHANNELS);
    }

    static void test(ServerSocketProducer ssp, String... jvmArgs) throws Exception {
        System.out.println("\nStarting test for " + ssp.name());

        List<String> commands = new ArrayList<>();
        commands.add(JAVA);
        for (String arg : jvmArgs)
            commands.add(arg);
        commands.add("-cp");
        commands.add(CLASSPATH);
        commands.add("AcceptInheritHandle");
        commands.add(ssp.name());

        System.out.println("Executing: "+ commands);
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process serverProcess = pb.start();
        DataInputStream dis = new DataInputStream(serverProcess.getInputStream());

        int port = dis.readInt();
        System.out.println("Server process listening on " + port + ", connecting...");

        String address;
        if (Arrays.stream(jvmArgs).anyMatch("-Djava.net.preferIPv4Stack=true"::equals)) {
            address = "127.0.0.1";
        } else {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            address = loopback.getHostAddress();
        }
        Socket socket = new Socket(address, port);
        String s = dis.readUTF();
        System.out.println("Server process said " + s);

        serverProcess.destroy();
        serverProcess.waitFor(30, TimeUnit.SECONDS);
        System.out.println("serverProcess exitCode:" + serverProcess.exitValue());

        try {
            socket.setSoTimeout(10 * 1000);
            socket.getInputStream().read();
        } catch (SocketTimeoutException x) {
            // failed
            throw new RuntimeException("Failed: should get reset, not " + x);
        } catch (SocketException x) {
            System.out.println("Expected:" + x);
        }
    }

    static void server(ServerSocketProducer producer) throws Exception {
        try (ServerSocket ss = producer.supplier().get()) {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            ss.bind(new InetSocketAddress(loopback, 0));
            int port = ss.getLocalPort();
            DataOutputStream dos = new DataOutputStream(System.out);
            dos.writeInt(port);
            dos.flush();

            ss.accept();  // do not close

            Runtime.getRuntime().exec("sleep 20");
            Thread.sleep(3 * 1000);

            dos.writeUTF("kill me!");
            dos.flush();
            Thread.sleep(30 * 1000);
        }
    }
}
