/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary.rtm;

/**
 * Current RTM locking implementation force transaction abort
 * before native method call by explicit xabort(0) call.
 */
public class XAbortProvoker extends AbortProvoker {

    static {
        try {
            System.loadLibrary("XAbortProvoker");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Could not load native library: " + e);
        }
    }

    public native int doAbort();

    // Following field have to be static in order to avoid escape analysis.
    @SuppressWarnings("UnsuedDeclaration")
    private static int field = 0;

    public XAbortProvoker() {
        this(new Object());
    }

    public XAbortProvoker(Object monitor) {
        super(monitor);
    }

    @Override
    public void forceAbort() {
        synchronized(monitor) {
            XAbortProvoker.field = doAbort();
        }
    }

    @Override
    public String[] getMethodsToCompileNames() {
        return new String[] {
                getMethodWithLockName(),
                XAbortProvoker.class.getName() + "::doAbort"
        };
    }
}
