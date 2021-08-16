/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4714674
   @summary Tests that JEditorPane opens HTTP connection asynchronously
   @author Peter Zhelezniakov
   @modules java.desktop
            jdk.httpserver
   @run main/othervm bug4714674
*/

import javax.swing.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;


public class bug4714674 {

    public static void main(String[] args) throws Exception {
        new bug4714674().test();
    }

    private void test() throws Exception {
        final DeafServer server = new DeafServer();
        final String baseURL = "http://localhost:" + server.getPort() + "/";

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    JEditorPane pane = new JEditorPane();
                    ((javax.swing.text.AbstractDocument)pane.getDocument()).
                            setAsynchronousLoadPriority(1);

                    // this will block EDT unless 4714674 is fixed
                    pane.setPage(baseURL);
                } catch (IOException e) {
                    // should not happen
                    throw new Error(e);
                }
            }
        });

        // if 4714674 is fixed, this executes before connection times out
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (server) {
                    server.wakeup = true;
                    server.notifyAll();
                }
                pass();
            }
        });

        // wait, then check test status
        try {
            Thread.sleep(5000);
            if (!passed()) {
                throw new RuntimeException("Failed: EDT was blocked");
            }
        } finally {
            server.destroy();
        }
    }

    private boolean passed = false;

    private synchronized boolean passed() {
        return passed;
    }

    private synchronized void pass() {
        passed = true;
    }
}

/**
 * A "deaf" HTTP server that does not respond to requests.
 */
class DeafServer {
    HttpServer server;
    boolean wakeup = false;

    /**
     * Create and start the HTTP server.
     */
    public DeafServer() throws IOException {
        InetSocketAddress addr = new InetSocketAddress(0);
        server = HttpServer.create(addr, 0);
        HttpHandler handler = new DeafHandler();
        server.createContext("/", handler);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    /**
     * Stop server without any delay.
     */
    public void destroy() {
        server.stop(0);
    }

    /**
     * Return actual server port number, useful for constructing request URIs.
     */
    public int getPort() {
        return server.getAddress().getPort();
    }


    class DeafHandler implements HttpHandler {
        public void handle(HttpExchange r) throws IOException {
            synchronized (DeafServer.this) {
                while (! wakeup) {
                    try {
                        DeafServer.this.wait();
                    } catch (InterruptedException e) {
                        // just wait again
                    }
                }
            }
        }
    }
}
