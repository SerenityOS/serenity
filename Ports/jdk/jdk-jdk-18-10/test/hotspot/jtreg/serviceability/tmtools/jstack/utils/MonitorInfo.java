/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package utils;

/**
 *
 * Represents monitor info string
 *
 */
public class MonitorInfo {

    private String type;
    private String monitorAddress;
    private String monitorClass;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMonitorAddress() {
        return monitorAddress;
    }

    public void setMonitorAddress(String monitorAddress) {
        this.monitorAddress = monitorAddress;
    }

    public String getMonitorClass() {
        return monitorClass;
    }

    public void setMonitorClass(String monitorClass) {
        this.monitorClass = monitorClass;
    }

    public boolean equals(MonitorInfo another) {
        if (!type.equals(another.type)) {
            Utils.log("type", type, another.type);
            return false;
        }

        if (!monitorAddress.equals(another.monitorAddress)) {
            Utils.log("monitorAddress", monitorAddress, another.monitorAddress);
            return false;
        }

        if (!monitorClass.equals(another.monitorClass)) {
            Utils.log("monitorClass", monitorClass, another.monitorClass);
            return false;
        }

        return true;
    }

    public String toString() {
        return type + " <" + monitorAddress + "> (" + monitorClass + ")";
    }
}
