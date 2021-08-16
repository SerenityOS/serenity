/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 * @requires vm.jvmci
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.services
 *
 * @build compiler.jvmci.common.JVMCIHelpers
 *        compiler.jvmci.events.JvmciShutdownEventListener
 * @run driver jdk.test.lib.FileInstaller ./JvmciShutdownEventTest.config
 *     ./META-INF/services/jdk.vm.ci.services.JVMCIServiceLocator
 * @run driver jdk.test.lib.helpers.ClassFileInstaller
 *      compiler.jvmci.common.JVMCIHelpers$EmptyHotspotCompiler
 *      compiler.jvmci.common.JVMCIHelpers$EmptyCompilerFactory
 *      compiler.jvmci.common.JVMCIHelpers$EmptyCompilationRequestResult
 *      compiler.jvmci.common.JVMCIHelpers$EmptyVMEventListener
 *      compiler.jvmci.events.JvmciShutdownEventListener
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      compiler.jvmci.events.JvmciShutdownEventTest
 */

package compiler.jvmci.events;

import jdk.test.lib.process.ExitCode;
import jdk.test.lib.cli.CommandLineOptionTest;

public class JvmciShutdownEventTest {
    private final static String[] MESSAGE = new String[]{
        JvmciShutdownEventListener.MESSAGE
    };

    private final static String[] ERROR_MESSAGE = new String[]{
        JvmciShutdownEventListener.GOT_INTERNAL_ERROR
    };

    public static void main(String args[]) throws Throwable {
        boolean addTestVMOptions = true;
        CommandLineOptionTest.verifyJVMStartup(MESSAGE, ERROR_MESSAGE,
                "Unexpected exit code with +EnableJVMCI",
                "Unexpected output with +EnableJVMCI", ExitCode.OK,
                addTestVMOptions, "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableJVMCI", "-XX:-UseJVMCICompiler", "-Xbootclasspath/a:.",
                JvmciShutdownEventListener.class.getName()
        );

        CommandLineOptionTest.verifyJVMStartup(ERROR_MESSAGE, MESSAGE,
                "Unexpected exit code with -EnableJVMCI",
                "Unexpected output with -EnableJVMCI", ExitCode.OK,
                addTestVMOptions, "-XX:+UnlockExperimentalVMOptions",
                "-XX:-EnableJVMCI", "-XX:-UseJVMCICompiler", "-Xbootclasspath/a:.",
                JvmciShutdownEventListener.class.getName()
        );
    }
}
