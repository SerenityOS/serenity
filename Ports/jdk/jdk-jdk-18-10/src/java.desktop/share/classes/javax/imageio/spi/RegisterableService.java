/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.spi;

/**
 * An optional interface that may be provided by service provider
 * objects that will be registered with a
 * {@code ServiceRegistry}.  If this interface is present,
 * notification of registration and deregistration will be performed.
 *
 * @see ServiceRegistry
 *
 */
public interface RegisterableService {

    /**
     * Called when an object implementing this interface is added to
     * the given {@code category} of the given
     * {@code registry}.  The object may already be registered
     * under another category or categories.
     *
     * @param registry a {@code ServiceRegistry} where this
     * object has been registered.
     * @param category a {@code Class} object indicating the
     * registry category under which this object has been registered.
     */
    void onRegistration(ServiceRegistry registry, Class<?> category);

    /**
     * Called when an object implementing this interface is removed
     * from the given {@code category} of the given
     * {@code registry}.  The object may still be registered
     * under another category or categories.
     *
     * @param registry a {@code ServiceRegistry} from which this
     * object is being (wholly or partially) deregistered.
     * @param category a {@code Class} object indicating the
     * registry category from which this object is being deregistered.
     */
    void onDeregistration(ServiceRegistry registry, Class<?> category);
}
