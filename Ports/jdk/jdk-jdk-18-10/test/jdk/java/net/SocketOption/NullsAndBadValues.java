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
 * @summary Basic test for NPE, UOE, and IAE for get/setOption
 * @run testng NullsAndBadValues
 * @run testng/othervm -Dsun.net.useExclusiveBind=false NullsAndBadValues
 */

import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketOption;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.Boolean.*;
import static java.net.StandardSocketOptions.*;
import static org.testng.Assert.expectThrows;

public class NullsAndBadValues {

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    @Test
    public void nulls() throws Exception {
        try (Socket s = new Socket()) {
            expectThrows(NPE, () -> s.setOption(null, null));
            expectThrows(NPE, () -> s.setOption(null, ""));
            expectThrows(NPE, () -> s.setOption(null, 1));
            expectThrows(NPE, () -> s.getOption(null));
        }
        try (ServerSocket ss = new ServerSocket()) {
            expectThrows(NPE, () -> ss.setOption(null, null));
            expectThrows(NPE, () -> ss.setOption(null, ""));
            expectThrows(NPE, () -> ss.setOption(null, 1));
            expectThrows(NPE, () -> ss.getOption(null));
        }
        try (DatagramSocket ds = new DatagramSocket()) {
            expectThrows(NPE, () -> ds.setOption(null, null));
            expectThrows(NPE, () -> ds.setOption(null, ""));
            expectThrows(NPE, () -> ds.setOption(null, 1));
            expectThrows(NPE, () -> ds.getOption(null));
        }
        try (MulticastSocket ms = new MulticastSocket()) {
            expectThrows(NPE, () -> ms.setOption(null, null));
            expectThrows(NPE, () -> ms.setOption(null, ""));
            expectThrows(NPE, () -> ms.setOption(null, 1));
            expectThrows(NPE, () -> ms.getOption(null));
        }
        try (Socket sa = SocketChannel.open().socket()) {
            expectThrows(NPE, () -> sa.setOption(null, null));
            expectThrows(NPE, () -> sa.setOption(null, ""));
            expectThrows(NPE, () -> sa.setOption(null, 1));
            expectThrows(NPE, () -> sa.getOption(null));
        }
        try (ServerSocket ssa = ServerSocketChannel.open().socket()) {
            expectThrows(NPE, () -> ssa.setOption(null, null));
            expectThrows(NPE, () -> ssa.setOption(null, ""));
            expectThrows(NPE, () -> ssa.setOption(null, 1));
            expectThrows(NPE, () -> ssa.getOption(null));
        }
        try (DatagramSocket dsa = DatagramChannel.open().socket()) {
            expectThrows(NPE, () -> dsa.setOption(null, null));
            expectThrows(NPE, () -> dsa.setOption(null, ""));
            expectThrows(NPE, () -> dsa.setOption(null, 1));
            expectThrows(NPE, () -> dsa.getOption(null));
        }
    }

    static final SocketOption<Boolean> FAKE_SOCK_OPT = new SocketOption<>() {
        @Override public String name() { return "FAKE_SOCK_OPT"; }
        @Override public Class<Boolean> type() { return Boolean.class; }
    };

    static final SocketOption RAW_SOCK_OPT = new SocketOption() {
        @Override public String name() { return "RAW_SOCK_OPT"; }
        @Override public Class type()  { return Boolean.class;  }
    };

