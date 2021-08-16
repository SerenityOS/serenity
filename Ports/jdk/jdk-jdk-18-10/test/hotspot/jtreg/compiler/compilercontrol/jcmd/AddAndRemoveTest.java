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
 * @summary Tests directives to be able to add and remove directives
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver compiler.compilercontrol.jcmd.AddAndRemoveTest
 */

package compiler.compilercontrol.jcmd;

import compiler.compilercontrol.share.AbstractTestBase;
import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.scenario.Command;
import compiler.compilercontrol.share.scenario.CompileCommand;
import compiler.compilercontrol.share.scenario.JcmdCommand;
import compiler.compilercontrol.share.scenario.Scenario;
import jdk.test.lib.Utils;

import java.lang.reflect.Executable;

public class AddAndRemoveTest extends AbstractTestBase {
    private static final int AMOUNT = Integer.getInteger(
            "compiler.compilercontrol.jcmd.AddAndRemoveTest.amount", 10);

    public static void main(String[] args) {
        new AddAndRemoveTest().test();
    }

    @Override
    public void test() {
        Scenario.Builder builder = Scenario.getBuilder();
        // Add some commands with JCMD
        for (int i = 0; i < AMOUNT; i++) {
            Executable exec = Utils.getRandomElement(METHODS).first;
            MethodDescriptor md = getValidMethodDescriptor(exec);
            CompileCommand compileCommand = new JcmdCommand(Command.COMPILEONLY,
                    md, null, Scenario.Type.JCMD, Scenario.JcmdType.ADD);
            builder.add(compileCommand);
        }
        // Remove half of them
        for (int i = 0; i < AMOUNT / 2; i++) {
            /* remove jcmd command doesn't need method, compiler etc.
               command will be ignored */
            builder.add(new JcmdCommand(Command.NONEXISTENT, null, null,
                    Scenario.Type.JCMD, Scenario.JcmdType.REMOVE));
        }
        Scenario scenario = builder.build();
        scenario.execute();
    }
}
