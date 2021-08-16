/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.net;

import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import sun.security.action.GetPropertyAction;

/**
 * Manages count of total number of UDP sockets and ensures
 * that exception is thrown if we try to create more than the
 * configured limit.
 *
 * This functionality could be put in NetHooks some time in future.
 */

public class ResourceManager {

    /* default maximum number of udp sockets per VM
     * when a security manager is enabled.
     * The default is 25 which is high enough to be useful
     * but low enough to be well below the maximum number
     * of port numbers actually available on all OSes
     * when multiplied by the maximum feasible number of VM processes
     * that could practically be spawned.
     */

    private static final int DEFAULT_MAX_SOCKETS = 25;
    private static final int maxSockets;
    private static final AtomicInteger numSockets;

    static {
        String prop = GetPropertyAction
                .privilegedGetProperty("sun.net.maxDatagramSockets");
        int defmax = DEFAULT_MAX_SOCKETS;
        try {
            if (prop != null) {
                defmax = Integer.parseInt(prop);
            }
        } catch (NumberFormatException e) {}
        maxSockets = defmax;
        numSockets = new AtomicInteger();
    }

    @SuppressWarnings("removal")
    public static void beforeUdpCreate() throws SocketException {
        if (System.getSecurityManager() != null) {
            if (numSockets.incrementAndGet() > maxSockets) {
                numSockets.decrementAndGet();
                throw new SocketException("maximum number of DatagramSockets reached");
            }
        }
    }

    @SuppressWarnings("removal")
    public static void afterUdpClose() {
        if (System.getSecurityManager() != null) {
            numSockets.decrementAndGet();
        }
    }
}
