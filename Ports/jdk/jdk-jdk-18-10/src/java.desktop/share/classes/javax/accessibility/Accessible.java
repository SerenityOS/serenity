/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.accessibility;

/**
 * Interface {@code Accessible} is the main interface for the accessibility
 * package. All components that support the accessibility package must implement
 * this interface. It contains a single method, {@link #getAccessibleContext},
 * which returns an instance of the class {@link AccessibleContext}.
 *
 * @author Peter Korn
 * @author Hans Muller
 * @author Willie Walker
 */
public interface Accessible {

    /**
     * Returns the {@code AccessibleContext} associated with this object. In
     * most cases, the return value should not be {@code null} if the object
     * implements interface {@code Accessible}. If a component developer creates
     * a subclass of an object that implements {@code Accessible}, and that
     * subclass is not {@code Accessible}, the developer should override the
     * {@code getAccessibleContext} method to return {@code null}.
     *
     * @return the {@code AccessibleContext} associated with this object
     */
    public AccessibleContext getAccessibleContext();
}
