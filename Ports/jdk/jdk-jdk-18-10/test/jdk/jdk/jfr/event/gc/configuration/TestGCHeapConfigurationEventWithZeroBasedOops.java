/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test TestGCHeapConfigurationEventWithZeroBasedOops
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc == "Parallel" | vm.gc == null
 * @requires os.family == "linux" | os.family == "windows"
 * @requires sun.arch.data.model == "64"
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps -XX:+UseParallelGC -XX:+UseCompressedOops -Xmx4g jdk.jfr.event.gc.configuration.TestGCHeapConfigurationEventWithZeroBasedOops
 */

/* See the shell script wrapper for the flags used when invoking the JVM */
public class TestGCHeapConfigurationEventWithZeroBasedOops extends GCHeapConfigurationEventTester {
    public static void main(String[] args) throws Exception {
        GCHeapConfigurationEventTester t = new TestGCHeapConfigurationEventWithZeroBasedOops();
        t.run();
    }

    @Override
    protected EventVerifier createVerifier(RecordedEvent e) {
        return new ZeroBasedOopsVerifier(e);
    }
}

class ZeroBasedOopsVerifier extends GCHeapConfigurationEventVerifier {
    public ZeroBasedOopsVerifier(RecordedEvent e) {
        super(e);
    }

    @Override
    public void verify() throws Exception {
        // Can't verify min and initial heap size due to constraints on
        // physical memory on tests machines

        verifyMaxHeapSizeIs(gigabytes(4));
        verifyUsesCompressedOopsIs(true);
        verifyObjectAlignmentInBytesIs(8);
        verifyHeapAddressBitsIs(32);
        verifyCompressedOopModeIs("Zero based");
    }
}
