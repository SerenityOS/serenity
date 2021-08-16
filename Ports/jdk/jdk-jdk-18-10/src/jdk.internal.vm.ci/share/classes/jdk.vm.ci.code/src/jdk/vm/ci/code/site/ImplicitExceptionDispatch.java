/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.code.site;

import jdk.vm.ci.code.DebugInfo;

/**
 * Represents an implicit exception dispatch in the code. Implicit exception dispatch is a
 * platform-specific optimization that makes use of an operating system's trap mechanism, to turn
 * specific branches into sequential code with implicit traps. Information contained in this class
 * will be used by the runtime to register implicit exception dispatch, i.e., a mapping from an
 * exceptional PC offset to a continuation PC offset.
 */
public final class ImplicitExceptionDispatch extends Infopoint {

    public final int dispatchOffset;

    /**
     * Construct an implicit exception dispatch.
     *
     * @param pcOffset the exceptional PC offset
     * @param dispatchOffset the continuation PC offset
     * @param debugInfo debugging information at the exceptional PC
     */
    public ImplicitExceptionDispatch(int pcOffset, int dispatchOffset, DebugInfo debugInfo) {
        super(pcOffset, debugInfo, InfopointReason.IMPLICIT_EXCEPTION);
        this.dispatchOffset = dispatchOffset;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ImplicitExceptionDispatch && super.equals(obj)) {
            ImplicitExceptionDispatch that = (ImplicitExceptionDispatch) obj;
            if (this.dispatchOffset == that.dispatchOffset) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(pcOffset);
        sb.append("->");
        sb.append(dispatchOffset);

        if (debugInfo != null) {
            appendDebugInfo(sb, debugInfo);
        }

        return sb.toString();
    }
}
