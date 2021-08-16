/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaMethodProfile;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.TriState;

final class HotSpotProfilingInfo implements ProfilingInfo {

    private final HotSpotMethodData methodData;
    private final HotSpotResolvedJavaMethod method;

    private boolean isMature;
    private int position;
    private int hintPosition;
    private int hintBCI;
    private HotSpotMethodDataAccessor dataAccessor;

    private boolean includeNormal;
    private boolean includeOSR;

    HotSpotProfilingInfo(HotSpotMethodData methodData, HotSpotResolvedJavaMethod method, boolean includeNormal, boolean includeOSR) {
        this.methodData = methodData;
        this.method = method;
        this.includeNormal = includeNormal;
        this.includeOSR = includeOSR;
        this.isMature = methodData.isProfileMature();
        hintPosition = 0;
        hintBCI = -1;
    }

    @Override
    public int getCodeSize() {
        return method.getCodeSize();
    }

    public int getDecompileCount() {
        return methodData.getDecompileCount();
    }

    public int getOverflowRecompileCount() {
        return methodData.getOverflowRecompileCount();
    }

    public int getOverflowTrapCount() {
        return methodData.getOverflowTrapCount();
    }

    @Override
    public JavaTypeProfile getTypeProfile(int bci) {
        if (!isMature) {
            return null;
        }
        findBCI(bci, false);
        return dataAccessor.getTypeProfile(methodData, position);
    }

    @Override
    public JavaMethodProfile getMethodProfile(int bci) {
        if (!isMature) {
            return null;
        }
        findBCI(bci, false);
        return dataAccessor.getMethodProfile(methodData, position);
    }

    @Override
    public double getBranchTakenProbability(int bci) {
        if (!isMature) {
            return -1;
        }
        findBCI(bci, false);
        return dataAccessor.getBranchTakenProbability(methodData, position);
    }

    @Override
    public double[] getSwitchProbabilities(int bci) {
        if (!isMature) {
            return null;
        }
        findBCI(bci, false);
        return dataAccessor.getSwitchProbabilities(methodData, position);
    }

    @Override
    public TriState getExceptionSeen(int bci) {
        findBCI(bci, true);
        return dataAccessor.getExceptionSeen(methodData, position);
    }

    @Override
    public TriState getNullSeen(int bci) {
        findBCI(bci, false);
        return dataAccessor.getNullSeen(methodData, position);
    }

    @Override
    public int getExecutionCount(int bci) {
        if (!isMature) {
            return -1;
        }
        findBCI(bci, false);
        return dataAccessor.getExecutionCount(methodData, position);
    }

    @Override
    public int getDeoptimizationCount(DeoptimizationReason reason) {
        int count = 0;
        if (includeNormal) {
            count += methodData.getDeoptimizationCount(reason);
        }
        if (includeOSR) {
            count += methodData.getOSRDeoptimizationCount(reason);
        }
        return count;
    }

    private void findBCI(int targetBCI, boolean searchExtraData) {
        assert targetBCI >= 0 : "invalid BCI";

        if (methodData.hasNormalData()) {
            int currentPosition = targetBCI < hintBCI ? 0 : hintPosition;
            HotSpotMethodDataAccessor currentAccessor;
            while ((currentAccessor = methodData.getNormalData(currentPosition)) != null) {
                int currentBCI = currentAccessor.getBCI(methodData, currentPosition);
                if (currentBCI == targetBCI) {
                    normalDataFound(currentAccessor, currentPosition, currentBCI);
                    return;
                } else if (currentBCI > targetBCI) {
                    break;
                }
                currentPosition = currentPosition + currentAccessor.getSize(methodData, currentPosition);
            }
        }

        boolean exceptionPossiblyNotRecorded = false;
        if (searchExtraData && methodData.hasExtraData()) {
            int currentPosition = methodData.getExtraDataBeginOffset();
            HotSpotMethodDataAccessor currentAccessor;
            while ((currentAccessor = methodData.getExtraData(currentPosition)) != null) {
                int currentBCI = currentAccessor.getBCI(methodData, currentPosition);
                if (currentBCI == targetBCI) {
                    extraDataFound(currentAccessor, currentPosition);
                    return;
                }
                currentPosition = currentPosition + currentAccessor.getSize(methodData, currentPosition);
            }

            if (!methodData.isWithin(currentPosition)) {
                exceptionPossiblyNotRecorded = true;
            }
        }

        noDataFound(exceptionPossiblyNotRecorded);
    }

    private void normalDataFound(HotSpotMethodDataAccessor data, int pos, int bci) {
        setCurrentData(data, pos);
        this.hintPosition = position;
        this.hintBCI = bci;
    }

    private void extraDataFound(HotSpotMethodDataAccessor data, int pos) {
        setCurrentData(data, pos);
    }

    private void noDataFound(boolean exceptionPossiblyNotRecorded) {
        HotSpotMethodDataAccessor accessor = HotSpotMethodData.getNoDataAccessor(exceptionPossiblyNotRecorded);
        setCurrentData(accessor, -1);
    }

    private void setCurrentData(HotSpotMethodDataAccessor dataAccessor, int position) {
        this.dataAccessor = dataAccessor;
        this.position = position;
    }

    @Override
    public boolean isMature() {
        return isMature;
    }

    public void ignoreMature() {
        isMature = true;
    }

    @Override
    public String toString() {
        return "HotSpotProfilingInfo<" + this.toString(null, "; ") + ">";
    }

    @Override
    public void setMature() {
        isMature = true;
    }

    /**
     * {@code MethodData::_jvmci_ir_size} (currently) supports at most one JVMCI compiler IR type
     * which will be determined by the first JVMCI compiler that calls
     * {@link #setCompilerIRSize(Class, int)}.
     */
    private static volatile Class<?> supportedCompilerIRType;

    @Override
    public boolean setCompilerIRSize(Class<?> irType, int size) {
        if (supportedCompilerIRType == null) {
            synchronized (HotSpotProfilingInfo.class) {
                if (supportedCompilerIRType == null) {
                    supportedCompilerIRType = irType;
                }
            }
        }
        if (supportedCompilerIRType != irType) {
            return false;
        }
        methodData.setCompiledIRSize(size);
        return true;
    }

    @Override
    public int getCompilerIRSize(Class<?> irType) {
        if (irType == supportedCompilerIRType) {
            return methodData.getCompiledIRSize();
        }
        return -1;
    }
}
