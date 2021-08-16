/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.httpserver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.security.PrivilegedAction;

/**
 * Parameters that users will not likely need to set
 * but are useful for debugging
 */

@SuppressWarnings("removal")
class ServerConfig {

    private static final int DEFAULT_CLOCK_TICK = 10000 ; // 10 sec.

    /* These values must be a reasonable multiple of clockTick */
    private static final long DEFAULT_IDLE_INTERVAL = 30 ; // 5 min
    private static final int DEFAULT_MAX_IDLE_CONNECTIONS = 200 ;

    private static final long DEFAULT_MAX_REQ_TIME = -1; // default: forever
    private static final long DEFAULT_MAX_RSP_TIME = -1; // default: forever
    private static final long DEFAULT_TIMER_MILLIS = 1000;
    private static final int  DEFAULT_MAX_REQ_HEADERS = 200;
    private static final long DEFAULT_DRAIN_AMOUNT = 64 * 1024;

    private static int clockTick;
    private static long idleInterval;
    // The maximum number of bytes to drain from an inputstream
    private static long drainAmount;
    private static int maxIdleConnections;
    // The maximum number of request headers allowable
    private static int maxReqHeaders;
    // max time a request or response is allowed to take
    private static long maxReqTime;
    private static long maxRspTime;
    private static long timerMillis;
    private static boolean debug;

    // the value of the TCP_NODELAY socket-level option
    private static boolean noDelay;

    static {
        java.security.AccessController.doPrivileged(
            new PrivilegedAction<Void>() {
                @Override
                public Void run () {
                    idleInterval = Long.getLong("sun.net.httpserver.idleInterval",
                            DEFAULT_IDLE_INTERVAL) * 1000;

                    clockTick = Integer.getInteger("sun.net.httpserver.clockTick",
                            DEFAULT_CLOCK_TICK);

                    maxIdleConnections = Integer.getInteger(
                            "sun.net.httpserver.maxIdleConnections",
                            DEFAULT_MAX_IDLE_CONNECTIONS);

                    drainAmount = Long.getLong("sun.net.httpserver.drainAmount",
                            DEFAULT_DRAIN_AMOUNT);

                    maxReqHeaders = Integer.getInteger(
                            "sun.net.httpserver.maxReqHeaders",
                            DEFAULT_MAX_REQ_HEADERS);

                    maxReqTime = Long.getLong("sun.net.httpserver.maxReqTime",
                            DEFAULT_MAX_REQ_TIME);

                    maxRspTime = Long.getLong("sun.net.httpserver.maxRspTime",
                            DEFAULT_MAX_RSP_TIME);

                    timerMillis = Long.getLong("sun.net.httpserver.timerMillis",
                            DEFAULT_TIMER_MILLIS);

                    debug = Boolean.getBoolean("sun.net.httpserver.debug");

                    noDelay = Boolean.getBoolean("sun.net.httpserver.nodelay");

                    return null;
                }
            });

    }

    static void checkLegacyProperties(final Logger logger) {

        // legacy properties that are no longer used
        // print a warning to logger if they are set.

        java.security.AccessController.doPrivileged(
            new PrivilegedAction<Void>() {
                public Void run () {
                    if (System.getProperty("sun.net.httpserver.readTimeout")
                                                !=null)
                    {
                        logger.log (Level.WARNING,
                            "sun.net.httpserver.readTimeout "+
                            "property is no longer used. "+
                            "Use sun.net.httpserver.maxReqTime instead."
                        );
                    }
                    if (System.getProperty("sun.net.httpserver.writeTimeout")
                                                !=null)
                    {
                        logger.log (Level.WARNING,
                            "sun.net.httpserver.writeTimeout "+
                            "property is no longer used. Use "+
                            "sun.net.httpserver.maxRspTime instead."
                        );
                    }
                    if (System.getProperty("sun.net.httpserver.selCacheTimeout")
                                                !=null)
                    {
                        logger.log (Level.WARNING,
                            "sun.net.httpserver.selCacheTimeout "+
                            "property is no longer used."
                        );
                    }
                    return null;
                }
            }
        );
    }

    static boolean debugEnabled() {
        return debug;
    }

    static long getIdleInterval() {
        return idleInterval;
    }

    static int getClockTick() {
        return clockTick;
    }

    static int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    static long getDrainAmount() {
        return drainAmount;
    }

    static int getMaxReqHeaders() {
        return maxReqHeaders;
    }

    static long getMaxReqTime() {
        return maxReqTime;
    }

    static long getMaxRspTime() {
        return maxRspTime;
    }

    static long getTimerMillis() {
        return timerMillis;
    }

    static boolean noDelay() {
        return noDelay;
    }
}
