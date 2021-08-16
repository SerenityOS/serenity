/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto;

/**
 * A secret (symmetric) key.
 * The purpose of this interface is to group (and provide type safety
 * for) all secret key interfaces.
 * <p>
 * Provider implementations of this interface must overwrite the
 * {@code equals} and {@code hashCode} methods inherited from
 * {@link java.lang.Object}, so that secret keys are compared based on
 * their underlying key material and not based on reference.
 * Implementations should override the default {@code destroy} and
 * {@code isDestroyed} methods from the
 * {@link javax.security.auth.Destroyable} interface to enable
 * sensitive key information to be destroyed, cleared, or in the case
 * where such information is immutable, unreferenced.
 * Finally, since {@code SecretKey} is {@code Serializable}, implementations
 * should also override
 * {@link java.io.ObjectOutputStream#writeObject(java.lang.Object)}
 * to prevent keys that have been destroyed from being serialized.
 *
 * <p>Keys that implement this interface return the string {@code RAW}
 * as their encoding format (see {@code getFormat}), and return the
 * raw key bytes as the result of a {@code getEncoded} method call. (The
 * {@code getFormat} and {@code getEncoded} methods are inherited
 * from the {@link java.security.Key} parent interface.)
 *
 * @author Jan Luehe
 *
 * @see SecretKeyFactory
 * @see Cipher
 * @since 1.4
 */

public interface SecretKey extends
    java.security.Key, javax.security.auth.Destroyable {

    /**
     * The class fingerprint that is set to indicate serialization
     * compatibility since J2SE 1.4.
     *
     * @deprecated A {@code serialVersionUID} field in an interface is
     * ineffectual. Do not use; no replacement.
     */
    @Deprecated
    @SuppressWarnings("serial")
    static final long serialVersionUID = -4795878709595146952L;
}
