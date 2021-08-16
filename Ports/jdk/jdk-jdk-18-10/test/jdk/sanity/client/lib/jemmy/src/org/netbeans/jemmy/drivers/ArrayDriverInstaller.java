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
package org.netbeans.jemmy.drivers;

/**
 * Auxiliary class making driver registration easier.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class ArrayDriverInstaller implements DriverInstaller {

    String[] ids;
    Object[] drivers;

    /**
     * Constructs an ArrayDriverInstaller object. Both parameter arrays mush
     * have same length, {@code drivers} must keep instances of
     * <a href = "Driver.html">Driver</a> or
     * <a href = "Driver.html">LightDriver</a> implementations.
     *
     * @param ids an array of driver IDs
     * @param drivers an array of drivers.
     */
    public ArrayDriverInstaller(String[] ids, Object[] drivers) {
        this.ids = ids;
        this.drivers = drivers;
    }

    /**
     * Installs drivers from the array passed into constructor.
     */
    @Override
    public void install() {
        for (int i = 0; i < ids.length; i++) {
            DriverManager.setDriver(ids[i], drivers[i]);
        }
    }
}
