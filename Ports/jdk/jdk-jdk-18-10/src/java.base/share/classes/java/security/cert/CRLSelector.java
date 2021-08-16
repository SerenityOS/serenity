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
 * A selector that defines a set of criteria for selecting {@code CRL}s.
 * Classes that implement this interface are often used to specify
 * which {@code CRL}s should be retrieved from a {@code CertStore}.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this interface are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CRL
 * @see CertStore
 * @see CertStore#getCRLs
 *
 * @author      Steve Hanna
 * @since       1.4
 */
public interface CRLSelector extends Cloneable {

    /**
     * Decides whether a {@code CRL} should be selected.
     *
     * @param   crl     the {@code CRL} to be checked
     * @return  {@code true} if the {@code CRL} should be selected,
     * {@code false} otherwise
     */
    boolean match(CRL crl);

    /**
     * Makes a copy of this {@code CRLSelector}. Changes to the
     * copy will not affect the original and vice versa.
     *
     * @return a copy of this {@code CRLSelector}
     */
    Object clone();
}
