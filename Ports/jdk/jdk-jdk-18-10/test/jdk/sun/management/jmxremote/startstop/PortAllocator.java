/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Random;

/**
 * Dynamically allocates distinct ports from the ephemeral range 49152-65535
 */
class PortAllocator {
    private final static int LOWER_BOUND = 49152;
    private final static int UPPER_BOUND = 65535;

    private final static Random RND = new Random(System.currentTimeMillis());

    static int[] allocatePorts(final int numPorts) {
        int[] ports = new int[numPorts];
        for (int i = 0; i < numPorts; i++) {
            int port = -1;
            while (port == -1) {
                port = RND.nextInt(UPPER_BOUND - LOWER_BOUND + 1) + LOWER_BOUND;
                for (int j = 0; j < i; j++) {
                    if (ports[j] == port) {
                        port = -1;
                        break;
                    }
                }
            }
            ports[i] = port;
        }
        return ports;
    }
}
