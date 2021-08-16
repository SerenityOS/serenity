/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4671598
 * @summary PermissionCollection is not properly synchronized
 */

import java.security.*; // AllPermission, BasicPermission, Permissions
import java.net.NetPermission;
import java.net.SocketPermission;
import java.io.FilePermission;
import java.util.PropertyPermission;
import javax.security.auth.AuthPermission;
import javax.security.auth.kerberos.DelegationPermission;
import javax.security.auth.kerberos.ServicePermission;
import javax.management.MBeanServerPermission;

import java.util.*;

public class Concurrent {
    private static final int LIMIT1 = 2000;
    private static final int LIMIT2 = 1000;
    private static final boolean debug = false;
    private static final Map errors =
        Collections.synchronizedMap(new HashMap());

    public static void main(String args[]) throws Exception {
        testPc(allp);
        testPc(filep);
        testPc(sockp);
        testPc(propp);
        testPc(basicp);
        testPc(delegatep);
        testPc(servicep);
        testPc(mbeanp);
        testPc(unresp);

        testPerms();

        if (errors.size() > 0) {
            if (true) {
                Iterator iter = errors.entrySet().iterator();
                while (iter.hasNext()) {
                    System.out.println(iter.next());
                }
            };
            throw (Exception) new Exception("Got errors");
        }
    }

    private static void testPc (final Permission[] perm) throws Exception {

        final PermissionCollection pc = perm[0].newPermissionCollection();

        new Thread() {
            {
                setDaemon(true);
                start();
            }
            public void run() {
                try {
                    for (int i = 0; i < LIMIT1; i++) {
                        for (int j = 0; j < perm.length; j++) {
                            pc.add(perm[j]);
                            if (debug) {
                                System.out.println("added " + perm[j]);
                            }
                        }
                    }
                } catch (Exception e) {
                    errors.put(perm[0].getClass().getName(), e);
                }
            }
        };
        try {
            for (int i = 0; i < LIMIT2; i++) {
                boolean result = pc.implies(perm[perm.length-1]);
                if (debug) {
                    System.out.println(perm[perm.length-1] + " implies " + result);
                }

                Enumeration en = pc.elements();
                while (en.hasMoreElements()) {
                    Object obj = en.nextElement();
                    if (debug) {
                        System.out.println(obj);
                    }
                }
            }
        } catch (Exception e) {
            errors.put(perm[0].getClass().getName(), e);
        }
    }

    private static void testPerms () throws Exception {

        final Permissions pc = new Permissions();

        new Thread() {
            {
                setDaemon(true);
                start();
            }
            public void run() {
                try {
                    for (int i = 0; i < LIMIT1; i++) {
                        for (int j = 0; j < permlist.length; j++) {
                            for (int k = 0; k < permlist[j].length; k++) {
                                pc.add(permlist[j][k]);
                            }
                        }
                    }
                } catch (Exception e) {
                    errors.put("java.security.Permissions", e);
                }
            }
        };
        try {
            for (int i = 0; i < LIMIT2; i++) {
                for (int j = 0; j < permlist.length; j++) {
                    boolean result = pc.implies(permlist[j][0]);
                    if (debug) {
                        System.out.println(permlist[j][0] + " implies " + result);
                    }
                }

                Enumeration en = pc.elements();
                while (en.hasMoreElements()) {
                    Object obj = en.nextElement();
                    if (debug) {
                        System.out.println(obj);
                    }
                }
            }
        } catch (Exception e) {
            errors.put("java.security.Permissions", e);
        }
    }

    private static final Permission[] allp = new Permission[]{
        new AllPermission(), new AllPermission()};

    private static final Permission[] filep = new Permission[]{
        new FilePermission("/tmp/foobar", "read"),
        new FilePermission("/tmp/foo", "write"),
        new FilePermission("/tmp/foobar", "read,write"),
            };

    private static final Permission[] sockp = new Permission[]{
        new SocketPermission("example.net", "connect"),
            new SocketPermission("www.sun.com", "resolve"),
            new SocketPermission("www.test1.com", "accept"),
            new SocketPermission("www.test3.com", "resolve,connect"),
            new SocketPermission("www.test4.com", "listen"),
            };

    private static final Permission[] propp = new Permission[]{
        new PropertyPermission("user.home", "read"),
            new PropertyPermission("java.home", "write"),
            new PropertyPermission("test.home", "write"),
            new PropertyPermission("test1.home", "read"),
            new PropertyPermission("test2.home", "read"),
            };

    private static final Permission[] basicp = new Permission[] {
        new NetPermission("setDefaultAuthenticator"),
            new NetPermission("requestPasswordAuthentication"),
            new NetPermission("specifyStreamHandler")
            };

    private static final Permission[] delegatep = new Permission[] {
        new DelegationPermission(
            "\"host/foo.example.com@EXAMPLE.COM\" \"cn=John,o=imc,c=us\""),
        new DelegationPermission(
            "\"user/rosanna@EXAMPLE.COM\" \"cn=John,o=imc,c=us\""),
        new DelegationPermission(
            "\"host/bar.example.com@EXAMPLE.COM\" \"cn=John,o=imc,c=us\"")
            };

    private static final Permission[] servicep = new Permission[]{
        new ServicePermission("krbtgt/EXAMPLE.COM@EXAMPLE.COM", "initiate"),
        new ServicePermission("ldap/EXAMPLE.COM@EXAMPLE.COM", "initiate"),
        new ServicePermission("imap/EXAMPLE.COM@EXAMPLE.COM", "accept"),
        new ServicePermission("acap/EXAMPLE.COM@EXAMPLE.COM", "initiate"),
        new ServicePermission("host/EXAMPLE.COM@EXAMPLE.COM", "initiate"),
            };

    private static final Permission[] mbeanp = new Permission[] {
        new MBeanServerPermission("createMBeanServer"),
        new MBeanServerPermission("findMBeanServer"),
        new MBeanServerPermission("newMBeanServer"),
        new MBeanServerPermission("releaseMBeanServer"),
            };

    private static final Permission[] unresp = new Permission[] {
        new UnresolvedPermission("com.unknown.TestClass", "UnknownPermission",
            "read,write", null),
        new UnresolvedPermission("com.unknown.TestClass", "APermission",
            "read,write", null),
        new UnresolvedPermission("com.unknown.TestClass", "BPermission",
            "read,write", null),
        new UnresolvedPermission("com.unknown.CClass", "CPermission",
            "read,write", null),
        new UnresolvedPermission("com.unknown.DClass", "DUnknownPermission",
            "read,write", null),
        new UnresolvedPermission("com.unknown.EClass", "EUnknownPermission",
            "read,write", null),
            };

    private static final Permission[][] permlist = new Permission[][]{
        allp, filep, sockp, propp, basicp, delegatep, servicep, mbeanp, unresp};
}
