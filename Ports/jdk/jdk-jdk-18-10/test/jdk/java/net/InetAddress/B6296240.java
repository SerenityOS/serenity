/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6296240
 * @summary  REGRESSION: InetAddress.getAllByName accepts badly formed address
 */

import java.net.*;
import java.util.BitSet;

public class B6296240 {
    public static void main(String[] args) {
        String[] malformedIPv4s = {"192.168.1.220..."};
        BitSet expectedExceptions = new BitSet(malformedIPv4s.length);
        expectedExceptions.clear();

        for (int i = 0; i < malformedIPv4s.length; i++) {
            try {
                InetAddress.getAllByName(malformedIPv4s[i]);
            } catch (UnknownHostException e) {
                expectedExceptions.set(i);
            }
        }

        for (int i = 0; i < malformedIPv4s.length; i++) {
            if (!expectedExceptions.get(i)) {
                System.out.println("getAllByName(\"" + malformedIPv4s[i] + "\") should throw exception.");
            }
        }

        if (expectedExceptions.cardinality() != malformedIPv4s.length) {
            throw new RuntimeException("Failed: some expected UnknownHostExceptions are not thrown.");
        }
    }
}
