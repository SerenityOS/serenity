/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.code.site;

import java.util.Map;
import java.util.Objects;

import jdk.vm.ci.code.BytecodePosition;
import jdk.vm.ci.code.DebugInfo;
import jdk.vm.ci.code.ReferenceMap;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterSaveLayout;
import jdk.vm.ci.meta.MetaUtil;

/**
 * Represents an infopoint with associated debug info. Note that safepoints are also infopoints.
 */
public class Infopoint extends Site implements Comparable<Infopoint> {

    public final DebugInfo debugInfo;

    public final InfopointReason reason;

    public Infopoint(int pcOffset, DebugInfo debugInfo, InfopointReason reason) {
        super(pcOffset);
        this.debugInfo = debugInfo;
        this.reason = reason;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(pcOffset);
        sb.append("[<infopoint>]");
        appendDebugInfo(sb, debugInfo);
        return sb.toString();
    }

    @Override
    public int compareTo(Infopoint o) {
        if (pcOffset < o.pcOffset) {
            return -1;
        } else if (pcOffset > o.pcOffset) {
            return 1;
        }
        return this.reason.compareTo(o.reason);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj.getClass() == getClass()) {
            Infopoint that = (Infopoint) obj;
            if (this.pcOffset == that.pcOffset && Objects.equals(this.debugInfo, that.debugInfo) && Objects.equals(this.reason, that.reason)) {
                return true;
            }
        }
        return false;
    }

    protected static void appendDebugInfo(StringBuilder sb, DebugInfo info) {
        if (info != null) {
            ReferenceMap refMap = info.getReferenceMap();
            if (refMap != null) {
                sb.append(refMap.toString());
                sb.append(']');
            }
            RegisterSaveLayout calleeSaveInfo = info.getCalleeSaveInfo();
            if (calleeSaveInfo != null) {
                sb.append(" callee-save-info[");
                String sep = "";
                for (Map.Entry<Register, Integer> e : calleeSaveInfo.registersToSlots(true).entrySet()) {
                    sb.append(sep).append(e.getKey()).append("->").append(e.getValue());
                    sep = ", ";
                }
                sb.append(']');
            }
            BytecodePosition codePos = info.getBytecodePosition();
            if (codePos != null) {
                MetaUtil.appendLocation(sb.append(" "), codePos.getMethod(), codePos.getBCI());
                if (info.hasFrame()) {
                    sb.append(" #locals=").append(info.frame().numLocals).append(" #expr=").append(info.frame().numStack);
                    if (info.frame().numLocks > 0) {
                        sb.append(" #locks=").append(info.frame().numLocks);
                    }
                }
            }
        }
    }
}
