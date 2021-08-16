/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 *  Summary
 *  -------
 *
 *  Define a lightweight network protocol for discovering running and
 *  manageable Java processes within a network subnet.
 *
 *
 * Description
 * -----------
 *
 * The protocol is lightweight multicast based, and works like a beacon,
 * broadcasting the JMXService URL needed to connect to the external JMX
 * agent if an application is started with appropriate parameters.
 *
 * The payload is structured like this:
 *
 *  4 bytes JDP magic (0xC0FFEE42)
 *  2 bytes JDP protocol version (1)
 *  2 bytes size of the next entry
 *      x bytes next entry (UTF-8 encoded)
 *  2 bytes size of next entry
 *    ...   Rinse and repeat...
 *
 * The payload will be parsed as even entries being keys, odd entries being
 * values.
 *
 * The standard JDP packet contains four entries:
 *
 * - `DISCOVERABLE_SESSION_UUID` -- Unique id of the instance; this id changes every time
 *    the discovery protocol starts and stops
 *
 * - `MAIN_CLASS` -- The value of the `sun.java.command` property
 *
 * - `JMX_SERVICE_URL` -- The URL to connect to the JMX agent
 *
 * - `INSTANCE_NAME` -- The user-provided name of the running instance
 *
 * The protocol sends packets to 224.0.23.178:7095 by default.
 *
 * The protocol uses system properties to control it's behaviour:
 * - `com.sun.management.jdp.port` -- override default port
 *
 * - `com.sun.management.jdp.address` -- override default address
 *
 * - `com.sun.management.jmxremote.autodiscovery` -- whether we should start autodiscovery or
 * not. Autodiscovery starts if and only if following conditions are met: (autodiscovery is
 * true OR (autodiscovery is not set AND jdp.port is set))
 *
 * - `com.sun.management.jdp.ttl`         -- set ttl for broadcast packet, default is 1
 * - `com.sun.management.jdp.pause`       -- set broadcast interval in seconds default is 5
 * - `com.sun.management.jdp.source_addr` -- an address of interface to use for broadcast
 */

package sun.management.jdp;
