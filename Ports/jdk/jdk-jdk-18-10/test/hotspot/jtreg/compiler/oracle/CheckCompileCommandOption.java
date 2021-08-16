/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test CheckCompileCommandOption
 * @summary Checks parsing of -XX:CompileCommand=option
 * @bug 8055286 8056964 8059847 8069035
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires vm.debug == true
 * @run driver compiler.oracle.CheckCompileCommandOption
 */

package compiler.oracle;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.File;

public class CheckCompileCommandOption {

    // Currently, two types of trailing options can be used with
    // -XX:CompileCommand=option
    //
    // (1) CompileCommand=option,Klass::method,option
    // (2) CompileCommand=option,Klass::method,type,option,value
    //
    // Type (1) is used to enable a boolean option for a method.
    //
    // Type (2) is used to support options with a value. Values can
    // have the the following types: intx, uintx, bool, ccstr,
    // ccstrlist, and double.

    private static final String[][] FILE_ARGUMENTS = {
        {
            "-XX:CompileCommandFile=" + new File(System.getProperty("test.src", "."), "command1.txt"),
            "-version"
        },
        {
            "-XX:CompileCommandFile=" + new File(System.getProperty("test.src", "."), "command2.txt"),
            "-version"
        }
    };

    private static final String[][] FILE_EXPECTED_OUTPUT = {
        {
            "com/oracle/Test.test1 bool TestOptionBool = true",
            "com/oracle/Test.test2 bool TestOptionBool = true",
            "com/oracle/Test.test3 bool TestOptionBool = true",
            "com/oracle/Test.test4 bool TestOptionBool = true",
            "com/oracle/Test.test4 bool TestOptionBool2 = true",
            "com/oracle/Test.test5 bool TestOptionBool = true",
            "com/oracle/Test.test5 bool TestOptionBool2 = true",
            "com/oracle/Test.test6(I) bool TestOptionBool = true",
            "com/oracle/Test.test7(I) bool TestOptionBool = true",
            "com/oracle/Test.test8(I) bool TestOptionBool = true",
            "com/oracle/Test.test9(I) bool TestOptionBool = true",
            "com/oracle/Test.test9(I) bool TestOptionBool2 = true",
            "com/oracle/Test.test10(I) bool TestOptionBool = true",
            "com/oracle/Test.test10(I) bool TestOptionBool2 = true"
        },
        {
            "Test.test const char* TestOptionList = '_foo _bar'",
            "Test.test const char* TestOptionStr = '_foo'",
            "Test.test bool TestOptionBool = false",
            "Test.test intx TestOptionInt = -1",
            "Test.test uintx TestOptionUint = 1",
            "Test.test bool TestOptionBool2 = true",
            "Test.test double TestOptionDouble = 1.123000"
        }
    };

    private static final String[][] TYPE_1_ARGUMENTS = {
        {
            "-XX:CompileCommand=option,com/oracle/Test.test,TestOptionBool",
            "-XX:CompileCommand=option,com/oracle/Test,test,TestOptionBool2",
            "-XX:CompileCommand=option,com/oracle/Test.test2,TestOptionBool2,TestOptionBool",
            "-version"
        }
    };

    private static final String[][] TYPE_1_EXPECTED_OUTPUTS = {
        {
            "com/oracle/Test.test bool TestOptionBool = true",
            "com/oracle/Test.test bool TestOptionBool2 = true",
            "com/oracle/Test.test2 bool TestOptionBool = true",
            "com/oracle/Test.test2 bool TestOptionBool2 = true",
        }
    };

    private static final String[][] TYPE_2_ARGUMENTS = {
        {
            "-XX:CompileCommand=option,Test::test,ccstrlist,TestOptionList,_foo,_bar",
            "-XX:CompileCommand=option,Test::test,ccstr,TestOptionStr,_foo",
            "-XX:CompileCommand=option,Test::test,bool,TestOptionBool,false",
            "-XX:CompileCommand=option,Test::test,intx,TestOptionInt,-1",
            "-XX:CompileCommand=option,Test::test,uintx,TestOptionUint,1",
            "-XX:CompileCommand=option,Test::test,TestOptionBool2",
            "-XX:CompileCommand=option,Test::test,double,TestOptionDouble,1.123",
            "-XX:CompileCommand=option,Test.test2,double,TestOptionDouble,1.123",
            "-version"
        }
    };

