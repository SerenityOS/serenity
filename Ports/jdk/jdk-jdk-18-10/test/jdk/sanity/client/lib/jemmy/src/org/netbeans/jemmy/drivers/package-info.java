/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
 * <h1>Drivers interfaces</h1>
 * Contains interfaces of "drivers".<br><br>
 * Driver is a class which actually implements action reproducing. There are
 * different types of drivers (mouse driver, keyboard driver, button drivers,
 * ...), each of them represented by interface (button driver - by ButtonDriver
 * interface, ...)<br><br>
 * Package also contains some classes allowing to manage driver set.<br><br>
 * Subpackages contain driver implementations.<br><br>
 * Drivers is low-level API: they are not supposed to be used directly from
 * test.<br>
 *
 * @since 04/17/2002
 * <hr>
 */
package org.netbeans.jemmy.drivers;
