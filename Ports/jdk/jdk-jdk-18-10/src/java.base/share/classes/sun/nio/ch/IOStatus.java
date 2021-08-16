/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.lang.annotation.Native;

// Constants for reporting I/O status

public final class IOStatus {

    private IOStatus() { }

    @Native public static final int EOF = -1;              // End of file
    @Native public static final int UNAVAILABLE = -2;      // Nothing available (non-blocking)
    @Native public static final int INTERRUPTED = -3;      // System call interrupted
    @Native public static final int UNSUPPORTED = -4;      // Operation not supported
    @Native public static final int THROWN = -5;           // Exception thrown in JNI code
    @Native public static final int UNSUPPORTED_CASE = -6; // This case not supported

    // The following two methods are for use in try/finally blocks where a
    // status value needs to be normalized before being returned to the invoker
    // but also checked for illegal negative values before the return
    // completes, like so:
    //
    //     int n = 0;
    //     try {
    //         begin();
    //         n = op(fd, buf, ...);
    //         return IOStatus.normalize(n);    // Converts UNAVAILABLE to zero
    //     } finally {
    //         end(n > 0);
    //         assert IOStatus.check(n);        // Checks other negative values
    //     }
    //

    public static int normalize(int n) {
        if (n == UNAVAILABLE)
            return 0;
        return n;
    }

    public static boolean check(int n) {
        return (n >= UNAVAILABLE);
    }

    public static long normalize(long n) {
        if (n == UNAVAILABLE)
            return 0;
        return n;
    }

    public static boolean check(long n) {
        return (n >= UNAVAILABLE);
    }

    // Return true iff n is not one of the IOStatus values
    public static boolean checkAll(long n) {
        return ((n > EOF) || (n < UNSUPPORTED_CASE));
    }

    /**
     * Returns true if the error code is UNAVAILABLE or INTERRUPTED, the
     * error codes to indicate that an I/O operation can be retried.
     */
    static boolean okayToRetry(long n) {
        return (n == IOStatus.UNAVAILABLE) || (n == IOStatus.INTERRUPTED);
    }

}
