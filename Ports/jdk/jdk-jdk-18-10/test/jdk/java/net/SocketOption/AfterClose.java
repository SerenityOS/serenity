/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224477
 * @summary Ensures that IOException is thrown after the socket is closed
 * @run testng AfterClose
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketOption;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.Boolean.*;
import static java.net.StandardSocketOptions.*;
import static org.testng.Assert.expectThrows;

public class AfterClose {

    static final Class<IOException> IOE = IOException.class;
    static final String RO = "READ_ONLY";

    static Map<SocketOption<?>,List<Object>> OPTION_VALUES_MAP = optionValueMap();

    static boolean supportsMulticast(NetworkInterface ni) {
        try {
            return ni.supportsMulticast();
        } catch (SocketException e) {
            return false;
        }
    }

    static List<Object> listNetworkInterfaces() {
        try {
            return NetworkInterface.networkInterfaces()
                    .filter(AfterClose::supportsMulticast)
                    .collect(Collectors.toList());
        } catch (Exception e) { }
        return List.of();
    }

    static Map<SocketOption<?>,List<Object>> optionValueMap() {
        Map<SocketOption<?>,List<Object>> map = new HashMap<>();
        map.put(IP_MULTICAST_IF,   listNetworkInterfaces() );
        map.put(IP_MULTICAST_LOOP, listOf(TRUE, FALSE) );
        map.put(IP_MULTICAST_TTL,  listOf(0, 100, 255) );
        map.put(IP_TOS,            listOf(0, 101, 255) );
        map.put(SO_BROADCAST,      listOf(TRUE, FALSE) );
        map.put(SO_KEEPALIVE,      listOf(TRUE, FALSE) );
        map.put(SO_LINGER,         listOf(0, 5, 15)    );
        map.put(SO_RCVBUF,         listOf(1, 100, 1000));
        map.put(SO_REUSEADDR,      listOf(TRUE, FALSE) );
        map.put(SO_REUSEPORT,      listOf(TRUE, FALSE) );
        map.put(SO_SNDBUF,         listOf(1, 100, 1000));
        map.put(TCP_NODELAY,       listOf(TRUE, FALSE) );
        // extended options
        try {
            Class<?> c = Class.forName("jdk.net.ExtendedSocketOptions");
            Field field = c.getField("TCP_QUICKACK");
            map.put((SocketOption<?>)field.get(null), listOf(TRUE, FALSE));
            field = c.getField("TCP_KEEPIDLE");
            map.put((SocketOption<?>)field.get(null), listOf(10, 100));
            field = c.getField("TCP_KEEPINTERVAL");
            map.put((SocketOption<?>)field.get(null), listOf(10, 100));
            field = c.getField("TCP_KEEPCOUNT");
            map.put((SocketOption<?>)field.get(null), listOf(10, 100));
            field = c.getField("SO_INCOMING_NAPI_ID");
            map.put((SocketOption<?>)field.get(null), listOf(RO));
        } catch (ClassNotFoundException e) {
            // ignore, jdk.net module not present
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return map;
    }

    // -- Socket

    @DataProvider(name = "socketOptionValues")
    public Object[][] socketOptionValues() throws Exception {
        try (Socket s = new Socket()) {
            return s.supportedOptions().stream()
                    .map(so -> new Object[] {so, OPTION_VALUES_MAP.get(so)})
                    .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "socketOptionValues")
    public <T> void closedSocketImplUncreated(SocketOption<T> option, List<T> values)
        throws IOException
    {
        Socket socket = createClosedSocketImplUncreated();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                expectThrows(IOE, () -> socket.setOption(option, value));
                expectThrows(IOE, () -> socket.getOption(option));
            }
        }
    }

