/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8078439 8194486
 * @summary SPNEGO auth fails if client proposes MS krb5 OID
 * @library /test/lib
 * @compile -XDignore.symbol.file MSOID2.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts MSOID2
 */

import sun.security.jgss.GSSUtil;
import jdk.test.lib.hexdump.HexPrinter;

// The basic krb5 test skeleton you can copy from
public class MSOID2 {

    public static void main(String[] args) throws Exception {

        new OneKDC(null).writeJAASConf();

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_SPNEGO_MECH_OID);
        s.startAsServer(GSSUtil.GSS_SPNEGO_MECH_OID);

        byte[] t = new byte[0];
        boolean first = true;
        while (true) {
            if (t != null || !c.x().isEstablished()) t = c.take(t);
            if (first) {
                // Tweak the packet to append an extra OID
                int len = t.length;
                byte[] nt = new byte[len + 11];
                System.arraycopy(t, 0, nt, 0, 0x23);
                System.arraycopy(t, 0x18, nt, 0x23, 11);    // dup the OID
                System.arraycopy(t, 0x23, nt, 0x2e, len-0x23);
                nt[0x1d] = (byte)0x82;  // change the 1st to MS OID
                // Length bytes to be tweaked
                for (int pos: new int[] {3, 0xf, 0x13, 0x15, 0x17}) {
                    int newLen = (nt[pos]&0xff) + 11;
                    // The length byte at nt[pos] might overflow. It's
                    // unlikely for nt[pos-1] to overflow, which means the size
                    // of token is bigger than 65535.
                    if (newLen >= 256) {
                        nt[pos-1] = (byte)(nt[pos-1] + 1);
                    }
                    nt[pos] = (byte)newLen;
                }
                t = nt;
                HexPrinter.simple().format(t);
            }
            if (t != null || !s.x().isEstablished()) t = s.take(t);
            if (c.x().isEstablished() && s.x().isEstablished()) break;
            first = false;
        }

        Context.transmit("i say high --", c, s);
        Context.transmit("   you say low", s, c);

        s.dispose();
        c.dispose();
    }
}
