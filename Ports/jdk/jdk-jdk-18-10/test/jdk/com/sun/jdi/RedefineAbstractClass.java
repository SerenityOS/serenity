/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6805864
 * @summary Redefine an abstract class that is called via a concrete
 * class and via two interface objects and verify that the right
 * methods are called.
 * @comment converted from test/jdk/com/sun/jdi/RedefineAbstractClass.sh
 *
 * @library /test/lib
 * @compile -g RedefineAbstractClass.java
 * @run main/othervm RedefineAbstractClass
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.ClassTransformer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class RedefineAbstractClassTarg {
    public static void main(String[] args) {
        System.out.println("This is RedefineAbstractClass");

        MyConcreteClass foo = new MyConcreteClass();
        // do the work once before redefine
        foo.doWork();

        System.out.println("stop here for redefine");  // @1 breakpoint

        // do the work again after redefine
        foo.doWork();

        System.out.println("stop here to check results");  // @2 breakpoint
    }
}

interface MyInterface1 {
    public boolean checkFunc();
    public boolean isMyInterface1();
}

interface MyInterface2 {
    public boolean checkFunc();
    public boolean isMyInterface2();
}

abstract class MyAbstractClass implements MyInterface1, MyInterface2 {
    static int counter = 0;
    public boolean checkFunc() {
        counter++;
        System.out.println("MyAbstractClass.checkFunc() called.");
        // @1 uncomment System.out.println("This is call " + counter + " to checkFunc");
        return true;
    }
    public boolean isMyInterface1() {
        System.out.println("MyAbstractClass.isMyInterface1() called.");
        return true;
    }
    public boolean isMyInterface2() {
        System.out.println("MyAbstractClass.isMyInterface2() called.");
        return true;
    }
}

class MyConcreteClass extends MyAbstractClass {
    public void doWork() {
        // checkFunc() is called via invokevirtual here; MyConcreteClass
        // inherits via MyAbstractClass
        System.out.println("In doWork() calling checkFunc(): " + checkFunc());

        MyInterface1 if1 = (MyInterface1) this;
        // checkFunc() is called via invokeinterface here; this call will
        // use the first itable entry
        System.out.println("In doWork() calling if1.checkFunc(): " + if1.checkFunc());

        MyInterface2 if2 = (MyInterface2) this;
        // checkFunc() is called via invokeinterface here; this call will
        // use the second itable entry
        System.out.println("In doWork() calling if2.checkFunc(): " + if2.checkFunc());
    }
}


public class RedefineAbstractClass extends JdbTest {
    public static void main(String argv[]) {
        new RedefineAbstractClass().run();
    }

    private RedefineAbstractClass() {
        super(DEBUGGEE_CLASS, SOURCE_FILE);
    }

    private static final String DEBUGGEE_CLASS = RedefineAbstractClassTarg.class.getName();
    private static final String SOURCE_FILE = "RedefineAbstractClass.java";
    private static final String ABSTRACT_CLASS = "MyAbstractClass";

    @Override
    protected void runCases() {
        setBreakpoints(1);
        setBreakpoints(2);
        jdb.command(JdbCommand.run());

        // modified version of redefineClass function
        String transformedClassFile = ClassTransformer.fromTestSource(SOURCE_FILE)
                .transform(1, ABSTRACT_CLASS, "-g");
        jdb.command(JdbCommand.redefine(ABSTRACT_CLASS, transformedClassFile));
        // end modified version of redefineClass function

        // this will continue to the second breakpoint
        jdb.command(JdbCommand.cont());

        new OutputAnalyzer(getDebuggeeOutput())
                .shouldContain("This is call 4 to checkFunc")
                .shouldContain("This is call 5 to checkFunc")
                .shouldContain("This is call 6 to checkFunc");
    }
}
