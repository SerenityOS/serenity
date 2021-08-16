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
 * @key randomness
 *
 * @summary converted from VM Testbase vm/mlvm/meth/func/jdi/breakpointOtherStratum.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, fds, jdk, quarantine]
 * VM Testbase comments: 8208257 8208255
 * VM Testbase readme:
 * DESCRIPTION
 *     Performs debugging of invokedynamic call in vm.mlvm.share.jdi.INDIFY_Debuggee (with added
 *     source debug information) and verifies that JDI reports correct SDE locations.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build debuggee class
 * @build vm.mlvm.share.jdi.MHDebuggee
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.meth.func.jdi.breakpointOtherStratum.Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @comment recompile SDE_MHDebuggeeBase with Stratum annotation processor
 * @clean vm.mlvm.share.jpda.SDE_MHDebuggeeBase
 * @run driver
 *      vm.mlvm.share.StratumClassesBuilder
 *      vmTestbase/vm/mlvm/share/jpda/SDE_MHDebuggeeBase.java
 *
 * @run main/othervm
 *      vm.mlvm.meth.func.jdi.breakpointOtherStratum.Test
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -debugee.vmkeys="-cp ./bin/classes${path.separator}${test.class.path}"
 *      -transport.address=dynamic
 *      -debugger.debuggeeClass vm.mlvm.share.jdi.MHDebuggee
 */

package vm.mlvm.meth.func.jdi.breakpointOtherStratum;

import vm.mlvm.share.jdi.ArgumentHandler;
import vm.mlvm.share.jdi.BreakpointInfo;
import vm.mlvm.share.jdi.JDIBreakpointTest;
import vm.mlvm.share.jpda.StratumInfo;

import java.util.ArrayList;
import java.util.List;

public class Test extends JDIBreakpointTest {
    @Override
    protected List<BreakpointInfo> getBreakpoints(String debuggeeClassName) {
        List<BreakpointInfo> result = new ArrayList<>();
        // invokeMH:S100/Logo=SDE_MHDebuggeeBase.logo:2
        {
            BreakpointInfo info = new BreakpointInfo("invokeMH");
            info.stepsToTrace = 100;
            info.stratumInfo = new StratumInfo("Logo", "SDE_MHDebuggeeBase.logo", 2);
            result.add(info);
        }
        // invokePlain:S100/Logo=SDE_MHDebuggeeBase.logo:4
        {
            BreakpointInfo info = new BreakpointInfo("invokePlain");
            info.stepsToTrace = 100;
            info.stratumInfo = new StratumInfo("Logo", "SDE_MHDebuggeeBase.logo", 4);
            result.add(info);
        }
        // mhTarget/Logo=SDE_MHDebuggeeBase.logo:3
        {
            BreakpointInfo info = new BreakpointInfo("mhTarget");
            info.stratumInfo = new StratumInfo("Logo", "SDE_MHDebuggeeBase.logo", 3);
            result.add(info);
        }
        // plainTarget/Logo=SDE_MHDebuggeeBase.logo:5
        {
            BreakpointInfo info = new BreakpointInfo("plainTarget");
            info.stratumInfo = new StratumInfo("Logo", "SDE_MHDebuggeeBase.logo", 5);
            result.add(info);
        }
        // stop/Logo=SDE_MHDebuggeeBase.logo:6
        {
            BreakpointInfo info = new BreakpointInfo("stop");
            info.stratumInfo = new StratumInfo("Logo", "SDE_MHDebuggeeBase.logo", 6);
            result.add(info);
        }

        return result;
    }

    public static void main(String[] args) {
        launch(new ArgumentHandler(args));
    }
}
