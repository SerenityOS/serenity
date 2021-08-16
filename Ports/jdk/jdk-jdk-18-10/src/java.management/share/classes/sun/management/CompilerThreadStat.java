/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/**
 */
@Deprecated
public class CompilerThreadStat implements java.io.Serializable {
    private String name;
    private long taskCount;
    private long compileTime;
    private MethodInfo lastMethod;

    CompilerThreadStat(String name, long taskCount, long time, MethodInfo lastMethod) {
        this.name = name;
        this.taskCount = taskCount;
        this.compileTime = time;
        this.lastMethod = lastMethod;
    };

    /**
     * Returns the name of the compiler thread associated with
     * this compiler thread statistic.
     *
     * @return the name of the compiler thread.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of compile tasks performed by the compiler thread
     * associated with this compiler thread statistic.
     *
     * @return the number of compile tasks performed by the compiler thread.
     */
    public long getCompileTaskCount() {
        return taskCount;
    }

    /**
     * Returns the accumulated elapsed time spent by the compiler thread
     * associated with this compiler thread statistic.
     *
     * @return the accumulated elapsed time spent by the compiler thread.
     */
    public long getCompileTime() {
        return compileTime;
    }

    /**
     * Returns the information about the last method compiled by
     * the compiler thread associated with this compiler thread statistic.
     *
     * @return a {@link MethodInfo} object for the last method
     * compiled by the compiler thread.
     */
    public MethodInfo getLastCompiledMethodInfo() {
        return lastMethod;
    }

    public String toString() {
        return getName() + " compileTasks = " + getCompileTaskCount()
            + " compileTime = " + getCompileTime();
    }

    private static final long serialVersionUID = 6992337162326171013L;

}
