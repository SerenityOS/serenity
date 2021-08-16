/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.management;

import sun.management.counter.Counter;

/**
 * Hotspot internal management interface for the compilation system.
 */
public interface HotspotCompilationMBean {

    /**
     * Returns the number of compiler threads.
     *
     * @return the number of compiler threads.
     */
    public int getCompilerThreadCount();

    /**
     * Returns the statistic of all compiler threads.
     *
     * @return a list of {@link CompilerThreadStat} object containing
     * the statistic of a compiler thread.
     *
     */
    @Deprecated
    public java.util.List<CompilerThreadStat> getCompilerThreadStats();

    /**
     * Returns the total number of compiles.
     *
     * @return the total number of compiles.
     */
    public long getTotalCompileCount();

    /**
     * Returns the number of bailout compiles.
     *
     * @return the number of bailout compiles.
     */
    public long getBailoutCompileCount();

    /**
     * Returns the number of invalidated compiles.
     *
     * @return the number of invalidated compiles.
     */
    public long getInvalidatedCompileCount();

    /**
     * Returns the method information of the last compiled method.
     *
     * @return a {@link MethodInfo} of the last compiled method.
     */
    public MethodInfo getLastCompile();

    /**
     * Returns the method information of the last failed compile.
     *
     * @return a {@link MethodInfo} of the last failed compile.
     */
    public MethodInfo getFailedCompile();

    /**
     * Returns the method information of the last invalidated compile.
     *
     * @return a {@link MethodInfo} of the last invalidated compile.
     */
    public MethodInfo getInvalidatedCompile();

    /**
     * Returns the number of bytes for the code of the
     * compiled methods.
     *
     * @return the number of bytes for the code of the compiled methods.
     */
    public long getCompiledMethodCodeSize();

    /**
     * Returns the number of bytes occupied by the compiled methods.
     *
     * @return the number of bytes occupied by the compiled methods.
     */
    public long getCompiledMethodSize();

    /**
     * Returns a list of internal counters maintained in the Java
     * virtual machine for the compilation system.
     *
     * @return a list of internal counters maintained in the VM
     * for the compilation system.
     */
    public java.util.List<Counter> getInternalCompilerCounters();
}
