/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The OrangeEchoImpl class implements the behavior of the remote "orange
 * echo" objects exported by the server.  The purpose of these objects
 * is simply to recursively call back to their caller.
 */
public class OrangeEchoImpl extends UnicastRemoteObject implements OrangeEcho {

    private static final Logger logger =
        Logger.getLogger("reliability.orangeecho");
    private final String name;

    public OrangeEchoImpl(String name) throws RemoteException {
        this.name = name;
    }

    /**
     * Call back on supplied "orange" object (presumably the caller)
     * with the same message data and a decremented recursion level.
     */
    public int[] recurse(Orange orange, int[] message, int level)
        throws RemoteException
    {
        String threadName = Thread.currentThread().getName();

        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".recurse(message["
            + message.length + "], " + level + "): BEGIN");

        int[] response = orange.recurse(this, message, level - 1);

        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".recurse(message["
            + message.length + "], " + level + "): END");

        return response;
    }

    public String toString() {
        return name;
    }
}
