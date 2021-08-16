/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.util.Optional;

/**
 * A domain that is registered under a "public suffix". The public suffix is
 * a top-level domain under which names can be registered. For example,
 * "com" and "co.uk" are public suffixes, and "example.com" and "example.co.uk"
 * are registered domains.
 * <p>
 * The primary purpose of this class is to determine if domains are safe to
 * use in various use-cases.
 */
public interface RegisteredDomain {

    public enum Type {
        /**
         * An ICANN registered domain.
         */
        ICANN,
        /**
         * A private registered domain.
         */
        PRIVATE
    }

    /**
     * Returns the name of the registered domain.
     *
     * @return the name of the registered domain
     */
    String name();

    /**
     * Returns the type of the registered domain.
     *
     * @return the type of the registered domain
     */
    Type type();

    /**
     * Returns the public suffix of the registered domain.
     *
     * @return the public suffix of the registered domain
     */
    String publicSuffix();

    /**
     * Returns an {@code Optional<RegisteredDomain>} representing the
     * registered part of the specified domain.
     *
     * @param domain the domain name
     * @return an {@code Optional<RegisteredDomain>}; the {@code Optional} is
     *    empty if the domain is unknown or not registerable
     * @throws NullPointerException if domain is null
     */
    public static Optional<RegisteredDomain> from(String domain) {
        return Optional.ofNullable(DomainName.registeredDomain(domain));
    }
}
