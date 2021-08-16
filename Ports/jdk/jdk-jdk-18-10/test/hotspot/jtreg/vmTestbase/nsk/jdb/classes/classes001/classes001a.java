/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.classes.classes001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class classes001a {

    /* TEST DEPENDANT VARIABLES AND CONSTANTS */
    static final String PACKAGE_NAME = "nsk.jdb.classes.classes001";

    public static void main(String args[]) {
       classes001a _classes001a = new classes001a();
       System.exit(classes001.JCK_STATUS_BASE + _classes001a.runIt(args, System.out));
    }


    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        init();

        log.display("Debuggee PASSED");
        return classes001.PASSED;
    }

    class Inner1 {}
    interface InnerInt1 {}

    public class Inner2 {}
    public interface InnerInt2 {}

    private class Inner3 {}
    private interface InnerInt3 {}

    protected class Inner4 {}
    protected interface InnerInt4 {}

    abstract class Inner5 {}
    abstract interface InnerInt5 {}

    final class Inner6 extends Inner5{}

    class Inner7 extends Outer2{}
    class Inner8 implements OuterInt1, OuterInt2, InnerInt1, InnerInt2, InnerInt3,  InnerInt4, InnerInt5 {}

    private void init () {
        Outer1 o1 = new Outer1();
        Outer3 o3 = new Outer3();
        Inner1 i1 = new Inner1();
        Inner2 i2 = new Inner2();
        Inner3 i3 = new Inner3();
        Inner4 i4 = new Inner4();
        Inner6 i6 = new Inner6();
        Inner7 i7 = new Inner7();
        Inner8 i8 = new Inner8();

        lastBreak();
    }
}

class Outer1 {}
interface OuterInt1 {}

abstract class Outer2 {}
abstract interface OuterInt2 {}

final class Outer3 {}
