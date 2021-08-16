/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
package test.java.lang.invoke.remote;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import test.java.lang.invoke.MethodHandlesTest;

/**
 * Out-of-package access into protected members of test.java.lang.invoke.remote.MethodHandle.PubExample.
 */
public class RemoteExample extends MethodHandlesTest.PubExample {
    public RemoteExample() { super("RemoteExample"); }
    public static Lookup lookup() { return MethodHandles.lookup(); }
    public final     void fin_v0() { MethodHandlesTest.called("Rem/fin_v0", this); }
    protected        void pro_v0() { MethodHandlesTest.called("Rem/pro_v0", this); }
    protected static void pro_s0() { MethodHandlesTest.called("Rem/pro_s0"); }
}
