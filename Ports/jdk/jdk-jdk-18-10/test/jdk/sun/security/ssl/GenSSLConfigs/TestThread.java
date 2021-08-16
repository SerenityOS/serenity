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

import java.io.PrintStream;
import java.security.SecureRandom;

//
// This holds all the configuration data that's shared between
// threads that are associated with passive and active SSL sockets.
//
// Passive sockets are SSLServer sockets, which produce active ones
// (SSLSockets) that inherit attributes specified here.
//
// Active sockets are associated with a client or server side handler.
// Those are almost identical from the application perspective.
//
class TestThread extends Thread
{
    protected String            basicCipherSuites [];
    protected SecureRandom      prng;
    protected int               iterations = -1;

    // basic test flags
    protected boolean           doRenegotiate;
    protected boolean           initiateHandshake;
    protected boolean           listenHandshake;
    protected boolean           reverseRole;

    // how much output to have, where
    protected int               verbosity = 0;
    protected PrintStream       out = System.out;

    TestThread (String s)
        { super (s); }


    //
    // Defines the cipher suites that'll be used in initial
    // handshaking
    //
    public void setBasicCipherSuites (String suites [])
        { basicCipherSuites = suites; }

    //
    // Says whether to register a callback on handshake
    // completeion.
    //
    public void setListenHandshake (boolean flag)
        { listenHandshake = flag; }

    //
    // Says whether to renegotiate after sending some
    // initial data.
    //
    public void setDoRenegotiate (boolean flag)
        { doRenegotiate = flag; }

    //
    // Says whether to try initiating handshaking.  It's
    // fine of both client and server do this, or if neither
    // does it; sending data triggers it regardless.
    //
    public void setInitiateHandshake (boolean flag)
        { initiateHandshake = flag; }

    //
    // For half-duplex tests, who sends data first?
    //
    public void setReverseRole (boolean flag)
        { reverseRole = flag; }

    //
    // Where does the diagnostic output go?
    //
    public void setOutput (PrintStream out)
        { this.out = out; }


    //
    // How much output is desired?  2 == noisy-typical, lower is less
    //
    public void setVerbosity (int level)
        { verbosity = level; }

    //
    // How many loops of random data should a given "client" start?
    //
    public void setIterations (int level)
        { iterations = level; }

    //
    // Provide some randomness for use with random data I/O.
    // By default, the "random" data is fully predictable (the
    // data is generated with a fixed seed).  However, both the
    // client and server could agree to use truly random data.
    //
    void setPRNG (SecureRandom prng)
        { this.prng = prng; }
}
