/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Principal;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

public final class TestJMXAuthenticator implements JMXAuthenticator {

    private String protocol = "";
    private MBeanServer mbs = null;

    public TestJMXAuthenticator() {
    }

    public TestJMXAuthenticator(String protocol) {
        this.protocol = protocol;
    }

    public TestJMXAuthenticator(String protocol, MBeanServer mbs) {
        this.protocol = protocol;
        this.mbs = mbs;
    }

    public Subject authenticate(Object credentials) {

        String credentials_username = "";
        String credentials_password = "";
        Principal aPrincipal = null;

        credentials_username = ((String[]) credentials)[0];
        credentials_password = ((String[]) credentials)[1];

        String authenticated_username = System.getProperty("susername");
        String authenticated_password = System.getProperty("spassword");
        String principal = System.getProperty("principal");

        System.out.println("TestJMXAuthenticator::authenticate: Start");
        System.out.println("TestJMXAuthenticator::authenticate: credentials username = " +
                credentials_username);
        System.out.println("TestJMXAuthenticator::authenticate: credentials password = " +
                credentials_password);
        System.out.println("TestJMXAuthenticator::authenticate: authenticated username = " +
                authenticated_username);
        System.out.println("TestJMXAuthenticator::authenticate: authenticated password = " +
                authenticated_password);
        System.out.println("TestJMXAuthenticator::authenticate: principal used for " +
                "authorization = " + principal);

        if (credentials_username.equals(authenticated_username) &&
                credentials_password.equals(authenticated_password)) {
            System.out.println("TestJMXAuthenticator::authenticate: " +
                    "Authenticator should succeed");
        } else {
            System.out.println("TestJMXAuthenticator::authenticate: " +
                    "Authenticator should reject");
            throw new SecurityException("TestJMXAuthenticator throws EXCEPTION");
        }

        // At this point, authentication has succeeded
        // (no SecurityException thrown).
        //
        // If no authorization is required, the returned subject (empty or not)
        // is useless.
        // Otherwise, the returned subject must define a principal
        // and authorization will be performed against this principal.
        //
        // Note that this custom JMXAuthenticator is used for test purpose and
        // the username used to perform authentication may be different from the
        // username used to perform authorization.
        //
        Subject subject = new Subject();

        if (principal != null) {
            System.out.println("TestJMXAuthenticator::authenticate: " +
                    "Add " + principal + " principal to the returned subject");
            subject.getPrincipals().add(new JMXPrincipal(principal));
        }

        return subject;
    }
}
