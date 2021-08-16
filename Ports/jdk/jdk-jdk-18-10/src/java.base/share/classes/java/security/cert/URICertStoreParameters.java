/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;

/**
 * Parameters used as input for {@code CertStore} algorithms which use
 * information contained in a URI to retrieve certificates and CRLs.
 * <p>
 * This class is used to provide necessary configuration parameters
 * through a URI as defined in RFC 5280 to implementations of
 * {@code CertStore} algorithms.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @since       9
 * @see         CertStore
 * @see         java.net.URI
 */
public final class URICertStoreParameters implements CertStoreParameters {

    /**
     * The uri, cannot be null
     */
    private final URI uri;

    /*
     * Hash code for this parameters.
     */
    private int myhash = -1;

    /**
     * Creates an instance of {@code URICertStoreParameters} with the
     * specified URI.
     *
     * @param uri the URI which contains configuration information.
     * @throws NullPointerException if {@code uri} is null
     */
    public URICertStoreParameters(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        this.uri = uri;
    }

    /**
     * Returns the URI used to construct this
     * {@code URICertStoreParameters} object.
     *
     * @return the URI.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Returns a copy of this object. Changes to the copy will not affect
     * the original and vice versa.
     *
     * @return the copy
     */
    @Override
    public URICertStoreParameters clone() {
        try {
            return new URICertStoreParameters(uri);
        } catch (NullPointerException e) {
            /* Cannot happen */
            throw new InternalError(e.toString(), e);
        }
    }

    /**
     * Returns a hash code value for this parameters object.
     * The hash code is generated using the URI supplied at construction.
     *
     * @return a hash code value for this parameters.
     */
    @Override
    public int hashCode() {
        if (myhash == -1) {
            myhash = uri.hashCode()*7;
        }
        return myhash;
    }

    /**
     * Compares the specified object with this parameters object for equality.
     * Two URICertStoreParameters are considered equal if the URIs used
     * to construct them are equal.
     *
     * @param p the object to test for equality with this parameters.
     *
     * @return true if the specified object is equal to this parameters object.
     */
    @Override
    public boolean equals(Object p) {
        if (p == this) {
            return true;
        }
        return p instanceof URICertStoreParameters other
                && uri.equals(other.getURI());
    }

    /**
     * Returns a formatted string describing the parameters
     * including the URI used to construct this object.
     *
     * @return a formatted string describing the parameters
     */
    @Override
    public String toString() {
        return "URICertStoreParameters: " + uri.toString();
    }
}
