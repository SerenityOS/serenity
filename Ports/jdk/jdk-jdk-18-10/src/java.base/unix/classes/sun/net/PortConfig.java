/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.net;

import sun.security.action.GetPropertyAction;

/**
 * Determines the ephemeral port range in use on this system.
 * If this cannot be determined, then the default settings
 * of the OS are returned.
 */

public final class PortConfig {

    private static int defaultUpper, defaultLower;
    private static final int upper, lower;

    private PortConfig() {}

    static {
        jdk.internal.loader.BootLoader.loadLibrary("net");
        String os = GetPropertyAction.privilegedGetProperty("os.name");
        if (os.startsWith("Linux")) {
            defaultLower = 32768;
            defaultUpper = 61000;
        } else if (os.contains("OS X")) {
            defaultLower = 49152;
            defaultUpper = 65535;
        } else if (os.startsWith("AIX")) {
            // The ephemeral port is OS version dependent on AIX:
            // http://publib.boulder.ibm.com/infocenter/aix/v7r1/topic/com.ibm.aix.rsct315.admin/bl503_ephport.htm
            // However, on AIX 5.3 / 6.1 / 7.1 we always see the
            // settings below by using:
            // /usr/sbin/no -a | fgrep ephemeral
            defaultLower = 32768;
            defaultUpper = 65535;
        } else {
            throw new InternalError(
                "sun.net.PortConfig: unknown OS");
        }

        int v = getLower0();
        if (v == -1) {
            v = defaultLower;
        }
        lower = v;

        v = getUpper0();
        if (v == -1) {
            v = defaultUpper;
        }
        upper = v;
    }

    static native int getLower0();
    static native int getUpper0();

    public static int getLower() {
        return lower;
    }

    public static int getUpper() {
        return upper;
    }
}
