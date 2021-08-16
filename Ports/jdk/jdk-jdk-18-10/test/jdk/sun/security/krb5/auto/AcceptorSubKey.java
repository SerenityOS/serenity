/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7077646 8194486
 * @summary gssapi wrap for CFX per-message tokens always set FLAG_ACCEPTOR_SUBKEY
 * @library /test/lib
 * @compile -XDignore.symbol.file AcceptorSubKey.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts AcceptorSubKey 0
 * @run main/othervm -Djdk.net.hosts.file=TestHosts AcceptorSubKey 4
 */

import sun.security.jgss.GSSUtil;

// The basic krb5 test skeleton you can copy from
public class AcceptorSubKey {

    public static void main(String[] args) throws Exception {

        int expected = Integer.parseInt(args[0]);

        new OneKDC(null).writeJAASConf();

        if (expected != 0) {
            System.setProperty("sun.security.krb5.acceptor.subkey", "true");
        }

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_SPNEGO_MECH_OID);
        s.startAsServer(GSSUtil.GSS_SPNEGO_MECH_OID);

        Context.handshake(c, s);

        byte[] msg = "i say high --".getBytes();
        byte[] wrapped = s.wrap(msg, false);

        // FLAG_ACCEPTOR_SUBKEY is 4
        int flagOn = wrapped[2] & 4;
        if (flagOn != expected) {
            throw new Exception("not expected");
        }

        s.dispose();
        c.dispose();
    }
}
