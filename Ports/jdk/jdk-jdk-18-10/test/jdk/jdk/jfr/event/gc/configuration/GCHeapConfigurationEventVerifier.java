/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.gc.configuration;

import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventVerifier;

public abstract class GCHeapConfigurationEventVerifier extends EventVerifier {
    public GCHeapConfigurationEventVerifier(RecordedEvent e) {
        super(e);
    }

    protected void verifyMinHeapSizeIs(long expected) throws Exception {
        verifyEquals("minSize", expected);
    }

    protected void verifyInitialHeapSizeIs(long expected) throws Exception {
        verifyEquals("initialSize", expected);
    }

    protected void verifyMaxHeapSizeIs(long expected) throws Exception {
        verifyEquals("maxSize", expected);
    }

    protected void verifyUsesCompressedOopsIs(boolean expected) throws Exception {
        verifyEquals("usesCompressedOops", expected);
    }

    protected void verifyObjectAlignmentInBytesIs(int expected) throws Exception {
        verifyEquals("objectAlignment", (long)expected);
    }

    protected void verifyHeapAddressBitsIs(int expected) throws Exception {
        verifyEquals("heapAddressBits", (byte)expected);
    }

    protected void verifyCompressedOopModeIs(String expected) throws Exception {
        verifyEquals("compressedOopsMode", expected);
    }

    protected void verifyCompressedOopModeContains(String expected) throws Exception {
        verifyContains("compressedOopsMode", expected);
    }

}