    @Test(dataProvider = "socketOptionValues")
    public <T> void closedSocketImplCreated(SocketOption<T> option, List<T> values)
        throws IOException
    {
        Socket socket = createClosedSocketImplCreated();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                expectThrows(IOE, () -> socket.setOption(option, value));
                expectThrows(IOE, () -> socket.getOption(option));
            }
        }
    }

    @Test(dataProvider = "socketOptionValues")
    public <T> void closedSocketAdapter(SocketOption<T> option, List<T> values)
        throws IOException
    {
        Socket socket = createClosedSocketFromAdapter();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                if (!RO.equals(value)) expectThrows(IOE, () -> socket.setOption(option, value));
                expectThrows(IOE, () -> socket.getOption(option));
            }
        }
    }

    // -- ServerSocket

    @DataProvider(name = "serverSocketOptionValues")
    public Object[][] serverSocketOptionValues() throws Exception {
        try (ServerSocket ss = new ServerSocket()) {
            return ss.supportedOptions().stream()
                     .map(so -> new Object[] {so, OPTION_VALUES_MAP.get(so)})
                     .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "serverSocketOptionValues")
    public <T> void closedServerSocketImplUncreated(SocketOption<T> option, List<T> values)
        throws IOException
    {
        ServerSocket serverSocket = createClosedServerSocketImplUncreated();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                expectThrows(IOE, () -> serverSocket.setOption(option, value));
                expectThrows(IOE, () -> serverSocket.getOption(option));
            }
        }
    }

    @Test(dataProvider = "serverSocketOptionValues")
    public <T> void closedServerSocketImplCreated(SocketOption<T> option, List<T> values)
        throws IOException
    {
        ServerSocket serverSocket = createClosedServerSocketImplCreated();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                expectThrows(IOE, () -> serverSocket.setOption(option, value));
                expectThrows(IOE, () -> serverSocket.getOption(option));
            }
        }
    }

    @Test(dataProvider = "serverSocketOptionValues")
    public <T> void closedServerSocketAdapter(SocketOption<T> option, List<T> values)
        throws IOException
    {
        if (option == IP_TOS)
            return;  // SSC does not support IP_TOS

        ServerSocket serverSocket = createClosedServerSocketFromAdapter();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                if (!RO.equals(value)) expectThrows(IOE, () -> serverSocket.setOption(option, value));
                expectThrows(IOE, () -> serverSocket.getOption(option));
            }
        }
    }

    // -- DatagramSocket

    @DataProvider(name = "datagramSocketOptionValues")
    public Object[][] datagramSocketOptionValues() throws Exception {
        try (DatagramSocket ds = new DatagramSocket()) {
            return ds.supportedOptions().stream()
                     .map(so -> new Object[] {so, OPTION_VALUES_MAP.get(so)})
                     .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "datagramSocketOptionValues")
    public <T> void closedUnboundDatagramSocket(SocketOption<T> option, List<T> values)
        throws IOException
    {
        DatagramSocket datagramSocket = createClosedUnboundDatagramSocket();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                if (!RO.equals(value)) expectThrows(IOE, () -> datagramSocket.setOption(option, value));
                expectThrows(IOE, () -> datagramSocket.getOption(option));
            }
        }
    }

    @Test(dataProvider = "datagramSocketOptionValues")
    public <T> void closedBoundDatagramSocket(SocketOption<T> option, List<T> values)
        throws IOException
    {
        DatagramSocket datagramSocket = createClosedBoundDatagramSocket();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                if (!RO.equals(value)) expectThrows(IOE, () -> datagramSocket.setOption(option, value));
                expectThrows(IOE, () -> datagramSocket.getOption(option));
            }
        }
    }

    @Test(dataProvider = "datagramSocketOptionValues")
    public <T> void closedDatagramAdapter(SocketOption<T> option, List<T> values)
        throws IOException
    {
        DatagramSocket datagramSocket = createClosedBoundDatagramSocket();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                if (!RO.equals(value)) expectThrows(IOE, () -> datagramSocket.setOption(option, value));
                expectThrows(IOE, () -> datagramSocket.getOption(option));
            }
        }
    }

    // -- MulticastSocket

    @DataProvider(name = "multicastSocketOptionValues")
    public Object[][] multicastSocketOptionValues() throws Exception {
        try (MulticastSocket ms = new MulticastSocket()) {
            return ms.supportedOptions().stream()
                     .map(so -> new Object[] {so, OPTION_VALUES_MAP.get(so)})
                     .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "multicastSocketOptionValues")
    public <T> void closedUnboundMulticastSocket(SocketOption<T> option, List<T> values)
        throws IOException
    {
        MulticastSocket multicastSocket = createClosedUnboundMulticastSocket();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                if (!RO.equals(value)) expectThrows(IOE, () -> multicastSocket.setOption(option, value));
                expectThrows(IOE, () -> multicastSocket.getOption(option));
            }
        }
    }

    @Test(dataProvider = "multicastSocketOptionValues")
    public <T> void closedBoundMulticastSocket(SocketOption<T> option, List<T> values)
        throws IOException
    {
        MulticastSocket multicastSocket = createClosedBoundMulticastSocket();
        for (int i=0; i<3; i++); {
            for (T value : values) {
                if (!RO.equals(value)) expectThrows(IOE, () -> multicastSocket.setOption(option, value));
                expectThrows(IOE, () -> multicastSocket.getOption(option));
            }
        }
    }

    // --

    static List<Object> listOf(Object... objs) {
        List<Object> l = new ArrayList<>();
        Arrays.stream(objs).forEachOrdered(l::add);
        return l;
    }

    // Returns a closed Socket that has an impl whose `create` method has NOT been invoked.
    static Socket createClosedSocketImplUncreated() throws IOException {
        Socket s = new Socket();
        s.close();
        return s;
    }

    // Returns a closed Socket that has an impl whose `create` method has been invoked.
    static Socket createClosedSocketImplCreated() throws IOException {
        Socket s = new Socket();
        s.bind(null);  // binding causes impl::create to be invoked
        s.close();
        return s;
    }

    // Returns a closed Socket created from a SocketChannel's adapter.
    static Socket createClosedSocketFromAdapter() throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.close();
        return sc.socket();
    }

    // Returns a closed ServerSocket that has an impl whose `create` method has NOT been invoked.
    static ServerSocket createClosedServerSocketImplUncreated() throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.close();
        return ss;
    }

    // Returns a closed ServerSocket that has an impl whose `create` method has been invoked.
    static ServerSocket createClosedServerSocketImplCreated() throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.bind(null);  // binding causes impl::create to be invoked
        ss.close();
        return ss;
    }

    // Returns a closed ServerSocket created from a ServerSocketChannel's adapter.
    static ServerSocket createClosedServerSocketFromAdapter() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.close();
        return ssc.socket();
    }

    // Returns a closed unbound DatagramSocket.
    static DatagramSocket createClosedUnboundDatagramSocket() throws IOException {
        DatagramSocket ds = new DatagramSocket(null);
        assert ds.isBound() == false;
        ds.close();
        return ds;
    }

    // Returns a closed bound DatagramSocket.
    static DatagramSocket createClosedBoundDatagramSocket() throws IOException {
        DatagramSocket ds = new DatagramSocket();
        assert ds.isBound() == true;
        ds.close();
        return ds;
    }

    // Returns a closed DatagramSocket that created from a DatagramChannel's adapter.
    static DatagramSocket createClosedDatagramSocketFromAdapter() throws IOException {
        DatagramChannel dc = DatagramChannel.open();
        dc.close();
        return dc.socket();
    }

    // Returns a closed unbound MulticastSocket.
    static MulticastSocket createClosedUnboundMulticastSocket() throws IOException {
        MulticastSocket ms = new MulticastSocket(null);
        assert ms.isBound() == false;
        ms.close();
        return ms;
    }

    // Returns a closed bound MulticastSocket.
    static MulticastSocket createClosedBoundMulticastSocket() throws IOException {
        MulticastSocket ms = new MulticastSocket();
        assert ms.isBound() == true;
        ms.close();
        return ms;
    }
}
