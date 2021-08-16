/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.access;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface JavaBeansAccess {
    /**
     * Returns the getter method for a property of the given name
     * @param clazz The JavaBeans class
     * @param property The property name
     * @return The resolved property getter method
     * @throws Exception
     */
    Method getReadMethod(Class<?> clazz, String property) throws Exception;

    /**
     * Return the <b>value</b> attribute of the associated
     * <code>@ConstructorProperties</code> annotation if that is present.
     * @param ctr The constructor to extract the annotation value from
     * @return The {@code value} attribute of the <code>@ConstructorProperties</code>
     *         annotation or {@code null} if the constructor is not annotated by
     *         this annotation or the annotation is not accessible.
     */
    String[] getConstructorPropertiesValue(Constructor<?> ctr);
}
