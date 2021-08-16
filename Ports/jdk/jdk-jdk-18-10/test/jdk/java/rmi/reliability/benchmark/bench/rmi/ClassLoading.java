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
import java.rmi.server.RMIClassLoader;
import java.security.CodeSource;

/**
 * Benchmark for testing speed of repeated loading of a class not found in
 * classpath.
 */
public class ClassLoading implements Benchmark {

    static final String ALTROOT = "!/bench/rmi/altroot/";
    static final String CLASSNAME = "Node";

    /**
     * Repeatedly load a class not found in classpath through RMIClassLoader.
     * Arguments: <# reps>
     */
    public long run(String[] args) throws Exception {
        int reps = Integer.parseInt(args[0]);
        CodeSource csrc = getClass().getProtectionDomain().getCodeSource();
        String url = "jar:" + csrc.getLocation().toString() + ALTROOT;

        long start = System.currentTimeMillis();
        for (int i = 0; i < reps; i++)
            RMIClassLoader.loadClass(url, CLASSNAME);
        long time = System.currentTimeMillis() - start;

        return time;
    }
}
