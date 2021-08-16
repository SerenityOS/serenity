/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.perfdata.monitor.protocol.local;

import sun.jvmstat.monitor.*;
import sun.jvmstat.perfdata.monitor.MonitorStatus;
import java.util.*;
import java.util.regex.*;
import java.io.*;

/**
 * Singleton Timer subclass to run polling tasks that generate events
 * for local Java Virtual Machines..
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class LocalEventTimer extends Timer {
    private static LocalEventTimer instance;   // singleton instance

    /**
     * Creates the singleton LocalEventTimer instance.
     */
    private LocalEventTimer() {
        super(true);
    }

    /**
     * Get the singleton LocalEventTimer instance
     *
     * @return LocalEventTimer - the singleton LocalEventTimer instance
     */
    public static synchronized LocalEventTimer getInstance() {
        if (instance == null) {
            instance = new LocalEventTimer();
        }
        return instance;
    }
}
