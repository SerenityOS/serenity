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

package nsk.jdi.Method.isBridge;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This is a debuggee class containing several dummy methods
 * used by a debugger.
 */
public class isbridge001t {
    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        Log log = argHandler.createDebugeeLog();

        // load tested classes
        isbridge001aa _isbridge001aa =
            new isbridge001aa();
        isbridge001bb _isbridge001bb =
            new isbridge001bb();
        isbridge001bb2 _isbridge001bb2 =
            new isbridge001bb2();
        Class _isbridge001dd;
        isbridge001dd2 _isbridge001dd2 =
            new isbridge001dd2();

        try {
            _isbridge001dd =
                Class.forName(isbridge001.classes[3]);
        } catch(Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: Class.forName: caught " + e);
            System.exit(Consts.JCK_STATUS_BASE +
                Consts.TEST_FAILED);
        }

        log.display("Debuggee: sending the command: "
            + isbridge001.COMMAND_READY);
        pipe.println(isbridge001.COMMAND_READY);
        String cmd = pipe.readln();
        log.display("Debuggee: received the command: "
            + cmd);

        if (!cmd.equals(isbridge001.COMMAND_QUIT)) {
            log.complain("TEST BUG: unknown debugger command: "
                + cmd);
            System.exit(Consts.JCK_STATUS_BASE +
                Consts.TEST_FAILED);
        }

        log.display("Debuggee: exiting");
        System.exit(Consts.JCK_STATUS_BASE +
            Consts.TEST_PASSED);
    }
}

/*
 * Dummy classes used only for verifying bridge method information
 * in a debugger.
 */

class isbridge001a {
    void isbridge001aMeth(Object i) {}
}

/* Test case 1: no bridge methods: one rule for bridge method
 generation is observed, but the tested classes are not generic. */
class isbridge001aa extends isbridge001a {
    void isbridge001aMeth(Double i) {}
}

class isbridge001b<T extends Number> {
    T isbridge001bMeth(T a) {
        return a;
    }

    void isbridge001bMeth2(T b, int i) {}
}

/* Test case 2: no bridge methods: the erasure of the return type/arguments
 types of overridding methods are the same with the original ones. */
class isbridge001bb extends isbridge001b<Number> {
    Number isbridge001bMeth(Number a) {
        return a;
    }

    void isbridge001bMeth2(Number b, int i) {}
}

/* Test case 3: bridge methods: the erasure of the return type/arguments
 types of overridding methods are differ from the original ones. */
class isbridge001bb2 extends isbridge001b<Byte> {
    Byte isbridge001bMeth(Byte a) {
        return a;
    }

    void isbridge001bMeth2(Byte b, int i) {}
}

/* Test case 4 and classes isbridge001c/cc were removed. Bug id: 5083386 */

abstract class isbridge001d<A, B, C> {
    abstract C isbridge001dMeth(A a, B b);
}

/* Test case 5: no bridge methods: non-overridden method of superclass
 is abstract. */
abstract class isbridge001dd extends isbridge001d<Integer, Long, Short> {}

/* Test case 6: bridge methods: overridden method of superclass
 is abstract. */
class isbridge001dd2 extends isbridge001d<Boolean, Character, String> {
    String isbridge001dMeth(Boolean a, Character b) {
        return (String) "bridge";
    }
}
