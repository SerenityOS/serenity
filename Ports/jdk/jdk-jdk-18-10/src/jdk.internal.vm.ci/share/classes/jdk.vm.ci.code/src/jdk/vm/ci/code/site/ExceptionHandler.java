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

/**
 * Represents exception handler information for a specific code position. It includes the catch code
 * position as well as the caught exception type.
 */
public final class ExceptionHandler extends Site {

    public final int handlerPos;

    public ExceptionHandler(int pcOffset, int handlerPos) {
        super(pcOffset);
        this.handlerPos = handlerPos;
    }

    @Override
    public String toString() {
        return String.format("%d[<exception edge to %d>]", pcOffset, handlerPos);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExceptionHandler) {
            ExceptionHandler that = (ExceptionHandler) obj;
            if (this.pcOffset == that.pcOffset && this.handlerPos == that.handlerPos) {
                return true;
            }
        }
        return false;
    }
}
