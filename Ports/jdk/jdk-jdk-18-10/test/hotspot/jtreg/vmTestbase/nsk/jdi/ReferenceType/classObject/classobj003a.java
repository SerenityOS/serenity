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

package nsk.jdi.ReferenceType.classObject;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged application of the test.
 */
public class classobj003a {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static ArgumentHandler argHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    public static void receiveSignal(String signal) {
        String line = pipe.readln();

        if ( !line.equals(signal) )
            throw new Failure("UNEXPECTED debugger's signal " + line);

        display("debugger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static Enum1 f1 = Enum1.e1;
    static Enum2 f2 = Enum2.e2;
    static Enum3 f3 = Enum3.e1;
    static Enum4.Enum4_ f4 = Enum4.Enum4_.e1;
    static classobj003Enum5 f5 = classobj003Enum5.e2;
    static classobj003Enum6 f6 = classobj003Enum6.e1;
    static classobj003Enum7 f7 = classobj003Enum7.e2;
    static classobj003Enum8.Enum8_ f8 = classobj003Enum8.Enum8_.e1;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        pipe.println(classobj003.SIGNAL_READY);


        //pipe.println(classobj003.SIGNAL_GO);
        receiveSignal(classobj003.SIGNAL_QUIT);

        display("completed succesfully.");
        System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
    }

    //--------------------------------------------------------- test specific inner classes

    enum Enum1 {
        e1, e2;
    }

    enum Enum2 {
        e1 {
           int val() {return 1;}
        },

        e2 {
           int val() {return 2;}
        };
        abstract int val();
    }

    enum Enum3 implements classobj003i {
        e1 {
           int val() {return i+1;}
        },

        e2 {
           int val() {return i+2;}
        };
        abstract int val();
    }

    enum Enum4 {
       e1, e2;

       enum Enum4_ {
           e1, e2;
       }
    }

}

//--------------------------------------------------------- test specific classes

enum classobj003Enum5 {
    e1, e2;
}

enum classobj003Enum6 {
    e1 {
       int val() {return 1;}
    },

    e2 {
       int val() {return 2;}
    };
    abstract int val();
}

enum classobj003Enum7 implements classobj003i {
    e1 {
       int val() {return i+1;}
    },

    e2 {
       int val() {return i+2;}
    };
    abstract int val();
}

enum classobj003Enum8 {
   e1, e2;
   enum Enum8_ {
       e1, e2;
   }
}

interface classobj003i {
    int i = 1;
}
