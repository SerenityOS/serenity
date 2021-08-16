/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import java.util.Arrays;

import jdk.vm.ci.code.stack.InspectedFrame;
import jdk.vm.ci.meta.ResolvedJavaMethod;

public class HotSpotStackFrameReference implements InspectedFrame {

    private CompilerToVM compilerToVM;
    // set in the VM when materializeVirtualObjects is called
    @SuppressWarnings("unused") private boolean objectsMaterialized;

    // information used to find the stack frame
    private long stackPointer;
    private int frameNumber;

    // information about the stack frame's contents
    private int bci;
    private HotSpotResolvedJavaMethod method;
    private Object[] locals;
    private boolean[] localIsVirtual;

    public long getStackPointer() {
        return stackPointer;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    @Override
    public Object getLocal(int index) {
        return locals[index];
    }

    @Override
    public boolean isVirtual(int index) {
        return localIsVirtual == null ? false : localIsVirtual[index];
    }

    @Override
    public void materializeVirtualObjects(boolean invalidateCode) {
        compilerToVM.materializeVirtualObjects(this, invalidateCode);
    }

    @Override
    public int getBytecodeIndex() {
        return bci;
    }

    @Override
    public ResolvedJavaMethod getMethod() {
        return method;
    }

    @Override
    public boolean isMethod(ResolvedJavaMethod otherMethod) {
        return method.equals(otherMethod);
    }

    @Override
    public boolean hasVirtualObjects() {
        return localIsVirtual != null;
    }

    @Override
    public String toString() {
        return "HotSpotStackFrameReference [stackPointer=" + stackPointer + ", frameNumber=" + frameNumber + ", bci=" + bci + ", method=" + getMethod() + ", locals=" + Arrays.toString(locals) +
                        ", localIsVirtual=" + Arrays.toString(localIsVirtual) + "]";
    }
}
