/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This is the core package of the Java Debug
 * Interface (JDI), it defines mirrors for values, types, and the target
 * VirtualMachine itself - as well bootstrapping facilities.
 * {@link com.sun.jdi.VirtualMachine} mirrors the target virtual machine and
 * is the origin of all information provided by the JDI.  A VirtualMachine
 * is typically created by using the
 * {@link com.sun.jdi.VirtualMachineManager} to create
 * a connection to the target virtual machine (see the
 * {@link com.sun.jdi.connect} package).  In turn the
 * {@link com.sun.jdi.VirtualMachineManager} is typically created by calling
 * {@link com.sun.jdi.Bootstrap#virtualMachineManager()}.
 * <p>
 * Most of the methods within this package can throw the unchecked exception
 * {@link com.sun.jdi.VMDisconnectedException}.
 * <p>
 * Methods may be added to the interfaces in the JDI packages in future
 * releases. Existing packages may be renamed if the JDI becomes a standard
 * extension.
 */

package com.sun.jdi;
