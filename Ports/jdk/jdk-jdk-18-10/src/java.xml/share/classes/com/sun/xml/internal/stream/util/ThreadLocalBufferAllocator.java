/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.stream.util;

import java.lang.ref.*;

/**
 * This is a Singleton class that allows you to allocate buffer that
 * are stored in ThreadLocal. This class stores 3 types of char
 * buffers - small (< 128 bytes), medium (<2K) and large (> 2K) as
 * well as three byte buffers - small, medium and large.
 * The local storage is activated on the return of the buffer.
 * The buffer returns null if it is already allocated.
 *
 * @author Binu John
 * @author Santiago PericasGeertsen
 */
public class ThreadLocalBufferAllocator {
    private static final ThreadLocal<SoftReference<BufferAllocator>> TL = new ThreadLocal<>();

    public static BufferAllocator getBufferAllocator() {
        BufferAllocator ba = null;
        SoftReference<BufferAllocator> sr = TL.get();
        if (sr != null) {
            ba = sr.get();
        }
        if (ba == null) {
            ba = new BufferAllocator();
            sr = new SoftReference<>(ba);
            TL.set(sr);
        }
        return ba;
    }
}