    @Test
    public void uoe() throws Exception {
        try (Socket s = new Socket()) {
            expectThrows(UOE, () -> s.setOption(FAKE_SOCK_OPT, null));
            expectThrows(UOE, () -> s.setOption(FAKE_SOCK_OPT, TRUE));
            expectThrows(UOE, () -> s.setOption(FAKE_SOCK_OPT, FALSE));
            expectThrows(UOE, () -> s.setOption(RAW_SOCK_OPT, ""));
            expectThrows(UOE, () -> s.setOption(RAW_SOCK_OPT, 1));
            expectThrows(UOE, () -> s.getOption(FAKE_SOCK_OPT));
            expectThrows(UOE, () -> s.getOption(RAW_SOCK_OPT));
        }
        try (ServerSocket ss = new ServerSocket()) {
            expectThrows(UOE, () -> ss.setOption(FAKE_SOCK_OPT, null));
            expectThrows(UOE, () -> ss.setOption(FAKE_SOCK_OPT, TRUE));
            expectThrows(UOE, () -> ss.setOption(FAKE_SOCK_OPT, FALSE));
            expectThrows(UOE, () -> ss.setOption(RAW_SOCK_OPT, ""));
            expectThrows(UOE, () -> ss.setOption(RAW_SOCK_OPT, 1));
            expectThrows(UOE, () -> ss.getOption(FAKE_SOCK_OPT));
            expectThrows(UOE, () -> ss.getOption(RAW_SOCK_OPT));
        }
        try (DatagramSocket ds = new DatagramSocket()) {
            expectThrows(UOE, () -> ds.setOption(FAKE_SOCK_OPT, null));
            expectThrows(UOE, () -> ds.setOption(FAKE_SOCK_OPT, TRUE));
            expectThrows(UOE, () -> ds.setOption(FAKE_SOCK_OPT, FALSE));
            expectThrows(UOE, () -> ds.setOption(RAW_SOCK_OPT, ""));
            expectThrows(UOE, () -> ds.setOption(RAW_SOCK_OPT, 1));
            expectThrows(UOE, () -> ds.getOption(FAKE_SOCK_OPT));
            expectThrows(UOE, () -> ds.getOption(RAW_SOCK_OPT));
        }
        try (MulticastSocket ms = new MulticastSocket()) {
            expectThrows(UOE, () -> ms.setOption(FAKE_SOCK_OPT, null));
            expectThrows(UOE, () -> ms.setOption(FAKE_SOCK_OPT, TRUE));
            expectThrows(UOE, () -> ms.setOption(FAKE_SOCK_OPT, FALSE));
            expectThrows(UOE, () -> ms.setOption(RAW_SOCK_OPT, ""));
            expectThrows(UOE, () -> ms.setOption(RAW_SOCK_OPT, 1));
            expectThrows(UOE, () -> ms.getOption(FAKE_SOCK_OPT));
            expectThrows(UOE, () -> ms.getOption(RAW_SOCK_OPT));
        }
        try (Socket sa = SocketChannel.open().socket()) {
            expectThrows(UOE, () -> sa.setOption(FAKE_SOCK_OPT, null));
            expectThrows(UOE, () -> sa.setOption(FAKE_SOCK_OPT, TRUE));
            expectThrows(UOE, () -> sa.setOption(FAKE_SOCK_OPT, FALSE));
            expectThrows(UOE, () -> sa.setOption(RAW_SOCK_OPT, ""));
            expectThrows(UOE, () -> sa.setOption(RAW_SOCK_OPT, 1));
            expectThrows(UOE, () -> sa.getOption(FAKE_SOCK_OPT));
            expectThrows(UOE, () -> sa.getOption(RAW_SOCK_OPT));
        }
        try (ServerSocket ssa = ServerSocketChannel.open().socket()) {
            expectThrows(UOE, () -> ssa.setOption(FAKE_SOCK_OPT, null));
            expectThrows(UOE, () -> ssa.setOption(FAKE_SOCK_OPT, TRUE));
            expectThrows(UOE, () -> ssa.setOption(FAKE_SOCK_OPT, FALSE));
            expectThrows(UOE, () -> ssa.setOption(RAW_SOCK_OPT, ""));
            expectThrows(UOE, () -> ssa.setOption(RAW_SOCK_OPT, 1));
            expectThrows(UOE, () -> ssa.getOption(FAKE_SOCK_OPT));
            expectThrows(UOE, () -> ssa.getOption(RAW_SOCK_OPT));
        }
        try (DatagramSocket dsa = DatagramChannel.open().socket()) {
            expectThrows(UOE, () -> dsa.setOption(FAKE_SOCK_OPT, null));
            expectThrows(UOE, () -> dsa.setOption(FAKE_SOCK_OPT, TRUE));
            expectThrows(UOE, () -> dsa.setOption(FAKE_SOCK_OPT, FALSE));
            expectThrows(UOE, () -> dsa.setOption(RAW_SOCK_OPT, ""));
            expectThrows(UOE, () -> dsa.setOption(RAW_SOCK_OPT, 1));
            expectThrows(UOE, () -> dsa.getOption(FAKE_SOCK_OPT));
            expectThrows(UOE, () -> dsa.getOption(RAW_SOCK_OPT));
        }
    }

    static Map<SocketOption<?>,List<Object>> BAD_OPTION_VALUES = badOptionValues();

