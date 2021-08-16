/*
 * Copyright (c) 1997, 2001, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.*;

//
// Base connection handler class -- server and client roles are almost
// identical, this class holds everything except what's different.
//
abstract class Handler extends TestThread
    implements HandshakeCompletedListener
{
    protected SSLSocket         s;
    protected boolean           roleIsClient;

    // generates the stream of test data
    private Traffic             traffic;

    // for optional use in renegotiation
    private String              renegotiateSuites [];

    // Test flag:  did we pass this test?
    private boolean             pass = false;


    Handler (String name)
    {
        super (name);
    }


    public void setRenegotiateSuites (String suites [])
        { renegotiateSuites = suites; }


    abstract public void setReverseRole (boolean flag);


    // XXX override setVerbosity() and pass that to
    // the traffic generation module


    public void run ()
    {
        try {
            traffic = new Traffic (s.getInputStream (), s.getOutputStream ());
        } catch (IOException e) {
            e.printStackTrace ();
            return;
        }

        if (prng != null)
            traffic.setPRNG (prng);

        if (listenHandshake || doRenegotiate)
            s.addHandshakeCompletedListener (this);

        try {
            if (initiateHandshake)
                s.startHandshake ();

            // XXX if use client auth ...

            doTraffic (0);

            if (doRenegotiate)
                s.startHandshake ();

            doTraffic (iterations);

            // XXX abortive shutdown should be a test option

            s.close ();

            // XXX want a close-this-session-down option

        } catch (IOException e) {
            String      message = e.getMessage ();

            synchronized (out) {
                if (message.equalsIgnoreCase ("no cipher suites in common")) {
                    out.println ("%% " + getName () + " " + message);

                } else {
                    out.println ("%% " + getName ());
                    e.printStackTrace (out);
                }
            }

        } catch (Throwable t) {
            synchronized (out) {
                out.println ("%% " + getName ());
                t.printStackTrace (out);
            }
        }
    }


    public boolean passed ()
        { return pass; }


    private void doTraffic (int n)
    throws IOException
    {
        try {
            if (roleIsClient)
                traffic.initiate (n);
            else
                traffic.respond (n);

            pass = true;

        } catch (SSLException e) {
            String      m = e.getMessage ();

            //
            // As of this writing, self-signed certs won't be accepted
            // by the simple trust decider.  That rules out testing all
            // of the SSL_DHE_DSS_* flavors for now, and for testers
            // that don't have a Verisign cert, it also rules out testing
            // SSL_RSA_* flavors.
            //
            // XXX need two things to fix this "right":  (a) ability to
            // let the 'simple trust decider import arbitrary certs, as
            // exported by a keystore; (b) specialized exceptions, since
            // comparing message strings is bogus.
            //
            if (m.equalsIgnoreCase ("untrusted server cert chain")
                    || m.equalsIgnoreCase (
                        "Received fatal alert: certificate_unknown")) {
                System.out.println ("%% " + Thread.currentThread ().getName ()
                    + ", " + m);
                s.close ();
            } else
                throw e;

        } catch (SocketException e) {
            String      m = e.getMessage ();

            if (m.equalsIgnoreCase ("Socket closed"))
                System.out.println ("%% " + Thread.currentThread ().getName ()
                    + ", " + m);
            else
                throw e;

        } catch (EOFException e) {
            // ignore
        }
    }


    public void handshakeCompleted (HandshakeCompletedEvent event)
    {
        if (verbosity >= 1) {
            Socket      sock = (Socket) event.getSource ();

            out.println ("%% " + getName ()
                + ", port " + sock.getLocalPort ()
                + " to " + sock.getInetAddress ().getHostName ()
                + ":" + sock.getPort ()
                + ", " + event.getCipherSuite ());

            // if more verbosity, give cert chain
        }
    }
}
