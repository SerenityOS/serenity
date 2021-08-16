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

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardProtocolFamily.INET6;
import static jdk.test.lib.net.IPSupport.*;

/*
 * @test
 * @summary Test SocketChannel, ServerSocketChannel and DatagramChannel
 *          open() and connect(), taking into consideration combinations of
 *          protocol families (INET, INET6, default),
 *          addresses (Inet4Address, Inet6Address).
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 * @run testng/othervm OpenAndConnect
 */


public class OpenAndConnect {
    static final Inet4Address IA4ANYLOCAL;
    static final Inet6Address IA6ANYLOCAL;
    static final Inet4Address IA4LOOPBACK;
    static final Inet6Address IA6LOOPBACK;
    static Inet4Address IA4LOCAL = null;
    static Inet6Address IA6LOCAL = null;
    static InetAddress DONT_BIND;

    static {
        try {
            IA4ANYLOCAL = (Inet4Address) InetAddress.getByName("0.0.0.0");
            IA6ANYLOCAL = (Inet6Address) InetAddress.getByName("::0");
            IA4LOOPBACK = (Inet4Address) InetAddress.getByName("127.0.0.1");
            IA6LOOPBACK = (Inet6Address) InetAddress.getByName("::1");

            // Special value to tell test not to call bind (address is not used)
            DONT_BIND = (Inet4Address) InetAddress.getByName("127.0.0.3");

            initAddrs();
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize addresses", e);
        }
    }

    @BeforeTest()
    public void setup() {
        NetworkConfiguration.printSystemConfiguration(out);
        IPSupport.printPlatformSupport(out);
        throwSkippedExceptionIfNonOperational();

        out.println("IA4LOCAL:    " + IA4LOCAL);
        out.println("IA6LOCAL:    " + IA6LOCAL);
        out.println("IA4ANYLOCAL: " + IA4ANYLOCAL);
        out.println("IA6ANYLOCAL: " + IA6ANYLOCAL);
        out.println("IA4LOOPBACK: " + IA4LOOPBACK);
        out.println("IA6LOOPBACK: " + IA6LOOPBACK);
    }

    @DataProvider(name = "openConnect")
    public Object[][] openConnect() {
        LinkedList<Object[]>  l = new LinkedList<>();
        if (IPSupport.hasIPv4()) {
            l.addAll(openConnectV4Tests);
            if (IA4LOCAL != null) {
                l.addAll(openConnectV4LocalTests);
            }
        }
        if (IPSupport.hasIPv6()) {
            l.addAll(openConnectV6Tests);
            if (IA6LOCAL != null) {
                l.addAll(openConnectV6LocalTests);
            }
        }
        if (IPSupport.hasIPv4() && IPSupport.hasIPv6()) {
            l.addAll(openConnectV4AndV6Tests);
            if (IA4LOCAL != null) {
                l.addAll(openConnectV4LocalAndV6Tests);
            }
        }
        return l.toArray(new Object[][]{});
    }

    //            +----- sfam is server/first socket family
    //            |
    //            |       +------ saddr is bind address for server/first socket
    //            |       |
    //            |       |              +---- cfam is family for client/second socket
    //            |       |              |
    //            |       |              |        +---- caddr is address client/second
    //            |       |              |        |     socket binds to. When the server
    //            |       |              |        |     has bound to a wildcard address
    //            |       |              |        |     this is address used for connect
    //            |       |              |        |     also.
    //            |       |              |        |
    //            |       |              |        |
    //            |       |              |        |
    //            |       |              |        |
    //            +       +              +        +
    //      {   sfam,   saddr,         cfam,    caddr,      }

