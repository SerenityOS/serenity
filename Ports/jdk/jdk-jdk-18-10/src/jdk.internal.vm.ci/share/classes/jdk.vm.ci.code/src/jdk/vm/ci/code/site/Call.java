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

import java.util.Objects;

import jdk.vm.ci.code.DebugInfo;
import jdk.vm.ci.meta.InvokeTarget;

/**
 * Represents a call in the code.
 */
public final class Call extends Infopoint {

    /**
     * The target of the call.
     */
    public final InvokeTarget target;

    /**
     * The size of the call instruction.
     */
    public final int size;

    /**
     * Specifies if this call is direct or indirect. A direct call has an immediate operand encoding
     * the absolute or relative (to the call itself) address of the target. An indirect call has a
     * register or memory operand specifying the target address of the call.
     */
    public final boolean direct;

    public Call(InvokeTarget target, int pcOffset, int size, boolean direct, DebugInfo debugInfo) {
        super(pcOffset, debugInfo, InfopointReason.CALL);
        this.size = size;
        this.target = target;
        this.direct = direct;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Call && super.equals(obj)) {
            Call that = (Call) obj;
            if (this.size == that.size && this.direct == that.direct && Objects.equals(this.target, that.target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(pcOffset);
        sb.append('[');
        sb.append(target);
        sb.append(']');

        if (debugInfo != null) {
            appendDebugInfo(sb, debugInfo);
        }

        return sb.toString();
    }
}
