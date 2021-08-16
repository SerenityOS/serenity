/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.swing.interop;

import java.awt.SecondaryLoop;
import java.awt.EventQueue;
import sun.awt.FwDispatcher;
import sun.awt.AWTAccessor;

/**
 * This class provides a wrapper over inner class DispatcherProxy
 * which implements jdk internal sun.awt.FwDispatcher interface
 * and provides APIs to be used by FX swing interop to access and use
 * FwDispatcher APIs.
 *
 * @since 11
 */
public abstract class DispatcherWrapper {
    private DispatcherProxy fwd;

    public DispatcherWrapper() {
        fwd = new DispatcherProxy();
    }

    public abstract boolean isDispatchThread();

    public abstract void scheduleDispatch(Runnable r);

    public abstract SecondaryLoop createSecondaryLoop();

    public static void setFwDispatcher(EventQueue eventQueue, DispatcherWrapper dispatcher) {
        AWTAccessor.getEventQueueAccessor().setFwDispatcher(eventQueue, dispatcher.fwd);
    }

    private class DispatcherProxy implements FwDispatcher {

        @Override
        public boolean isDispatchThread() {
            return DispatcherWrapper.this.isDispatchThread();
        }

        @Override
        public void scheduleDispatch(Runnable r) {
            DispatcherWrapper.this.scheduleDispatch(r);
        }

        @Override
        public SecondaryLoop createSecondaryLoop() {
            return DispatcherWrapper.this.createSecondaryLoop();
        }
    }
}
