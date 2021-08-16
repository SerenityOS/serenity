/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7032354
 * @library /test/lib
 * @run main/othervm NoAddresses setup
 * @run main/othervm -Djdk.net.hosts.file=TestHosts NoAddresses 1
 * @run main/othervm -Djdk.net.hosts.file=TestHosts NoAddresses 2
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts NoAddresses 3
 * @summary no-addresses should not be used on acceptor side
 */

import java.net.InetAddress;
import org.ietf.jgss.ChannelBinding;
import sun.security.jgss.GSSUtil;
import sun.security.krb5.Config;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.nio.file.*;

public class NoAddresses {

    public static void main(String[] args)
            throws Exception {

        if (args[0].equalsIgnoreCase("setup")) {
            // add a mapping of test host name to 127.0.0.1 to test's hosts file
            InetAddress localHost = InetAddress.getLocalHost();
            String localHostName = localHost.getHostName();
            String hostsFileName = System.getProperty("test.src", ".") + "/TestHosts";
            String hostsFileNameLocal = "TestHosts";
            String loopBackAddress = "127.0.0.1";
            Files.copy(Paths.get(hostsFileName), Paths.get(hostsFileNameLocal));
            addMappingToHostsFile(localHostName, loopBackAddress, hostsFileNameLocal, true);
        } else {
        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();
        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                "noaddresses = false",
                "default_keytab_name = " + OneKDC.KTAB);
        Config.refresh();

        Context c = Context.fromJAAS("client");
        Context s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        InetAddress initiator = InetAddress.getLocalHost();
        InetAddress acceptor = InetAddress.getLocalHost();
        switch (args[0]) {
            case "1":
                // no initiator host address available, should be OK
                break;
            case "2":
                // correct initiator host address, still fine
                c.x().setChannelBinding(
                        new ChannelBinding(initiator, acceptor, null));
                s.x().setChannelBinding(
                        new ChannelBinding(initiator, acceptor, null));
                break;
            case "3":
                // incorrect initiator host address, fail
                initiator = InetAddress.getByAddress(new byte[]{1,1,1,1});
                c.x().setChannelBinding(
                        new ChannelBinding(initiator, acceptor, null));
                s.x().setChannelBinding(
                        new ChannelBinding(initiator, acceptor, null));
                break;
        }

        Context.handshake(c, s);
    }
}

    private static void addMappingToHostsFile (String host,
                                               String addr,
                                               String hostsFileName,
                                               boolean append)
                                             throws Exception {
        String mapping = addr + " " + host;
        try (PrintWriter hfPWriter = new PrintWriter(new BufferedWriter(
                new FileWriter(hostsFileName, append)))) {
            hfPWriter.println(mapping);
        }
    }
}
