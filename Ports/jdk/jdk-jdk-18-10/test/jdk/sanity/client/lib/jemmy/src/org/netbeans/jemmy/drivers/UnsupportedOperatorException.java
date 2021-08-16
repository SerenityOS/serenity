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

import org.netbeans.jemmy.JemmyException;

/**
 * Is thrown as a result of attempt to use driver for unsupported operator type.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class UnsupportedOperatorException extends JemmyException {

    private static final long serialVersionUID = 42L;

    /**
     * Constructor.
     *
     * @param driver a driver
     * @param operator an operator
     */
    public UnsupportedOperatorException(Class<?> driver, Class<?> operator) {
        super(driver.getName() + " operators are not supported by "
                + operator.getName() + " driver!");
    }

    /**
     * Checks if operator class is in the list of supported classes.
     *
     * @param driver Driver class
     * @param supported Supported classes.
     * @param operator Operator class.
     * @throws UnsupportedOperatorException if class is not supported.
     */
    public static void checkSupported(Class<?> driver, Class<?>[] supported, Class<?> operator) {
        for (Class<?> aSupported : supported) {
            if (aSupported.isAssignableFrom(operator)) {
                return;
            }
        }
        throw (new UnsupportedOperatorException(driver, operator));
    }

    /**
     * Checks if operator class name is in the list of supported classes names.
     *
     * @param driver Driver class
     * @param supported Supported classes names.
     * @param operator Operator class.
     * @throws UnsupportedOperatorException if class is not supported.
     */
    public static void checkSupported(Class<?> driver, String[] supported, Class<?> operator) {
        Class<?> opClass = operator;
        do {
            for (String aSupported : supported) {
                if (opClass.getName().equals(aSupported)) {
                    return;
                }
            }
        } while ((opClass = opClass.getSuperclass()) != null);
        throw (new UnsupportedOperatorException(driver, operator));
    }
}
