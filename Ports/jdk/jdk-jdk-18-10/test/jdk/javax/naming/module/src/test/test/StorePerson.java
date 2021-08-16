/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Demonstrate Java object storage and retrieval using an LDAP directory.
 * The Person object and its associated object and state factory is supplied by
 * a third-party module. As the Person object does not implement
 * javax.naming.Referenceable, the classname of its state and object factory
 * must be specified to the JNDI initial context.
 */

package test;

import java.io.PrintStream;
import java.net.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

import org.example.person.Person;

public class StorePerson {

    static {
        final PrintStream out = new PrintStream(System.out, true);
        final PrintStream err = new PrintStream(System.err, true);

        System.setOut(out);
        System.setErr(err);
    }

    // LDAP capture file
    private static final String LDAP_CAPTURE_FILE =
        System.getProperty("test.src") + "/src/test/test/StorePerson.ldap";

    public static void main(String[] args) throws Exception {

        /*
         * Process arguments
         */

        int argc = args.length;
        if ((argc < 1) ||
            ((argc == 1) && (args[0].equalsIgnoreCase("-help")))) {

            System.err.println("\nUsage:   StorePerson <ldapurl>\n");
            System.err.println("        <ldapurl> is the LDAP URL of the parent entry\n");
            System.err.println("example:");
            System.err.println("        java StorePerson ldap://oasis/o=airius.com");
            return;
        }

        /*
         * Launch the LDAP server with the StorePerson.ldap capture file
         */

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new LDAPServer(serverSocket, LDAP_CAPTURE_FILE);
                    } catch (Exception e) {
                        System.out.println("ERROR: unable to launch LDAP server");
                        e.printStackTrace();
                    }
                }
            }).start();

            /*
             * Store Person objects in the LDAP directory
             */

            Hashtable<String,Object> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");
            URI ldapUri = new URI(args[0]);
            if (ldapUri.getPort() == -1) {
                ldapUri = new URI(ldapUri.getScheme(), null, ldapUri.getHost(),
                        serverSocket.getLocalPort(), ldapUri.getPath(), null, null);
            }
            env.put(Context.PROVIDER_URL, ldapUri.toString());
            if (args[args.length - 1].equalsIgnoreCase("-trace")) {
                env.put("com.sun.jndi.ldap.trace.ber", System.out);
            }

            // Specify the factory classname explicitly
            env.put(Context.STATE_FACTORIES, "org.example.person.PersonFactory");
            env.put(Context.OBJECT_FACTORIES, "org.example.person.PersonFactory");

            System.out.println("StorePerson: connecting to " + ldapUri);
            DirContext ctx = new InitialDirContext(env);
            Person person = null;
            String name = "John Smith";
            String dn = "cn=" + name;

            try {
                person = new Person(name, "Smith");
                person.setMailAddress("jsmith@smith.com");
                ctx.bind(dn, person);
                System.out.println("StorePerson: created entry '" + dn + "'");
            } catch (NameAlreadyBoundException e) {
                System.err.println("StorePerson: entry '" + dn +
                        "' already exists");
                cleanup(ctx, (String)null);
                return;
            }

            name = "Jill Smyth";
            String dn2 = "cn=" + name;
            Person person2 = new Person(name, "Smyth");
            person2.setMailAddress("jsmyth@smith.com");

            try {
                ctx.bind(dn2, person2);
                System.out.println("StorePerson: created entry '" + dn2 + "'");
            } catch (NameAlreadyBoundException e) {
                System.err.println("StorePerson: entry '" + dn2 +
                        "' already exists");
                cleanup(ctx, dn);
                return;
            }

            /*
             * Retrieve Person objects from the LDAP directory
             */

            try {
                Person person3 = (Person) ctx.lookup(dn);
                System.out.println("StorePerson: retrieved object: " + person3);
                if (person.getAttributes().equals(person3.getAttributes())) {
                    System.out.println(
                            "StorePerson: retrieved person matches original");
                } else {
                    System.out.println(
                            "StorePerson: retrieved person does NOT match original");
                }
            } catch (NamingException e) {
                System.err.println("StorePerson: error retrieving entry '" +
                        dn + "' " + e);
                e.printStackTrace();
                cleanup(ctx, dn, dn2);
                return;
            }

            try {
                Person person4 = (Person) ctx.lookup(dn2);
                System.out.println("StorePerson: retrieved object: " + person4);
                if (person2.getAttributes().equals(person4.getAttributes())) {
                    System.out.println(
                            "StorePerson: retrieved person matches original");
                } else {
                    System.out.println(
                            "StorePerson: retrieved person does NOT match original");
                }
            } catch (NamingException e) {
                System.err.println("StorePerson: error retrieving entry '" +
                        dn2 + "' " + e);
                e.printStackTrace();
                cleanup(ctx, dn, dn2);
                return;
            }

            cleanup(ctx, dn, dn2);
        }
    }

    /*
     * Remove objects from the LDAP directory
     */
    private static void cleanup(DirContext ctx, String... dns)
        throws NamingException {

        for (String dn : dns) {
            try {
                ctx.destroySubcontext(dn);
                System.out.println("StorePerson: removed entry '" + dn + "'");
            } catch (NamingException e) {
                System.err.println("StorePerson: error removing entry '" + dn +
                    "' " + e);
            }
        }
        ctx.close();
    }
}
