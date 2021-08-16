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
 * @summary Test that public premain method from non-public agent is NOT rejected to load
 * @library /test/lib
 * @modules java.instrument
 * @build jdk.java.lang.instrument.PremainClass.NonPublicAgent
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *             NonPublicAgent NonPublicAgent.jar
 * @run main/othervm -javaagent:NonPublicAgent.jar DummyMain
 */

import java.lang.instrument.Instrumentation;

// This class is intentionally non-public to ensure its premain method is NOT rejected.
class NonPublicAgent {

    // This premain method has to be resolved even if its class is not public
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("premain: NonPublicAgent was loaded");
    }
}