    static Map<SocketOption<?>,List<Object>> badOptionValues() {
        Map<SocketOption<?>,List<Object>> map = new HashMap<>();
        map.put(IP_MULTICAST_IF,   listOf(null)         );
        map.put(IP_MULTICAST_LOOP, listOf(null)         );
        map.put(IP_MULTICAST_TTL,  listOf(null, -1, 256));
        map.put(IP_TOS,            listOf(null, -1, 256));
        map.put(SO_BROADCAST,      listOf(null)         );
        map.put(SO_KEEPALIVE,      listOf(null)         );
        map.put(SO_LINGER,         listOf(null)         );
        map.put(SO_RCVBUF,         listOf(null, -1)     );
        map.put(SO_REUSEADDR,      listOf(null)         );
        map.put(SO_REUSEPORT,      listOf(null)         );
        map.put(SO_SNDBUF,         listOf(null, -1)     );
        map.put(TCP_NODELAY,       listOf(null)         );
        // extended options, not in the map, will get a null value
        return map;
    }

    // -- Socket

    @DataProvider(name = "socketBadOptionValues")
    public Object[][] socketBadOptionValues() throws Exception {
        try (Socket s = new Socket()) {
            return s.supportedOptions().stream()
                    .flatMap(NullsAndBadValues::socketOptionToBadValues)
                    .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "socketBadOptionValues")
    public <T> void socket(SocketOption<T> option, T value)
        throws Exception
    {
        try (Socket s = new Socket()) {
            expectThrows(IAE, () -> s.setOption(option, value));
        }
    }

    @Test(dataProvider = "socketBadOptionValues")
    public <T> void socketAdapter(SocketOption<T> option, T value)
        throws Exception
    {
        try (Socket s = SocketChannel.open().socket()) {
            expectThrows(IAE, () -> s.setOption(option, value));
        }
    }

    // -- ServerSocket

    @DataProvider(name = "serverSocketBadOptionValues")
    public Object[][] serverSocketBadOptionValues() throws Exception {
        try (ServerSocket ss = new ServerSocket()) {
            return ss.supportedOptions().stream()
                     .flatMap(NullsAndBadValues::socketOptionToBadValues)
                     .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "serverSocketBadOptionValues")
    public <T> void serverSocket(SocketOption<T> option, T value)
        throws Exception
    {
        try (ServerSocket ss = new ServerSocket()) {
            expectThrows(IAE, () -> ss.setOption(option, value));
        }
    }

    @Test(dataProvider = "serverSocketBadOptionValues")
    public <T> void serverSocketAdapter(SocketOption<T> option, T value)
        throws Exception
    {
        if (option == IP_TOS)
            return;  // SSC does not support IP_TOS

        try (ServerSocket ss = ServerSocketChannel.open().socket()) {
            expectThrows(IAE, () -> ss.setOption(option, value));
        }
    }

    // -- DatagramSocket

    @DataProvider(name = "datagramSocketBadOptionValues")
    public Object[][] datagramSocketBadOptionValues() throws Exception {
        try (DatagramSocket ds = new DatagramSocket()) {
            return ds.supportedOptions().stream()
                     .flatMap(NullsAndBadValues::socketOptionToBadValues)
                     .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "datagramSocketBadOptionValues")
    public <T> void datagramSocket(SocketOption<T> option, T value)
        throws Exception
    {
        try (DatagramSocket ds = new DatagramSocket()) {
            expectThrows(IAE, () -> ds.setOption(option, value));
        }
    }

    @Test(dataProvider = "datagramSocketBadOptionValues")
    public <T> void datagramSocketAdapter(SocketOption<T> option, T value)
        throws Exception
    {
        try (DatagramSocket ds = DatagramChannel.open().socket()) {
            expectThrows(IAE, () -> ds.setOption(option, value));
        }
    }

    // -- MulticastSocket

    @DataProvider(name = "multicastSocketBadOptionValues")
    public Object[][] multicastSocketBadOptionValues() throws Exception {
        try (MulticastSocket ms = new MulticastSocket()) {
            return ms.supportedOptions().stream()
                     .flatMap(NullsAndBadValues::socketOptionToBadValues)
                     .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "multicastSocketBadOptionValues")
    public <T> void multicastSocket(SocketOption<T> option, T value)
        throws Exception
    {
        try (MulticastSocket ms = new MulticastSocket()) {
            expectThrows(IAE, () -> ms.setOption(option, value));
        }
    }

    // --

    static List<Object> listOf(Object... objs) {
        List<Object> l = new ArrayList<>();
        if (objs == null)
            l.add(null);
        else
            Arrays.stream(objs).forEachOrdered(l::add);
        return l;
    }

    static Stream<Object[]> socketOptionToBadValues(SocketOption<?> socketOption) {
        List<Object> values = BAD_OPTION_VALUES.get(socketOption);
        if (values == null) {
            Object[][] a = new Object[][] { new Object[] { socketOption, null } };
            return Stream.of(a);
        }
        return values.stream()
                .flatMap(v -> Stream.of(new Object[][] { new Object[] { socketOption, v } }) );
    }
}
