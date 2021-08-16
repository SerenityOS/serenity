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

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps -XX:NewRatio=4 jdk.jfr.event.gc.configuration.TestGCYoungGenerationConfigurationEventWithNewRatio
 */
public class TestGCYoungGenerationConfigurationEventWithNewRatio
    extends GCYoungGenerationConfigurationEventTester {
    public static void main(String[] args) throws Exception {
        GCYoungGenerationConfigurationEventTester t = new TestGCYoungGenerationConfigurationEventWithNewRatio();
        t.run();
    }

    @Override protected EventVerifier createVerifier(RecordedEvent e) {
        return new NewRatioVerifier(e);
    }
}

class NewRatioVerifier extends EventVerifier {
    public NewRatioVerifier(RecordedEvent event) {
        super(event);
    }

    @Override public void verify() throws Exception {
        verifyNewRatioIs(4);

        // Can't test minSize and maxSize at the same time as newRatio,
        // because the NewSize and MaxNewSize flags can't be set when the flag
        // MaxNewSize is set.
    }

    private void verifyNewRatioIs(int expected) throws Exception {
        verifyEquals("newRatio", expected);
    }
}
