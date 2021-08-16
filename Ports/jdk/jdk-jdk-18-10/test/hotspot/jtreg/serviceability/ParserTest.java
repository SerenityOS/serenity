/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that the diagnostic command arguemnt parser works
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI ParserTest
 */

import java.math.BigInteger;

import jdk.test.whitebox.parser.DiagnosticCommand;
import jdk.test.whitebox.parser.DiagnosticCommand.DiagnosticArgumentType;
import sun.hotspot.WhiteBox;

public class ParserTest {
    WhiteBox wb;

    public ParserTest() throws Exception {
        wb = WhiteBox.getWhiteBox();

        testNanoTime();
        testJLong();
        testBool();
        testQuotes();
        testMemorySize();
        testSingleLetterArg();
    }

    public static void main(String... args) throws Exception  {
         new ParserTest();
    }

    public void testNanoTime() throws Exception {
        String name = "name";
        DiagnosticCommand arg = new DiagnosticCommand(name,
                "desc", DiagnosticArgumentType.NANOTIME,
                false, "0");
        DiagnosticCommand[] args = {arg};

        BigInteger bi = new BigInteger("7");
        //These should work
        parse(name, bi.toString(), name + "=7ns", args);

        bi = bi.multiply(BigInteger.valueOf(1000));
        parse(name, bi.toString(), name + "=7us", args);

        bi = bi.multiply(BigInteger.valueOf(1000));
        parse(name, bi.toString(), name + "=7ms", args);

        bi = bi.multiply(BigInteger.valueOf(1000));
        parse(name, bi.toString(), name + "=7s", args);

        bi = bi.multiply(BigInteger.valueOf(60));
        parse(name, bi.toString() , name + "=7m", args);

        bi = bi.multiply(BigInteger.valueOf(60));
        parse(name, bi.toString() , name + "=7h", args);

        bi = bi.multiply(BigInteger.valueOf(24));
        parse(name, bi.toString() , name + "=7d", args);

        parse(name, "0", name + "=0", args);

        shouldFail(name + "=7xs", args);
        shouldFail(name + "=7mms", args);
        shouldFail(name + "=7f", args);
        //Currently, only value 0 is allowed without unit
        shouldFail(name + "=7", args);
    }

    public void testJLong() throws Exception {
        String name = "name";
        DiagnosticCommand arg = new DiagnosticCommand(name,
                "desc", DiagnosticArgumentType.JLONG,
                false, "0");
        DiagnosticCommand[] args = {arg};

        wb.parseCommandLine(name + "=10", ',', args);
        parse(name, "10", name + "=10", args);
        parse(name, "-5", name + "=-5", args);

        //shouldFail(name + "=12m", args); <-- should fail, doesn't
    }

    public void testBool() throws Exception {
        String name = "name";
        DiagnosticCommand arg = new DiagnosticCommand(name,
                "desc", DiagnosticArgumentType.BOOLEAN,
                false, "false");
        DiagnosticCommand[] args = {arg};

        parse(name, "true", name + "=true", args);
        parse(name, "false", name + "=false", args);
        parse(name, "true", name, args);

        //Empty commandline to parse, tests default value
        //of the parameter "name"
        parse(name, "false", "", args);
    }

    public void testQuotes() throws Exception {
        String name = "name";
        DiagnosticCommand arg1 = new DiagnosticCommand(name,
                "desc", DiagnosticArgumentType.STRING,
                false, null);
        DiagnosticCommand arg2 = new DiagnosticCommand("arg",
                "desc", DiagnosticArgumentType.STRING,
                false, null);
        DiagnosticCommand[] args = {arg1, arg2};

        // try with a quoted value
        parse(name, "Recording 1", name + "=\"Recording 1\"", args);
        // try with a quoted argument
        parse(name, "myrec", "\"" + name + "\"" + "=myrec", args);
        // try with both a quoted value and a quoted argument
        parse(name, "Recording 1", "\"" + name + "\"" + "=\"Recording 1\"", args);

        // now the same thing but with other arguments after

        // try with a quoted value
        parse(name, "Recording 1", name + "=\"Recording 1\",arg=value", args);
        // try with a quoted argument
        parse(name, "myrec", "\"" + name + "\"" + "=myrec,arg=value", args);
        // try with both a quoted value and a quoted argument
        parse(name, "Recording 1", "\"" + name + "\"" + "=\"Recording 1\",arg=value", args);
    }

    public void testSingleLetterArg() throws Exception {
        DiagnosticCommand[] args = new DiagnosticCommand[]{
            new DiagnosticCommand("flag", "desc", DiagnosticArgumentType.STRING, true, false, null),
            new DiagnosticCommand("value", "desc", DiagnosticArgumentType.STRING, true, false, null)
        };
        parse("flag", "flag", "flag v", ' ', args);
        parse("value", "v", "flag v", ' ', args);
    }

    public void testMemorySize() throws Exception {
        String name = "name";
        String defaultValue = "1024";
        DiagnosticCommand arg = new DiagnosticCommand(name,
                "desc", DiagnosticArgumentType.MEMORYSIZE,
                false, defaultValue);
        DiagnosticCommand[] args = {arg};

        BigInteger bi = new BigInteger("7");
        parse(name, bi.toString(), name + "=7b", args);

        bi = bi.multiply(BigInteger.valueOf(1024));
        parse(name, bi.toString(), name + "=7k", args);

        bi = bi.multiply(BigInteger.valueOf(1024));
        parse(name, bi.toString(), name + "=7m", args);

        bi = bi.multiply(BigInteger.valueOf(1024));
        parse(name, bi.toString(), name + "=7g", args);
        parse(name, defaultValue, "", args);

        //shouldFail(name + "=7gg", args); <---- should fail, doesn't
        //shouldFail(name + "=7t", args);  <----- should fail, doesn't
    }

    public void parse(String searchName, String expectedValue,
            String cmdLine, DiagnosticCommand[] argumentTypes) throws Exception {
        parse(searchName, expectedValue, cmdLine, ',', argumentTypes);
    }
    public void parse(String searchName, String expectedValue,
            String cmdLine, char delim, DiagnosticCommand[] argumentTypes) throws Exception {
        //parseCommandLine will return an object array that looks like
        //{<name of parsed object>, <of parsed object> ... }
        Object[] res = wb.parseCommandLine(cmdLine, delim, argumentTypes);
        for (int i = 0; i < res.length-1; i+=2) {
            String parsedName = (String) res[i];
            if (searchName.equals(parsedName)) {
                String parsedValue = (String) res[i+1];
                if (expectedValue.equals(parsedValue)) {
                    return;
                } else {
                    throw new Exception("Parsing of cmdline '" + cmdLine + "' failed!\n"
                            + searchName + " parsed as " + parsedValue
                            + "! Expected: " + expectedValue);
                }
            }
        }
        throw new Exception(searchName + " not found as a parsed Argument!");
    }

    private void shouldFail(String argument, DiagnosticCommand[] argumentTypes) throws Exception {
        shouldFail(argument, ',', argumentTypes);
    }
    private void shouldFail(String argument, char delim, DiagnosticCommand[] argumentTypes) throws Exception {
        try {
            wb.parseCommandLine(argument, delim, argumentTypes);
            throw new Exception("Parser accepted argument: " + argument);
        } catch (IllegalArgumentException e) {
            //expected
        }
    }
}
