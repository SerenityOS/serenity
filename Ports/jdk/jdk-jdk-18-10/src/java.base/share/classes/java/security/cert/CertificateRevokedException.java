/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.x500.X500Principal;

import sun.security.util.IOUtils;
import sun.security.util.KnownOIDs;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.InvalidityDateExtension;

/**
 * An exception that indicates an X.509 certificate is revoked. A
 * {@code CertificateRevokedException} contains additional information
 * about the revoked certificate, such as the date on which the
 * certificate was revoked and the reason it was revoked.
 *
 * @author Sean Mullan
 * @since 1.7
 * @see CertPathValidatorException
 */
public class CertificateRevokedException extends CertificateException {

    @java.io.Serial
    private static final long serialVersionUID = 7839996631571608627L;

    /**
     * @serial the date on which the certificate was revoked
     */
    private Date revocationDate;
    /**
     * @serial the revocation reason
     */
    private final CRLReason reason;
    /**
     * @serial the {@code X500Principal} that represents the name of the
     * authority that signed the certificate's revocation status information
     */
    private final X500Principal authority;

    private transient Map<String, Extension> extensions;

    /**
     * Constructs a {@code CertificateRevokedException} with
     * the specified revocation date, reason code, authority name, and map
     * of extensions.
     *
     * @param revocationDate the date on which the certificate was revoked. The
     *    date is copied to protect against subsequent modification.
     * @param reason the revocation reason
     * @param extensions a map of X.509 Extensions. Each key is an OID String
     *    that maps to the corresponding Extension. The map is copied to
     *    prevent subsequent modification.
     * @param authority the {@code X500Principal} that represents the name
     *    of the authority that signed the certificate's revocation status
     *    information
     * @throws NullPointerException if {@code revocationDate},
     *    {@code reason}, {@code authority}, or
     *    {@code extensions} is {@code null}
     * @throws ClassCastException if {@code extensions} contains an incorrectly
     *    typed key or value
     */
    public CertificateRevokedException(Date revocationDate, CRLReason reason,
        X500Principal authority, Map<String, Extension> extensions) {
        if (revocationDate == null || reason == null || authority == null ||
            extensions == null) {
            throw new NullPointerException();
        }
        this.revocationDate = new Date(revocationDate.getTime());
        this.reason = reason;
        this.authority = authority;
        // make sure Map only contains correct types
        this.extensions = Collections.checkedMap(new HashMap<>(),
                                                 String.class, Extension.class);
        this.extensions.putAll(extensions);
    }

    /**
     * Returns the date on which the certificate was revoked. A new copy is
     * returned each time the method is invoked to protect against subsequent
     * modification.
     *
     * @return the revocation date
     */
    public Date getRevocationDate() {
        return (Date) revocationDate.clone();
    }

    /**
     * Returns the reason the certificate was revoked.
     *
     * @return the revocation reason
     */
    public CRLReason getRevocationReason() {
        return reason;
    }

    /**
     * Returns the name of the authority that signed the certificate's
     * revocation status information.
     *
     * @return the {@code X500Principal} that represents the name of the
     *     authority that signed the certificate's revocation status information
     */
    public X500Principal getAuthorityName() {
        return authority;
    }

    /**
     * Returns the invalidity date, as specified in the Invalidity Date
     * extension of this {@code CertificateRevokedException}. The
     * invalidity date is the date on which it is known or suspected that the
     * private key was compromised or that the certificate otherwise became
     * invalid. This implementation calls {@code getExtensions()} and
     * checks the returned map for an entry for the Invalidity Date extension
     * OID ("2.5.29.24"). If found, it returns the invalidity date in the
     * extension; otherwise null. A new Date object is returned each time the
     * method is invoked to protect against subsequent modification.
     *
     * @return the invalidity date, or {@code null} if not specified
     */
    public Date getInvalidityDate() {
        Extension ext = getExtensions().get(KnownOIDs.InvalidityDate.value());
        if (ext == null) {
            return null;
        } else {
            try {
                Date invalidity = InvalidityDateExtension.toImpl(ext).get("DATE");
                return new Date(invalidity.getTime());
            } catch (IOException ioe) {
                return null;
            }
        }
    }

    /**
     * Returns a map of X.509 extensions containing additional information
     * about the revoked certificate, such as the Invalidity Date
     * Extension. Each key is an OID String that maps to the corresponding
     * Extension.
     *
     * @return an unmodifiable map of X.509 extensions, or an empty map
     *    if there are no extensions
     */
    public Map<String, Extension> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    @Override
    public String getMessage() {
        return "Certificate has been revoked, reason: "
               + reason + ", revocation date: " + revocationDate
               + ", authority: " + authority + ", extension OIDs: "
               + extensions.keySet();
    }

    /**
     * Serialize this {@code CertificateRevokedException} instance.
     *
     * @serialData the size of the extensions map (int), followed by all of
     * the extensions in the map, in no particular order. For each extension,
     * the following data is emitted: the OID String (Object), the criticality
     * flag (boolean), the length of the encoded extension value byte array
     * (int), and the encoded extension value bytes.
     *
     * @param  oos the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Write out the non-transient fields
        // (revocationDate, reason, authority)
        oos.defaultWriteObject();

        // Write out the size (number of mappings) of the extensions map
        oos.writeInt(extensions.size());

        // For each extension in the map, the following are emitted (in order):
        // the OID String (Object), the criticality flag (boolean), the length
        // of the encoded extension value byte array (int), and the encoded
        // extension value byte array. The extensions themselves are emitted
        // in no particular order.
        for (Map.Entry<String, Extension> entry : extensions.entrySet()) {
            Extension ext = entry.getValue();
            oos.writeObject(ext.getId());
            oos.writeBoolean(ext.isCritical());
            byte[] extVal = ext.getValue();
            oos.writeInt(extVal.length);
            oos.write(extVal);
        }
    }

    /**
     * Deserialize the {@code CertificateRevokedException} instance.
     *
     * @param  ois the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
        // Read in the non-transient fields
        // (revocationDate, reason, authority)
        ois.defaultReadObject();

        // Defensively copy the revocation date
        revocationDate = new Date(revocationDate.getTime());

        // Read in the size (number of mappings) of the extensions map
        // and create the extensions map
        int size = ois.readInt();
        if (size == 0) {
            extensions = Collections.emptyMap();
        } else if (size < 0) {
            throw new IOException("size cannot be negative");
        } else {
            extensions = new HashMap<>(size > 20 ? 20 : size);
        }

        // Read in the extensions and put the mappings in the extensions map
        for (int i = 0; i < size; i++) {
            String oid = (String) ois.readObject();
            boolean critical = ois.readBoolean();
            byte[] extVal = IOUtils.readExactlyNBytes(ois, ois.readInt());
            Extension ext = sun.security.x509.Extension.newExtension
                (ObjectIdentifier.of(oid), critical, extVal);
            extensions.put(oid, ext);
        }
    }
}
