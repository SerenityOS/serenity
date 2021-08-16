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

/*
 * @test
 * @bug 4511950 4843082
 * @summary 1. jdb's expression evaluation doesn't perform string conversion properly
 *          2. TTY: run on expression evaluation
 * @comment converted from test/jdk/com/sun/jdi/StringConvertTest.sh
 *
 * @library /test/lib
 * @compile -g StringConvertTest.java
 * @run main/othervm StringConvertTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

class StringConvertTarg {
    String me;
    static JJ1 x1;
    static JJ2 x2;
    static JJ2[] x3 = new JJ2[2];
    static String x4 = "abc";
    static int ii = 89;
    static String grower = "grower";
    static StringBuffer sbGrower = new StringBuffer("sbGrower");
    int ivar = 89;
    StringConvertTarg(String xx) {
        me = xx;
    }

    static String fred() {
        return "a static method";
    }

    void  gus() {
        int gusLoc = 1;
        StringBuffer sbTim = new StringBuffer("tim");
        int kk = 1;                          //@1 breakpoint
    }

    static String growit(String extra) {
        grower += extra;
        return grower;
    }

    static String sbGrowit(String extra) {
        sbGrower.append(extra);
        return sbGrower.toString();
    }

    public static void main(String[] args) {
        x1 = new JJ1("first JJ1");
        x2 = new JJ2("first JJ2");
        x3[0] = new JJ2("array0");
        x3[1] = new JJ2("array1");
        StringConvertTarg loc1 = new StringConvertTarg("first me");

        // These just show what output should look like
        System.out.println("x1 = " + x1);
        System.out.println("x2 = " + x2);
        System.out.println("x3.toString = " + x3.toString());
        System.out.println("x4.toString = " + x4.toString());

        // Dont want to call growit since it would change
        // the value.

        System.out.println("loc1 = " + loc1);
        System.out.println("-" + loc1);
        loc1.gus();
     }

    // This does not have a toString method
    static class JJ1 {
        String me;

        JJ1(String whoAmI) {
            me = whoAmI;
        }
    }

    // This has a toString method
    static class JJ2 {
        String me;

        JJ2(String whoAmI) {
            me = whoAmI;
        }
        public String toString() {
            return me;
        }

        public int meth1() {
            return 89;
        }
    }
}

public class StringConvertTest extends JdbTest {
    public static void main(String argv[]) {
        new StringConvertTest().run();
    }

    private StringConvertTest() {
        super(DEBUGGEE_CLASS, SOURCE_FILE);
    }

    private static final String DEBUGGEE_CLASS = StringConvertTarg.class.getName();
    private static final String SOURCE_FILE = "StringConvertTest.java";

    @Override
    protected void runCases() {
        setBreakpoints(1);
        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        // Each print without the 'toString()' should print the
        // same thing as the following print with the toString().
        // The "print 1"s are just spacers
        jdb.command(JdbCommand.print("StringConvertTarg.x1"));
        jdb.command(JdbCommand.print("StringConvertTarg.x1.toString()"));
        jdb.command(JdbCommand.print("1"));

        jdb.command(JdbCommand.print("StringConvertTarg.x2"));
        jdb.command(JdbCommand.print("StringConvertTarg.x2.toString()"));
        jdb.command(JdbCommand.print("1"));

        // arrays is a special case.
        // StringConvertTarg prints:
        //      x3.toString = [LStringConvertTarg$JJ2;@61443d8f
        // jdb "print ((Object)StringConvertTarg.x3).toString()" prints:
        //      com.sun.tools.example.debug.expr.ParseException:
        //              No instance field or method with the name toString in StringConvertTarg$JJ2[]
        //      ((Object)StringConvertTarg.x3).toString() = null
        // jdb "print (Object)(StringConvertTarg.x3)" prints:
        //      (Object)(StringConvertTarg.x3) = instance of StringConvertTarg$JJ2[2] (id=624)
        /*
        jdb.command(JdbCommand.print("(Object)(StringConvertTarg.x3)"));
        jdb.command(JdbCommand.print("((Object)StringConvertTarg.x3).toString()"));
        jdb.command(JdbCommand.print("1"));
        */

        jdb.command(JdbCommand.print("StringConvertTarg.x4"));
        jdb.command(JdbCommand.print("StringConvertTarg.x4.toString()"));
        jdb.command(JdbCommand.print("1"));

        // Make sure jdb doesn't call a method multiple times.
        jdb.command(JdbCommand.print("StringConvertTarg.growit(\"xyz\")"));
        jdb.command(JdbCommand.eval("StringConvertTarg.sbGrower.append(\"xyz\")"));
        jdb.command(JdbCommand.print("1"));

        jdb.command(JdbCommand.eval("sbTim.toString()"));
        jdb.command(JdbCommand.print("1"));

        jdb.command(JdbCommand.print("this"));
        jdb.command(JdbCommand.print("this.toString()"));
        jdb.command(JdbCommand.print("1"));

        // A possible bug is that this ends up with multiple "s
        jdb.command(JdbCommand.print("\"--\"StringConvertTarg.x1"));
        jdb.command(JdbCommand.print("1"));

        // This too
        jdb.command(JdbCommand.print("StringConvertTarg.x4 + 2"));
        jdb.command(JdbCommand.print("1"));

        jdb.command(JdbCommand.print("this.ivar"));
        jdb.command(JdbCommand.print("gusLoc"));
        jdb.command(JdbCommand.print("1"));

        new OutputAnalyzer(jdb.getJdbOutput())
                .shouldNotContain("\"\"")
                .shouldNotContain("instance of")
                .shouldNotContain("xyzxyz");
    }
}
