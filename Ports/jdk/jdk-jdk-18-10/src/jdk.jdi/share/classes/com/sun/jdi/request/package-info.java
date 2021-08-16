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
 * This package is used to request that a JDI
 * event be sent under specified conditions.
 * With the exception of termination events, which are
 * always sent, there is one kind of
 * {@link com.sun.jdi.request.EventRequest} for each kind of
 * {@link com.sun.jdi.event.Event Event} - for example,
 * {@link com.sun.jdi.request.BreakpointRequest} is used to request a
 * {@link com.sun.jdi.event.BreakpointEvent BreakpointEvent}.
 * Event requests are created by the
 * {@link com.sun.jdi.request.EventRequestManager}.
 * Events and event processing are defined in the
 * {@link com.sun.jdi.event} package.
 * <p>
 * Methods may be added to the interfaces in the JDI packages in future
 * releases. Existing packages may be renamed if the JDI becomes a standard
 * extension.
 */

package com.sun.jdi.request;
