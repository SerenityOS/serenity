/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4531817 8026245
 * @library /test/lib
 * @summary Inet[46]Address.localHost need doPrivileged
 * @run main/othervm -Djava.security.manager=allow GetLocalHostWithSM
 * @run main/othervm -Djava.security.manager=allow -Djava.net.preferIPv4Stack=true GetLocalHostWithSM
 * @run main/othervm -Djava.security.manager=allow -Djava.net.preferIPv6Addresses=true GetLocalHostWithSM
 * files needed: GetLocalHostWithSM.java, MyPrincipal.java, and policy.file
 */

import java.net.*;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import jdk.test.lib.net.IPSupport;

public class GetLocalHostWithSM {

        public static void main(String[] args) throws Exception {
            IPSupport.throwSkippedExceptionIfNonOperational();

            // try setting the local hostname
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost.isLoopbackAddress()) {
                System.err.println("Local host name is resolved into a loopback address. Quit now!");
                return;
            }
            System.setProperty("host.name", localHost.
                                            getHostName());
            String policyFileName = System.getProperty("test.src", ".") +
                          "/" + "policy.file";
            System.setProperty("java.security.policy", policyFileName);
            System.setSecurityManager(new SecurityManager());

            InetAddress localHost1 = null;
            InetAddress localHost2 = null;

            localHost1 = InetAddress.getLocalHost();

            Subject mySubject = new Subject();
            MyPrincipal userPrincipal = new MyPrincipal("test");
            mySubject.getPrincipals().add(userPrincipal);
            localHost2 = (InetAddress)Subject.doAsPrivileged(mySubject,
                                new MyAction(), null);

            if (localHost1.equals(localHost2)) {
                System.out.println("localHost1 = " + localHost1);
                throw new RuntimeException("InetAddress.getLocalHost() test " +
                                           " fails. localHost2 should be " +
                                           " the real address instead of " +
                                           " the loopback address."+localHost2);
            }
        }
}


class MyAction implements PrivilegedExceptionAction {

    public Object run() throws Exception {
        return InetAddress.getLocalHost();
    }
}
