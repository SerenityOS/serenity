/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jvmci.events;

import jdk.vm.ci.services.JVMCIServiceLocator;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotVMEventListener;

public class JvmciShutdownEventListener extends JVMCIServiceLocator implements HotSpotVMEventListener {
    public static final String MESSAGE = "Shutdown notified";
    public static final String GOT_INTERNAL_ERROR = "Got internal error";

    public static void main(String args[]) {
        try {
            HotSpotJVMCIRuntime.runtime(); // let's trigger that lazy jvmci init
        } catch (Error e) {
            System.out.println(GOT_INTERNAL_ERROR);
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
    public void notifyShutdown() {
        System.out.println(MESSAGE);
    }
}
