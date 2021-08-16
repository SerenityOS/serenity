/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004675
 * @summary  Inet6Address.getHostAddress should use string scope
 *           identifier where available
 */

import java.net.*;
import java.util.Enumeration;

public class StringScope {

    public static void main(String args[]) throws Exception {
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface iface = e.nextElement();
            Enumeration<InetAddress> iadrs = iface.getInetAddresses();
            while (iadrs.hasMoreElements()) {
                InetAddress iadr = iadrs.nextElement();
                if (iadr instanceof Inet6Address) {
                    Inet6Address i6adr = (Inet6Address) iadr;
                    NetworkInterface nif = i6adr.getScopedInterface();
                    if (nif == null)
                        continue;

                    String nifName = nif.getName();
                    String i6adrHostAddress = i6adr.getHostAddress();
                    int index = i6adrHostAddress.indexOf('%');
                    String i6adrScopeName = i6adrHostAddress.substring(index+1);

                    if (!nifName.equals(i6adrScopeName))
                        throw new RuntimeException("Expected nifName ["
                                      + nifName + "], to equal i6adrScopeName ["
                                      + i6adrScopeName + "] ");
                }
            }
        }
    }
}

