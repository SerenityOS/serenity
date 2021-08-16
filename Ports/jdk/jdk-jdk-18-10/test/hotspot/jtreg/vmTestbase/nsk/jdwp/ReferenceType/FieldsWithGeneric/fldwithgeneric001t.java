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

package nsk.jdwp.ReferenceType.FieldsWithGeneric;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

public class fldwithgeneric001t {
    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);

        // load a tested class
        fldwithgeneric001a _fldwithgeneric001a =
            new fldwithgeneric001a();

        log.display("Debuggee VM started\nSending command: "
            + fldwithgeneric001.COMMAND_READY);
        pipe.println(fldwithgeneric001.COMMAND_READY);

        log.display("Waiting for command: "
            + fldwithgeneric001.COMMAND_QUIT + " ...");
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

class fldwithgeneric001b<L extends String> {}

class fldwithgeneric001c<A, B extends Integer> {}

interface fldwithgeneric001if<I> {}

class fldwithgeneric001d<T> implements fldwithgeneric001if<T> {}

class fldwithgeneric001e {}

class fldwithgeneric001f extends fldwithgeneric001e implements fldwithgeneric001if {}

class fldwithgeneric001g<E extends fldwithgeneric001e & fldwithgeneric001if> {}

class fldwithgeneric001h {
    // dummy fields to be inherited. They must not be included into
    // information for reference type fields.
    public fldwithgeneric001 _fldwithgeneric001h =
        new fldwithgeneric001();
    public fldwithgeneric001b<String> _fldwithgeneric001bh =
        new fldwithgeneric001b<String>();
}

class fldwithgeneric001a extends fldwithgeneric001h {
    // dummy fields used for testing
    public static fldwithgeneric001 _fldwithgeneric001St =
        new fldwithgeneric001();
    fldwithgeneric001b<String> _fldwithgeneric001b =
        new fldwithgeneric001b<String>();
    static fldwithgeneric001b<String> _fldwithgeneric001bSt =
        new fldwithgeneric001b<String>();
    fldwithgeneric001c<Boolean, Integer> _fldwithgeneric001c =
        new fldwithgeneric001c<Boolean, Integer>();
    static fldwithgeneric001c<Boolean, Integer> _fldwithgeneric001cSt =
        new fldwithgeneric001c<Boolean, Integer>();
    fldwithgeneric001e _fldwithgeneric001e =
        new fldwithgeneric001e();
    static fldwithgeneric001e _fldwithgeneric001eSt =
        new fldwithgeneric001e();
    fldwithgeneric001if<Object> _fldwithgeneric001if =
        new fldwithgeneric001d<Object>();
    static fldwithgeneric001if<Object> _fldwithgeneric001ifSt =
        new fldwithgeneric001d<Object>();
    fldwithgeneric001g<fldwithgeneric001f> _fldwithgeneric001g =
        new fldwithgeneric001g<fldwithgeneric001f>();
    static fldwithgeneric001g<fldwithgeneric001f> _fldwithgeneric001gSt =
        new fldwithgeneric001g<fldwithgeneric001f>();
    fldwithgeneric001g[] _fldwithgeneric001gArr =
        new fldwithgeneric001g[]{};
}
