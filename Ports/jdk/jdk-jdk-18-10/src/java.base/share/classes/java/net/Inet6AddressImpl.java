/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package java.net;

import java.io.IOException;

import static java.net.InetAddress.IPv6;
import static java.net.InetAddress.PREFER_IPV6_VALUE;
import static java.net.InetAddress.PREFER_SYSTEM_VALUE;

/*
 * Package private implementation of InetAddressImpl for dual
 * IPv4/IPv6 stack.
 * <p>
 * If InetAddress.preferIPv6Address is true then anyLocalAddress()
 * and localHost() will return IPv6 addresses, otherwise IPv4 addresses.
 *
 * loopbackAddress() will return the first valid loopback address in
 * [IPv6 loopback, IPv4 loopback] if InetAddress.preferIPv6Address is true,
 * else [IPv4 loopback, IPv6 loopback].
 * If neither are valid it will fallback to the first address tried.
 *
 * @since 1.4
 */
class Inet6AddressImpl implements InetAddressImpl {

    public native String getLocalHostName() throws UnknownHostException;

    public native InetAddress[] lookupAllHostAddr(String hostname)
        throws UnknownHostException;

    public native String getHostByAddr(byte[] addr) throws UnknownHostException;

    private native boolean isReachable0(byte[] addr, int scope, int timeout,
                                        byte[] inf, int ttl, int if_scope)
        throws IOException;

    public boolean isReachable(InetAddress addr, int timeout,
                               NetworkInterface netif, int ttl)
        throws IOException
    {
        byte[] ifaddr = null;
        int scope = -1;
        int netif_scope = -1;
        if (netif != null) {
            /*
             * Let's make sure we bind to an address of the proper family.
             * Which means same family as addr because at this point it could
             * be either an IPv6 address or an IPv4 address (case of a dual
             * stack system).
             */
            java.util.Enumeration<InetAddress> it = netif.getInetAddresses();
            InetAddress inetaddr;
            while (it.hasMoreElements()) {
                inetaddr = it.nextElement();
                if (inetaddr.getClass().isInstance(addr)) {
                    ifaddr = inetaddr.getAddress();
                    if (inetaddr instanceof Inet6Address) {
                        netif_scope = ((Inet6Address) inetaddr).getScopeId();
                    }
                    break;
                }
            }
            if (ifaddr == null) {
                // Interface doesn't support the address family of
                // the destination
                return false;
            }
        }
        if (addr instanceof Inet6Address)
            scope = ((Inet6Address) addr).getScopeId();
        return isReachable0(addr.getAddress(), scope, timeout, ifaddr, ttl, netif_scope);
    }

    public synchronized InetAddress anyLocalAddress() {
        if (anyLocalAddress == null) {
            if (InetAddress.preferIPv6Address == PREFER_IPV6_VALUE ||
                InetAddress.preferIPv6Address == PREFER_SYSTEM_VALUE) {
                anyLocalAddress = new Inet6Address();
                anyLocalAddress.holder().hostName = "::";
            } else {
                anyLocalAddress = (new Inet4AddressImpl()).anyLocalAddress();
            }
        }
        return anyLocalAddress;
    }

    public synchronized InetAddress loopbackAddress() {
        if (loopbackAddress == null) {
            boolean preferIPv6Address =
                InetAddress.preferIPv6Address == PREFER_IPV6_VALUE ||
                InetAddress.preferIPv6Address == PREFER_SYSTEM_VALUE;

            for (int i = 0; i < 2; i++) {
                InetAddress address;
                // Order the candidate addresses by preference.
                if (i == (preferIPv6Address ? 0 : 1)) {
                    address = new Inet6Address("localhost",
                            new byte[]{ 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01});
                } else {
                    address = new Inet4Address("localhost", new byte[]{ 0x7f,0x00,0x00,0x01 });
                }
                if (i == 0) {
                    // In case of failure, default to the preferred address.
                    loopbackAddress = address;
                }
                try {
                    if (!NetworkInterface.isBoundInetAddress(address)) {
                        continue;
                    }
                } catch (SocketException e) {
                    continue;
                }
                loopbackAddress = address;
                break;
            }
        }
        return loopbackAddress;
    }

    private InetAddress anyLocalAddress;
    private InetAddress loopbackAddress;
}
