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
import java.security.cert.CertPath;

/**
 * This class encapsulates information about a code signer.
 * It is immutable.
 *
 * @since 1.5
 * @author Vincent Ryan
 */

public final class CodeSigner implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 6819288105193937581L;

    /**
     * The signer's certificate path.
     *
     * @serial
     */
    private CertPath signerCertPath;

    /**
     * The signature timestamp.
     *
     * @serial
     */
    private Timestamp timestamp;

    /*
     * Hash code for this code signer.
     */
    private transient int myhash = -1;

    /**
     * Constructs a CodeSigner object.
     *
     * @param signerCertPath The signer's certificate path.
     *                       It must not be {@code null}.
     * @param timestamp A signature timestamp.
     *                  If {@code null} then no timestamp was generated
     *                  for the signature.
     * @throws NullPointerException if {@code signerCertPath} is
     *                              {@code null}.
     */
    public CodeSigner(CertPath signerCertPath, Timestamp timestamp) {
        if (signerCertPath == null) {
            throw new NullPointerException();
        }
        this.signerCertPath = signerCertPath;
        this.timestamp = timestamp;
    }

    /**
     * Returns the signer's certificate path.
     *
     * @return A certificate path.
     */
    public CertPath getSignerCertPath() {
        return signerCertPath;
    }

    /**
     * Returns the signature timestamp.
     *
     * @return The timestamp or {@code null} if none is present.
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the hash code value for this code signer.
     * The hash code is generated using the signer's certificate path and the
     * timestamp, if present.
     *
     * @return a hash code value for this code signer.
     */
    public int hashCode() {
        if (myhash == -1) {
            if (timestamp == null) {
                myhash = signerCertPath.hashCode();
            } else {
                myhash = signerCertPath.hashCode() + timestamp.hashCode();
            }
        }
        return myhash;
    }

    /**
     * Tests for equality between the specified object and this
     * code signer. Two code signers are considered equal if their
     * signer certificate paths are equal and if their timestamps are equal,
     * if present in both.
     *
     * @param obj the object to test for equality with this object.
     *
     * @return true if the objects are considered equal, false otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == null || (!(obj instanceof CodeSigner that))) {
            return false;
        }

        if (this == that) {
            return true;
        }
        Timestamp thatTimestamp = that.getTimestamp();
        if (timestamp == null) {
            if (thatTimestamp != null) {
                return false;
            }
        } else {
            if (thatTimestamp == null ||
                (! timestamp.equals(thatTimestamp))) {
                return false;
            }
        }
        return signerCertPath.equals(that.getSignerCertPath());
    }

    /**
     * Returns a string describing this code signer.
     *
     * @return A string comprising the signer's certificate and a timestamp,
     *         if present.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("Signer: " + signerCertPath.getCertificates().get(0));
        if (timestamp != null) {
            sb.append("timestamp: " + timestamp);
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
    }
}
