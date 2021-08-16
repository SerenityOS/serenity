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
package jdk.vm.ci.meta;

/**
 * An implementation of {@link ProfilingInfo} that can used in the absence of real profile
 * information.
 */
public final class DefaultProfilingInfo implements ProfilingInfo {

    private static final ProfilingInfo[] NO_PROFILING_INFO = new ProfilingInfo[]{new DefaultProfilingInfo(TriState.TRUE), new DefaultProfilingInfo(TriState.FALSE),
                    new DefaultProfilingInfo(TriState.UNKNOWN)};

    private final TriState exceptionSeen;

    DefaultProfilingInfo(TriState exceptionSeen) {
        this.exceptionSeen = exceptionSeen;
    }

    @Override
    public int getCodeSize() {
        return 0;
    }

    @Override
    public JavaTypeProfile getTypeProfile(int bci) {
        return null;
    }

    @Override
    public JavaMethodProfile getMethodProfile(int bci) {
        return null;
    }

    @Override
    public double getBranchTakenProbability(int bci) {
        return -1;
    }

    @Override
    public double[] getSwitchProbabilities(int bci) {
        return null;
    }

    @Override
    public TriState getExceptionSeen(int bci) {
        return exceptionSeen;
    }

    @Override
    public TriState getNullSeen(int bci) {
        return TriState.UNKNOWN;
    }

    @Override
    public int getExecutionCount(int bci) {
        return -1;
    }

    public static ProfilingInfo get(TriState exceptionSeen) {
        return NO_PROFILING_INFO[exceptionSeen.ordinal()];
    }

    @Override
    public int getDeoptimizationCount(DeoptimizationReason reason) {
        return 0;
    }

    @Override
    public boolean isMature() {
        return false;
    }

    @Override
    public String toString() {
        return "DefaultProfilingInfo<" + this.toString(null, "; ") + ">";
    }

    @Override
    public void setMature() {
        // Do nothing
    }

    @Override
    public boolean setCompilerIRSize(Class<?> irType, int nodeCount) {
        return false;
    }

    @Override
    public int getCompilerIRSize(Class<?> irType) {
        return -1;
    }
}
