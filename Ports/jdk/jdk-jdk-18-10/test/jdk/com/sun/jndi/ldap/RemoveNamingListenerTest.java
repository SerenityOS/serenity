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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;
import jdk.test.lib.net.URIBuilder;

/**
 * @test
 * @bug 8176192 8241130
 * @summary Incorrect usage of Iterator in Java 8 In com.sun.jndi.ldap.
 * EventSupport.removeNamingListener
 * @modules java.naming
 * @library lib/ /test/lib
 * @run main RemoveNamingListenerTest
 */
public class RemoveNamingListenerTest {

    private static volatile Exception exception;

    public static void main(String args[]) throws Exception {
        // start the LDAP server
        TestLDAPServer server = new TestLDAPServer();
        server.start();

        URI providerURI = URIBuilder.newBuilder()
                .scheme("ldap")
                .loopback()
                .port(server.getPort())
                .path("/o=example")
                .build();

        // Set up environment for creating initial context
        Hashtable<String, Object> env = new Hashtable<>(3);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, providerURI.toString());
        env.put("com.sun.jndi.ldap.connect.timeout", "2000");
        EventContext ctx = null;

        try {
            ctx = (EventContext) (new InitialContext(env).lookup(""));
            String target = "cn=Vyom Tewari";

            // Create listeners
            NamingListener oneListener = new SampleListener();
            NamingListener objListener = new SampleListener();
            NamingListener subListener = new SampleListener();

            // Register listeners using different scopes
            ctx.addNamingListener(target, EventContext.ONELEVEL_SCOPE, oneListener);
            ctx.addNamingListener(target, EventContext.OBJECT_SCOPE, objListener);
            ctx.addNamingListener(target, EventContext.SUBTREE_SCOPE, subListener);

            //remove a listener in different thread
            Thread t = new Thread(new RemoveNamingListener(ctx, subListener));
            t.start();
            t.join();

            if (exception != null) {
                throw exception;
            }
            System.out.println("Test run OK!!!");
        } finally {
            if (ctx != null) {
                ctx.close();
            }
            server.close();
        }
    }

    /**
     * Helper thread that removes the naming listener.
     */
    static class RemoveNamingListener implements Runnable {

        final EventContext ctx;
        final NamingListener listener;

        RemoveNamingListener(EventContext ctx, NamingListener listener) {
            this.ctx = ctx;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                ctx.removeNamingListener(listener);
            } catch (NamingException | ConcurrentModificationException ex) {
                exception = ex;
            }
        }
    }

    static class SampleListener implements ObjectChangeListener {

        @Override
        public void objectChanged(NamingEvent ne) {
            //do nothing
        }

        @Override
        public void namingExceptionThrown(NamingExceptionEvent nee) {
            //do nothing
        }
    }
}

class TestLDAPServer extends BaseLdapServer {

    private byte[] bindResponse = {0x30, 0x0C, 0x02, 0x01, 0x01, 0x61, 0x07, 0x0A, 0x01, 0x00, 0x04, 0x00, 0x04, 0x00};
    private byte[] searchResponse = {0x30, 0x0C, 0x02, 0x01, 0x02, 0x65, 0x07, 0x0A, 0x01, 0x00, 0x04, 0x00, 0x04, 0x00};

    public TestLDAPServer() throws IOException {
    }

    @Override
    protected void handleRequest(Socket socket, LdapMessage request,
            OutputStream out) throws IOException {
        switch (request.getOperation()) {
            case BIND_REQUEST:
                // Write an LDAP BindResponse
                out.write(bindResponse);
                break;
            case SEARCH_REQUEST:
                // Write an LDAP SearchResponse
                out.write(searchResponse);
                break;
            default:
                break;
        }
    }
}
