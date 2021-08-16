/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * That class is to use as an MBean operation parameter or returned value.
 * The property Glop with its public getter + setter is only there to be
 * reconstructible following MXBean specification, so that SqeParameter can be
 * used for what it is designed to.
 */
public class SqeParameter implements Serializable {

    private static boolean weird;
    private String glop;

    static {
        if ( System.getProperty("WEIRD_PARAM") != null ) {
            weird = true;
        }
    }

    /**
     * Creates a new instance of SqeParameter.
     * <br>When the Java property WEIRD_PARAM is set, that constructor
     * throws an exception.
     * <br>That can be used to ensure the class is instantiated on server side
     * but never on client side.
     */
    public SqeParameter() throws Exception {
        if ( weird ) {
            throw new Exception();
        }
    }

    public String getGlop() {
        return glop;
    }

    public void setGlop(String value) {
        glop = value;
    }
}
