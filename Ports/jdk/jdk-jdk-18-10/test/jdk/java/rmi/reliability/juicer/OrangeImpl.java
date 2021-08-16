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
 * The OrangeImpl class implements the behavior of the remote "orange"
 * objects exported by the appplication.
 */
public class OrangeImpl extends UnicastRemoteObject implements Orange {

    private static final Logger logger = Logger.getLogger("reliability.orange");
    private final String name;

    public OrangeImpl(String name) throws RemoteException {
        this.name = name;
    }

    /**
     * Return inverted message data, call through supplied OrangeEcho
     * object if not at recursion level zero.
     */
    public int[] recurse(OrangeEcho echo, int[] message, int level)
        throws RemoteException
    {
        String threadName = Thread.currentThread().getName();
        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".recurse(message["
            + message.length + "], " + level + "): BEGIN");

        int[] response;
        if (level > 0) {
            response = echo.recurse(this, message, level);
        } else {
            for (int i = 0; i < message.length; i++) {
                message[i] = ~message[i];
            }
            response = message;
        }

        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".recurse(message["
            + message.length + "], " + level + "): END");

        return response;
    }

    public String toString() {
        return name;
    }
}
