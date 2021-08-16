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

import static jdk.vm.ci.hotspot.CompilerToVM.compilerToVM;

import jdk.vm.ci.code.InstalledCode;

/**
 * Implementation of {@link InstalledCode} for HotSpot representing a {@code CodeBlob}. The address
 * of the {@code CodeBlob} is stored in {@link InstalledCode#address}.
 */
public abstract class HotSpotInstalledCode extends InstalledCode {

    /**
     * Total size of the code blob (i.e. {@code CodeBlob::size()}).
     */
    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "field is set by the native part") private int size;

    /**
     * Start address of the code (i.e. {@code CodeBlob::code_begin()}).
     */
    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "field is set by the native part") private long codeStart;

    /**
     * Size of the code (i.e. {@code CodeBlob::code_size()}).
     */
    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "field is set by the native part") private int codeSize;

    public HotSpotInstalledCode(String name) {
        super(name);
    }

    /**
     * Gets the value of {@code CodeBlob::size()}.
     */
    public int getSize() {
        return size;
    }

    @Override
    public abstract String toString();

    /**
     * Gets the value of {@code CodeBlob::code_begin()} if {@linkplain #isValid() valid}, 0
     * otherwise.
     */
    @Override
    public long getStart() {
        return codeStart;
    }

    /**
     * Gets the value of {@code CodeBlob::code_size()} if {@linkplain #isValid() valid}, 0
     * otherwise.
     */
    public long getCodeSize() {
        return codeSize;
    }

    @Override
    public byte[] getCode() {
        return compilerToVM().getCode(this);
    }
}
