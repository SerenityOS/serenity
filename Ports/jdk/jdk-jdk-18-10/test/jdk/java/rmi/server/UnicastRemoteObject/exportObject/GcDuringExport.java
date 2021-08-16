/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Portions Copyright (c) 2011 IBM Corporation
 */

/*
 * @test
 * @bug 6597112
 * @summary GC'ing objects whilst being exported to RMI should not cause exceptions
 * @author Neil Richards <neil.richards@ngmr.net>, <neil_richards@uk.ibm.com>
 * @run main/othervm GcDuringExport
 * @key intermittent
 */

import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;

public class GcDuringExport {
    private static final long MAX_EXPORT_ITERATIONS = 50000;

    public static void main(String[] args) throws Exception {
        Thread gcInducingThread = new Thread() {
            public void run() {
                while (true) {
                    System.gc();
                    try { Thread.sleep(1); } catch (InterruptedException e) { }
                }
            }
        };
        gcInducingThread.setDaemon(true);
        gcInducingThread.start();

        long i = 0;
        try {
            while (i < MAX_EXPORT_ITERATIONS) {
                i++;
                UnicastRemoteObject.exportObject(new Remote() { }, 0);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Test FAILED on iteration " + i + ".", e);
        }

        System.out.println("Test successfully exported " + i + " objects.");
    }
}
