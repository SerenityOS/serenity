/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8156034
 * @requires vm.jvmci & !vm.graal.enabled & vm.compMode == "Xmixed" & vm.opt.TieredStopAtLevel == null
 * @library / /test/lib
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.services
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @build compiler.jvmci.common.JVMCIHelpers
 * @run driver jdk.test.lib.FileInstaller ./JvmciNotifyBootstrapFinishedEventTest.config
 *     ./META-INF/services/jdk.vm.ci.services.JVMCIServiceLocator
 * @run driver jdk.test.lib.helpers.ClassFileInstaller
 *      compiler.jvmci.common.JVMCIHelpers$EmptyHotspotCompiler
 *      compiler.jvmci.common.JVMCIHelpers$EmptyCompilerFactory
 *      compiler.jvmci.common.JVMCIHelpers$EmptyCompilationRequestResult
 *      compiler.jvmci.common.JVMCIHelpers$EmptyVMEventListener
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *     -Djvmci.Compiler=EmptyCompiler -Xbootclasspath/a:.
 *     -XX:+UseJVMCICompiler -XX:-BootstrapJVMCI -XX:-UseJVMCINativeLibrary
 *     -Dcompiler.jvmci.events.JvmciNotifyBootstrapFinishedEventTest.bootstrap=false
 *     compiler.jvmci.events.JvmciNotifyBootstrapFinishedEventTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *     -Djvmci.Compiler=EmptyCompiler -Xbootclasspath/a:.
 *     -XX:+UseJVMCICompiler -XX:+BootstrapJVMCI -XX:-UseJVMCINativeLibrary
 *     -Dcompiler.jvmci.events.JvmciNotifyBootstrapFinishedEventTest.bootstrap=true
 *     compiler.jvmci.events.JvmciNotifyBootstrapFinishedEventTest
 */

package compiler.jvmci.events;

import jdk.test.lib.Asserts;
import jdk.vm.ci.services.JVMCIServiceLocator;
import jdk.vm.ci.hotspot.HotSpotVMEventListener;

public class JvmciNotifyBootstrapFinishedEventTest extends JVMCIServiceLocator implements HotSpotVMEventListener {
    private static final boolean BOOTSTRAP = Boolean
            .getBoolean("compiler.jvmci.events.JvmciNotifyBootstrapFinishedEventTest.bootstrap");
    private static volatile int gotBoostrapNotification = 0;

    public static void main(String args[]) {
        if (BOOTSTRAP) {
            Asserts.assertEQ(gotBoostrapNotification, 1, "Did not receive expected number of bootstrap events");
        } else {
            Asserts.assertEQ(gotBoostrapNotification, 0, "Got unexpected bootstrap event");
        }
    }

    @Override
    public <S> S getProvider(Class<S> service) {
        if (service == HotSpotVMEventListener.class) {
            return service.cast(this);
        }
        return null;
    }

    @Override
    public void notifyBootstrapFinished() {
        gotBoostrapNotification++;
    }
}