    private static final String[][] TYPE_2_EXPECTED_OUTPUTS = {
        {
            "Test.test const char* TestOptionList = '_foo _bar'",
            "Test.test const char* TestOptionStr = '_foo'",
            "Test.test bool TestOptionBool = false",
            "Test.test intx TestOptionInt = -1",
            "Test.test uintx TestOptionUint = 1",
            "Test.test bool TestOptionBool2 = true",
            "Test.test double TestOptionDouble = 1.123000",
            "Test.test2 double TestOptionDouble = 1.123000"
        }
    };

    private static final String[][] TYPE_3_ARGUMENTS = {
        {
            "-XX:CompileCommand=option,Test::test,bool,TestOptionBool,false,intx,TestOptionInt,-1,uintx,TestOptionUint,1,TestOptionBool2,double,TestOptionDouble,1.123",
            "-version"
        }
    };

    private static final String[][] TYPE_3_EXPECTED_OUTPUTS = {
        {
            "Test.test bool TestOptionBool = false",
            "Test.test intx TestOptionInt = -1",
            "Test.test uintx TestOptionUint = 1",
            "Test.test bool TestOptionBool2 = true",
            "Test.test double TestOptionDouble = 1.123000"
        }
    };

    private static final String[][] TYPE_4_ARGUMENTS = {
        {
            "-XX:CompileCommand=TestOptionList,Test::test,_foo,_bar",
            "-XX:CompileCommand=TestOptionStr,Test::test,_foo",
            "-XX:CompileCommand=TestOptionBool,Test::test,false",
            "-XX:CompileCommand=TestOptionInt,Test::test,-1",
            "-XX:CompileCommand=TestOptionUint,Test::test,1",
            "-XX:CompileCommand=TestOptionBool2,Test::test",
            "-XX:CompileCommand=TestOptionDouble,Test::test,1.123",
            "-XX:CompileCommand=TestOptionDouble,Test.test2,1.123",
            "-version"
        }
    };

    private static final String[][] TYPE_4_EXPECTED_OUTPUTS = {
        {
            "CompileCommand: TestOptionList Test.test const char* TestOptionList = '_foo _bar'",
            "CompileCommand: TestOptionStr Test.test const char* TestOptionStr = '_foo'",
            "CompileCommand: TestOptionBool Test.test bool TestOptionBool = false",
            "CompileCommand: TestOptionInt Test.test intx TestOptionInt = -1",
            "CompileCommand: TestOptionUint Test.test uintx TestOptionUint = 1",
            "CompileCommand: TestOptionBool2 Test.test bool TestOptionBool2 = true",
            "CompileCommand: TestOptionDouble Test.test double TestOptionDouble = 1.123000",
            "CompileCommand: TestOptionDouble Test.test2 double TestOptionDouble = 1.123000"
        }
    };

    private static final String[][] TYPE_4_INVALID_ARGUMENTS = {
        {
            "-XX:CompileCommand=InvalidOption,Test::test,_foo,_bar",
            "-XX:CompileCommand=TestOptionInt,Test::test,_foo",
            "-XX:CompileCommand=TestOptionBool,Test::test,1",
            "-XX:CompileCommand=TestOptionDouble,Test::test,-1",
            "-XX:CompileCommand=TestOptionUint,Test::test",
            "-XX:CompileCommand=TestOptionBool2,Test::test,falsee",
            "-XX:CompileCommand=TestOptionDouble,Test::test,true",
            "-XX:CompileCommand=TestOptionDouble,Test.test2,1.f",
            "-version"
        }
    };

    private static final String[][] TYPE_4_INVALID_OUTPUTS = {
        {
            "Unrecognized option 'InvalidOption'",
            "Value cannot be read for option 'TestOptionInt' of type 'intx'",
            "Value cannot be read for option 'TestOptionBool' of type 'bool'",
            "Value cannot be read for option 'TestOptionDouble' of type 'double'",
            "Option 'TestOptionUint' is not followed by a value",
            "Value cannot be read for option 'TestOptionBool2' of type 'bool'",
            "Value cannot be read for option 'TestOptionDouble' of type 'double'",
            "Value cannot be read for option 'TestOptionDouble' of type 'double'"
        }
    };

