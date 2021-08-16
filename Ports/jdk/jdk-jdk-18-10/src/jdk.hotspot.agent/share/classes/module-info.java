/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Defines the implementation of the HotSpot Serviceability Agent.
 *
 * <p> This module includes the <em>{@index jhsdb jhsdb tool}</em> tool to
 * attach to a running Java Virtual Machine (JVM) or launch a postmortem
 * debugger to analyze the content of a core-dump from a crashed JVM.
 *
 * @toolGuide jhsdb
 *
 * @moduleGraph
 * @since 9
 */
module jdk.hotspot.agent {
    requires java.datatransfer;
    requires java.desktop;
    requires java.rmi;
    requires java.scripting;

    // RMI needs to serialize types in this package
    exports sun.jvm.hotspot.debugger.remote to java.rmi;

}
