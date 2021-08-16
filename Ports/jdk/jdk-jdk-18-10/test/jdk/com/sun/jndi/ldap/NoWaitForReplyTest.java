/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6748156
 * @summary add an new JNDI property to control the boolean flag WaitForReply
 * @library lib/ /test/lib
 */

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;
import jdk.test.lib.net.URIBuilder;

public class NoWaitForReplyTest {

    public static void main(String[] args) throws Exception {

        boolean passed = false;

        // start the LDAP server
        var ldapServer = new BaseLdapServer().start();

        // Set up the environment for creating the initial context
        Hashtable<Object, Object> env = new Hashtable<>(11);
        env.put(Context.PROVIDER_URL, URIBuilder.newBuilder()
                .scheme("ldap")
                .loopback()
                .port(ldapServer.getPort())
                .build().toString());
        env.put(Context.INITIAL_CONTEXT_FACTORY,
            "com.sun.jndi.ldap.LdapCtxFactory");

        // Wait up to 10 seconds for a response from the LDAP server
        env.put("com.sun.jndi.ldap.read.timeout", "10000");

        // Don't wait until the first search reply is received
        env.put("com.sun.jndi.ldap.search.waitForReply", "false");

        // Send the LDAP search request without first authenticating (no bind)
        env.put("java.naming.ldap.version", "3");


        try (ldapServer) {

            // Create initial context
            System.out.println("Client: connecting to the server");
            DirContext ctx = new InitialDirContext(env);

            SearchControls scl = new SearchControls();
            scl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            System.out.println("Client: performing search");
            NamingEnumeration<SearchResult> answer =
            ctx.search("ou=People,o=JNDITutorial", "(objectClass=*)", scl);

            // Server will never reply: either we waited in the call above until
            // the timeout (fail) or we did not wait and reached here (pass).
            passed = true;
            System.out.println("Client: did not wait until first reply");

            // Close the context when we're done
            ctx.close();

        } catch (NamingException e) {
            // timeout (ignore)
        }

        if (!passed) {
            throw new Exception(
                "Test FAILED: should not have waited until first search reply");
        }
        System.out.println("Test PASSED");
    }
}
