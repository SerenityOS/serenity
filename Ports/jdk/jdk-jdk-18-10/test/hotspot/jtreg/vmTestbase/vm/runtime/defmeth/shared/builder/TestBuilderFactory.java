/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.builder;

import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.ExecutionMode;
import vm.share.options.Option;

/**
 * Factory for TestBuilder instances.
 *
 * Parameterizes TestBuilder with some specific configuration properties.
 * It is used to run the very same set of tests in different configurations.
 * TestBuilderFactory.getBuilder() is called from individual tests to get
 * TestBuilder instance for test construction.
 */
public class TestBuilderFactory {
    @Option(name="ver", description="class file major version", default_value="52")
    private int minMajorVer;

    @Option(name="flags", description="additional access flags on default methods", default_value="0")
    private int accFlags;

    @Option(name="redefine", description="redefine classes during execution", default_value="false")
    private boolean redefineClasses;

    @Option(name="retransform", description="retransform classes during execution", default_value="false")
    private boolean retransformClasses;

    @Option(name="execMode", description="override execution mode with a concrete mode (DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY)", default_value="")
    private String modeName;

    private final DefMethTest testInstance;

    // Default construction is used when the instance will be configured
    // by OptionSupport.setup*
    public TestBuilderFactory(DefMethTest testInstance) {
        this.testInstance = testInstance;
    }

    public TestBuilderFactory(DefMethTest testInstance, int minMajorVer,
                              int accFlags, boolean redefineClasses, boolean retransformClasses,
                              String modeName) {
        this.testInstance = testInstance;
        this.minMajorVer = minMajorVer;
        this.accFlags = accFlags;
        this.redefineClasses = redefineClasses;
        this.retransformClasses = retransformClasses;
        this.modeName = modeName;
    }

    public TestBuilder getBuilder() {
        ExecutionMode mode = (!"".equals(modeName) ? ExecutionMode.valueOf(modeName) : null);
        return new TestBuilder(testInstance, minMajorVer, accFlags, redefineClasses, retransformClasses, mode);
    }

    public int getVer() { return minMajorVer; }
    public void setVer(int x) { minMajorVer = x; }

    public int getFlags() { return accFlags; }
    public void setFlags(int x) { accFlags = x; }

    public boolean isRedefineClasses() { return redefineClasses; }
    public void setRedefineClasses(boolean x) { redefineClasses = x; }

    public boolean isRetransformClasses() { return retransformClasses; }
    public void setRetransformClasses(boolean x) { retransformClasses = x; }

    public String getExecutionMode() { return (!"".equals(modeName) ? modeName : null); }
    public void setExecutionMode(String x) { modeName = x; }

    @Override
    public String toString() {
        return String.format("{ver=%d; flags=%d; redefine=%b; execMode=%s}",
                minMajorVer, accFlags, redefineClasses, modeName);
    }
}
