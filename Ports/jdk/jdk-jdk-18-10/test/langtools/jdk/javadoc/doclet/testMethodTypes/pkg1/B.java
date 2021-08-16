/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package pkg1;

/**
 * This interface has different types of methods such as "Static Methods",
 * "Instance Methods", "Abstract Methods", "Default Methods".
 */
public interface B {

    /**
     * This is the first abstract instance method.
     */
    public void setName();

    /**
     * This is the second abstract instance method.
     * @return a string
     */
    public String getName();

    /**
     * This is the third abstract instance method.
     * @return a boolean value
     */
    public boolean addEntry();

    /**
     * This is the fourth abstract instance method.
     * @return a boolean value
     */
    public boolean removeEntry();

    /**
     * This is the fifth abstract instance method.
     * @return a string
     */
    public String getPermissions();

    /**
     * A static interface method.
     */
    public static void aStaticMethod() {}

    /**
     * Another static interface method.
     */
    public static void anotherStaticMethod() {}

    /**
     * A default method.
     */
    public default void aDefaultMethod() {}

    /**
     * Another default method.
     */
    public default void anotherDefaultMethod() {}
}
