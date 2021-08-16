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

import jdk.vm.ci.meta.JavaMethodProfile;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.TriState;

/**
 * Base class for accessing the different kinds of data in a HotSpot {@code MethodData}. This is
 * similar to {@link ProfilingInfo}, but most methods require a {@link HotSpotMethodData} and the
 * exact position within the method data.
 */
abstract class HotSpotMethodDataAccessor {

    final int tag;
    final int staticSize;
    final HotSpotMethodData.VMState state;
    final HotSpotVMConfig config;

    protected HotSpotMethodDataAccessor(HotSpotMethodData.VMState state, int tag, int staticSize) {
        this.state = state;
        this.config = state.config;
        this.tag = tag;
        this.staticSize = staticSize;
    }

    /**
     * Returns the tag stored in the LayoutData header.
     *
     * @return tag stored in the LayoutData header
     */
    int getTag() {
        return tag;
    }

    static int readTag(HotSpotVMConfig config, HotSpotMethodData data, int position) {
        final int tag = data.readUnsignedByte(position, config.dataLayoutTagOffset);
        assert tag >= config.dataLayoutNoTag && tag <= config.dataLayoutSpeculativeTrapDataTag : "profile data tag out of bounds: " + tag;
        return tag;
    }

    /**
     * Returns the BCI stored in the LayoutData header.
     *
     * @return an integer between 0 and {@link Short#MAX_VALUE} inclusive, or -1 if not supported
     */
    int getBCI(HotSpotMethodData data, int position) {
        return data.readUnsignedShort(position, config.dataLayoutBCIOffset);
    }

    /**
     * Computes the size for the specific data at the given position.
     *
     * @return a value greater than 0
     */
    final int getSize(HotSpotMethodData data, int position) {
        int size = staticSize + getDynamicSize(data, position);
        // Sanity check against VM
        int vmSize = HotSpotJVMCIRuntime.runtime().compilerToVm.methodDataProfileDataSize(data.metaspaceMethodData, position);
        assert size == vmSize : size + " != " + vmSize;
        return size;
    }

    TriState getExceptionSeen(HotSpotMethodData data, int position) {
        final int exceptionsMask = 1 << config.bitDataExceptionSeenFlag;
        return TriState.get((getFlags(data, position) & exceptionsMask) != 0);
    }

    /**
     * @param data
     * @param position
     */
    JavaTypeProfile getTypeProfile(HotSpotMethodData data, int position) {
        return null;
    }

    /**
     * @param data
     * @param position
     */
    JavaMethodProfile getMethodProfile(HotSpotMethodData data, int position) {
        return null;
    }

    /**
     * @param data
     * @param position
     */
    double getBranchTakenProbability(HotSpotMethodData data, int position) {
        return -1;
    }

    /**
     * @param data
     * @param position
     */
    double[] getSwitchProbabilities(HotSpotMethodData data, int position) {
        return null;
    }

    /**
     * @param data
     * @param position
     */
    int getExecutionCount(HotSpotMethodData data, int position) {
        return -1;
    }

    /**
     * @param data
     * @param position
     */
    TriState getNullSeen(HotSpotMethodData data, int position) {
        return TriState.UNKNOWN;
    }

    protected int getFlags(HotSpotMethodData data, int position) {
        return data.readUnsignedByte(position, config.dataLayoutFlagsOffset);
    }

    /**
     * @param data
     * @param position
     */
    protected int getDynamicSize(HotSpotMethodData data, int position) {
        return 0;
    }

    abstract StringBuilder appendTo(StringBuilder sb, HotSpotMethodData data, int pos);

}
