/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package bench.rmi;

import bench.Benchmark;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;

/**
 * Benchmark for testing speed of UnicastRemoteObject.exportObject().
 */
public class ExportObjs implements Benchmark {

    static class RemoteObj implements Remote {
    }

    /**
     * Export remote objects.
     * Arguments: <# objects>
     */
    public long run(String[] args) throws Exception {
        int size = Integer.parseInt(args[0]);
        Remote[] objs = new Remote[size];
        for (int i = 0; i < size; i++)
            objs[i] = new RemoteObj();

        long start = System.currentTimeMillis();
        for (int i = 0; i < size; i++)
            UnicastRemoteObject.exportObject(objs[i],0);
        long time = System.currentTimeMillis() - start;

        for (int i = 0; i < size; i++)
            UnicastRemoteObject.unexportObject(objs[i], true);
        return time;
    }
}
