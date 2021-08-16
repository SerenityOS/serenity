/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JDWP {

    public record ListenAddress(String transport, String address) {
    }

    // lazy initialized
    private static Pattern listenRegexp;

    /**
     * Parses debuggee output to get listening transport and address.
     * Returns null if the string specified does not contain required info.
     */
    public static ListenAddress parseListenAddress(String debuggeeOutput) {
        if (listenRegexp == null) {
            listenRegexp = Pattern.compile("Listening for transport \\b(.+)\\b at address: \\b(.+)\\b");
        }
        Matcher m = listenRegexp.matcher(debuggeeOutput);
        if (m.find()) {
            return new ListenAddress(m.group(1), m.group(2));
        }
        return null;
    }

}
