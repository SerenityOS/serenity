/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.CertPath;
import java.security.cert.X509Extension;
import java.util.Date;
import java.util.List;

/**
 * This class encapsulates information about a signed timestamp.
 * It is immutable.
 * It includes the timestamp's date and time as well as information about the
 * Timestamping Authority (TSA) which generated and signed the timestamp.
 *
 * @since 1.5
 * @author Vincent Ryan
 */

public final class Timestamp implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = -5502683707821851294L;

    /**
     * The timestamp's date and time
     *
     * @serial
     */
    private Date timestamp;

    /**
     * The TSA's certificate path.
     *
     * @serial
     */
    private CertPath signerCertPath;

    /*
     * Hash code for this timestamp.
     */
    private transient int myhash = -1;

    /**
     * Constructs a Timestamp.
     *
     * @param timestamp is the timestamp's date and time. It must not be null.
     * @param signerCertPath is the TSA's certificate path. It must not be null.
     * @throws NullPointerException if timestamp or signerCertPath is null.
     */
    public Timestamp(Date timestamp, CertPath signerCertPath) {
        if (timestamp == null || signerCertPath == null) {
            throw new NullPointerException();
        }
        this.timestamp = new Date(timestamp.getTime()); // clone
        this.signerCertPath = signerCertPath;
    }

    /**
     * Returns the date and time when the timestamp was generated.
     *
     * @return The timestamp's date and time.
     */
    public Date getTimestamp() {
        return new Date(timestamp.getTime()); // clone
    }

    /**
     * Returns the certificate path for the Timestamping Authority.
     *
     * @return The TSA's certificate path.
     */
    public CertPath getSignerCertPath() {
        return signerCertPath;
    }

    /**
     * Returns the hash code value for this timestamp.
     * The hash code is generated using the date and time of the timestamp
     * and the TSA's certificate path.
     *
     * @return a hash code value for this timestamp.
     */
    public int hashCode() {
        if (myhash == -1) {
            myhash = timestamp.hashCode() + signerCertPath.hashCode();
        }
        return myhash;
    }

    /**
     * Tests for equality between the specified object and this
     * timestamp. Two timestamps are considered equal if the date and time of
     * their timestamp's and their signer's certificate paths are equal.
     *
     * @param obj the object to test for equality with this timestamp.
     *
     * @return true if the timestamp are considered equal, false otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof Timestamp other
                && (timestamp.equals(other.getTimestamp()) &&
                signerCertPath.equals(other.getSignerCertPath()));
    }

    /**
     * Returns a string describing this timestamp.
     *
     * @return A string comprising the date and time of the timestamp and
     *         its signer's certificate.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("timestamp: " + timestamp);
        List<? extends Certificate> certs = signerCertPath.getCertificates();
        if (!certs.isEmpty()) {
            sb.append("TSA: " + certs.get(0));
        } else {
            sb.append("TSA: <empty>");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Restores the state of this object from the stream, and explicitly
     * resets hash code value to -1.
     *
     * @param  ois the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        myhash = -1;
        timestamp = new Date(timestamp.getTime());
    }
}
