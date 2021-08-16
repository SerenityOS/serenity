/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * An immutable sequence of certificates (a certification path).
 * <p>
 * This is an abstract class that defines the methods common to all
 * {@code CertPath}s. Subclasses can handle different kinds of
 * certificates (X.509, PGP, etc.).
 * <p>
 * All {@code CertPath} objects have a type, a list of
 * {@code Certificate}s, and one or more supported encodings. Because the
 * {@code CertPath} class is immutable, a {@code CertPath} cannot
 * change in any externally visible way after being constructed. This
 * stipulation applies to all public fields and methods of this class and any
 * added or overridden by subclasses.
 * <p>
 * The type is a {@code String} that identifies the type of
 * {@code Certificate}s in the certification path. For each
 * certificate {@code cert} in a certification path {@code certPath},
 * {@code cert.getType().equals(certPath.getType())} must be
 * {@code true}.
 * <p>
 * The list of {@code Certificate}s is an ordered {@code List} of
 * zero or more {@code Certificate}s. This {@code List} and all
 * of the {@code Certificate}s contained in it must be immutable.
 * <p>
 * Each {@code CertPath} object must support one or more encodings
 * so that the object can be translated into a byte array for storage or
 * transmission to other parties. Preferably, these encodings should be
 * well-documented standards (such as PKCS#7). One of the encodings supported
 * by a {@code CertPath} is considered the default encoding. This
 * encoding is used if no encoding is explicitly requested (for the
 * {@link #getEncoded() getEncoded()} method, for instance).
 * <p>
 * All {@code CertPath} objects are also {@code Serializable}.
 * {@code CertPath} objects are resolved into an alternate
 * {@link CertPathRep CertPathRep} object during serialization. This allows
 * a {@code CertPath} object to be serialized into an equivalent
 * representation regardless of its underlying implementation.
 * <p>
 * {@code CertPath} objects can be created with a
 * {@code CertificateFactory} or they can be returned by other classes,
 * such as a {@code CertPathBuilder}.
 * <p>
 * By convention, X.509 {@code CertPath}s (consisting of
 * {@code X509Certificate}s), are ordered starting with the target
 * certificate and ending with a certificate issued by the trust anchor. That
 * is, the issuer of one certificate is the subject of the following one. The
 * certificate representing the {@link TrustAnchor TrustAnchor} should not be
 * included in the certification path. Unvalidated X.509 {@code CertPath}s
 * may not follow these conventions. PKIX {@code CertPathValidator}s will
 * detect any departure from these conventions that cause the certification
 * path to be invalid and throw a {@code CertPathValidatorException}.
 *
 * <p> Every implementation of the Java platform is required to support the
 * following standard {@code CertPath} encodings:
 * <ul>
 * <li>{@code PKCS7}</li>
 * <li>{@code PkiPath}</li>
 * </ul>
 * These encodings are described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#certpath-encodings">
 * CertPath Encodings section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other encodings are supported.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * All {@code CertPath} objects must be thread-safe. That is, multiple
 * threads may concurrently invoke the methods defined in this class on a
 * single {@code CertPath} object (or more than one) with no
 * ill effects. This is also true for the {@code List} returned by
 * {@code CertPath.getCertificates}.
 * <p>
 * Requiring {@code CertPath} objects to be immutable and thread-safe
 * allows them to be passed around to various pieces of code without worrying
 * about coordinating access.  Providing this thread-safety is
 * generally not difficult, since the {@code CertPath} and
 * {@code List} objects in question are immutable.
 *
 * @see CertificateFactory
 * @see CertPathBuilder
 *
 * @author      Yassir Elley
 * @since       1.4
 */
public abstract class CertPath implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 6068470306649138683L;

    /** The type of certificates in this chain. */
    private String type;

    /**
     * Creates a {@code CertPath} of the specified type.
     * <p>
     * This constructor is protected because most users should use a
     * {@code CertificateFactory} to create {@code CertPath}s.
     *
     * @param type the standard name of the type of
     * {@code Certificate}s in this path
     */
    protected CertPath(String type) {
        this.type = type;
    }

    /**
     * Returns the type of {@code Certificate}s in this certification
     * path. This is the same string that would be returned by
     * {@link java.security.cert.Certificate#getType() cert.getType()}
     * for all {@code Certificate}s in the certification path.
     *
     * @return the type of {@code Certificate}s in this certification
     * path (never null)
     */
    public String getType() {
        return type;
    }

    /**
     * Returns an iteration of the encodings supported by this certification
     * path, with the default encoding first. Attempts to modify the returned
     * {@code Iterator} via its {@code remove} method result in an
     * {@code UnsupportedOperationException}.
     *
     * @return an {@code Iterator} over the names of the supported
     *         encodings (as Strings)
     */
    public abstract Iterator<String> getEncodings();

    /**
     * Compares this certification path for equality with the specified
     * object. Two {@code CertPath}s are equal if and only if their
     * types are equal and their certificate {@code List}s (and by
     * implication the {@code Certificate}s in those {@code List}s)
     * are equal. A {@code CertPath} is never equal to an object that is
     * not a {@code CertPath}.
     * <p>
     * This algorithm is implemented by this method. If it is overridden,
     * the behavior specified here must be maintained.
     *
     * @param other the object to test for equality with this certification path
     * @return true if the specified object is equal to this certification path,
     * false otherwise
     */
    public boolean equals(Object other) {
        if (this == other)
            return true;

        return other instanceof CertPath that
                && that.getType().equals(this.type)
                && this.getCertificates().equals(that.getCertificates());
    }

    /**
     * Returns the hashcode for this certification path. The hash code of
     * a certification path is defined to be the result of the following
     * calculation:
     * <pre>{@code
     *  hashCode = path.getType().hashCode();
     *  hashCode = 31*hashCode + path.getCertificates().hashCode();
     * }</pre>
     * This ensures that {@code path1.equals(path2)} implies that
     * {@code path1.hashCode()==path2.hashCode()} for any two certification
     * paths, {@code path1} and {@code path2}, as required by the
     * general contract of {@code Object.hashCode}.
     *
     * @return the hashcode value for this certification path
     */
    public int hashCode() {
        int hashCode = type.hashCode();
        hashCode = 31*hashCode + getCertificates().hashCode();
        return hashCode;
    }

    /**
     * Returns a string representation of this certification path.
     * This calls the {@code toString} method on each of the
     * {@code Certificate}s in the path.
     *
     * @return a string representation of this certification path
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<? extends Certificate> stringIterator =
                                        getCertificates().iterator();

        sb.append("\n" + type + " Cert Path: length = "
            + getCertificates().size() + ".\n");
        sb.append("[\n");
        int i = 1;
        while (stringIterator.hasNext()) {
            sb.append("=========================================="
                + "===============Certificate " + i + " start.\n");
            Certificate stringCert = stringIterator.next();
            sb.append(stringCert.toString());
            sb.append("\n========================================"
                + "=================Certificate " + i + " end.\n\n\n");
            i++;
        }

        sb.append("\n]");
        return sb.toString();
    }

    /**
     * Returns the encoded form of this certification path, using the default
     * encoding.
     *
     * @return the encoded bytes
     * @throws    CertificateEncodingException if an encoding error occurs
     */
    public abstract byte[] getEncoded()
        throws CertificateEncodingException;

    /**
     * Returns the encoded form of this certification path, using the
     * specified encoding.
     *
     * @param encoding the name of the encoding to use
     * @return the encoded bytes
     * @throws    CertificateEncodingException if an encoding error occurs or
     *   the encoding requested is not supported
     */
    public abstract byte[] getEncoded(String encoding)
        throws CertificateEncodingException;

    /**
     * Returns the list of certificates in this certification path.
     * The {@code List} returned must be immutable and thread-safe.
     *
     * @return an immutable {@code List} of {@code Certificate}s
     *         (may be empty, but not null)
     */
    public abstract List<? extends Certificate> getCertificates();

    /**
     * Replaces the {@code CertPath} to be serialized with a
     * {@code CertPathRep} object.
     *
     * @return the {@code CertPathRep} to be serialized
     *
     * @throws ObjectStreamException if a {@code CertPathRep} object
     * representing this certification path could not be created
     */
    @java.io.Serial
    protected Object writeReplace() throws ObjectStreamException {
        try {
            return new CertPathRep(type, getEncoded());
        } catch (CertificateException ce) {
            NotSerializableException nse =
                new NotSerializableException
                    ("java.security.cert.CertPath: " + type);
            nse.initCause(ce);
            throw nse;
        }
    }

    /**
     * Alternate {@code CertPath} class for serialization.
     * @since 1.4
     */
    protected static class CertPathRep implements Serializable {

        @java.io.Serial
        private static final long serialVersionUID = 3015633072427920915L;

        /** The Certificate type */
        private String type;
        /** The encoded form of the cert path */
        private byte[] data;

        /**
         * Creates a {@code CertPathRep} with the specified
         * type and encoded form of a certification path.
         *
         * @param type the standard name of a {@code CertPath} type
         * @param data the encoded form of the certification path
         */
        protected CertPathRep(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        /**
         * Returns a {@code CertPath} constructed from the type and data.
         *
         * @return the resolved {@code CertPath} object
         *
         * @throws ObjectStreamException if a {@code CertPath} could not
         * be constructed
         */
        @java.io.Serial
        protected Object readResolve() throws ObjectStreamException {
            try {
                CertificateFactory cf = CertificateFactory.getInstance(type);
                return cf.generateCertPath(new ByteArrayInputStream(data));
            } catch (CertificateException ce) {
                NotSerializableException nse =
                    new NotSerializableException
                        ("java.security.cert.CertPath: " + type);
                nse.initCause(ce);
                throw nse;
            }
        }
    }
}