    private static final String[][] TYPE_2_INVALID_ARGUMENTS = {
        {
            // bool flag name missing
            "-XX:CompileCommand=option,Test::test,bool",
            "-version"
        },
        {
            // bool flag value missing
            "-XX:CompileCommand=option,Test::test,bool,MyBoolOption",
            "-version"
        },
        {
            // wrong value for bool flag
            "-XX:CompileCommand=option,Test::test,bool,MyBoolOption,100",
            "-version"
        },
        {
            // intx flag name missing
            "-XX:CompileCommand=option,Test::test,bool,MyBoolOption,false,intx",
            "-version"
        },
        {
            // intx flag value missing
            "-XX:CompileCommand=option,Test::test,bool,MyBoolOption,false,intx,MyIntOption",
            "-version"
        },
        {
            // wrong value for intx flag
            "-XX:CompileCommand=option,Test::test,bool,MyBoolOption,false,intx,MyIntOption,true",
            "-version"
        },
        {
            // wrong value for flag double flag
            "-XX:CompileCommand=option,Test::test,double,MyDoubleOption,1",
            "-version"
        }
    };

    private static void verifyValidOption(String[] arguments, String[] expected_outputs) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder(arguments);
        out = new OutputAnalyzer(pb.start());

        for (String expected_output : expected_outputs) {
            out.shouldContain(expected_output);
        }

        out.shouldNotContain("CompileCommand: An error occurred during parsing");
        out.shouldHaveExitValue(0);
    }

    private static void verifyInvalidOption(String[] arguments) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder(arguments);
        out = new OutputAnalyzer(pb.start());

        out.shouldContain("CompileCommand: An error occurred during parsing");
        out.shouldHaveExitValue(0);
    }

    private static void verifyInvalidOption(String[] arguments, String[] expected_outputs) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder(arguments);
        out = new OutputAnalyzer(pb.start());

        for (String expected_output : expected_outputs) {
            out.shouldContain(expected_output);
        }

        out.shouldContain("CompileCommand: An error occurred during parsing");
        out.shouldHaveExitValue(0);

    }


    public static void main(String[] args) throws Exception {

        if (TYPE_1_ARGUMENTS.length != TYPE_1_EXPECTED_OUTPUTS.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments and expected outputs for type (1) options does not match.");
        }

        if (TYPE_2_ARGUMENTS.length != TYPE_2_EXPECTED_OUTPUTS.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments and expected outputs for type (2) options does not match.");
        }

        if (TYPE_3_ARGUMENTS.length != TYPE_3_EXPECTED_OUTPUTS.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments and expected outputs for type (3) options does not match.");
        }

        if (TYPE_4_ARGUMENTS.length != TYPE_4_EXPECTED_OUTPUTS.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments and expected outputs for type (4) options does not match.");
        }

        // Check if type (1) options are parsed correctly
        for (int i = 0; i < TYPE_1_ARGUMENTS.length; i++) {
            verifyValidOption(TYPE_1_ARGUMENTS[i], TYPE_1_EXPECTED_OUTPUTS[i]);
        }

        // Check if type (2) options are parsed correctly
        for (int i = 0; i < TYPE_2_ARGUMENTS.length; i++) {
            verifyValidOption(TYPE_2_ARGUMENTS[i], TYPE_2_EXPECTED_OUTPUTS[i]);
        }

        // Check if type (3) options are parsed correctly
        for (int i = 0; i < TYPE_3_ARGUMENTS.length; i++) {
            verifyValidOption(TYPE_3_ARGUMENTS[i], TYPE_3_EXPECTED_OUTPUTS[i]);
        }

        // Check if type (4) options are parsed correctly
        for (int i = 0; i < TYPE_4_ARGUMENTS.length; i++) {
            verifyValidOption(TYPE_4_ARGUMENTS[i], TYPE_4_EXPECTED_OUTPUTS[i]);
        }

        // Check if error is reported for invalid type (2) options
        // (flags with type information specified)
        for (String[] arguments: TYPE_2_INVALID_ARGUMENTS) {
            verifyInvalidOption(arguments);
        }

        // Check if error is reported for invalid type (2) options
        // (flags with type information specified)
        for (int i = 0; i < TYPE_4_INVALID_ARGUMENTS.length; i++) {
            verifyInvalidOption(TYPE_4_INVALID_ARGUMENTS[i], TYPE_4_INVALID_OUTPUTS[i]);
        }

        // Check if commands in command file are parsed correctly
        for (int i = 0; i < FILE_ARGUMENTS.length; i++) {
            verifyValidOption(FILE_ARGUMENTS[i], FILE_EXPECTED_OUTPUT[i]);
        }
    }
}
