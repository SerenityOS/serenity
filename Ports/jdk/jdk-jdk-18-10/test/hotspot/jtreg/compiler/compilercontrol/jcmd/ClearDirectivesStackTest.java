/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8137167
 * @summary Tests clear JCMD command
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver compiler.compilercontrol.jcmd.ClearDirectivesStackTest
 */

package compiler.compilercontrol.jcmd;

import compiler.compilercontrol.share.AbstractTestBase;
import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.scenario.Command;
import compiler.compilercontrol.share.scenario.CommandGenerator;
import compiler.compilercontrol.share.scenario.CompileCommand;
import compiler.compilercontrol.share.scenario.JcmdCommand;
import compiler.compilercontrol.share.scenario.Scenario;
import jdk.test.lib.Utils;

import java.lang.reflect.Executable;

public class ClearDirectivesStackTest extends AbstractTestBase {
    private static final int AMOUNT = Utils.getRandomInstance().nextInt(100);
    private final CommandGenerator cmdGen = new CommandGenerator();

    public static void main(String[] args) {
        new ClearDirectivesStackTest().test();
    }

    @Override
    public void test() {
        Scenario.Builder builder = Scenario.getBuilder();
        // Add some commands with JCMD
        for (int i = 0; i < AMOUNT; i++) {
            Executable exec = Utils.getRandomElement(METHODS).first;
            MethodDescriptor methodDescriptor = getValidMethodDescriptor(exec);
            CompileCommand compileCommand = new JcmdCommand(
                    cmdGen.generateCommand(), methodDescriptor,
                    cmdGen.generateCompiler(), Scenario.Type.JCMD,
                    Scenario.JcmdType.ADD);
            builder.add(compileCommand);
        }
        // clear the stack
        builder.add(new JcmdCommand(Command.NONEXISTENT, null, null,
                Scenario.Type.JCMD, Scenario.JcmdType.CLEAR));
        // print all directives after the clear
        builder.add(new JcmdCommand(Command.NONEXISTENT, null, null,
                Scenario.Type.JCMD, Scenario.JcmdType.PRINT));
        Scenario scenario = builder.build();
        scenario.execute();
    }
}
