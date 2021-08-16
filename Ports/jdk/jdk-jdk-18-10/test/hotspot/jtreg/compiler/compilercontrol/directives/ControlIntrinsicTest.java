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
 * @bug 8247732
 * @summary Tests  -XX:CompilerDirectivesFile=directives.json
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver compiler.compilercontrol.directives.ControlIntrinsicTest
 */

package compiler.compilercontrol.directives;

import compiler.compilercontrol.share.IntrinsicCommand;
import compiler.compilercontrol.share.IntrinsicCommand.IntrinsicId;
import compiler.compilercontrol.share.scenario.Scenario;

public class ControlIntrinsicTest {
    public static void main(String[] args) {
        IntrinsicId ids[] = new IntrinsicId[3];

        ids[0] = new IntrinsicId("_newArray", true);
        ids[1] = new IntrinsicId("_minF", false);
        ids[2] = new IntrinsicId("_copyOf", true);
        new IntrinsicCommand(Scenario.Type.DIRECTIVE, ids).test();

        // invalid compileCommands, hotspot exits with non-zero retval
        ids[0] = new IntrinsicId("brokenIntrinsic", true);
        ids[1] = new IntrinsicId("invalidIntrinsic", false);
        new IntrinsicCommand(Scenario.Type.DIRECTIVE, ids).test();
    }
}
