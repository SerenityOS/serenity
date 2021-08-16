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
 * @compile -XDignore.symbol.file AcceptPermissions.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djava.security.manager=allow -Djdk.net.hosts.file=TestHosts AcceptPermissions two
 * @run main/othervm -Djava.security.manager=allow -Djdk.net.hosts.file=TestHosts AcceptPermissions unbound
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Permission;
import javax.security.auth.kerberos.ServicePermission;
import sun.security.jgss.GSSUtil;
import java.util.*;

public class AcceptPermissions extends SecurityManager {

    private static Map<Permission,String> perms = new HashMap<>();
    @Override
    public void checkPermission(Permission perm) {
        if (!(perm instanceof ServicePermission)) {
            return;
        }
        ServicePermission sp = (ServicePermission)perm;
        if (!sp.getActions().equals("accept")) {
            return;
        }
        // We only care about accept ServicePermission in this test
        try {
            super.checkPermission(sp);
        } catch (SecurityException se) {
            if (perms.containsKey(sp)) {
                perms.put(sp, "checked");
            } else {
                throw se;   // We didn't expect this is needed
            }
        }
    }

    // Fills in permissions we are expecting
    private static void initPerms(String... names) {
        perms.clear();
        for (String name: names) {
            perms.put(new ServicePermission(
                    name + "@" + OneKDC.REALM, "accept"), "expected");
        }
    }

    // Checks if they are all checked
    private static void checkPerms() {
        for (Map.Entry<Permission,String> entry: perms.entrySet()) {
            if (entry.getValue().equals("expected")) {
                throw new RuntimeException(
                        "Expected but not used: " + entry.getKey());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.setSecurityManager(new AcceptPermissions());
        new OneKDC(null).writeJAASConf();
        String moreEntries = "two {\n"
                + " com.sun.security.auth.module.Krb5LoginModule required"
                + "     principal=\"" + OneKDC.SERVER + "\" useKeyTab=true"
                + "     isInitiator=false storeKey=true;\n"
                + " com.sun.security.auth.module.Krb5LoginModule required"
                + "     principal=\"" + OneKDC.BACKEND + "\" useKeyTab=true"
                + "     isInitiator=false storeKey=true;\n"
                + "};\n"
                + "unbound {"
                + " com.sun.security.auth.module.Krb5LoginModule required"
                + "     principal=* useKeyTab=true"
                + "     isInitiator=false storeKey=true;\n"
                + "};\n";
        Files.write(Paths.get(OneKDC.JAAS_CONF), moreEntries.getBytes(),
                StandardOpenOption.APPEND);

        Context c, s;

        // In all cases, a ServicePermission on the acceptor name is needed
        // for a handshake. For default principal with no predictable name,
        // permission not needed (yet) for credentials creation.

        // Named principal
        initPerms(OneKDC.SERVER);
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");
        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        checkPerms();
        initPerms(OneKDC.SERVER);
        Context.handshake(c, s);
        checkPerms();

        // Named principal (even if there are 2 JAAS modules)
        initPerms(OneKDC.SERVER);
        c = Context.fromJAAS("client");
        s = Context.fromJAAS(args[0]);
        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        checkPerms();
        initPerms(OneKDC.SERVER);
        Context.handshake(c, s);
        checkPerms();

        // Default principal with a predictable name
        initPerms(OneKDC.SERVER);
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");
        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        checkPerms();
        initPerms(OneKDC.SERVER);
        Context.handshake(c, s);
        checkPerms();

        // Default principal with no predictable name
        initPerms();    // permission not needed for cred !!!
        c = Context.fromJAAS("client");
        s = Context.fromJAAS(args[0]);
        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        checkPerms();
        initPerms(OneKDC.SERVER);   // still needed for handshake !!!
        Context.handshake(c, s);
        checkPerms();
    }
}
