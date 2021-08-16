/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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
 * @test TestChunkInputStreamAvailable
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestChunkInputStreamAvailable
 */
package jdk.jfr.api.consumer;

import java.io.InputStream;

import jdk.jfr.Recording;
import jdk.test.lib.Asserts;

public class TestChunkInputStreamAvailable {

    public static void main(String[] args) throws Exception {
        try (Recording r = new Recording()) {
            r.start();
            try (Recording s = new Recording()) {
                s.start();
                s.stop();
            }
            r.stop();
            try (InputStream stream = r.getStream(null, null)) {
                int left = stream.available();
                Asserts.assertEquals(r.getSize(), (long) left);
                while (stream.read() != -1) {
                    left--;
                    Asserts.assertEquals(left, stream.available());
                }
                Asserts.assertEquals(0, left);
            }
        }
    }
}
