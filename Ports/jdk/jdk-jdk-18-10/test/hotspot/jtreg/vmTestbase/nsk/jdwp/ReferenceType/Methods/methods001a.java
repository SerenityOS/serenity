/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdwp.ReferenceType.Methods;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

public class methods001a {

    public static void main(String args[]) {
        methods001a _methods001a = new methods001a();
        System.exit(methods001.JCK_STATUS_BASE + _methods001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        Log log = new Log(out, argumentHandler);
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);
        log.display("Creating object of tested class");
        TestedClass foo = new TestedClass();
        log.display("Sending command: " + "ready");
        pipe.println("ready");
        log.display("Waiting for command: " + "quit");
        String command = pipe.readln();
        log.display("Received command: " + command);
        log.display("Debugee PASSED");
        return methods001.PASSED;
    }

    static class TestedClass {
        public TestedClass() {}
        public byte byteMethod(byte b) { return b; }
        public boolean booleanMethod() { return true; }
        public char charMethod(byte b) { return (char) b; }
        public short shortMethod(short x, short y) { return (short) (x - y); }
        public int intMethod(int x, short y) { return x - y; }
        public long longMethod(int x) { return (long) x; }
        public float floatMethod(double x) { return (float) x; }
        public double doubleMethod() { return 2.48e-10; }
        public String stringMethod(String s, char ch) { return s + ch; };
        public TestedClass objectMethod() { return this; }
        public int[] intArrayMethod(int n) { return new int[n]; };
        public TestedClass(boolean b) {}
    }
}
