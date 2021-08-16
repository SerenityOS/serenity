/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Implementation of {@link InstalledCode} for code installed as a {@code RuntimeStub}. The address
 * of the {@code RuntimeStub} is stored in {@link InstalledCode#address} and the value of
 * {@code RuntimeStub::entry_point()} is in {@link InstalledCode#entryPoint}.
 */
public class HotSpotRuntimeStub extends HotSpotInstalledCode {

    public HotSpotRuntimeStub(String name) {
        super(name);
    }

    public ResolvedJavaMethod getMethod() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void invalidate() {
    }

    @Override
    public String toString() {
        return String.format("InstalledRuntimeStub[stub=%s, codeBlob=0x%x]", name, getAddress());
    }

    @Override
    public Object executeVarargs(Object... args) throws InvalidInstalledCodeException {
        throw new InternalError("Cannot call stub " + name);
    }
}
