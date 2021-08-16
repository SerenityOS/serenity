/*
 * @test
 * @build TestThread Traffic Handler ServerHandler ServerThread ClientThread
 * @run main/othervm/timeout=140 -Djsse.enableCBCProtection=false main
 * @summary Make sure that different configurations of SSL sockets work
 * @key randomness
 */

/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.security.SecureRandom;
import java.security.KeyStore;
import java.util.Date;
import java.util.Vector;
import java.util.ArrayList;

import javax.net.ssl.*;

public class main
{
    // NOTE:  "prng" doesn't need to be a SecureRandom

    private static final SecureRandom   prng
        = new SecureRandom ();
    private static SSLContext sslContext;

    private static void usage() {
        System.err.println (
            "usage: tests.ssl.main default|random|cipher_suite [nthreads]");
    }

    /**
     * Runs a test ... there are a variety of configurations, and the way
     * they're invoked is subject to change.  This program can support
     * single and multiple process tests, but by default it's set up for
     * single process testing.
     *
     * <P> The first commandline argument identifies a test configuration.
     * Currently identified configurations include "default", "random".
     *
     * <P> The second commandline argument identifies the number of
     * client threads to use.
     */
    public static void main (String argv [])
    {
        String          config;
        int             NTHREADS;

        initContext();
        String          supported [] = sslContext.getSocketFactory()
                            .getSupportedCipherSuites();

        // Strip out any Kerberos Suites for now.
        ArrayList list = new ArrayList(supported.length);
        for (int i = 0; i < supported.length; i++) {
            if (!supported[i].startsWith("TLS_KRB5")) {
                list.add(supported[i]);
            }
        }
        supported = (String [])list.toArray(new String [0]);

        if (argv.length == 2) {
            config = argv [0];
            NTHREADS = Integer.parseInt (argv [1]);
        } else if (argv.length == 1) {
            config = argv [0];
            NTHREADS = 15;
        } else {
            /* temporaraly changed to make it run under jtreg with
             * default configuration, when no input parameters are
             * given
             */
            //usage();
            //return;
            config = "default";
            NTHREADS = supported.length;
        }

        // More options ... port #. different clnt/svr configs,
        // cipher suites, etc.

        ServerThread    server = new ServerThread (0, NTHREADS, sslContext);
        Vector          clients = new Vector (NTHREADS);

        if (!(config.equals("default") || config.equals("random")))
            supported = new String[] {config};

        System.out.println("Supported cipher suites are:");
        for(int i=0; i < supported.length; i++) {
            System.out.println(supported[i]);
        }

        setConfig (server, config, supported);

        // if (OS != Win95)
            server.setUseMT (true);

        server.start ();
        server.waitTillReady ();

        //
        // iterate over all cipher suites
        //
        int             next = 0;
        int             passes = 0;

        if (usesRandom (config))
            next = nextUnsignedRandom ();

        for (int i = 0; i < NTHREADS; i++, next++) {
            ClientThread        client = new ClientThread (server.getServerPort(), sslContext);
            String              cipher [] = new String [1];

            setConfig (client, config, supported);
            next = next % supported.length;
            cipher [0] = supported [next];
            client.setBasicCipherSuites (cipher);

            //
            // Win95 has been observed to choke if you throw many
            // connections at it.  So we make it easy to unthread
            // everything; it can be handy outside Win95 too.
            //
            client.start ();
            if (!server.getUseMT ()) {
                waitForClient (client);
                if (client.passed ())
                    passes++;
            } else
                clients.addElement (client);
        }

        while (!clients.isEmpty ()) {
            ClientThread        client;

            client = (ClientThread) clients.elementAt (0);
            clients.removeElement (client);
            waitForClient (client);
            if (client.passed ())
                passes++;
        }

        System.out.println ("SUMMARY:  threads = " + NTHREADS
            + ", passes = " + passes);
    }


    //
    // Rather than replicating code, a helper function!
    //
    private static void waitForClient (Thread client)
    {
        while (true)
            try {
                client.join ();

                // System.out.println ("Joined:  " + client.getName ());
                break;
            } catch (InterruptedException e) {
                continue;
            }
    }

    private static void initContext()
    {
        try {
            String testRoot = System.getProperty("test.src", ".");
            System.setProperty("javax.net.ssl.trustStore", testRoot
                                + "/../../../../javax/net/ssl/etc/truststore");

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(testRoot
                                + "/../../../../javax/net/ssl/etc/truststore"),
                    "passphrase".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "passphrase".toCharArray());
            TrustManagerFactory tmf =
                TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Throwable t) {
            // oh well; ignore it, the tester presumably intends this
            System.out.println("Failed to read keystore/truststore file... Continuing");
            t.printStackTrace();
        }
    }

    private static int nextUnsignedRandom ()
    {
        int retval = prng.nextInt ();

        if (retval < 0)
            return -retval;
        else
            return retval;
    }


    //
    // Randomness in testing can be good and bad ... covers more
    // territory, but not reproducibly.
    //
    private static boolean usesRandom (String config)
    {
        return config.equalsIgnoreCase ("random");
    }


    private static void setConfig (
        TestThread      test,
        String          config,
        String          supported []
    )
    {
        test.setBasicCipherSuites (supported);
        test.setOutput (System.out);
        test.setVerbosity (3);

        if (test instanceof ClientThread) {
            test.setListenHandshake (true);
            test.setIterations (20);
        }

// XXX role reversals !!!

        //
        // We can establish a reasonable degree of variability
        // on the test data and configs ... expecting that the
        // diagnostics will identify any problems that exist.
        // Client and server must agree on these things.
        //
        // Unless we do this, only the SSL nonces and ephemeral
        // keys will be unpredictable in a given test run.  Those
        // affect only the utmost innards of SSL, details which
        // are not visible to applications.
        //
        if (usesRandom (config)) {
            int rand = nextUnsignedRandom ();

            if (test instanceof ClientThread)
                test.setIterations (rand % 35);

            if ((rand & 0x080) == 0)
                test.setInitiateHandshake (true);
//          if ((rand & 0x040) == 0)
//              test.setDoRenegotiate (true);

            test.setPRNG (new SecureRandom ());
        }
    }
}
