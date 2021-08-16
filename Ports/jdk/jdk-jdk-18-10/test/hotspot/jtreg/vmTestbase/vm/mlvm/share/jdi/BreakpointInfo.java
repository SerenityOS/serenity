/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share.jdi;

import java.util.List;

import vm.mlvm.share.jpda.StratumInfo;

import com.sun.jdi.request.BreakpointRequest;

public class BreakpointInfo {
    public static enum Type {
        /** set breakpoint via BreakpointRequest */
        EXPLICIT,
        /**
         * don't set JDI breakpoint, verify that we can reach the location
         * via stepping
         */
        IMPLICIT
    };

    // Initial information
    public Type type = Type.EXPLICIT;
    public String className = "";
    public final String methodName;
    public int methodLine = 0;

    /** Breakpoint stratum (JSR-045). null == default stratum */
    public StratumInfo stratumInfo = null;

    /**
     * How many times this breakpoint should be hit. null == any number of
     * hits
     */
    public Integer requiredHits = null;

    /** How many steps do via StepRequest after reaching the breakpoint */
    public int stepsToTrace = 0;

    /** Conditional breakpoints should not be enabled by default */
    public final boolean isConditional;

    /** Sub-breakpoints */
    public List<BreakpointInfo> subBreakpoints = null;

    // Fields below are filled in by debugger
    public long bci = -1;
    public BreakpointRequest bpReq = null;
    public int hits = 0;

    public BreakpointInfo(String methodName) {
        this(methodName, false);
    }

    public BreakpointInfo(String methodName, boolean isConditional) {
        this.methodName = methodName;
        this.isConditional = isConditional;
    }

    public boolean isHit() {
        if (requiredHits == null) {
            return hits > 0;
        } else {
            return hits == requiredHits;
        }
    }

    @Override
    public String toString() {
        return className + "." + methodName + ":" + methodLine + "[bci=" + bci + ",bp=" + bpReq + "]";
    }

}
