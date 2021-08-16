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

import java.net.*;
import javax.net.ssl.*;

class ClientThread extends Handler
    implements HandshakeCompletedListener
{
    private int         port;
    private InetAddress server;
    private SSLSocketFactory factory;

    static private int  threadCounter = 0;

    private static synchronized int getCounter ()
        { return ++threadCounter; }

    ClientThread (int port, SSLContext ctx)
    {
        super ("Client-" + getCounter ());
        roleIsClient = true;
        factory = ctx.getSocketFactory();

        try {
            this.server = InetAddress.getLocalHost ();
            this.port = port;
        } catch (UnknownHostException e) {
            synchronized (out) {
                out.println ("%% " + getName ());
                e.printStackTrace (out);
            }
        }
    }

    ClientThread (InetAddress server, int port, SSLContext ctx)
    {
        super ("Client-" + getCounter ());
        roleIsClient = true;
        factory = ctx.getSocketFactory();

        this.server = server;
        this.port = port;
    }


    public void setReverseRole (boolean flag)
    {
        if (flag)
            roleIsClient = false;
        else
            roleIsClient = true;
    }

    public void run ()
    {
        try {
            s = (SSLSocket) factory.createSocket(server, port);

        } catch (Throwable t) {
            synchronized (out) {
                out.println ("%% " + getName ());
                t.printStackTrace (out);
            }
            return;
        }

        if (basicCipherSuites != null) {
            s.setEnabledCipherSuites (basicCipherSuites);
            if (basicCipherSuites.length == 1)
                System.out.println ("%% " + getName () + " trying "
                        + basicCipherSuites [0]);
        }

        super.run ();

        out.println ("%% " + getName ()
            + (passed () ? ", Passed!" : " ... FAILED"));
    }

}
