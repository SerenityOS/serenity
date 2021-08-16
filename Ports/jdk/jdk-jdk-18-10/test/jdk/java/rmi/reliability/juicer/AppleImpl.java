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
 * The AppleImpl class implements the behavior of the remote "apple"
 * objects exported by the application.
 */
public class AppleImpl extends UnicastRemoteObject implements Apple {

    private static final Logger logger = Logger.getLogger("reliability.apple");
    private final String name;

    public AppleImpl(String name) throws RemoteException {
        this.name = name;
    }

    /**
     * Receive an array of AppleEvent objects.
     */
    public void notify(AppleEvent[] events) {
        String threadName = Thread.currentThread().getName();
        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".notify: BEGIN");

        for (int i = 0; i < events.length; i++) {
            logger.log(Level.FINEST,
                threadName + ": " + toString() + ".notify(): events["
                + i + "] = " + events[i].toString());
        }

        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".notify(): END");
    }

    /**
     * Return a newly created and exported orange implementation.
     */
    public Orange newOrange(String name) throws RemoteException {
        String threadName = Thread.currentThread().getName();
        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".newOrange(" + name + "): BEGIN");

        Orange orange = new OrangeImpl(name);

        logger.log(Level.FINEST,
            threadName + ": " + toString() + ".newOrange(" + name + "): END");

        return orange;
    }

    public String toString() {
        return name;
    }
}
