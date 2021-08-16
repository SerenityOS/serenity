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

import org.netbeans.jemmy.operators.ComponentOperator;

/**
 * Allows to declare supported operator classes.
 */
abstract public class SupportiveDriver implements Driver {

    private Class<?>[] supported;

    /**
     * Creates an instance.
     *
     * @param supported Array of operator classes which are supported by this
     * driver.
     */
    public SupportiveDriver(Class<?>[] supported) {
        this.supported = supported;
    }

    /**
     * Throws {@code UnsupportedOperatorException} exception if parameter's
     * class is not in list of supported classes.
     *
     * @param oper Operator whose class should be checked.
     * @throws UnsupportedOperatorException
     */
    public void checkSupported(ComponentOperator oper) {
        UnsupportedOperatorException.checkSupported(getClass(), supported, oper.getClass());
    }

    /**
     * Returns array of operator classes which are supported by this driver.
     */
    @Override
    public Class<?>[] getSupported() {
        return supported;
    }
}
