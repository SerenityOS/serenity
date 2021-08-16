/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.GuardingDynamicLinkerExporter;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

/**
 * A trusted linker exporter (build file gives appropriate permission to the jar containing this class!).
 */
public final class TestGuardingDynamicLinkerExporter extends GuardingDynamicLinkerExporter {

    private static final ThreadLocal<CallSiteDescriptor> lastDescriptor = new ThreadLocal<>();
    private static boolean enabled = false;

    public static void enable() {
        reset(true);
    }

    public static void disable() {
        reset(false);
    }
    public static boolean isLastCallSiteDescriptor(final CallSiteDescriptor desc) {
        return lastDescriptor.get() == desc;
    }

    private static void reset(final boolean enable) {
        lastDescriptor.set(null);
        enabled = enable;
    }

    @Override
    public List<GuardingDynamicLinker> get() {
        return List.of(((final LinkRequest linkRequest, final LinkerServices linkerServices) -> {
            if (enabled) {
                lastDescriptor.set(linkRequest.getCallSiteDescriptor());
            }
            return null;
        }));
    }
}
