/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * <p>Performs one or more checks on each {@code Certificate} of a
 * {@code CertPath}.
 *
 * <p>A {@code CertPathChecker} implementation is typically created to extend
 * a certification path validation algorithm. For example, an implementation
 * may check for and process a critical private extension of each certificate
 * in a certification path.
 *
 * @since 1.8
 */
public interface CertPathChecker {

    /**
     * Initializes the internal state of this {@code CertPathChecker}.
     *
     * <p>The {@code forward} flag specifies the order that certificates will
     * be passed to the {@link #check check} method (forward or reverse).
     *
     * @param forward the order that certificates are presented to the
     *        {@code check} method. If {@code true}, certificates are
     *        presented from target to trust anchor (forward); if
     *        {@code false}, from trust anchor to target (reverse).
     * @throws CertPathValidatorException if this {@code CertPathChecker} is
     *         unable to check certificates in the specified order
     */
    void init(boolean forward) throws CertPathValidatorException;

    /**
     * Indicates if forward checking is supported. Forward checking refers
     * to the ability of the {@code CertPathChecker} to perform its checks
     * when certificates are presented to the {@code check} method in the
     * forward direction (from target to trust anchor).
     *
     * @return {@code true} if forward checking is supported, {@code false}
     *         otherwise
     */
    boolean isForwardCheckingSupported();

    /**
     * Performs the check(s) on the specified certificate using its internal
     * state. The certificates are presented in the order specified by the
     * {@code init} method.
     *
     * @param cert the {@code Certificate} to be checked
     * @throws CertPathValidatorException if the specified certificate does
     *         not pass the check
     */
    void check(Certificate cert) throws CertPathValidatorException;
}
