/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8165276
 * @summary Test that agent with non-public premain method is rejected to load
 * @library /test/lib
 * @library /test
 * @modules java.instrument
 * @build jdk.java.lang.instrument.PremainClass.NonPublicPremainAgent
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *             NonPublicPremainAgent NonPublicPremainAgent.jar
 * @run main/othervm jdk.java.lang.instrument.NegativeAgentRunner NonPublicPremainAgent IllegalAccessException
 */

import java.lang.RuntimeException;
import java.lang.instrument.Instrumentation;

public class NonPublicPremainAgent {

    // This premain method is intentionally non-public to ensure it is rejected.
    static void premain(String agentArgs, Instrumentation inst) {
        throw new RuntimeException("premain: NonPublicPremainAgent was not expected to be loaded");
    }
}
