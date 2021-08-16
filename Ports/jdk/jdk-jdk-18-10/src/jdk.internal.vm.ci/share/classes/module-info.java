/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

module jdk.internal.vm.ci {
    exports jdk.vm.ci.services to
        jdk.internal.vm.compiler,
        jdk.internal.vm.compiler.management;
    exports jdk.vm.ci.runtime to
        jdk.internal.vm.compiler,
        jdk.internal.vm.compiler.management;
    exports jdk.vm.ci.meta to jdk.internal.vm.compiler;
    exports jdk.vm.ci.code to jdk.internal.vm.compiler;
    exports jdk.vm.ci.hotspot to jdk.internal.vm.compiler;

    uses jdk.vm.ci.services.JVMCIServiceLocator;
    uses jdk.vm.ci.hotspot.HotSpotJVMCIBackendFactory;

    provides jdk.vm.ci.hotspot.HotSpotJVMCIBackendFactory with
        jdk.vm.ci.hotspot.aarch64.AArch64HotSpotJVMCIBackendFactory,
        jdk.vm.ci.hotspot.amd64.AMD64HotSpotJVMCIBackendFactory;
}
