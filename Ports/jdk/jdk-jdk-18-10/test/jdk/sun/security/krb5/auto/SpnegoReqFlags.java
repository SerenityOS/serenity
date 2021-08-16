/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6815182 8194486
 * @summary GSSAPI/SPNEGO does not work with server using MIT Kerberos library
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts SpnegoReqFlags
 */

import sun.security.jgss.GSSUtil;
import sun.security.util.BitArray;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

public class SpnegoReqFlags {

    public static void main(String[] args)
            throws Exception {

        // Create and start the KDC
        new OneKDC(null).writeJAASConf();
        new SpnegoReqFlags().go();
    }

    void go() throws Exception {
        Context c = Context.fromJAAS("client");
        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_SPNEGO_MECH_OID);

        byte[] token = c.doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] input) throws Exception {
                me.x().requestCredDeleg(true);
                me.x().requestReplayDet(false);
                me.x().requestSequenceDet(false);
                return me.x().initSecContext(new byte[0], 0, 0);
            }
        }, null);

        DerValue d = new DerValue(token);   // GSSToken
        DerInputStream ins = d.data;        // OID + mech token
        d.data.getDerValue();               // skip OID
        d = d.data.getDerValue();           // NegTokenInit
        d = d.data.getDerValue();           // The SEQUENCE inside

        boolean found = false;

        // Go through all fields inside NegTokenInit. The reqFlags field
        // is optional. It's even not recommended in RFC 4178.
        while (d.data.available() > 0) {
            DerValue d2 = d.data.getDerValue();
            if (d2.isContextSpecific((byte)1)) {
                found = true;
                System.out.println("regFlags field located.");
                BitArray ba = d2.data.getUnalignedBitString();
                if (ba.length() != 7) {
                    throw new Exception("reqFlags should contain 7 bits");
                }
                if (!ba.get(0)) {
                    throw new Exception("delegFlag should be true");
                }
                if (ba.get(2) || ba.get(3)) {
                    throw new Exception("replay/sequenceFlag should be false");
                }
            }
        }

        if (!found) {
            System.out.println("Warning: regFlags field not found, too new?");
        }
        c.dispose();
    }
}
