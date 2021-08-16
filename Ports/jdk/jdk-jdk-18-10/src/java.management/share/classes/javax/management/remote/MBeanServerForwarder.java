/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.remote;

import javax.management.MBeanServer;

/**
 * <p>An object of this class implements the MBeanServer interface and
 * wraps another object that also implements that interface.
 * Typically, an implementation of this interface performs some action
 * in some or all methods of the <code>MBeanServer</code> interface
 * before and/or after forwarding the method to the wrapped object.
 * Examples include security checking and logging.</p>
 *
 * @since 1.5
 */
public interface MBeanServerForwarder extends MBeanServer {

    /**
     * Returns the MBeanServer object to which requests will be forwarded.
     *
     * @return the MBeanServer object to which requests will be forwarded,
     * or null if there is none.
     *
     * @see #setMBeanServer
     */
    public MBeanServer getMBeanServer();

    /**
     * Sets the MBeanServer object to which requests will be forwarded
     * after treatment by this object.
     *
     * @param mbs the MBeanServer object to which requests will be forwarded.
     *
     * @exception IllegalArgumentException if this object is already
     * forwarding to an MBeanServer object or if <code>mbs</code> is
     * null or if <code>mbs</code> is identical to this object.
     *
     * @see #getMBeanServer
     */
    public void setMBeanServer(MBeanServer mbs);
}
