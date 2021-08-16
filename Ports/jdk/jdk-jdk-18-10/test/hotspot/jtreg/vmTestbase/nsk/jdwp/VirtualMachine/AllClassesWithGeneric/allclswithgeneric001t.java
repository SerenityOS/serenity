/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.VirtualMachine.AllClassesWithGeneric;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

public class allclswithgeneric001t {
    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);

        // load tested classes
        allclswithgeneric001b<String> _allclswithgeneric001b =
            new allclswithgeneric001b<String>();
        allclswithgeneric001c<Boolean, Integer> _allclswithgeneric001c =
            new allclswithgeneric001c<Boolean, Integer>();
        allclswithgeneric001e _allclswithgeneric001e =
            new allclswithgeneric001e();
        allclswithgeneric001if<Object> _allclswithgeneric001if =
            new allclswithgeneric001d<Object>();
        allclswithgeneric001f _allclswithgeneric001f =
            new allclswithgeneric001f();
        allclswithgeneric001g<allclswithgeneric001f> _allclswithgeneric001g =
            new allclswithgeneric001g<allclswithgeneric001f>();
        allclswithgeneric001h<Byte, Double, Float> _allclswithgeneric001h =
            new allclswithgeneric001h<Byte, Double, Float>();

        log.display("Debuggee VM started\nSending command: "
            + allclswithgeneric001.COMMAND_READY);
        pipe.println(allclswithgeneric001.COMMAND_READY);

        log.display("Waiting for command: "
            + allclswithgeneric001.COMMAND_QUIT + " ...");
        String cmd = pipe.readln();
        log.display(" ... Received command: " + cmd
            + "\nDebuggee is exiting ...");

        System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_PASSED);
    }
}

/*
 * Dummy classes used only for verifying generic signature information
 * in a debugger.
 */

class allclswithgeneric001b<L extends String> {}

class allclswithgeneric001c<A, B extends Integer> {}

interface allclswithgeneric001if<I> {
    int allclswithgeneric001ifMeth();

    <I> int allclswithgeneric001ifMeth2(I v);
}

class allclswithgeneric001d<T> implements allclswithgeneric001if<T> {
    public int allclswithgeneric001ifMeth() {
        return 1;
    }

    public <T> int allclswithgeneric001ifMeth2(T v) {
        return 2;
    }
}

class allclswithgeneric001e {}

class allclswithgeneric001f extends allclswithgeneric001e implements allclswithgeneric001if {
    public int allclswithgeneric001ifMeth() {
        return 3;
    }

    public int allclswithgeneric001ifMeth2(Object v) {
        return 4;
    }
}

class allclswithgeneric001g<E extends allclswithgeneric001e & allclswithgeneric001if> {}

interface allclswithgeneric001if2<A, B, C> {}

class allclswithgeneric001h<A1, B1, C1>
    extends allclswithgeneric001d<A1>
    implements allclswithgeneric001if2<A1, B1, C1> {}
