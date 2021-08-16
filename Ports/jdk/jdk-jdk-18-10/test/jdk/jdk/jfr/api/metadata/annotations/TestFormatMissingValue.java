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

package jdk.jfr.api.metadata.annotations;

import jdk.jfr.DataAmount;
import jdk.jfr.Event;
import jdk.jfr.Frequency;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @summary Check that event values are properly formatted and sanity check
 *              that extreme values don't throws exceptions
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestFormatMissingValue
 */
public class TestFormatMissingValue {

    @StackTrace(false)
    static class MultiContentTypeEvent extends Event {
        @DataAmount(DataAmount.BYTES)
        @Frequency
        long a = Long.MIN_VALUE;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        long b = Long.MAX_VALUE;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        int c = Integer.MIN_VALUE;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        int d = Integer.MAX_VALUE;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        double e = Double.NEGATIVE_INFINITY;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        double f = Double.POSITIVE_INFINITY;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        double g = Double.NaN;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        float h = Float.NEGATIVE_INFINITY;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        float i = Float.POSITIVE_INFINITY;

        @DataAmount(DataAmount.BYTES)
        @Frequency
        float j = Float.NaN;
    }

    public static void main(String[] args) throws Exception {
        try (Recording r = new Recording()) {
            r.start();
            MultiContentTypeEvent m = new MultiContentTypeEvent();
            m.commit();
            r.stop();
            for (RecordedEvent e : Events.fromRecording(r)) {
                String t = e.toString();
                assertContains(t, "a = N/A");
                assertContains(t, "c = N/A");
                assertContains(t, "e = N/A");
                assertContains(t, "g = N/A");
                assertContains(t, "h = N/A");
                assertContains(t, "j = N/A");

                assertNotContains(t, "b = N/A");
                assertNotContains(t, "d = N/A");
                assertNotContains(t, "f = N/A");
                assertNotContains(t, "i = N/A");
            }
        }
    }

    private static void assertContains(String t, String text) {
        if (!t.contains(text)) {
            Asserts.fail("Expected '" + t + "' to contain text '" + text + "'");
        }
    }

    private static void assertNotContains(String t, String text) {
        if (t.contains(text)) {
            Asserts.fail("Found unexpected value '" + text + "'  in text '" + t + "'");
        }
    }
}
