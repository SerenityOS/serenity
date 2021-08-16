/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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


import java.net.URL;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.*;
import java.util.Objects;

import sun.net.util.URLUtil;
import sun.security.util.IOUtils;

/**
 *
 * <p>This class extends the concept of a codebase to
 * encapsulate not only the location (URL) but also the certificate chains
 * that were used to verify signed code originating from that location.
 *
 * @author Li Gong
 * @author Roland Schemers
 * @since 1.2
 */

public class CodeSource implements java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 4977541819976013951L;

    /**
     * The code location.
     *
     * @serial
     */
    private final URL location;

    /*
     * The code signers.
     */
    private transient CodeSigner[] signers = null;

    /*
     * The code signers. Certificate chains are concatenated.
     */
    private transient java.security.cert.Certificate[] certs = null;

    // cached SocketPermission used for matchLocation
    private transient SocketPermission sp;

    // for generating cert paths
    private transient CertificateFactory factory = null;

    /**
     * A String form of the URL for use as a key in HashMaps/Sets. The String
     * form should be behave in the same manner as the URL when compared for
     * equality in a HashMap/Set, except that no nameservice lookup is done
     * on the hostname (only string comparison), and the fragment is not
     * considered.
     */
    private transient String locationNoFragString;

    /**
     * Constructs a CodeSource and associates it with the specified
     * location and set of certificates.
     *
     * @param url the location (URL).  It may be {@code null}.
     * @param certs the certificate(s). It may be {@code null}. The contents
     * of the array are copied to protect against subsequent modification.
     */
    public CodeSource(URL url, java.security.cert.Certificate[] certs) {
        this.location = url;
        if (url != null) {
            this.locationNoFragString = URLUtil.urlNoFragString(url);
        }

        // Copy the supplied certs
        if (certs != null) {
            this.certs = certs.clone();
        }
    }

    /**
     * Constructs a CodeSource and associates it with the specified
     * location and set of code signers.
     *
     * @param url the location (URL).  It may be {@code null}.
     * @param signers the code signers. It may be {@code null}. The contents
     * of the array are copied to protect against subsequent modification.
     *
     * @since 1.5
     */
    public CodeSource(URL url, CodeSigner[] signers) {
        this.location = url;
        if (url != null) {
            this.locationNoFragString = URLUtil.urlNoFragString(url);
        }

        // Copy the supplied signers
        if (signers != null) {
            this.signers = signers.clone();
        }
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        if (location != null)
            return location.hashCode();
        else
            return 0;
    }

    /**
     * Tests for equality between the specified object and this
     * object. Two CodeSource objects are considered equal if their
     * locations are of identical value and if their signer certificate
     * chains are of identical value. It is not required that
     * the certificate chains be in the same order.
     *
     * @param obj the object to test for equality with this object.
     *
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        // objects types must be equal
        return (obj instanceof CodeSource other)
                && Objects.equals(location, other.location)
                && matchCerts(other, true);
    }

    /**
     * Returns the location associated with this CodeSource.
     *
     * @return the location (URL), or {@code null} if no URL was supplied
     * during construction.
     */
    public final URL getLocation() {
        /* since URL is practically immutable, returning itself is not
           a security problem */
        return this.location;
    }

    /**
     * Returns a String form of the URL for use as a key in HashMaps/Sets.
     */
    String getLocationNoFragString() {
        return locationNoFragString;
    }

    /**
     * Returns the certificates associated with this CodeSource.
     * <p>
     * If this CodeSource object was created using the
     * {@link #CodeSource(URL url, CodeSigner[] signers)}
     * constructor then its certificate chains are extracted and used to
     * create an array of Certificate objects. Each signer certificate is
     * followed by its supporting certificate chain (which may be empty).
     * Each signer certificate and its supporting certificate chain is ordered
     * bottom-to-top (i.e., with the signer certificate first and the (root)
     * certificate authority last).
     *
     * @return a copy of the certificate array, or {@code null} if there
     * is none.
     */
    public final java.security.cert.Certificate[] getCertificates() {
        if (certs != null) {
            return certs.clone();

        } else if (signers != null) {
            // Convert the code signers to certs
            ArrayList<java.security.cert.Certificate> certChains =
                        new ArrayList<>();
            for (int i = 0; i < signers.length; i++) {
                certChains.addAll(
                    signers[i].getSignerCertPath().getCertificates());
            }
            certs = certChains.toArray(
                        new java.security.cert.Certificate[certChains.size()]);
            return certs.clone();

        } else {
            return null;
        }
    }

    /**
     * Returns the code signers associated with this CodeSource.
     * <p>
     * If this CodeSource object was created using the
     * {@link #CodeSource(URL url, java.security.cert.Certificate[] certs)}
     * constructor then its certificate chains are extracted and used to
     * create an array of CodeSigner objects. Note that only X.509 certificates
     * are examined - all other certificate types are ignored.
     *
     * @return a copy of the code signer array, or {@code null} if there
     * is none.
     *
     * @since 1.5
     */
    public final CodeSigner[] getCodeSigners() {
        if (signers != null) {
            return signers.clone();

        } else if (certs != null) {
            // Convert the certs to code signers
            signers = convertCertArrayToSignerArray(certs);
            return signers.clone();

        } else {
            return null;
        }
    }

    /**
     * Returns true if this CodeSource object "implies" the specified CodeSource.
     * <p>
     * More specifically, this method makes the following checks.
     * If any fail, it returns false. If they all succeed, it returns true.
     * <ul>
     * <li> <i>codesource</i> must not be null.
     * <li> If this object's certificates are not null, then all
     * of this object's certificates must be present in <i>codesource</i>'s
     * certificates.
     * <li> If this object's location (getLocation()) is not null, then the
     * following checks are made against this object's location and
     * <i>codesource</i>'s:
     *   <ul>
     *     <li>  <i>codesource</i>'s location must not be null.
     *
     *     <li>  If this object's location
     *           equals <i>codesource</i>'s location, then return true.
     *
     *     <li>  This object's protocol (getLocation().getProtocol()) must be
     *           equal to <i>codesource</i>'s protocol, ignoring case.
     *
     *     <li>  If this object's host (getLocation().getHost()) is not null,
     *           then the SocketPermission
     *           constructed with this object's host must imply the
     *           SocketPermission constructed with <i>codesource</i>'s host.
     *
     *     <li>  If this object's port (getLocation().getPort()) is not
     *           equal to -1 (that is, if a port is specified), it must equal
     *           <i>codesource</i>'s port or default port
     *           (codesource.getLocation().getDefaultPort()).
     *
     *     <li>  If this object's file (getLocation().getFile()) doesn't equal
     *           <i>codesource</i>'s file, then the following checks are made:
     *           If this object's file ends with "/-",
     *           then <i>codesource</i>'s file must start with this object's
     *           file (exclusive the trailing "-").
     *           If this object's file ends with a "/*",
     *           then <i>codesource</i>'s file must start with this object's
     *           file and must not have any further "/" separators.
     *           If this object's file doesn't end with a "/",
     *           then <i>codesource</i>'s file must match this object's
     *           file with a '/' appended.
     *
     *     <li>  If this object's reference (getLocation().getRef()) is
     *           not null, it must equal <i>codesource</i>'s reference.
     *
     *   </ul>
     * </ul>
     * <p>
     * For example, the codesource objects with the following locations
     * and null certificates all imply
     * the codesource with the location "http://www.example.com/classes/foo.jar"
     * and null certificates:
     * <pre>
     *     http:
     *     http://*.example.com/classes/*
     *     http://www.example.com/classes/-
     *     http://www.example.com/classes/foo.jar
     * </pre>
     *
     * Note that if this CodeSource has a null location and a null
     * certificate chain, then it implies every other CodeSource.
     *
     * @param codesource CodeSource to compare against.
     *
     * @return true if the specified codesource is implied by this codesource,
     * false if not.
     */
    public boolean implies(CodeSource codesource)
    {
        if (codesource == null)
            return false;

        return matchCerts(codesource, false) && matchLocation(codesource);
    }

    /**
     * Returns true if all the certs in this
     * CodeSource are also in <i>that</i>.
     *
     * @param that the CodeSource to check against.
     * @param strict if true then a strict equality match is performed.
     *               Otherwise a subset match is performed.
     */
    boolean matchCerts(CodeSource that, boolean strict)
    {
        boolean match;

        // match any key
        if (certs == null && signers == null) {
            if (strict) {
                return (that.certs == null && that.signers == null);
            } else {
                return true;
            }
        // both have signers
        } else if (signers != null && that.signers != null) {
            if (strict && signers.length != that.signers.length) {
                return false;
            }
            for (int i = 0; i < signers.length; i++) {
                match = false;
                for (int j = 0; j < that.signers.length; j++) {
                    if (signers[i].equals(that.signers[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            return true;

        // both have certs
        } else if (certs != null && that.certs != null) {
            if (strict && certs.length != that.certs.length) {
                return false;
            }
            for (int i = 0; i < certs.length; i++) {
                match = false;
                for (int j = 0; j < that.certs.length; j++) {
                    if (certs[i].equals(that.certs[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            return true;
        }

        return false;
    }


    /**
     * Returns true if two CodeSource's have the "same" location.
     *
     * @param that CodeSource to compare against
     */
    private boolean matchLocation(CodeSource that) {
        if (location == null)
            return true;

        if ((that == null) || (that.location == null))
            return false;

        if (location.equals(that.location))
            return true;

        if (!location.getProtocol().equalsIgnoreCase(that.location.getProtocol()))
            return false;

        int thisPort = location.getPort();
        if (thisPort != -1) {
            int thatPort = that.location.getPort();
            int port = thatPort != -1 ? thatPort
                                      : that.location.getDefaultPort();
            if (thisPort != port)
                return false;
        }

        if (location.getFile().endsWith("/-")) {
            // Matches the directory and (recursively) all files
            // and subdirectories contained in that directory.
            // For example, "/a/b/-" implies anything that starts with
            // "/a/b/"
            String thisPath = location.getFile().substring(0,
                                            location.getFile().length()-1);
            if (!that.location.getFile().startsWith(thisPath))
                return false;
        } else if (location.getFile().endsWith("/*")) {
            // Matches the directory and all the files contained in that
            // directory.
            // For example, "/a/b/*" implies anything that starts with
            // "/a/b/" but has no further slashes
            int last = that.location.getFile().lastIndexOf('/');
            if (last == -1)
                return false;
            String thisPath = location.getFile().substring(0,
                                            location.getFile().length()-1);
            String thatPath = that.location.getFile().substring(0, last+1);
            if (!thatPath.equals(thisPath))
                return false;
        } else {
            // Exact matches only.
            // For example, "/a/b" and "/a/b/" both imply "/a/b/"
            if ((!that.location.getFile().equals(location.getFile()))
                && (!that.location.getFile().equals(location.getFile()+"/"))) {
                return false;
            }
        }

        if (location.getRef() != null
            && !location.getRef().equals(that.location.getRef())) {
            return false;
        }

        String thisHost = location.getHost();
        String thatHost = that.location.getHost();
        if (thisHost != null) {
            if (("".equals(thisHost) || "localhost".equals(thisHost)) &&
                ("".equals(thatHost) || "localhost".equals(thatHost))) {
                // ok
            } else if (!thisHost.equals(thatHost)) {
                if (thatHost == null) {
                    return false;
                }
                if (this.sp == null) {
                    this.sp = new SocketPermission(thisHost, "resolve");
                }
                if (that.sp == null) {
                    that.sp = new SocketPermission(thatHost, "resolve");
                }
                if (!this.sp.implies(that.sp)) {
                    return false;
                }
            }
        }
        // everything matches
        return true;
    }

    /**
     * Returns a string describing this CodeSource, telling its
     * URL and certificates.
     *
     * @return information about this CodeSource.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(this.location);

        if (this.certs != null && this.certs.length > 0) {
            for (int i = 0; i < this.certs.length; i++) {
                sb.append( " " + this.certs[i]);
            }

        } else if (this.signers != null && this.signers.length > 0) {
            for (int i = 0; i < this.signers.length; i++) {
                sb.append( " " + this.signers[i]);
            }
        } else {
            sb.append(" <no signer certificates>");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Writes this object out to a stream (i.e., serializes it).
     *
     * @serialData An initial {@code URL} is followed by an
     * {@code int} indicating the number of certificates to follow
     * (a value of "zero" denotes that there are no certificates associated
     * with this object).
     * Each certificate is written out starting with a {@code String}
     * denoting the certificate type, followed by an
     * {@code int} specifying the length of the certificate encoding,
     * followed by the certificate encoding itself which is written out as an
     * array of bytes. Finally, if any code signers are present then the array
     * of code signers is serialized and written out too.
     *
     * @param  oos the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream oos)
        throws IOException
    {
        oos.defaultWriteObject(); // location

        // Serialize the array of certs
        if (certs == null || certs.length == 0) {
            oos.writeInt(0);
        } else {
            // write out the total number of certs
            oos.writeInt(certs.length);
            // write out each cert, including its type
            for (int i = 0; i < certs.length; i++) {
                java.security.cert.Certificate cert = certs[i];
                try {
                    oos.writeUTF(cert.getType());
                    byte[] encoded = cert.getEncoded();
                    oos.writeInt(encoded.length);
                    oos.write(encoded);
                } catch (CertificateEncodingException cee) {
                    throw new IOException(cee.getMessage());
                }
            }
        }

        // Serialize the array of code signers (if any)
        if (signers != null && signers.length > 0) {
            oos.writeObject(signers);
        }
    }

    /**
     * Restores this object from a stream (i.e., deserializes it).
     *
     * @param  ois the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream ois)
        throws IOException, ClassNotFoundException
    {
        CertificateFactory cf;
        Hashtable<String, CertificateFactory> cfs = null;
        List<java.security.cert.Certificate> certList = null;

        ois.defaultReadObject(); // location

        // process any new-style certs in the stream (if present)
        int size = ois.readInt();
        if (size > 0) {
            // we know of 3 different cert types: X.509, PGP, SDSI, which
            // could all be present in the stream at the same time
            cfs = new Hashtable<>(3);
            certList = new ArrayList<>(size > 20 ? 20 : size);
        } else if (size < 0) {
            throw new IOException("size cannot be negative");
        }

        for (int i = 0; i < size; i++) {
            // read the certificate type, and instantiate a certificate
            // factory of that type (reuse existing factory if possible)
            String certType = ois.readUTF();
            if (cfs.containsKey(certType)) {
                // reuse certificate factory
                cf = cfs.get(certType);
            } else {
                // create new certificate factory
                try {
                    cf = CertificateFactory.getInstance(certType);
                } catch (CertificateException ce) {
                    throw new ClassNotFoundException
                        ("Certificate factory for " + certType + " not found");
                }
                // store the certificate factory so we can reuse it later
                cfs.put(certType, cf);
            }
            // parse the certificate
            byte[] encoded = IOUtils.readExactlyNBytes(ois, ois.readInt());
            ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
            try {
                certList.add(cf.generateCertificate(bais));
            } catch (CertificateException ce) {
                throw new IOException(ce.getMessage());
            }
            bais.close();
        }

        if (certList != null) {
            this.certs = certList.toArray(
                    new java.security.cert.Certificate[size]);
        }
        // Deserialize array of code signers (if any)
        try {
            this.signers = ((CodeSigner[])ois.readObject()).clone();
        } catch (IOException ioe) {
            // no signers present
        }

        if (location != null) {
            locationNoFragString = URLUtil.urlNoFragString(location);
        }
    }

    /*
     * Convert an array of certificates to an array of code signers.
     * The array of certificates is a concatenation of certificate chains
     * where the initial certificate in each chain is the end-entity cert.
     *
     * @return an array of code signers or null if none are generated.
     */
    private CodeSigner[] convertCertArrayToSignerArray(
        java.security.cert.Certificate[] certs) {

        if (certs == null) {
            return null;
        }

        try {
            // Initialize certificate factory
            if (factory == null) {
                factory = CertificateFactory.getInstance("X.509");
            }

            // Iterate through all the certificates
            int i = 0;
            List<CodeSigner> signers = new ArrayList<>();
            while (i < certs.length) {
                List<java.security.cert.Certificate> certChain =
                        new ArrayList<>();
                certChain.add(certs[i++]); // first cert is an end-entity cert
                int j = i;

                // Extract chain of certificates
                // (loop while certs are not end-entity certs)
                while (j < certs.length &&
                    certs[j] instanceof X509Certificate &&
                    ((X509Certificate)certs[j]).getBasicConstraints() != -1) {
                    certChain.add(certs[j]);
                    j++;
                }
                i = j;
                CertPath certPath = factory.generateCertPath(certChain);
                signers.add(new CodeSigner(certPath, null));
            }

            if (signers.isEmpty()) {
                return null;
            } else {
                return signers.toArray(new CodeSigner[signers.size()]);
            }

        } catch (CertificateException e) {
            return null; //TODO - may be better to throw an ex. here
        }
    }
}
