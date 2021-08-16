/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.server;

import com.sun.tools.sjavac.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Monitors the presence of a port file and shuts down the given SjavacServer
 * whenever the port file is deleted or invalidated.
 *
 * TODO: JDK-8046882
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class PortFileMonitor {

    // Check if the portfile is gone, every 5 seconds.
    private static final int CHECK_PORTFILE_INTERVAL = 5000;

    private final Timer timer = new Timer();
    private final PortFile portFile;
    private final SjavacServer server;

    public PortFileMonitor(PortFile portFile,
                           SjavacServer server) {
        this.portFile = portFile;
        this.server = server;
    }

    public void start() {
        Log log = Log.get();
        TimerTask shutdownCheck = new TimerTask() {
            public void run() {
                Log.setLogForCurrentThread(log);
                Log.debug("Checking port file status...");
                try {
                    if (!portFile.exists()) {
                        // Time to quit because the portfile was deleted by another
                        // process, probably by the makefile that is done building.
                        server.shutdown("Quitting because portfile was deleted!");
                    } else if (portFile.markedForStop()) {
                        // Time to quit because another process touched the file
                        // server.port.stop to signal that the server should stop.
                        // This is necessary on some operating systems that lock
                        // the port file hard!
                        server.shutdown("Quitting because a portfile.stop file was found!");
                    } else if (!portFile.stillMyValues()) {
                        // Time to quit because another build has started.
                        server.shutdown("Quitting because portfile is now owned by another javac server!");
                    }
                } catch (IOException e) {
                    Log.error("IOException caught in PortFileMonitor.");
                    Log.debug(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.error(e);
                }
            }
        };

        timer.schedule(shutdownCheck, 0, CHECK_PORTFILE_INTERVAL);
    }

    public void shutdown() {
        timer.cancel();
    }
}
