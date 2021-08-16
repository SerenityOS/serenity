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
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run driver jdk.jfr.event.gc.configuration.TestGCYoungGenerationConfigurationEventWithMinAndMaxSize
 */
public class TestGCYoungGenerationConfigurationEventWithMinAndMaxSize {
    public static void main(String[] args) throws Exception {
        String[] jvm_args = {"-XX:+UnlockExperimentalVMOptions",
                             "-XX:-UseFastUnorderedTimeStamps",
                             "-XX:NewSize=12m",
                             "-cp",
                             System.getProperty("java.class.path"),
                             "-XX:MaxNewSize=16m",
                             "-Xms32m",
                             "-Xmx64m",
                             Tester.class.getName()};
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(jvm_args);
        OutputAnalyzer analyzer = ProcessTools.executeProcess(pb);
        analyzer.shouldHaveExitValue(0);
    }
}

class Tester extends GCYoungGenerationConfigurationEventTester {
    public static void main(String[] args) throws Exception {
        new Tester().run();
    }

    @Override protected EventVerifier createVerifier(RecordedEvent e) {
        return new MinAndMaxSizeVerifier(e);
    }
}

class MinAndMaxSizeVerifier extends EventVerifier {
    public MinAndMaxSizeVerifier(RecordedEvent e) {
        super(e);
    }

    @Override public void verify() throws Exception {
        verifyMinSizeIs(megabytes(12));
        verifyMaxSizeIs(megabytes(16));

        // Can't test newRatio at the same time as minSize and maxSize,
        // because the NewRatio flag can't be set when the flags NewSize and
        // MaxNewSize are set.
    }

    private void verifyMinSizeIs(long expected) throws Exception {
        verifyEquals("minSize", expected);
    }

    private void verifyMaxSizeIs(long expected) throws Exception {
        verifyEquals("maxSize", expected);
    }
}
