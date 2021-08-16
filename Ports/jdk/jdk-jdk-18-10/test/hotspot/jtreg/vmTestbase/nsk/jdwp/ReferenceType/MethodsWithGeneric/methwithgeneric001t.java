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

package nsk.jdwp.ReferenceType.MethodsWithGeneric;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

public class methwithgeneric001t {
    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);

        // load tested classes
        methwithgeneric001b<String> _methwithgeneric001b =
            new methwithgeneric001b<String>();
        methwithgeneric001c<Boolean, Integer> _methwithgeneric001c =
            new methwithgeneric001c<Boolean, Integer>();
        methwithgeneric001e _methwithgeneric001e =
            new methwithgeneric001e();
        methwithgeneric001if<Object> _methwithgeneric001if =
            new methwithgeneric001d<Object>();
        methwithgeneric001g<methwithgeneric001f> _methwithgeneric001g =
            new methwithgeneric001g<methwithgeneric001f>();

        log.display("Debuggee VM started\nSending command: "
            + methwithgeneric001.COMMAND_READY);
        pipe.println(methwithgeneric001.COMMAND_READY);

        log.display("Waiting for command: "
            + methwithgeneric001.COMMAND_QUIT + " ...");
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

class methwithgeneric001b<L extends String> {
    <L extends String> methwithgeneric001b<String> methwithgeneric001bMeth(methwithgeneric001b<L> m) {
        return new methwithgeneric001b<String>();
    }

    static <T extends String> methwithgeneric001b<String> methwithgeneric001bMethSt(methwithgeneric001b<T> m) {
        return new methwithgeneric001b<String>();
    }
}

class methwithgeneric001c<A, B extends Integer> {
    public <U> U methwithgeneric001cMeth(Class<U> klass) throws Exception {
        return klass.newInstance();
    }

    static public <U> U methwithgeneric001cMethSt(Class<U> klass) throws Exception {
        return klass.newInstance();
    }
}

interface methwithgeneric001if<I> {
    int methwithgeneric001ifMeth();

    <I> int methwithgeneric001ifMeth2(I v);
}

class methwithgeneric001d<T> implements methwithgeneric001if<T> {
    public int methwithgeneric001ifMeth() {
        return 1;
    }

    public <T> int methwithgeneric001ifMeth2(T v) {
        return 2;
    }
}

class methwithgeneric001e {
    void methwithgeneric001eMeth(methwithgeneric001e e) {}
    static void methwithgeneric001eMethSt(methwithgeneric001e e) {}
}

class methwithgeneric001f extends methwithgeneric001e implements methwithgeneric001if {
    public int methwithgeneric001ifMeth() {
        return 3;
    }

    public int methwithgeneric001ifMeth2(Object v) {
        return 4;
    }
}

class methwithgeneric001g<E extends methwithgeneric001e & methwithgeneric001if> {
    <A extends Byte, B extends Double> void methwithgeneric001gMeth(A a, B b, Class<?>[] c) {}

    static <A extends Byte, B extends Double> void methwithgeneric001gMethSt(A a, B b) {}
}
