/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.x500;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.Destroyable;

/**
 * <p> This class represents an {@code X500PrivateCredential}.
 * It associates an X.509 certificate, corresponding private key and the
 * KeyStore alias used to reference that exact key pair in the KeyStore.
 * This enables looking up the private credentials for an X.500 principal
 * in a subject.
 *
 * @since 1.4
 */
public final class X500PrivateCredential implements Destroyable {
    private X509Certificate cert;
    private PrivateKey key;
    private String alias;

    /**
     * Creates an X500PrivateCredential that associates an X.509 certificate,
     * a private key and the KeyStore alias.
     *
     * @param cert X509Certificate
     * @param key  PrivateKey for the certificate
     * @exception IllegalArgumentException if either {@code cert} or
     * {@code key} is null
     *
     */

    public X500PrivateCredential(X509Certificate cert, PrivateKey key) {
        if (cert == null || key == null )
            throw new IllegalArgumentException();
        this.cert = cert;
        this.key = key;
        this.alias=null;
    }

    /**
     * Creates an X500PrivateCredential that associates an X.509 certificate,
     * a private key and the KeyStore alias.
     *
     * @param cert X509Certificate
     * @param key  PrivateKey for the certificate
     * @param alias KeyStore alias
     * @exception IllegalArgumentException if either {@code cert},
     * {@code key} or {@code alias} is null
     *
     */
    public X500PrivateCredential(X509Certificate cert, PrivateKey key,
                                 String alias) {
        if (cert == null || key == null|| alias == null )
            throw new IllegalArgumentException();
        this.cert = cert;
        this.key = key;
        this.alias=alias;
    }

    /**
     * Returns the X.509 certificate.
     *
     * @return the X509Certificate
     */

    public X509Certificate getCertificate() {
        return cert;
    }

    /**
     * Returns the PrivateKey.
     *
     * @return the PrivateKey
     */
    public PrivateKey getPrivateKey() {
        return key;
    }

    /**
     * Returns the KeyStore alias.
     *
     * @return the KeyStore alias
     */

    public String getAlias() {
        return alias;
    }

    /**
     * Clears the references to the X.509 certificate, private key and the
     * KeyStore alias in this object.
     */

    public void destroy() {
        cert = null;
        key = null;
        alias =null;
    }

    /**
     * Determines if the references to the X.509 certificate and private key
     * in this object have been cleared.
     *
     * @return true if X509Certificate and the PrivateKey are null
     */
    public boolean isDestroyed() {
        return cert == null && key == null && alias==null;
    }
}
