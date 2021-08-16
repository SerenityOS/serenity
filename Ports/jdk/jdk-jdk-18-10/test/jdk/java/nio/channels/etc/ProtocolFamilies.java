/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert.ThrowingRunnable;
import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import static java.lang.System.out;
import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardProtocolFamily.INET6;
import static jdk.test.lib.net.IPSupport.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

/*
 * @test
 * @summary Test SocketChannel, ServerSocketChannel and DatagramChannel
 *          with various ProtocolFamily combinations
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 * @run testng ProtocolFamilies
 * @run testng/othervm -Djava.net.preferIPv4Stack=true ProtocolFamilies
 */


public class ProtocolFamilies {
    static final boolean hasIPv6 = hasIPv6();
    static final boolean preferIPv4 = preferIPv4Stack();
    static Inet4Address ia4;
    static Inet6Address ia6;

    @BeforeTest()
    public void setup() throws Exception {
        NetworkConfiguration.printSystemConfiguration(out);
        IPSupport.printPlatformSupport(out);
        throwSkippedExceptionIfNonOperational();

        ia4 = getLocalIPv4Address();
        ia6 = getLocalIPv6Address();
        out.println("ia4: " + ia4);
        out.println("ia6: " + ia6 + "\n");
    }

    static final Class<UnsupportedAddressTypeException> UATE = UnsupportedAddressTypeException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    @DataProvider(name = "open")
    public Object[][] open() {
        if (hasIPv6 && !preferIPv4) {
            return new Object[][]{
                    {  INET,   null  },
                    {  INET6,  null  }
            };
        } else {
            return new Object[][]{
                    {  INET,   null  },
                    {  INET6,  UOE   }
            };
        }
    }

    @Test(dataProvider = "open")
    public void scOpen(StandardProtocolFamily family,
                       Class<? extends Exception> expectedException)
        throws Throwable
    {
        SocketChannel sc = null;
        try {
            if (expectedException == UOE) {
                try {
                    sc = openSC(family);
                } catch (UnsupportedOperationException e) {}
            } else {
                sc = openSC(family);
            }
        } finally {
            if (sc != null)
                sc.close();
        }
    }

    @Test(dataProvider = "open")
    public void sscOpen(StandardProtocolFamily family,
                        Class<? extends Exception> expectedException)
        throws Throwable
    {
        ServerSocketChannel ssc = null;
        try {
            if (expectedException == UOE) {
                try {
                    ssc = openSSC(family);
                } catch (UnsupportedOperationException e) {}
            } else {
                openSSC(family);
            }
        } finally {
            if (ssc != null)
                ssc.close();
        }
    }

    @Test(dataProvider = "open")
    public void dcOpen(StandardProtocolFamily family,
                       Class<? extends Exception> expectedException)
        throws Throwable
    {
        DatagramChannel dc = null;
        try {
            if (expectedException == UOE) {
                try {
                    dc = openDC(family);
                } catch (UnsupportedOperationException e) {}
            } else {
                openDC(family);
            }
        } finally {
            if (dc != null)
                dc.close();
        }
    }

    @DataProvider(name = "openBind")
    public Object[][] openBind() {
        if (hasIPv6 && !preferIPv4) {
            return new Object[][]{
                    {   INET,   INET,   null   },
                    {   INET,   INET6,  UATE   },
                    {   INET,   null,   null   },
                    {   INET6,  INET,   null   },
                    {   INET6,  INET6,  null   },
                    {   INET6,  null,   null   },
                    {   null,   INET,   null   },
                    {   null,   INET6,  null   },
                    {   null,   null,   null   }
            };
        } else {
            return new Object[][]{
                    {   INET,   INET,   null   },
                    {   INET,   INET6,  UATE   },
                    {   INET,   null,   null   },
                    {   null,   INET,   null   },
                    {   null,   INET6,  UATE   },
                    {   null,   null,   null   }
            };
        }
    }

    // SocketChannel open - INET, INET6, default
    // SocketChannel bind - INET, INET6, null

    @Test(dataProvider = "openBind")
    public void scOpenBind(StandardProtocolFamily ofamily,
                           StandardProtocolFamily bfamily,
                           Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (SocketChannel sc = openSC(ofamily)) {
            SocketAddress addr = getSocketAddress(bfamily);
            ThrowingRunnable bindOp = () -> sc.bind(addr);
                if (expectedException == null)
                    bindOp.run();
            else
                assertThrows(expectedException, bindOp);
        }
    }

    //  ServerSocketChannel open - INET, INET6, default
    //  ServerSocketChannel bind - INET, INET6, null

    @Test(dataProvider = "openBind")
    public void sscOpenBind(StandardProtocolFamily ofamily,
                            StandardProtocolFamily bfamily,
                            Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (ServerSocketChannel ssc = openSSC(ofamily)) {
            SocketAddress addr = getSocketAddress(bfamily);
            ThrowingRunnable bindOp = () -> ssc.bind(addr);
            if (expectedException == null)
                bindOp.run();
            else
                assertThrows(expectedException, bindOp);
        }
    }

    //  DatagramChannel open - INET, INET6, default
    //  DatagramChannel bind - INET, INET6, null

