/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.util.Date;

/**
 * The AppleEvent class is simply an object to be passed to a
 * remote object exported by an applet.  The intent is to verify
 * proper object serialization of arrays.
 */
public class AppleEvent implements Serializable {

    public static final int BUY   = 0;
    public static final int EAT   = 1;
    public static final int THROW = 2;

    private final int what;
    private final Date when;

    public AppleEvent(int what) {
        this.what = what;
        this.when = new Date();
    }

    public String toString() {
        String desc = "[";
        switch (what) {
        case BUY:
            desc += "BUY";
            break;
        case EAT:
            desc += "EAT";
            break;
        case THROW:
            desc += "THROW";
            break;
        }
        desc += " @ " + when + "]";
        return desc;
    }
}
