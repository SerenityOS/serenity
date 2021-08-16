/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import sun.management.counter.*;

/**
 */
public class MethodInfo implements java.io.Serializable {
    private String name;
    private long type;
    private int compileSize;

    MethodInfo(String name, long type, int compileSize) {
        this.name = name;
        this.type = type;
        this.compileSize = compileSize;
    }

    /**
     * Returns the name of the compiled method.
     *
     * @return the name of the compiled method.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the compiled method such as normal-compile,
     * osr-compile, and native-compile.
     *
     * @return the type of the compiled method.
     */
    public long getType() {
        return type;
    }

    /**
     * Returns the number of bytes occupied by this compiled method.
     * This method returns -1 if not available.
     *
     * @return the number of bytes occupied by this compiled method.
     */
    public int getCompileSize() {
        return compileSize;
    }

    public String toString() {
        return getName() + " type = " + getType() +
            " compileSize = " + getCompileSize();
    }

    private static final long serialVersionUID = 6992337162326171013L;

}
