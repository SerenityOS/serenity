/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

/**
 * A specification of {@code CertStore} parameters.
 * <p>
 * The purpose of this interface is to group (and provide type safety for)
 * all {@code CertStore} parameter specifications. All
 * {@code CertStore} parameter specifications must implement this
 * interface.
 * <p>
 * Typically, a {@code CertStoreParameters} object is passed as a parameter
 * to one of the {@link CertStore#getInstance CertStore.getInstance} methods.
 * The {@code getInstance} method returns a {@code CertStore} that
 * is used for retrieving {@code Certificate}s and {@code CRL}s. The
 * {@code CertStore} that is returned is initialized with the specified
 * parameters. The type of parameters needed may vary between different types
 * of {@code CertStore}s.
 *
 * @see CertStore#getInstance
 *
 * @since       1.4
 * @author      Steve Hanna
 */
public interface CertStoreParameters extends Cloneable {

    /**
     * Makes a copy of this {@code CertStoreParameters}.
     * <p>
     * The precise meaning of "copy" may depend on the class of
     * the {@code CertStoreParameters} object. A typical implementation
     * performs a "deep copy" of this object, but this is not an absolute
     * requirement. Some implementations may perform a "shallow copy" of some
     * or all of the fields of this object.
     * <p>
     * Note that the {@code CertStore.getInstance} methods make a copy
     * of the specified {@code CertStoreParameters}. A deep copy
     * implementation of {@code clone} is safer and more robust, as it
     * prevents the caller from corrupting a shared {@code CertStore} by
     * subsequently modifying the contents of its initialization parameters.
     * However, a shallow copy implementation of {@code clone} is more
     * appropriate for applications that need to hold a reference to a
     * parameter contained in the {@code CertStoreParameters}. For example,
     * a shallow copy clone allows an application to release the resources of
     * a particular {@code CertStore} initialization parameter immediately,
     * rather than waiting for the garbage collection mechanism. This should
     * be done with the utmost care, since the {@code CertStore} may still
     * be in use by other threads.
     * <p>
     * Each subclass should state the precise behavior of this method so
     * that users and developers know what to expect.
     *
     * @return a copy of this {@code CertStoreParameters}
     */
    Object clone();
}
