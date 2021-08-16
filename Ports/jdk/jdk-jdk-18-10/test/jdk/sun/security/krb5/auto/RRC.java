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
 * @bug 7077640 8194486
 * @summary gss wrap for cfx doesn't handle rrc != 0
 * @library /test/lib
 * @compile -XDignore.symbol.file RRC.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts RRC
 */

import java.util.Arrays;
import sun.security.jgss.GSSUtil;

// The basic krb5 test skeleton you can copy from
public class RRC {

    public static void main(String[] args) throws Exception {

        new OneKDC(null).writeJAASConf();

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_SPNEGO_MECH_OID);
        s.startAsServer(GSSUtil.GSS_SPNEGO_MECH_OID);

        Context.handshake(c, s);

        byte[] msg = "i say high --".getBytes();
        byte[] wrapped = c.wrap(msg, false);

        // Simulate RRC equals to EC
        int rrc = wrapped[5];
        byte[] rotated = new byte[wrapped.length];
        System.arraycopy(wrapped, 0, rotated, 0, 16);
        System.arraycopy(wrapped, wrapped.length-rrc, rotated, 16, rrc);
        System.arraycopy(wrapped, 16, rotated, 16+rrc, wrapped.length-16-rrc);
        rotated[7] = (byte)rrc;

        byte[] unwrapped = s.unwrap(rotated, false);
        if (!Arrays.equals(msg, unwrapped)) {
            throw new Exception("Failure");
        }

        s.dispose();
        c.dispose();
    }
}
