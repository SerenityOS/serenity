/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary.rtm;

/**
 * In order to provoke transactional execution abort due to
 * internal's buffer overflow BufferOverflowProvoker modifies
 * 1MB of BYTES during single transaction.
 */
class BufferOverflowProvoker extends AbortProvoker {
    /**
     * To force buffer overflow abort we modify memory region with
     * size more then L1d cache size.
     */
    private static final int MORE_THAN_L1D_SIZE = 1024 * 1024;
    private static final byte[] DATA = new byte[MORE_THAN_L1D_SIZE];

    @Override
    public void forceAbort() {
        synchronized(monitor) {
            for (int i = 0; i < BufferOverflowProvoker.DATA.length; i++) {
                BufferOverflowProvoker.DATA[i]++;
            }
        }
    }
}
