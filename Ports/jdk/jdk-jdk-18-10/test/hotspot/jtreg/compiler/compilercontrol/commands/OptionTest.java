/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
 * @bug 8257800
 * @summary Tests CompileCommand=option,package/class,ccstrlist,ControlIntrinsic,+_id
 * @library /test/lib /
 *
 * @run driver compiler.compilercontrol.commands.OptionTest
 */

package compiler.compilercontrol.commands;

import jdk.test.lib.process.ProcessTools;

public class OptionTest {
    public static void main(String[] args) throws Exception {
        ProcessTools.executeTestJvm("-XX:CompileCommand=option,package/class,ccstrlist,ControlIntrinsic,+_id", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("CompileCommand: An error occurred during parsing")
                    .shouldContain("Error: Did not specify any method name")
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");

        ProcessTools.executeTestJvm("-XX:CompileCommand=option,*,ccstrlist,ControlIntrinsic,+_id", "-version")
                    .shouldHaveExitValue(0)
                    .shouldContain("CompileCommand: An error occurred during parsing")
                    .shouldContain("Error: Did not specify any method name")
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");

        // corner case:
        // ccstrlist could be a valid method name, so it is accepted in the well-formed case.
        ProcessTools.executeTestJvm("-XX:CompileCommand=option,*.ccstrlist,ccstrlist,ControlIntrinsic,+_id", "-version")
                    .shouldContain("CompileCommand: ControlIntrinsic *.ccstrlist const char* ControlIntrinsic")
                    .shouldHaveExitValue(0)
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");

        ProcessTools.executeTestJvm("-XX:CompileCommand=option,*.*,ccstrlist,ControlIntrinsic,+_id", "-version")
                    .shouldContain("CompileCommand: ControlIntrinsic *.* const char* ControlIntrinsic")
                    .shouldHaveExitValue(0)
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");

        ProcessTools.executeTestJvm("-XX:CompileCommand=option,class,PrintIntrinsics", "-version")
                    .shouldHaveExitValue(0)
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");

        // corner case:
        // PrintIntrinsics could be a valid method name, so it is accepted in the well-formed case.
        ProcessTools.executeTestJvm("-XX:CompileCommand=option,class.PrintIntrinsics,PrintIntrinsics", "-version")
                    .shouldContain("CompileCommand: PrintIntrinsics class.PrintIntrinsics bool PrintIntrinsics = true")
                    .shouldHaveExitValue(0)
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");

        // corner case:
        // _dontinline_* is a valid method pattern, so it should be accepted
        ProcessTools.executeTestJvm("-XX:CompileCommand=dontinline,*::dontinline_*", "-version")
                    .shouldContain("CompileCommand: dontinline *.dontinline_* bool dontinline = true")
                    .shouldHaveExitValue(0)
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");

        ProcessTools.executeTestJvm("-XX:CompileCommand=dontinline,*.dontinline", "-version")
                    .shouldContain("CompileCommand: dontinline *.dontinline bool dontinline = true")
                    .shouldHaveExitValue(0)
                    .shouldNotContain("Error: Did not specify any method name")
                    .shouldNotContain("# A fatal error has been detected by the Java Runtime Environment");
    }
}
