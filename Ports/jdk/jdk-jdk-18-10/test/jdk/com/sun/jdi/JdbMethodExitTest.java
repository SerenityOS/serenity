/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6202891
 * @summary TTY: Add support for method exit event return values to jdb
 * @comment converted from test/jdk/com/sun/jdi/JdbMethodExitTest.sh
 *
 * @library /test/lib
 * @compile -g JdbMethodExitTest.java
 * @run main/othervm JdbMethodExitTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

import java.util.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.stream.Collectors;

/*
 * This tests the jdb trace command
 */

class JdbMethodExitTestTarg {
    // These are the values that will be returned by the methods
    static URL[] urls = new URL[1];
    public static byte      byteValue = 89;
    public static char      charValue = 'x';
    public static double    doubleValue = 2.2;
    public static float     floatValue = 3.3f;
    public static int       intValue = 1;
    public static short     shortValue = 8;
    public static boolean   booleanValue = false;

    public static Class       classValue = Object.class;
    public static ClassLoader classLoaderValue;
    {
        try {
            urls[0] = new URL("file:/foo");
        } catch (java.net.MalformedURLException ee) {
        }
        classLoaderValue = new URLClassLoader(urls);
    }

    public static Thread      threadValue;
    public static ThreadGroup threadGroupValue;
    public static String      stringValue = "abc";
    public static int[]       intArrayValue = new int[] {1, 2, 3};

    public static JdbMethodExitTestTarg  objectValue =
        new JdbMethodExitTestTarg();
    public String ivar = stringValue;

    // These are the instance methods
    public byte i_bytef()            { return byteValue; }
    public char i_charf()            { return charValue; }
    public double i_doublef()        { return doubleValue; }
    public float i_floatf()          { return floatValue; }
    public int i_intf()              { return intValue; }
    public short i_shortf()          { return shortValue; }
    public boolean i_booleanf()      { return booleanValue; }
    public String i_stringf()        { return stringValue; }
    public Class i_classf()          { return classValue; }
    public ClassLoader i_classLoaderf()
                                     { return classLoaderValue; }
    public Thread i_threadf()        { return threadValue = Thread.currentThread(); }
    public ThreadGroup i_threadGroupf()
                                     { return threadGroupValue = threadValue.getThreadGroup(); }
    public int[] i_intArrayf()       { return intArrayValue; }
    public Object i_nullObjectf()    { return null; }
    public Object i_objectf()        { return objectValue; }
    public void i_voidf()            {}

    static void doit(JdbMethodExitTestTarg xx) {

        xx.i_bytef();
        xx.i_charf();
        xx.i_doublef();
        xx.i_floatf();
        xx.i_intf();
        xx.i_shortf();
        xx.i_booleanf();
        xx.i_stringf();
        xx.i_intArrayf();
        xx.i_classf();
        xx.i_classLoaderf();
        xx.i_threadf();
        xx.i_threadGroupf();
        xx.i_nullObjectf();
        xx.i_objectf();
        xx.i_voidf();

        // Prove it works for native methods too
        StrictMath.sin(doubleValue);
        stringValue.intern();
    }

    public static void bkpt() {
       int i = 0;     //@1 breakpoint
    }

    public static String traceMethods() {
        return "traceMethods";
    }

    public static String traceMethods1() {
        return "traceMethods1";
    }

    public static String traceExits() {
        return "traceExits";
    }

    public static String traceExits1() {
        return "traceExits1";
    }

    public static String traceExit() {
        return "traceExit";
    }

    public static String traceExit1() {
        return "traceExit1";
    }

    public static void main(String[] args) {
        // The debugger will stop at the start of main,
        // enable method exit events, and then do
        // a resume.

        JdbMethodExitTestTarg xx = new JdbMethodExitTestTarg();
        System.out.println("threadid="+Thread.currentThread().getId());
        bkpt();

        // test all possible return types
        doit(xx);
        bkpt();

       // test trace methods
       traceMethods();

       // test trace go methods
       traceMethods1();
       bkpt();

       // test trace method exits
       traceExits();

       // test trace method exits
       traceExits1();
       bkpt();

       // test trace method exit
       traceExit();

       // test trace method exit
       traceExit1();
       bkpt();

    }
}

public class JdbMethodExitTest extends JdbTest {
    public static void main(String argv[]) {
        new JdbMethodExitTest().run();
    }

    private JdbMethodExitTest() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = JdbMethodExitTestTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("JdbMethodExitTest.java", 1);

        // test all possible return types
        execCommand(JdbCommand.run())
                .shouldContain("Breakpoint hit");
        Integer threadId = Integer.parseInt(
                new OutputAnalyzer(getDebuggeeOutput())
                        .firstMatch("^threadid=(.*)$", 1));
        jdb.command(JdbCommand.untrace());