    @Test(dataProvider = "openBind")
    public void dcOpenBind(StandardProtocolFamily ofamily,
                           StandardProtocolFamily bfamily,
                           Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (DatagramChannel dc = openDC(ofamily)) {
            SocketAddress addr = getSocketAddress(bfamily);
            ThrowingRunnable bindOp = () -> dc.bind(addr);
            if (expectedException == null)
                bindOp.run();
            else
                assertThrows(expectedException, bindOp);
        }
    }

    //  SocketChannel open    - INET, INET6, default
    //  SocketChannel connect - INET, INET6, default

    @DataProvider(name = "openConnect")
    public Object[][] openConnect() {
        if (hasIPv6 && !preferIPv4) {
            return new Object[][]{
                    {   INET,   INET,   null   },
                    {   INET,   INET6,  null   },
                    {   INET,   null,   null   },
                    {   INET6,  INET,   UATE   },
                    {   INET6,  INET6,  null   },
                    {   INET6,  null,   null   },
                    {   null,   INET,   UATE   },
                    {   null,   INET6,  null   },
                    {   null,   null,   null   }
            };
        } else {
            // INET6 channels cannot be created - UOE - tested elsewhere
            return new Object[][]{
                    {   INET,   INET,   null   },
                    {   INET,   null,   null   },
                    {   null,   INET,   null   },
                    {   null,   null,   null   }
            };
        }
    }

    @Test(dataProvider = "openConnect")
    public void scOpenConnect(StandardProtocolFamily sfamily,
                              StandardProtocolFamily cfamily,
                              Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (ServerSocketChannel ssc = openSSC(sfamily)) {
            ssc.bind(null);
            SocketAddress saddr = ssc.getLocalAddress();
            try (SocketChannel sc = openSC(cfamily)) {
                if (expectedException == null)
                    sc.connect(saddr);
                else
                    assertThrows(expectedException, () -> sc.connect(saddr));
            }
        }
    }

    static final Class<NullPointerException> NPE = NullPointerException.class;

    // Tests null handling
    @Test
    public void testNulls() {
        assertThrows(NPE, () -> SocketChannel.open((ProtocolFamily)null));
        assertThrows(NPE, () -> ServerSocketChannel.open(null));
        assertThrows(NPE, () -> DatagramChannel.open(null));

        assertThrows(NPE, () -> SelectorProvider.provider().openSocketChannel(null));
        assertThrows(NPE, () -> SelectorProvider.provider().openServerSocketChannel(null));
        assertThrows(NPE, () -> SelectorProvider.provider().openDatagramChannel(null));
    }

    static final ProtocolFamily BAD_PF = () -> "BAD_PROTOCOL_FAMILY";

    // Tests UOE handling
    @Test
    public void testUoe() {
        assertThrows(UOE, () -> SocketChannel.open(BAD_PF));
        assertThrows(UOE, () -> ServerSocketChannel.open(BAD_PF));
        assertThrows(UOE, () -> DatagramChannel.open(BAD_PF));

        assertThrows(UOE, () -> SelectorProvider.provider().openSocketChannel(BAD_PF));
        assertThrows(UOE, () -> SelectorProvider.provider().openServerSocketChannel(BAD_PF));
        assertThrows(UOE, () -> SelectorProvider.provider().openDatagramChannel(BAD_PF));
    }

    // Helper methods

    private static SocketChannel openSC(StandardProtocolFamily family)
            throws IOException {
        SocketChannel sc = family == null ? SocketChannel.open()
                : SocketChannel.open(family);
        return sc;
    }

    private static ServerSocketChannel openSSC(StandardProtocolFamily family)
            throws IOException {
        ServerSocketChannel ssc = family == null ? ServerSocketChannel.open()
                : ServerSocketChannel.open(family);
        return ssc;
    }

    private static DatagramChannel openDC(StandardProtocolFamily family)
            throws IOException {
        DatagramChannel dc = family == null ? DatagramChannel.open()
                : DatagramChannel.open(family);
        return dc;
    }

    private static SocketAddress getSocketAddress(StandardProtocolFamily family) {
        return family == null ? null : switch (family) {
            case INET -> new InetSocketAddress(ia4, 0);
            case INET6 -> new InetSocketAddress(ia6, 0);
            default -> throw new RuntimeException("Unexpected protocol family");
        };
    }

    private static SocketAddress getLoopback(StandardProtocolFamily family, int port)
            throws UnknownHostException {
        if ((family == null || family == INET6) && hasIPv6) {
            return new InetSocketAddress(InetAddress.getByName("::1"), port);
        } else {
            return new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port);
        }
    }

    private static Inet4Address getLocalIPv4Address()
            throws Exception {
        return NetworkConfiguration.probe()
                .ip4Addresses()
                .filter(a -> !a.isLoopbackAddress())
                .findFirst()
                .orElse((Inet4Address)InetAddress.getByName("0.0.0.0"));
    }

    private static Inet6Address getLocalIPv6Address()
            throws Exception {
        return NetworkConfiguration.probe()
                .ip6Addresses()
                .filter(a -> !a.isLoopbackAddress())
                .findFirst()
                .orElse((Inet6Address) InetAddress.getByName("::0"));
    }
}
