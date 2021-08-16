/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

package sun.nio.ch;

import java.lang.annotation.Native;

/**
 * Manipulates a native array of structs corresponding to (fd, events) pairs.
 *
 * typedef struct pollfd {
 *    SOCKET fd;            // 4 bytes
 *    short events;         // 2 bytes
 * } pollfd_t;
 *
 * @author Konstantin Kladko
 * @author Mike McCloskey
 */

class PollArrayWrapper {

    private AllocatedNativeObject pollArray; // The fd array

    long pollArrayAddress; // pollArrayAddress

    @Native private static final short FD_OFFSET     = 0; // fd offset in pollfd
    @Native private static final short EVENT_OFFSET  = 4; // events offset in pollfd

    static short SIZE_POLLFD = 8; // sizeof pollfd struct

    private int size; // Size of the pollArray

    PollArrayWrapper(int newSize) {
        int allocationSize = newSize * SIZE_POLLFD;
        pollArray = new AllocatedNativeObject(allocationSize, true);
        pollArrayAddress = pollArray.address();
        this.size = newSize;
    }

    // Prepare another pollfd struct for use.
    void putEntry(int index, SelectionKeyImpl ski) {
        putDescriptor(index, ski.getFDVal());
        putEventOps(index, 0);
    }

    // Writes the pollfd entry from the source wrapper at the source index
    // over the entry in the target wrapper at the target index.
    void replaceEntry(PollArrayWrapper source, int sindex,
                                     PollArrayWrapper target, int tindex) {
        target.putDescriptor(tindex, source.getDescriptor(sindex));
        target.putEventOps(tindex, source.getEventOps(sindex));
    }

    // Grows the pollfd array to new size
    void grow(int newSize) {
        PollArrayWrapper temp = new PollArrayWrapper(newSize);
        for (int i = 0; i < size; i++)
            replaceEntry(this, i, temp, i);
        pollArray.free();
        pollArray = temp.pollArray;
        this.size = temp.size;
        pollArrayAddress = pollArray.address();
    }

    void free() {
        pollArray.free();
    }

    // Access methods for fd structures
    void putDescriptor(int i, int fd) {
        pollArray.putInt(SIZE_POLLFD * i + FD_OFFSET, fd);
    }

    void putEventOps(int i, int event) {
        pollArray.putShort(SIZE_POLLFD * i + EVENT_OFFSET, (short)event);
    }

    int getEventOps(int i) {
        return pollArray.getShort(SIZE_POLLFD * i + EVENT_OFFSET);
    }

    int getDescriptor(int i) {
       return pollArray.getInt(SIZE_POLLFD * i + FD_OFFSET);
    }

    // Adds Windows wakeup socket at a given index.
    void addWakeupSocket(int fdVal, int index) {
        putDescriptor(index, fdVal);
        putEventOps(index, Net.POLLIN);
    }
}