    // Basic tests for when an IPv4 is available
    public static List<Object[]> openConnectV4Tests =
        Arrays.asList(new Object[][] {
            {   INET,   IA4LOOPBACK,   INET,    IA4LOOPBACK },
            {   INET,   IA4LOOPBACK,   null,    IA4LOOPBACK },
            {   INET,   IA4ANYLOCAL,   null,    IA4LOOPBACK },
            {   INET,   IA4ANYLOCAL,   INET,    IA4LOOPBACK },
            {   null,   IA4LOOPBACK,   INET,    IA4ANYLOCAL },
            {   null,   IA4LOOPBACK,   INET,    IA4LOOPBACK },
            {   null,   IA4LOOPBACK,   INET,    null        },
            {   null,   IA4LOOPBACK,   null,    null        }
        });

    // Additional tests for when an IPv4 local address is available
    public List<Object[]>  openConnectV4LocalTests =
        Arrays.asList(new Object[][] {
            {   INET,   IA4LOCAL,      INET,    IA4LOCAL    },
            {   INET,   IA4LOCAL,      null,    IA4LOCAL    },
            {   INET,   IA4LOCAL,      null,    DONT_BIND   },
            {   INET,   IA4ANYLOCAL,   INET,    IA4LOCAL    },
            {   INET,   IA4ANYLOCAL,   null,    IA4LOCAL    },
            {   null,   IA4LOCAL,      INET,    IA4ANYLOCAL },
            {   null,   IA4LOCAL,      INET,    IA4LOCAL    },
            {   null,   IA4LOCAL,      INET,    null        },
            {   null,   IA4LOCAL,      null,    null        }
        });

    // Basic tests for when an IPv6 is available
    public List<Object[]> openConnectV6Tests =
        Arrays.asList(new Object[][] {
            {   INET6,  IA6ANYLOCAL,   null,    IA6LOOPBACK },
            {   INET6,  IA6ANYLOCAL,   INET6,   IA6LOOPBACK },
            {   INET6,  IA6LOOPBACK,   INET6,   IA6LOOPBACK },
            {   INET6,  IA6LOOPBACK,   INET6,   IA6LOOPBACK },
            {   null,   IA6ANYLOCAL,   null,    IA6LOOPBACK },
            {   null,   IA6ANYLOCAL,   INET6,   IA6LOOPBACK },
            {   null,   IA6LOOPBACK,   INET6,   IA6LOOPBACK },
            {   null,   IA6LOOPBACK,   INET6,   DONT_BIND   },
            {   null,   IA6LOOPBACK,   INET6,   null        },
            {   null,   IA6LOOPBACK,   null,    IA6LOOPBACK },
            {   null,   IA6LOOPBACK,   null,    null        },
            {   null,   IA6LOOPBACK,   INET6,   IA6ANYLOCAL },
            {   null,   IA6LOOPBACK,   null,    IA6ANYLOCAL }
        });

    // Additional tests for when an IPv6 local address is available
    public List<Object[]> openConnectV6LocalTests =
        Arrays.asList(new Object[][] {
            {   INET6,  IA6ANYLOCAL,   null,    IA6LOCAL    },
            {   INET6,  IA6ANYLOCAL,   INET6,   IA6LOCAL    },
            {   INET6,  IA6LOCAL,      INET6,   IA6LOCAL    },
            {   INET6,  IA6LOCAL,      null,    IA6LOCAL    },
            {   INET6,  IA6LOCAL,      null,    DONT_BIND   },
            {   INET6,  IA6LOCAL,      INET6,   IA6LOCAL    },
            {   null,   IA6ANYLOCAL,   null,    IA6LOCAL    },
            {   null,   IA6ANYLOCAL,   INET6,   IA6LOCAL    },
            {   null,   IA6LOCAL,      INET6,   IA6LOCAL    },
            {   null,   IA6LOCAL,      INET6,   IA6ANYLOCAL },
            {   null,   IA6LOCAL,      null,    IA6ANYLOCAL },
            {   null,   IA6LOCAL,      null,    IA6LOCAL    },
            {   null,   IA6LOCAL,      INET6,   null        },
            {   null,   IA6LOCAL,      null,    null        }
        });

