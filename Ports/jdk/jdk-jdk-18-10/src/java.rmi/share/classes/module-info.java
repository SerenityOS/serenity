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
 * Defines the Remote Method Invocation (RMI) API.
 *
 * <p> The JDK implementation of this module includes
 * the <em>{@index rmiregistry rmiregistry tool}</em> tool to start a remote
 * object registry.
 *
 * @toolGuide rmiregistry
 *
 * @uses java.rmi.server.RMIClassLoaderSpi
 *
 * @moduleGraph
 * @since 9
 */
module java.rmi {
    requires java.logging;

    exports java.rmi;
    exports java.rmi.dgc;
    exports java.rmi.registry;
    exports java.rmi.server;
    exports javax.rmi.ssl;

    exports sun.rmi.registry to
        jdk.management.agent;
    exports sun.rmi.server to
        java.management.rmi,
        jdk.management.agent,
        jdk.jconsole;
    exports sun.rmi.transport to
        java.management.rmi,
        jdk.management.agent,
        jdk.jconsole;

    uses java.rmi.server.RMIClassLoaderSpi;
}
