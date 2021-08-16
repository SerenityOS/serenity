/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jvmci.common;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotVMEventListener;
import jdk.vm.ci.services.JVMCIServiceLocator;
import jdk.vm.ci.runtime.JVMCICompiler;
import jdk.vm.ci.runtime.JVMCIRuntime;
import jdk.vm.ci.runtime.JVMCICompilerFactory;

/*
 * A stub classes to be able to use jvmci
 */
public class JVMCIHelpers extends JVMCIServiceLocator {

    @Override
    public <S> S getProvider(Class<S> service) {
        if (service == JVMCICompilerFactory.class) {
            return service.cast(new EmptyCompilerFactory());
        }
        if (service == HotSpotVMEventListener.class) {
            return service.cast(new EmptyVMEventListener());
        }
        return null;
    }

    public static class EmptyVMEventListener implements HotSpotVMEventListener {
        // just empty, using default interface methods
    }

    public static class EmptyCompilationRequestResult implements CompilationRequestResult {
        @Override
        public Object getFailure() {
            return "no compiler configured";
        }
    }
    public static class EmptyHotspotCompiler implements JVMCICompiler {

        @Override
        public CompilationRequestResult compileMethod(CompilationRequest request) {
            // do nothing
            return new EmptyCompilationRequestResult();
        }
    }

    public static class EmptyCompilerFactory implements JVMCICompilerFactory {

        @Override
        public String getCompilerName() {
            return "EmptyCompiler";
        }

        @Override
        public JVMCICompiler createCompiler(JVMCIRuntime runtime) {
            return new EmptyHotspotCompiler();
        }
    }
}
