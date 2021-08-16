/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.unit.FollowReferences;

import java.io.PrintStream;
import nsk.share.jvmti.*;
import nsk.share.jvmti.unit.*;
import nsk.share.*;

public class followref004 extends DebugeeClass {

    FollowRefObjects _mesh;

    static {
        loadLibrary("followref004");
    }

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new followref004().runIt(argv, out);
    }

    public int runIt(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);

        log.display("Java: Creating FollowRefObjects mesh");

        // Let agent's code pass SYNC POINT 1 (see .c file for details)
        _mesh = new FollowRefObjects();
        _mesh.createObjects();

        log.display("Java: check status");
        int status = checkStatus(0);

        log.display("Java: exiting with code " + status);
        return status;
    }

}
