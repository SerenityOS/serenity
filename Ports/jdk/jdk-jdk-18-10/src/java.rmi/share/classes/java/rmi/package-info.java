/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Provides the RMI package. RMI is Remote Method Invocation.  It is a
 * mechanism that enables an object on one Java virtual machine to invoke
 * methods on an object in another Java virtual machine.  Any object that
 * can be invoked this way must implement the Remote interface. When such
 * an object is invoked, its arguments are ``marshalled'' and sent from the
 * local virtual machine to the remote one, where the arguments are
 * ``unmarshalled.''  When the method terminates, the results are
 * marshalled from the remote machine and sent to the caller's virtual
 * machine.  If the method invocation results in an exception being
 * thrown, the exception is indicated to caller.
 *
 * @since 1.1
 */
package java.rmi;
