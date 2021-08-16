/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.X11;

import jdk.internal.misc.Unsafe;
import sun.util.logging.PlatformLogger;

class UnsafeXDisposerRecord implements sun.java2d.DisposerRecord {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.UnsafeXDisposerRecord");
    private static Unsafe unsafe = XlibWrapper.unsafe;
    final long[] unsafe_ptrs, x_ptrs;
    final String name;
    volatile boolean disposed;
    final Throwable place;
    public UnsafeXDisposerRecord(String name, long[] unsafe_ptrs, long[] x_ptrs) {
        this.unsafe_ptrs = unsafe_ptrs;
        this.x_ptrs = x_ptrs;
        this.name = name;
        if (XlibWrapper.isBuildInternal) {
            place = new Throwable();
        } else {
            place = null;
        }
    }
    public UnsafeXDisposerRecord(String name, long ... unsafe_ptrs) {
        this.unsafe_ptrs = unsafe_ptrs;
        this.x_ptrs = null;
        this.name = name;
        if (XlibWrapper.isBuildInternal) {
            place = new Throwable();
        } else {
            place = null;
        }
    }

    public void dispose() {
        XToolkit.awtLock();
        try {
            if (!disposed) {
                if (XlibWrapper.isBuildInternal && "Java2D Disposer".equals(Thread.currentThread().getName()) && log.isLoggable(PlatformLogger.Level.WARNING)) {
                    if (place != null) {
                        log.warning(name + " object was not disposed before finalization!", place);
                    } else {
                        log.warning(name + " object was not disposed before finalization!");
                    }
                }

                if (unsafe_ptrs != null) {
                    for (long l : unsafe_ptrs) {
                        if (l != 0) {
                            unsafe.freeMemory(l);
                        }
                    }
                }
                if (x_ptrs != null) {
                    for (long l : x_ptrs) {
                        if (l != 0) {
                            if (Native.getLong(l) != 0) {
                                XlibWrapper.XFree(Native.getLong(l));
                            }
                            unsafe.freeMemory(l);
                        }
                    }
                }
                disposed = true;
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }
}
