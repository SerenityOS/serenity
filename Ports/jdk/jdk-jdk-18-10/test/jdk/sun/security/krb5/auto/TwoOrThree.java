/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005447 8194486
 * @summary default principal can act as anyone
 * @library /test/lib
 * @compile -XDignore.symbol.file TwoOrThree.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts TwoOrThree first first
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts TwoOrThree first second
 * @run main/othervm -Djdk.net.hosts.file=TestHosts TwoOrThree - first
 * @run main/othervm -Djdk.net.hosts.file=TestHosts TwoOrThree - second
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts TwoOrThree - third
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javax.security.auth.Subject;
import sun.security.jgss.GSSUtil;

/*
 * The JAAS login has two krb5 modules
 *   1. principal is A
 *   2. principal is B
 * A named principal can only accept itself. The default principal can accept
 * either, but not any other service even if the keytab also include its keys.
 */
public class TwoOrThree {

    public static void main(String[] args) throws Exception {

        String server = args[0].equals("-") ? null : args[0];
        String target = args[1];
        OneKDC kdc = new OneKDC(null);
        kdc.addPrincipal("first", "first".toCharArray());
        kdc.addPrincipal("second", "second".toCharArray());
        kdc.addPrincipal("third", "third".toCharArray());
        kdc.writeKtab(OneKDC.KTAB);

        Context c = Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);

        // Using keytabs
        Subject sub4s = new Subject();
        Context.fromUserKtab(sub4s, "first", OneKDC.KTAB, true);
        Context s = Context.fromUserKtab(sub4s, "second", OneKDC.KTAB, true);
        c.startAsClient(target, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(server, GSSUtil.GSS_KRB5_MECH_OID);
        Context.handshake(c, s);

        // Using keys
        sub4s = new Subject();
        Context.fromUserPass(sub4s, "first", "first".toCharArray(), true);
        s = Context.fromUserPass(sub4s, "second", "second".toCharArray(), true);
        c.startAsClient(target, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(server, GSSUtil.GSS_KRB5_MECH_OID);
        Context.handshake(c, s);

        s.dispose();
        c.dispose();
    }
}
