/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase vm/mlvm/indy/func/jdi/breakpoint.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, quarantine]
 * VM Testbase comments: 8199578
 * VM Testbase readme:
 * DESCRIPTION
 *     Using JDI set a debugger breakpoint on invokedynamic instruction.
 *     Go few steps, obtaining various information from JVM.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build debuggee class
 * @build vm.mlvm.share.jdi.IndyDebuggee
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.indy.func.jdi.breakpoint.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm
 *      vm.mlvm.indy.func.jdi.breakpoint.Test
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugger.debuggeeClass vm.mlvm.share.jdi.IndyDebuggee
 */

package vm.mlvm.indy.func.jdi.breakpoint;

import vm.mlvm.share.jdi.ArgumentHandler;
import vm.mlvm.share.jdi.BreakpointInfo;
import vm.mlvm.share.jdi.JDIBreakpointTest;

import java.util.ArrayList;
import java.util.List;

public class Test extends JDIBreakpointTest {
    // indyWrapper:S8000,bootstrap,target,stop
    @Override
    protected List<BreakpointInfo> getBreakpoints(String debuggeeClassName) {
        List<BreakpointInfo> result = new ArrayList<>();
        {
            BreakpointInfo info = new BreakpointInfo("indyWrapper");
            info.stepsToTrace = 8000;
            result.add(info);
        }
        result.add(new BreakpointInfo("bootstrap"));
        result.add(new BreakpointInfo("target"));
        result.add(new BreakpointInfo("stop"));

        return result;
    }

    public static void main(String[] args) {
        launch(new ArgumentHandler(args));
    }
}
