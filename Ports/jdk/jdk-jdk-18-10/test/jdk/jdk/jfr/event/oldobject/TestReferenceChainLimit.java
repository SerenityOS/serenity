/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.oldobject;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestReferenceChainLimit
 */
public class TestReferenceChainLimit {
    private final static int TEST_CHAIN_LENGTH_FACTOR = 10; // scaling factor for the how long chain to be used in the test

    final static class ChainNode {
        final ChainNode next;
        byte[] leak;

        public ChainNode(ChainNode node) {
            next = node;
            leak = new byte[10_000];
        }
    }

    private static ChainNode createChain() {
        ChainNode node = null;
        final int testChainLength = OldObjects.MAX_CHAIN_LENGTH * TEST_CHAIN_LENGTH_FACTOR;
        for (int i = 0; i < testChainLength; i++) {
            node = new ChainNode(node);
        }
        return node;
    }

    public static ChainNode leak;

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        try (Recording r = new Recording()) {
            r.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
            r.start();
            leak = createChain();
            List<RecordedEvent> events = Events.fromRecording(r);
            if (OldObjects.countMatchingEvents(events, byte[].class, null, null, -1, "createChain") == 0) {
                throw new Exception("Could not find ChainNode");
            }
            for (RecordedEvent e : events) {
                OldObjects.validateReferenceChainLimit(e, OldObjects.MAX_CHAIN_LENGTH);
            }
        }
    }
}