    // Additional tests for when IPv4 and IPv6 are available
     public List<Object[]> openConnectV4AndV6Tests =
        Arrays.asList(new Object[][] {
            {   null,   IA4LOOPBACK,   INET6,   IA6ANYLOCAL },
            {   null,   IA4LOOPBACK,   null,    IA6ANYLOCAL },
            {   null,   IA4LOOPBACK,   INET6,   DONT_BIND   },
            {   null,   IA4LOOPBACK,   INET6,   null        }
        });

    // Additional tests for when IPv4 local address and IPv6 are available
    public List<Object[]> openConnectV4LocalAndV6Tests =
        Arrays.asList(new Object[][] {
            {   null,   IA4LOCAL,      INET6,   IA6ANYLOCAL },
            {   null,   IA4LOCAL,      INET6,   null        },
            {   null,   IA4LOCAL,      null,    IA6ANYLOCAL }
        });

    /**
     * If the destination address is the wildcard, it is replaced by the alternate
     * using the port number from destination. Otherwise destination is returned.
     * Only used by dcOpenAndConnect
     */
    static InetSocketAddress getDestinationAddress(SocketAddress destination, InetAddress alternate) {
        InetSocketAddress isa = (InetSocketAddress)destination;
        if (isa.getAddress().isAnyLocalAddress())
            return new InetSocketAddress(alternate, isa.getPort());
        else
            return isa;
    }

    @Test(dataProvider = "openConnect")
    public void scOpenAndConnect(ProtocolFamily sfam,
                                 InetAddress saddr,
                                 ProtocolFamily cfam,
                                 InetAddress caddr) throws IOException
    {
        out.printf("scOpenAndConnect: server bind: %s client bind: %s\n", saddr, caddr);
        try (ServerSocketChannel ssc = openSSC(sfam)) {
            ssc.bind(getSocketAddress(saddr));
            InetSocketAddress ssa = (InetSocketAddress)ssc.getLocalAddress();
            ssa = getDestinationAddress(ssa, caddr);
            out.println(ssa);
            try (SocketChannel csc = openSC(cfam)) {
                if (caddr != DONT_BIND) {
                    csc.bind(getSocketAddress(caddr));
                }
                csc.connect(ssa);
            }
        }
    }

    @Test(dataProvider = "openConnect")
    public void dcOpenAndConnect(ProtocolFamily sfam,
                                 InetAddress saddr,
                                 ProtocolFamily cfam,
                                 InetAddress caddr) throws IOException
    {
        try (DatagramChannel sdc = openDC(sfam)) {
            sdc.bind(getSocketAddress(saddr));
            SocketAddress ssa = sdc.socket().getLocalSocketAddress();
            ssa = getDestinationAddress(ssa, caddr);
            out.println(ssa);
            try (DatagramChannel dc = openDC(cfam)) {
                if (caddr != DONT_BIND) {
                    dc.bind(getSocketAddress(caddr));
                }
                dc.connect(ssa);
            }
        }
    }

    // Helper methods

    private static SocketChannel openSC(ProtocolFamily fam) throws IOException {
        return fam == null ? SocketChannel.open() : SocketChannel.open(fam);
    }

    private static ServerSocketChannel openSSC(ProtocolFamily fam)
            throws IOException {
        return fam == null ? ServerSocketChannel.open()
                : ServerSocketChannel.open(fam);
    }

    private static DatagramChannel openDC(ProtocolFamily fam)
            throws IOException {
        return fam == null ? DatagramChannel.open()
                : DatagramChannel.open(fam);
    }

    private static SocketAddress getSocketAddress(InetAddress ia) {
        return ia == null ? null : new InetSocketAddress(ia, 0);
    }

    private static void initAddrs() throws IOException {

        NetworkConfiguration cfg = NetworkConfiguration.probe();

        IA4LOCAL = cfg.ip4Addresses()
                .filter(a -> !a.isLoopbackAddress())
                .findFirst()
                .orElse(null);

        IA6LOCAL = cfg.ip6Addresses()
                .filter(a -> !a.isLoopbackAddress())
                .findFirst()
                .orElse(null);
    }
}