        jdb.command(JdbCommand.traceMethods(false, null));
        execCommand(JdbCommand.trace())
                .shouldContain("trace methods in effect");

        jdb.command(JdbCommand.traceMethods(true, null));
        execCommand(JdbCommand.trace())
                .shouldContain("trace go methods in effect");

        jdb.command(JdbCommand.traceMethodExits(false, null));
        execCommand(JdbCommand.trace())
                .shouldContain("trace method exits in effect");

        jdb.command(JdbCommand.traceMethodExits(true, null));
        execCommand(JdbCommand.trace())
                .shouldContain("trace go method exits in effect");

        jdb.command(JdbCommand.traceMethodExit(false, null));
        execCommand(JdbCommand.trace())
                .shouldContain("trace method exit in effect for JdbMethodExitTestTarg.bkpt");

        jdb.command(JdbCommand.traceMethodExit(true, null));
        execCommand(JdbCommand.trace())
                .shouldContain("trace go method exit in effect for JdbMethodExitTestTarg.bkpt");


        // trace exit of methods with all the return values
        // (but just check a couple of them)
        jdb.command(JdbCommand.traceMethodExits(true, threadId));
        execCommand(JdbCommand.cont())
                .shouldContain("instance of JdbMethodExitTestTarg")
                .shouldContain("return value = 8");

        // Get out of bkpt back to the call to traceMethods
        jdb.command(JdbCommand.stepUp());


        jdb.command(JdbCommand.traceMethods(false, threadId));
        execCommand(JdbCommand.cont())
                .shouldContain("Method entered:");
        execCommand(JdbCommand.cont())
                .shouldContain("Method exited: return value = \"traceMethods\"");
        jdb.command(JdbCommand.stepUp());


        List<String> reply = new LinkedList<>();
        reply.addAll(jdb.command(JdbCommand.traceMethods(true, threadId)));
        reply.addAll(jdb.command(JdbCommand.cont()));
        reply.addAll(jdb.command(JdbCommand.cont()));
        reply.addAll(jdb.command(JdbCommand.cont()));
        new OutputAnalyzer(reply.stream().collect(Collectors.joining(lineSeparator)))
                .shouldContain("Method entered: \"thread=main\", JdbMethodExitTestTarg.traceMethods1")
                .shouldMatch("Method exited: .* JdbMethodExitTestTarg.traceMethods1");
        jdb.command(JdbCommand.untrace());
        jdb.command(JdbCommand.stepUp());


        reply.clear();
        reply.addAll(jdb.command(JdbCommand.traceMethodExits(false, threadId)));
        reply.addAll(jdb.command(JdbCommand.cont()));
        new OutputAnalyzer(reply.stream().collect(Collectors.joining(lineSeparator)))
                .shouldContain("Method exited: return value = \"traceExits\"");
        jdb.command(JdbCommand.untrace());
        jdb.command(JdbCommand.stepUp());


        reply.clear();
        reply.addAll(jdb.command(JdbCommand.traceMethodExits(true, threadId)));
        reply.addAll(jdb.command(JdbCommand.cont()));
        new OutputAnalyzer(reply.stream().collect(Collectors.joining(lineSeparator)))
                .shouldMatch("Method exited: .* JdbMethodExitTestTarg.traceExits1");
        jdb.command(JdbCommand.untrace());
        jdb.command(JdbCommand.stepUp());


        reply.clear();
        reply.addAll(jdb.command(JdbCommand.step()));   // step into traceExit()
        reply.addAll(jdb.command(JdbCommand.traceMethodExit(false, threadId)));
        reply.addAll(jdb.command(JdbCommand.cont()));
        new OutputAnalyzer(reply.stream().collect(Collectors.joining(lineSeparator)))
                .shouldContain("Method exited: return value = \"traceExit\"");
        jdb.command(JdbCommand.untrace());
        jdb.command(JdbCommand.stepUp());


        reply.clear();
        reply.addAll(jdb.command(JdbCommand.step()));
        reply.addAll(jdb.command(JdbCommand.step()));   // skip over setting return value in caller :-(
        reply.addAll(jdb.command(JdbCommand.traceMethodExit(true, threadId)));
        reply.addAll(jdb.command(JdbCommand.cont()));
        new OutputAnalyzer(reply.stream().collect(Collectors.joining(lineSeparator)))
                .shouldMatch("Method exited: .*JdbMethodExitTestTarg.traceExit1");

        new OutputAnalyzer(getJdbOutput())
                .shouldContain("Breakpoint hit");
    }
}
