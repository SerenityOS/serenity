/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.smartcardio;

import java.security.AccessController;
import java.security.PrivilegedAction;

// Platform specific code and constants
class PlatformPCSC {

    static final Throwable initException;

    PlatformPCSC() {
        // empty
    }

    static {
        initException = loadLibrary();
    }

    @SuppressWarnings("removal")
    private static Throwable loadLibrary() {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("j2pcsc");
                    return null;
                }
            });
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

    // PCSC constants defined differently under Windows and MUSCLE
    // Windows version
    final static int SCARD_PROTOCOL_T0     =  0x0001;
    final static int SCARD_PROTOCOL_T1     =  0x0002;
    final static int SCARD_PROTOCOL_RAW    =  0x10000;

    final static int SCARD_UNKNOWN         =  0x0000;
    final static int SCARD_ABSENT          =  0x0001;
    final static int SCARD_PRESENT         =  0x0002;
    final static int SCARD_SWALLOWED       =  0x0003;
    final static int SCARD_POWERED         =  0x0004;
    final static int SCARD_NEGOTIABLE      =  0x0005;
    final static int SCARD_SPECIFIC        =  0x0006;

}
