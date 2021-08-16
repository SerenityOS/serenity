/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jvmci.errors;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.hotspot.HotSpotCompiledCode.Comment;
import jdk.vm.ci.hotspot.HotSpotCompiledNmethod;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCIBackend;
import org.junit.Assert;

import java.lang.reflect.Method;

public class CodeInstallerTest {

    protected final Architecture arch;
    protected final CodeCacheProvider codeCache;
    protected final MetaAccessProvider metaAccess;
    protected final HotSpotConstantReflectionProvider constantReflection;

    protected final HotSpotResolvedJavaMethod dummyMethod;

    public static void dummyMethod() {
    }

    protected CodeInstallerTest() {
        JVMCIBackend backend = JVMCI.getRuntime().getHostJVMCIBackend();
        metaAccess = backend.getMetaAccess();
        codeCache = backend.getCodeCache();
        constantReflection = (HotSpotConstantReflectionProvider) backend.getConstantReflection();
        arch = codeCache.getTarget().arch;

        Method method = null;
        try {
            method = CodeInstallerTest.class.getMethod("dummyMethod");
        } catch (NoSuchMethodException e) {
            Assert.fail();
        }

        dummyMethod = (HotSpotResolvedJavaMethod) metaAccess.lookupJavaMethod(method);
    }

    protected void installEmptyCode(Site[] sites, Assumption[] assumptions, Comment[] comments, int dataSectionAlignment, DataPatch[] dataSectionPatches, StackSlot deoptRescueSlot) {
        HotSpotCompiledCode code = new HotSpotCompiledNmethod("dummyMethod", new byte[0], 0, sites, assumptions, new ResolvedJavaMethod[]{dummyMethod}, comments, new byte[8], dataSectionAlignment,
                        dataSectionPatches, false, 0, deoptRescueSlot,
                        dummyMethod, 0, 1, 0L, false);
        codeCache.addCode(dummyMethod, code, null, null);
    }

    protected Register getRegister(PlatformKind kind, int index) {
        int idx = index;
        RegisterArray allRegs = arch.getAvailableValueRegisters();
        for (Register reg : allRegs) {
            if (arch.canStoreValue(reg.getRegisterCategory(), kind)) {
                if (idx-- == 0) {
                    return reg;
                }
            }
        }
        return null;
    }
}
