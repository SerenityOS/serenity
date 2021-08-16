/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Provides access to the profiling information of one specific method. Every accessor method
 * returns the information that is available at the time of invocation. If a method is invoked
 * multiple times, it may return significantly different results for every invocation as the
 * profiling information may be changed by other Java threads at any time.
 */
public interface ProfilingInfo {

    /**
     * Returns the length of the bytecodes associated with this profile.
     */
    int getCodeSize();

    /**
     * Returns an estimate of how often the branch at the given byte code was taken.
     *
     * @return The estimated probability, with 0.0 meaning never and 1.0 meaning always, or -1 if
     *         this information is not available.
     */
    double getBranchTakenProbability(int bci);

    /**
     * Returns an estimate of how often the switch cases are taken at the given BCI. The default
     * case is stored as the last entry.
     *
     * @return A double value that contains the estimated probabilities, with 0.0 meaning never and
     *         1.0 meaning always, or -1 if this information is not available.
     */
    double[] getSwitchProbabilities(int bci);

    /**
     * Returns the TypeProfile for the given BCI.
     *
     * @return Returns a JavaTypeProfile object, or null if not available.
     */
    JavaTypeProfile getTypeProfile(int bci);

    /**
     * Returns the MethodProfile for the given BCI.
     *
     * @return Returns a JavaMethodProfile object, or null if not available.
     */
    JavaMethodProfile getMethodProfile(int bci);

    /**
     * Returns information if the given BCI did ever throw an exception.
     *
     * @return {@link TriState#TRUE} if the instruction has thrown an exception at least once,
     *         {@link TriState#FALSE} if it never threw an exception, and {@link TriState#UNKNOWN}
     *         if this information was not recorded.
     */
    TriState getExceptionSeen(int bci);

    /**
     * Returns information if null was ever seen for the given BCI. This information is collected
     * for the aastore, checkcast and instanceof bytecodes.
     *
     * @return {@link TriState#TRUE} if null was seen for the instruction, {@link TriState#FALSE} if
     *         null was NOT seen, and {@link TriState#UNKNOWN} if this information was not recorded.
     */
    TriState getNullSeen(int bci);

    /**
     * Returns an estimate how often the current BCI was executed. Avoid comparing execution counts
     * to each other, as the returned value highly depends on the time of invocation.
     *
     * @return the estimated execution count or -1 if not available.
     */
    int getExecutionCount(int bci);

    /**
     * Returns how frequently a method was deoptimized for the given deoptimization reason. This
     * only indicates how often the method did fall back to the interpreter for the execution and
     * does not indicate how often it was recompiled.
     *
     * @param reason the reason for which the number of deoptimizations should be queried
     * @return the number of times the compiled method deoptimized for the given reason.
     */
    int getDeoptimizationCount(DeoptimizationReason reason);

    /**
     * Records the size of the compiler intermediate representation (IR) associated with this
     * method.
     *
     * @param irType the IR type for which the size is being recorded
     * @param irSize the IR size to be recorded. The unit depends on the IR.
     * @return whether recording this information for {@code irType} is supported
     */
    boolean setCompilerIRSize(Class<?> irType, int irSize);

    /**
     * Gets the size of the compiler intermediate representation (IR) associated with this method
     * last recorded by {@link #setCompilerIRSize(Class, int)}.
     *
     * @param irType the IR type for which the size is being requested
     * @return the requested IR size or -1 if it is unavailable for {@code irType}
     */
    int getCompilerIRSize(Class<?> irType);

    /**
     * Returns true if the profiling information can be assumed as sufficiently accurate.
     *
     * @return true if the profiling information was recorded often enough mature enough, false
     *         otherwise.
     */
    boolean isMature();

    /**
     * Force data to be treated as mature if possible.
     */
    void setMature();

    /**
     * Formats this profiling information to a string.
     *
     * @param method an optional method that augments the profile string returned
     * @param sep the separator to use for each separate profile record
     */
    default String toString(ResolvedJavaMethod method, String sep) {
        StringBuilder buf = new StringBuilder(100);
        if (method != null) {
            buf.append(String.format("canBeStaticallyBound: %b%s", method.canBeStaticallyBound(), sep));
        }
        for (int i = 0; i < getCodeSize(); i++) {
            if (getExecutionCount(i) != -1) {
                buf.append(String.format("executionCount@%d: %d%s", i, getExecutionCount(i), sep));
            }

            if (getBranchTakenProbability(i) != -1) {
                buf.append(String.format("branchProbability@%d: %.6f%s", i, getBranchTakenProbability(i), sep));
            }

            double[] switchProbabilities = getSwitchProbabilities(i);
            if (switchProbabilities != null) {
                buf.append(String.format("switchProbabilities@%d:", i));
                for (int j = 0; j < switchProbabilities.length; j++) {
                    buf.append(String.format(" %.6f", switchProbabilities[j]));
                }
                buf.append(sep);
            }

            if (getExceptionSeen(i) != TriState.UNKNOWN) {
                buf.append(String.format("exceptionSeen@%d: %s%s", i, getExceptionSeen(i).name(), sep));
            }

            if (getNullSeen(i) != TriState.UNKNOWN) {
                buf.append(String.format("nullSeen@%d: %s%s", i, getNullSeen(i).name(), sep));
            }

            JavaTypeProfile typeProfile = getTypeProfile(i);
            MetaUtil.appendProfile(buf, typeProfile, i, "types", sep);

            JavaMethodProfile methodProfile = getMethodProfile(i);
            MetaUtil.appendProfile(buf, methodProfile, i, "methods", sep);
        }

        boolean firstDeoptReason = true;
        for (DeoptimizationReason reason : DeoptimizationReason.values()) {
            int count = getDeoptimizationCount(reason);
            if (count > 0) {
                if (firstDeoptReason) {
                    buf.append("deoptimization history").append(sep);
                    firstDeoptReason = false;
                }
                buf.append(String.format(" %s: %d%s", reason.name(), count, sep));
            }
        }
        if (buf.length() == 0) {
            return "";
        }
        String s = buf.toString();
        return s.substring(0, s.length() - sep.length());
    }

}
