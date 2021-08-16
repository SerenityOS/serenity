/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jpda;

import nsk.share.*;
import nsk.share.jdi.Binder;

/**
 * This class implements communicational channel between
 * debugger and debugee used for synchronization and data exchange.
 * This channel is based on TCP/IP sockets and works in all
 * modes (local, remote and manual). In a remote mode
 * connection to <code>BindServer</code> is used for redirecting IOPipe messages.
 * In all other modes direct TCP/IP coonnection between two VMs is used.
 *
 * @see BindServer
 */
public class IOPipe extends SocketIOPipe {

    public static final byte PORTS_COUNT = 10;
    public static final byte NO_PORTS = 0;

    public static final String PIPE_LOG_PREFIX = "IOPipe> ";

    /**
      * Make <code>IOPipe</code> at debugee's side.
      *
      * @deprecated Use DebugeeArgumentHandler.createDebugeeIOPipe(Log) instead.
      *
      * @see DebugeeArgumentHandler#createDebugeeIOPipe(Log)
      */
    @Deprecated
    public IOPipe(DebugeeArgumentHandler argumentHandler, Log log) {
        this(log, getTestHost(argumentHandler), argumentHandler.getPipePortNumber(),
                (long)argumentHandler.getWaitTime() * 60 * 1000, false);
    }

    /**
      * Make <code>IOPipe</code> at debugger's side
      * with given <code>Debugee</code> mirror.
      *
      * @deprecated Preferred way is to start IOPipe before launching debuggee process.
      *
      * @see #startDebuggerPipe
      */
    @Deprecated
    public IOPipe(DebugeeProcess debugee) {
        this(debugee.getLog(),
                debugee.getArgumentHandler().getDebugeeHost(),
                debugee.getArgumentHandler().getPipePortNumber(),
                (long)debugee.getArgumentHandler().getWaitTime() * 60 * 1000,
                true);
        setServerSocket(debugee.getPipeServerSocket());
    }

    /**
      * Make general <code>IOPipe</code> object with specified parameters.
      */
    protected IOPipe(Log log, String host, int port, long timeout, boolean listening) {
        super("IOPipe", log, PIPE_LOG_PREFIX, host, port, timeout, listening);
    }

    /**
     * Creates and starts listening <code>IOPipe</code> at debugger side.
     */
    public static IOPipe startDebuggerPipe(Binder binder) {
        IOPipe ioPipe = new IOPipe(binder.getLog(),
                binder.getArgumentHandler().getDebugeeHost(),
                binder.getArgumentHandler().getPipePortNumber(),
                (long)binder.getArgumentHandler().getWaitTime() * 60 * 1000,
                true);
        ioPipe.setServerSocket(binder.getPipeServerSocket());
        ioPipe.startListening();
        return ioPipe;
    }


    protected void connect() {
        super.connect();
    }

    /**
     * Get appropriate test host name relying on the provided argumnets.
     */
    private static String getTestHost(DebugeeArgumentHandler argumentHandler) {
        return argumentHandler.getTestHost();
    }

} // IOPipe
