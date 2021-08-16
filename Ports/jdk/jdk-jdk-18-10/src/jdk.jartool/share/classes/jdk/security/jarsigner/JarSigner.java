/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.security.jarsigner;

import com.sun.jarsigner.ContentSigner;
import com.sun.jarsigner.ContentSignerParameters;
import jdk.internal.access.JavaUtilZipFileAccess;
import jdk.internal.access.SharedSecrets;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs.PKCS9Attributes;
import sun.security.timestamp.HttpTimestamper;
import sun.security.tools.PathList;
import sun.security.util.Event;
import sun.security.util.ManifestDigester;
import sun.security.util.SignatureFileVerifier;
import sun.security.util.SignatureUtil;
import sun.security.x509.AlgorithmId;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * An immutable utility class to sign a jar file.
 * <p>
 * A caller creates a {@code JarSigner.Builder} object, (optionally) sets
 * some parameters, and calls {@link JarSigner.Builder#build build} to create
 * a {@code JarSigner} object. This {@code JarSigner} object can then
 * be used to sign a jar file.
 * <p>
 * Unless otherwise stated, calling a method of {@code JarSigner} or
 * {@code JarSigner.Builder} with a null argument will throw
 * a {@link NullPointerException}.
 * <p>
 * Example:
 * <pre>
 * JarSigner signer = new JarSigner.Builder(key, certPath)
 *         .digestAlgorithm("SHA-1")
 *         .signatureAlgorithm("SHA1withDSA")
 *         .build();
 * try (ZipFile in = new ZipFile(inputFile);
 *         FileOutputStream out = new FileOutputStream(outputFile)) {
 *     signer.sign(in, out);
 * }
 * </pre>
 *
 * @since 9
 */
public final class JarSigner {

    static final JavaUtilZipFileAccess JUZFA = SharedSecrets.getJavaUtilZipFileAccess();

    /**
     * A mutable builder class that can create an immutable {@code JarSigner}
     * from various signing-related parameters.
     *
     * @since 9
     */
    public static class Builder {

        // Signer materials:
        final PrivateKey privateKey;
        final X509Certificate[] certChain;

        // JarSigner options:
        // Support multiple digestalg internally. Can be null, but not empty
        String[] digestalg;
        String sigalg;
        // Precisely should be one provider for each digestalg, maybe later
        Provider digestProvider;
        Provider sigProvider;
        URI tsaUrl;
        String signerName;
        BiConsumer<String,String> handler;

        // Implementation-specific properties:
        String tSAPolicyID;
        String tSADigestAlg;
        boolean sectionsonly = false;
        boolean internalsf = false;
        String altSignerPath;
        String altSigner;

        /**
         * Creates a {@code JarSigner.Builder} object with
         * a {@link KeyStore.PrivateKeyEntry} object.
         *
         * @param entry the {@link KeyStore.PrivateKeyEntry} of the signer.
         */
        public Builder(KeyStore.PrivateKeyEntry entry) {
            this.privateKey = entry.getPrivateKey();
            try {
                // called internally, no need to clone
                Certificate[] certs = entry.getCertificateChain();
                this.certChain = Arrays.copyOf(certs, certs.length,
                        X509Certificate[].class);
            } catch (ArrayStoreException ase) {
                // Wrong type, not X509Certificate. Won't document.
                throw new IllegalArgumentException(
                        "Entry does not contain X509Certificate");
            }
        }

        /**
         * Creates a {@code JarSigner.Builder} object with a private key and
         * a certification path.
         *
         * @param privateKey the private key of the signer.
         * @param certPath the certification path of the signer.
         * @throws IllegalArgumentException if {@code certPath} is empty, or
         *      the {@code privateKey} algorithm does not match the algorithm
         *      of the {@code PublicKey} in the end entity certificate
         *      (the first certificate in {@code certPath}).
         */
        public Builder(PrivateKey privateKey, CertPath certPath) {
            List<? extends Certificate> certs = certPath.getCertificates();
            if (certs.isEmpty()) {
                throw new IllegalArgumentException("certPath cannot be empty");
            }
            if (!privateKey.getAlgorithm().equals
                    (certs.get(0).getPublicKey().getAlgorithm())) {
                throw new IllegalArgumentException
                        ("private key algorithm does not match " +
                                "algorithm of public key in end entity " +
                                "certificate (the 1st in certPath)");
            }
            this.privateKey = privateKey;
            try {
                this.certChain = certs.toArray(new X509Certificate[certs.size()]);
            } catch (ArrayStoreException ase) {
                // Wrong type, not X509Certificate.
                throw new IllegalArgumentException(
                        "Entry does not contain X509Certificate");
            }
        }

        /**
         * Sets the digest algorithm. If no digest algorithm is specified,
         * the default algorithm returned by {@link #getDefaultDigestAlgorithm}
         * will be used.
         *
         * @param algorithm the standard name of the algorithm. See
         *      the {@code MessageDigest} section in the <a href=
         *      "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
         *      Java Cryptography Architecture Standard Algorithm Name
         *      Documentation</a> for information about standard algorithm names.
         * @return the {@code JarSigner.Builder} itself.
         * @throws NoSuchAlgorithmException if {@code algorithm} is not available.
         */
        public Builder digestAlgorithm(String algorithm) throws NoSuchAlgorithmException {
            MessageDigest.getInstance(Objects.requireNonNull(algorithm));
            this.digestalg = new String[]{algorithm};
            this.digestProvider = null;
            return this;
        }

        /**
         * Sets the digest algorithm from the specified provider.
         * If no digest algorithm is specified, the default algorithm
         * returned by {@link #getDefaultDigestAlgorithm} will be used.
         *
         * @param algorithm the standard name of the algorithm. See
         *      the {@code MessageDigest} section in the <a href=
         *      "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
         *      Java Cryptography Architecture Standard Algorithm Name
         *      Documentation</a> for information about standard algorithm names.
         * @param provider the provider.
         * @return the {@code JarSigner.Builder} itself.
         * @throws NoSuchAlgorithmException if {@code algorithm} is not
         *      available in the specified provider.
         */
        public Builder digestAlgorithm(String algorithm, Provider provider)
                throws NoSuchAlgorithmException {
            MessageDigest.getInstance(
                    Objects.requireNonNull(algorithm),
                    Objects.requireNonNull(provider));
            this.digestalg = new String[]{algorithm};
            this.digestProvider = provider;
            return this;
        }

        /**
         * Sets the signature algorithm. If no signature algorithm
         * is specified, the default signature algorithm returned by
         * {@link #getDefaultSignatureAlgorithm} for the private key
         * will be used.
         *
         * @param algorithm the standard name of the algorithm. See
         *      the {@code Signature} section in the <a href=
         *      "{@docRoot}/../specs/security/standard-names.html#signature-algorithms">
         *      Java Cryptography Architecture Standard Algorithm Name
         *      Documentation</a> for information about standard algorithm names.
         * @return the {@code JarSigner.Builder} itself.
         * @throws NoSuchAlgorithmException if {@code algorithm} is not available.
         * @throws IllegalArgumentException if {@code algorithm} is not
         *      compatible with the algorithm of the signer's private key.
         */
        public Builder signatureAlgorithm(String algorithm)
                throws NoSuchAlgorithmException {
            // Check availability
            Signature.getInstance(Objects.requireNonNull(algorithm));
            SignatureUtil.checkKeyAndSigAlgMatch(privateKey, algorithm);
            this.sigalg = algorithm;
            this.sigProvider = null;
            return this;
        }

        /**
         * Sets the signature algorithm from the specified provider. If no
         * signature algorithm is specified, the default signature algorithm
         * returned by {@link #getDefaultSignatureAlgorithm} for the private
         * key will be used.
         *
         * @param algorithm the standard name of the algorithm. See
         *      the {@code Signature} section in the <a href=
         *      "{@docRoot}/../specs/security/standard-names.html#signature-algorithms">
         *      Java Cryptography Architecture Standard Algorithm Name
         *      Documentation</a> for information about standard algorithm names.
         * @param provider  the provider.
         * @return the {@code JarSigner.Builder} itself.
         * @throws NoSuchAlgorithmException if {@code algorithm} is not
         *      available in the specified provider.
         * @throws IllegalArgumentException if {@code algorithm} is not
         *      compatible with the algorithm of the signer's private key.
         */
        public Builder signatureAlgorithm(String algorithm, Provider provider)
                throws NoSuchAlgorithmException {
            // Check availability
            Signature.getInstance(
                    Objects.requireNonNull(algorithm),
                    Objects.requireNonNull(provider));
            SignatureUtil.checkKeyAndSigAlgMatch(privateKey, algorithm);
            this.sigalg = algorithm;
            this.sigProvider = provider;
            return this;
        }

        /**
         * Sets the URI of the Time Stamping Authority (TSA).
         *
         * @param uri the URI.
         * @return the {@code JarSigner.Builder} itself.
         */
        public Builder tsa(URI uri) {
            this.tsaUrl = Objects.requireNonNull(uri);
            return this;
        }

        /**
         * Sets the signer name. The name will be used as the base name for
         * the signature files. All lowercase characters will be converted to
         * uppercase for signature file names. If a signer name is not
         * specified, the string "SIGNER" will be used.
         *
         * @param name the signer name.
         * @return the {@code JarSigner.Builder} itself.
         * @throws IllegalArgumentException if {@code name} is empty or has
         *      a size bigger than 8, or it contains characters not from the
         *      set "a-zA-Z0-9_-".
         */
        public Builder signerName(String name) {
            if (name.isEmpty() || name.length() > 8) {
                throw new IllegalArgumentException("Name too long");
            }

            name = name.toUpperCase(Locale.ENGLISH);

            for (int j = 0; j < name.length(); j++) {
                char c = name.charAt(j);
                if (!
                        ((c >= 'A' && c <= 'Z') ||
                                (c >= '0' && c <= '9') ||
                                (c == '-') ||
                                (c == '_'))) {
                    throw new IllegalArgumentException(
                            "Invalid characters in name");
                }
            }
            this.signerName = name;
            return this;
        }

        /**
         * Sets en event handler that will be triggered when a {@link JarEntry}
         * is to be added, signed, or updated during the signing process.
         * <p>
         * The handler can be used to display signing progress. The first
         * argument of the handler can be "adding", "signing", or "updating",
         * and the second argument is the name of the {@link JarEntry}
         * being processed.
         *
         * @param handler the event handler.
         * @return the {@code JarSigner.Builder} itself.
         */
        public Builder eventHandler(BiConsumer<String,String> handler) {
            this.handler = Objects.requireNonNull(handler);
            return this;
        }

        /**
         * Sets an additional implementation-specific property indicated by
         * the specified key.
         *
         * @implNote This implementation supports the following properties:
         * <ul>
         * <li>"tsaDigestAlg": algorithm of digest data in the timestamping
         * request. The default value is the same as the result of
         * {@link #getDefaultDigestAlgorithm}.
         * <li>"tsaPolicyId": TSAPolicyID for Timestamping Authority.
         * No default value.
         * <li>"internalsf": "true" if the .SF file is included inside the
         * signature block, "false" otherwise. Default "false".
         * <li>"sectionsonly": "true" if the .SF file only contains the hash
         * value for each section of the manifest and not for the whole
         * manifest, "false" otherwise. Default "false".
         * </ul>
         * All property names are case-insensitive.
         *
         * @param key the name of the property.
         * @param value the value of the property.
         * @return the {@code JarSigner.Builder} itself.
         * @throws UnsupportedOperationException if the key is not supported
         *      by this implementation.
         * @throws IllegalArgumentException if the value is not accepted as
         *      a legal value for this key.
         */
        public Builder setProperty(String key, String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            switch (key.toLowerCase(Locale.US)) {
                case "tsadigestalg":
                    try {
                        MessageDigest.getInstance(value);
                    } catch (NoSuchAlgorithmException nsae) {
                        throw new IllegalArgumentException(
                                "Invalid tsadigestalg", nsae);
                    }
                    this.tSADigestAlg = value;
                    break;
                case "tsapolicyid":
                    this.tSAPolicyID = value;
                    break;
                case "internalsf":
                    this.internalsf = parseBoolean("interalsf", value);
                    break;
                case "sectionsonly":
                    this.sectionsonly = parseBoolean("sectionsonly", value);
                    break;
                case "altsignerpath":
                    altSignerPath = value;
                    break;
                case "altsigner":
                    altSigner = value;
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unsupported key " + key);
            }
            return this;
        }

        private static boolean parseBoolean(String name, String value) {
            switch (value) {
                case "true":
                    return true;
                case "false":
                    return false;
                default:
                    throw new IllegalArgumentException(
                            "Invalid " + name + " value");
            }
        }

        /**
         * Gets the default digest algorithm.
         *
         * @implNote This implementation returns "SHA-256". The value may
         * change in the future.
         *
         * @return the default digest algorithm.
         */
        public static String getDefaultDigestAlgorithm() {
            return "SHA-256";
        }

        /**
         * Gets the default signature algorithm for a private key.
         * For example, SHA256withRSA for a 2048-bit RSA key, and
         * SHA384withECDSA for a 384-bit EC key.
         *
         * @implNote This implementation makes use of comparable strengths
         * as defined in Tables 2 and 3 of NIST SP 800-57 Part 1-Rev.4.
         * Specifically, if a DSA or RSA key with a key size greater than 7680
         * bits, or an EC key with a key size greater than or equal to 512 bits,
         * SHA-512 will be used as the hash function for the signature.
         * If a DSA or RSA key has a key size greater than 3072 bits, or an
         * EC key has a key size greater than or equal to 384 bits, SHA-384 will
         * be used. Otherwise, SHA-256 will be used. The value may
         * change in the future.
         *
         * @param key the private key.
         * @return the default signature algorithm. Returns null if a default
         *      signature algorithm cannot be found. In this case,
         *      {@link #signatureAlgorithm} must be called to specify a
         *      signature algorithm. Otherwise, the {@link #build} method
         *      will throw an {@link IllegalArgumentException}.
         */
        public static String getDefaultSignatureAlgorithm(PrivateKey key) {
            // Attention: sync the spec with SignatureUtil::ecStrength and
            // SignatureUtil::ifcFfcStrength.
            return SignatureUtil.getDefaultSigAlgForKey(Objects.requireNonNull(key));
        }

        /**
         * Builds a {@code JarSigner} object from the parameters set by the
         * setter methods.
         * <p>
         * This method does not modify internal state of this {@code Builder}
         * object and can be called multiple times to generate multiple
         * {@code JarSigner} objects. After this method is called, calling
         * any method on this {@code Builder} will have no effect on
         * the newly built {@code JarSigner} object.
         *
         * @return the {@code JarSigner} object.
         * @throws IllegalArgumentException if a signature algorithm is not
         *      set and cannot be derived from the private key using the
         *      {@link #getDefaultSignatureAlgorithm} method.
         */
        public JarSigner build() {
            return new JarSigner(this);
        }
    }

    private static final String META_INF = "META-INF/";

    // All fields in Builder are duplicated here as final. Those not
    // provided but has a default value will be filled with default value.

    // Precisely, a final array field can still be modified if only
    // reference is copied, no clone is done because we are concerned about
    // casual change instead of malicious attack.

    // Signer materials:
    private final PrivateKey privateKey;
    private final X509Certificate[] certChain;

    // JarSigner options:
    private final String[] digestalg;
    private final String sigalg;
    private final Provider digestProvider;
    private final Provider sigProvider;
    private final URI tsaUrl;
    private final String signerName;
    private final BiConsumer<String,String> handler;

    // Implementation-specific properties:
    private final String tSAPolicyID;
    private final String tSADigestAlg;
    private final boolean sectionsonly; // do not "sign" the whole manifest
    private final boolean internalsf; // include the .SF inside the PKCS7 block

    @Deprecated(since="16", forRemoval=true)
    private final String altSignerPath;
    @Deprecated(since="16", forRemoval=true)
    private final String altSigner;
    private boolean extraAttrsDetected;

    private JarSigner(JarSigner.Builder builder) {

        this.privateKey = builder.privateKey;
        this.certChain = builder.certChain;
        if (builder.digestalg != null) {
            // No need to clone because builder only accepts one alg now
            this.digestalg = builder.digestalg;
        } else {
            this.digestalg = new String[] {
                    Builder.getDefaultDigestAlgorithm() };
        }
        this.digestProvider = builder.digestProvider;
        if (builder.sigalg != null) {
            this.sigalg = builder.sigalg;
        } else {
            this.sigalg = JarSigner.Builder
                    .getDefaultSignatureAlgorithm(privateKey);
            if (this.sigalg == null) {
                throw new IllegalArgumentException(
                        "No signature alg for " + privateKey.getAlgorithm());
            }
        }
        this.sigProvider = builder.sigProvider;
        this.tsaUrl = builder.tsaUrl;

        if (builder.signerName == null) {
            this.signerName = "SIGNER";
        } else {
            this.signerName = builder.signerName;
        }
        this.handler = builder.handler;

        if (builder.tSADigestAlg != null) {
            this.tSADigestAlg = builder.tSADigestAlg;
        } else {
            this.tSADigestAlg = Builder.getDefaultDigestAlgorithm();
        }
        this.tSAPolicyID = builder.tSAPolicyID;
        this.sectionsonly = builder.sectionsonly;
        this.internalsf = builder.internalsf;
        this.altSigner = builder.altSigner;
        this.altSignerPath = builder.altSignerPath;

        // altSigner cannot support modern algorithms like RSASSA-PSS and EdDSA
        if (altSigner != null
                && !sigalg.toUpperCase(Locale.ENGLISH).contains("WITH")) {
            throw new IllegalArgumentException(
                    "Customized ContentSigner is not supported for " + sigalg);
        }
    }

    /**
     * Signs a file into an {@link OutputStream}. This method will not close
     * {@code file} or {@code os}.
     * <p>
     * If an I/O error or signing error occurs during the signing, then it may
     * do so after some bytes have been written. Consequently, the output
     * stream may be in an inconsistent state. It is strongly recommended that
     * it be promptly closed in this case.
     *
     * @param file the file to sign.
     * @param os the output stream.
     * @throws JarSignerException if the signing fails.
     */
    public void sign(ZipFile file, OutputStream os) {
        try {
            sign0(Objects.requireNonNull(file),
                    Objects.requireNonNull(os));
        } catch (SocketTimeoutException | CertificateException e) {
            // CertificateException is thrown when the received cert from TSA
            // has no id-kp-timeStamping in its Extended Key Usages extension.
            throw new JarSignerException("Error applying timestamp", e);
        } catch (IOException ioe) {
            throw new JarSignerException("I/O error", ioe);
        } catch (NoSuchAlgorithmException | InvalidKeyException
                | InvalidParameterSpecException e) {
            throw new JarSignerException("Error in signer materials", e);
        } catch (SignatureException se) {
            throw new JarSignerException("Error creating signature", se);
        }
    }

    /**
     * Returns the digest algorithm for this {@code JarSigner}.
     * <p>
     * The return value is never null.
     *
     * @return the digest algorithm.
     */
    public String getDigestAlgorithm() {
        return digestalg[0];
    }

    /**
     * Returns the signature algorithm for this {@code JarSigner}.
     * <p>
     * The return value is never null.
     *
     * @return the signature algorithm.
     */
    public String getSignatureAlgorithm() {
        return sigalg;
    }

    /**
     * Returns the URI of the Time Stamping Authority (TSA).
     *
     * @return the URI of the TSA.
     */
    public URI getTsa() {
        return tsaUrl;
    }

    /**
     * Returns the signer name of this {@code JarSigner}.
     * <p>
     * The return value is never null.
     *
     * @return the signer name.
     */
    public String getSignerName() {
        return signerName;
    }

    /**
     * Returns the value of an additional implementation-specific property
     * indicated by the specified key. If a property is not set but has a
     * default value, the default value will be returned.
     *
     * @implNote See {@link JarSigner.Builder#setProperty} for a list of
     * properties this implementation supports. All property names are
     * case-insensitive.
     *
     * @param key the name of the property.
     * @return the value for the property.
     * @throws UnsupportedOperationException if the key is not supported
     *      by this implementation.
     */
    public String getProperty(String key) {
        Objects.requireNonNull(key);
        switch (key.toLowerCase(Locale.US)) {
            case "tsadigestalg":
                return tSADigestAlg;
            case "tsapolicyid":
                return tSAPolicyID;
            case "internalsf":
                return Boolean.toString(internalsf);
            case "sectionsonly":
                return Boolean.toString(sectionsonly);
            case "altsignerpath":
                return altSignerPath;
            case "altsigner":
                return altSigner;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported key " + key);
        }
    }

    private void sign0(ZipFile zipFile, OutputStream os)
            throws IOException, CertificateException, NoSuchAlgorithmException,
            SignatureException, InvalidKeyException, InvalidParameterSpecException {
        MessageDigest[] digests;
        try {
            digests = new MessageDigest[digestalg.length];
            for (int i = 0; i < digestalg.length; i++) {
                if (digestProvider == null) {
                    digests[i] = MessageDigest.getInstance(digestalg[i]);
                } else {
                    digests[i] = MessageDigest.getInstance(
                            digestalg[i], digestProvider);
                }
            }
        } catch (NoSuchAlgorithmException asae) {
            // Should not happen. User provided alg were checked, and default
            // alg should always be available.
            throw new AssertionError(asae);
        }

        ZipOutputStream zos = new ZipOutputStream(os);

        Manifest manifest = new Manifest();
        byte[] mfRawBytes = null;

        // Check if manifest exists
        ZipEntry mfFile = getManifestFile(zipFile);
        boolean mfCreated = mfFile == null;
        if (!mfCreated) {
            // Manifest exists. Read its raw bytes.
            mfRawBytes = zipFile.getInputStream(mfFile).readAllBytes();
            manifest.read(new ByteArrayInputStream(mfRawBytes));
        } else {
            // Create new manifest
            Attributes mattr = manifest.getMainAttributes();
            mattr.putValue(Attributes.Name.MANIFEST_VERSION.toString(),
                    "1.0");
            String javaVendor = System.getProperty("java.vendor");
            String jdkVersion = System.getProperty("java.version");
            mattr.putValue("Created-By", jdkVersion + " (" + javaVendor
                    + ")");
            mfFile = new ZipEntry(JarFile.MANIFEST_NAME);
        }

        /*
         * For each entry in jar
         * (except for signature-related META-INF entries),
         * do the following:
         *
         * - if entry is not contained in manifest, add it to manifest;
         * - if entry is contained in manifest, calculate its hash and
         *   compare it with the one in the manifest; if they are
         *   different, replace the hash in the manifest with the newly
         *   generated one. (This may invalidate existing signatures!)
         */
        Vector<ZipEntry> mfFiles = new Vector<>();

        boolean wasSigned = false;

        for (Enumeration<? extends ZipEntry> enum_ = zipFile.entries();
             enum_.hasMoreElements(); ) {
            ZipEntry ze = enum_.nextElement();

            if (ze.getName().startsWith(META_INF)) {
                // Store META-INF files in vector, so they can be written
                // out first
                mfFiles.addElement(ze);

                String zeNameUp = ze.getName().toUpperCase(Locale.ENGLISH);
                if (SignatureFileVerifier.isBlockOrSF(zeNameUp)
                    // no need to preserve binary manifest portions
                    // if the only existing signature will be replaced
                        && !zeNameUp.startsWith(SignatureFile
                            .getBaseSignatureFilesName(signerName))) {
                    wasSigned = true;
                }

                if (SignatureFileVerifier.isSigningRelated(ze.getName())) {
                    // ignore signature-related and manifest files
                    continue;
                }
            }

            if (manifest.getAttributes(ze.getName()) != null) {
                // jar entry is contained in manifest, check and
                // possibly update its digest attributes
                updateDigests(ze, zipFile, digests, manifest);
            } else if (!ze.isDirectory()) {
                // Add entry to manifest
                Attributes attrs = getDigestAttributes(ze, zipFile, digests);
                manifest.getEntries().put(ze.getName(), attrs);
            }
        }

        /*
         * Note:
         *
         * The Attributes object is based on HashMap and can handle
         * continuation lines. Therefore, even if the contents are not changed
         * (in a Map view), the bytes that it write() may be different from
         * the original bytes that it read() from. Since the signature is
         * based on raw bytes, we must retain the exact bytes.
         */
        boolean mfModified;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (mfCreated || !wasSigned) {
            mfModified = true;
            manifest.write(baos);
            mfRawBytes = baos.toByteArray();
        } else {

            // the manifest before updating
            Manifest oldManifest = new Manifest(
                    new ByteArrayInputStream(mfRawBytes));
            mfModified = !oldManifest.equals(manifest);
            if (!mfModified) {
                // leave whole manifest (mfRawBytes) unmodified
            } else {
                // reproduce the manifest raw bytes for unmodified sections
                manifest.write(baos);
                byte[] mfNewRawBytes = baos.toByteArray();
                baos.reset();

                ManifestDigester oldMd = new ManifestDigester(mfRawBytes);
                ManifestDigester newMd = new ManifestDigester(mfNewRawBytes);

                // main attributes
                if (manifest.getMainAttributes().equals(
                        oldManifest.getMainAttributes())
                        && (manifest.getEntries().isEmpty() ||
                            oldMd.getMainAttsEntry().isProperlyDelimited())) {
                    oldMd.getMainAttsEntry().reproduceRaw(baos);
                } else {
                    newMd.getMainAttsEntry().reproduceRaw(baos);
                }

                // individual sections
                for (Map.Entry<String,Attributes> entry :
                        manifest.getEntries().entrySet()) {
                    String sectionName = entry.getKey();
                    Attributes entryAtts = entry.getValue();
                    if (entryAtts.equals(oldManifest.getAttributes(sectionName))
                            && oldMd.get(sectionName).isProperlyDelimited()) {
                        oldMd.get(sectionName).reproduceRaw(baos);
                    } else {
                        newMd.get(sectionName).reproduceRaw(baos);
                    }
                }

                mfRawBytes = baos.toByteArray();
            }
        }

        // Write out the manifest
        if (mfModified) {
            // manifest file has new length
            mfFile = new ZipEntry(JarFile.MANIFEST_NAME);
        }
        if (handler != null) {
            if (mfCreated || !mfModified) {
                handler.accept("adding", mfFile.getName());
            } else {
                handler.accept("updating", mfFile.getName());
            }
        }
        zos.putNextEntry(mfFile);
        zos.write(mfRawBytes);

        // Calculate SignatureFile (".SF") and SignatureBlockFile
        ManifestDigester manDig = new ManifestDigester(mfRawBytes);
        SignatureFile sf = new SignatureFile(digests, manifest, manDig,
                signerName, sectionsonly);

        byte[] block;

        baos.reset();
        sf.write(baos);
        byte[] content = baos.toByteArray();

        if (altSigner == null) {
            Function<byte[], PKCS9Attributes> timestamper = null;
            if (tsaUrl != null) {
                timestamper = s -> {
                    try {
                        // Timestamp the signature
                        HttpTimestamper tsa = new HttpTimestamper(tsaUrl);
                        byte[] tsToken = PKCS7.generateTimestampToken(
                                tsa, tSAPolicyID, tSADigestAlg, s);

                        return new PKCS9Attributes(new PKCS9Attribute[]{
                                new PKCS9Attribute(
                                        PKCS9Attribute.SIGNATURE_TIMESTAMP_TOKEN_OID,
                                        tsToken)});
                    } catch (IOException | CertificateException e) {
                        throw new RuntimeException(e);
                    }
                };
            }
            // We now create authAttrs in block data, so "direct == false".
            block = PKCS7.generateNewSignedData(sigalg, sigProvider, privateKey, certChain,
                    content, internalsf, false, timestamper);
        } else {
            Signature signer = SignatureUtil.fromKey(sigalg, privateKey, sigProvider);
            signer.update(content);
            byte[] signature = signer.sign();

            @SuppressWarnings("removal")
            ContentSignerParameters params =
                    new JarSignerParameters(null, tsaUrl, tSAPolicyID,
                            tSADigestAlg, signature,
                            signer.getAlgorithm(), certChain, content, zipFile);
            @SuppressWarnings("removal")
            ContentSigner signingMechanism = loadSigningMechanism(altSigner, altSignerPath);
            block = signingMechanism.generateSignedData(
                    params,
                    !internalsf,
                    params.getTimestampingAuthority() != null
                            || params.getTimestampingAuthorityCertificate() != null);
        }

        String sfFilename = sf.getMetaName();
        String bkFilename = sf.getBlockName(privateKey);

        ZipEntry sfFile = new ZipEntry(sfFilename);
        ZipEntry bkFile = new ZipEntry(bkFilename);

        long time = System.currentTimeMillis();
        sfFile.setTime(time);
        bkFile.setTime(time);

        // signature file
        zos.putNextEntry(sfFile);
        sf.write(zos);

        if (handler != null) {
            if (zipFile.getEntry(sfFilename) != null) {
                handler.accept("updating", sfFilename);
            } else {
                handler.accept("adding", sfFilename);
            }
        }

        // signature block file
        zos.putNextEntry(bkFile);
        zos.write(block);

        if (handler != null) {
            if (zipFile.getEntry(bkFilename) != null) {
                handler.accept("updating", bkFilename);
            } else {
                handler.accept("adding", bkFilename);
            }
        }

        // Write out all other META-INF files that we stored in the
        // vector
        for (int i = 0; i < mfFiles.size(); i++) {
            ZipEntry ze = mfFiles.elementAt(i);
            if (!ze.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME)
                    && !ze.getName().equalsIgnoreCase(sfFilename)
                    && !ze.getName().equalsIgnoreCase(bkFilename)) {
                if (ze.getName().startsWith(SignatureFile
                        .getBaseSignatureFilesName(signerName))
                        && SignatureFileVerifier.isBlockOrSF(ze.getName())) {
                    if (handler != null) {
                        handler.accept("updating", ze.getName());
                    }
                    continue;
                }
                if (handler != null) {
                    if (manifest.getAttributes(ze.getName()) != null) {
                        handler.accept("signing", ze.getName());
                    } else if (!ze.isDirectory()) {
                        handler.accept("adding", ze.getName());
                    }
                }
                writeEntry(zipFile, zos, ze);
            }
        }

        // Write out all other files
        for (Enumeration<? extends ZipEntry> enum_ = zipFile.entries();
             enum_.hasMoreElements(); ) {
            ZipEntry ze = enum_.nextElement();

            if (!ze.getName().startsWith(META_INF)) {
                if (handler != null) {
                    if (manifest.getAttributes(ze.getName()) != null) {
                        handler.accept("signing", ze.getName());
                    } else {
                        handler.accept("adding", ze.getName());
                    }
                }
                writeEntry(zipFile, zos, ze);
            }
        }
        zipFile.close();
        zos.close();
    }

    private void writeEntry(ZipFile zf, ZipOutputStream os, ZipEntry ze)
            throws IOException {
        ZipEntry ze2 = new ZipEntry(ze.getName());
        ze2.setMethod(ze.getMethod());
        ze2.setTime(ze.getTime());
        ze2.setComment(ze.getComment());
        ze2.setExtra(ze.getExtra());
        int extraAttrs = JUZFA.getExtraAttributes(ze);
        if (!extraAttrsDetected && extraAttrs != -1) {
            extraAttrsDetected = true;
            Event.report(Event.ReporterCategory.ZIPFILEATTRS, "detected");
        }
        JUZFA.setExtraAttributes(ze2, extraAttrs);
        if (ze.getMethod() == ZipEntry.STORED) {
            ze2.setSize(ze.getSize());
            ze2.setCrc(ze.getCrc());
        }
        os.putNextEntry(ze2);
        writeBytes(zf, ze, os);
    }

    private void writeBytes
            (ZipFile zf, ZipEntry ze, ZipOutputStream os) throws IOException {
        try (InputStream is = zf.getInputStream(ze)) {
            is.transferTo(os);
        }
    }

    private void updateDigests(ZipEntry ze, ZipFile zf,
                                  MessageDigest[] digests,
                                  Manifest mf) throws IOException {
        Attributes attrs = mf.getAttributes(ze.getName());
        String[] base64Digests = getDigests(ze, zf, digests);

        for (int i = 0; i < digests.length; i++) {
            // The entry name to be written into attrs
            String name = null;
            try {
                // Find if the digest already exists. An algorithm could have
                // different names. For example, last time it was SHA, and this
                // time it's SHA-1.
                AlgorithmId aid = AlgorithmId.get(digests[i].getAlgorithm());
                for (Object key : attrs.keySet()) {
                    if (key instanceof Attributes.Name) {
                        String n = key.toString();
                        if (n.toUpperCase(Locale.ENGLISH).endsWith("-DIGEST")) {
                            String tmp = n.substring(0, n.length() - 7);
                            if (AlgorithmId.get(tmp).equals(aid)) {
                                name = n;
                                break;
                            }
                        }
                    }
                }
            } catch (NoSuchAlgorithmException nsae) {
                // Ignored. Writing new digest entry.
            }

            if (name == null) {
                name = digests[i].getAlgorithm() + "-Digest";
            }
            attrs.putValue(name, base64Digests[i]);
        }
    }

    private Attributes getDigestAttributes(
            ZipEntry ze, ZipFile zf, MessageDigest[] digests)
            throws IOException {

        String[] base64Digests = getDigests(ze, zf, digests);
        Attributes attrs = new Attributes();

        for (int i = 0; i < digests.length; i++) {
            attrs.putValue(digests[i].getAlgorithm() + "-Digest",
                    base64Digests[i]);
        }
        return attrs;
    }

    /*
     * Returns manifest entry from given jar file, or null if given jar file
     * does not have a manifest entry.
     */
    private ZipEntry getManifestFile(ZipFile zf) {
        ZipEntry ze = zf.getEntry(JarFile.MANIFEST_NAME);
        if (ze == null) {
            // Check all entries for matching name
            Enumeration<? extends ZipEntry> enum_ = zf.entries();
            while (enum_.hasMoreElements() && ze == null) {
                ze = enum_.nextElement();
                if (!JarFile.MANIFEST_NAME.equalsIgnoreCase
                        (ze.getName())) {
                    ze = null;
                }
            }
        }
        return ze;
    }

    private String[] getDigests(
            ZipEntry ze, ZipFile zf, MessageDigest[] digests)
            throws IOException {

        int n, i;
        try (InputStream is = zf.getInputStream(ze)) {
            long left = ze.getSize();
            byte[] buffer = new byte[8192];
            while ((left > 0)
                    && (n = is.read(buffer, 0, buffer.length)) != -1) {
                for (i = 0; i < digests.length; i++) {
                    digests[i].update(buffer, 0, n);
                }
                left -= n;
            }
        }

        // complete the digests
        String[] base64Digests = new String[digests.length];
        for (i = 0; i < digests.length; i++) {
            base64Digests[i] = Base64.getEncoder()
                    .encodeToString(digests[i].digest());
        }
        return base64Digests;
    }

    /*
     * Try to load the specified signing mechanism.
     * The URL class loader is used.
     */
    @SuppressWarnings("removal")
    private ContentSigner loadSigningMechanism(String signerClassName,
                                               String signerClassPath) {

        // If there is no signerClassPath provided, search from here
        if (signerClassPath == null) {
            signerClassPath = ".";
        }

        // construct class loader
        String cpString;   // make sure env.class.path defaults to dot

        // do prepends to get correct ordering
        cpString = PathList.appendPath(
                System.getProperty("env.class.path"), null);
        cpString = PathList.appendPath(
                System.getProperty("java.class.path"), cpString);
        cpString = PathList.appendPath(signerClassPath, cpString);
        URL[] urls = PathList.pathToURLs(cpString);
        ClassLoader appClassLoader = new URLClassLoader(urls);

        try {
            // attempt to find signer
            Class<?> signerClass = appClassLoader.loadClass(signerClassName);
            Object signer = signerClass.getDeclaredConstructor().newInstance();
            return (ContentSigner) signer;
        } catch (ClassNotFoundException|InstantiationException|
                IllegalAccessException|ClassCastException|
                NoSuchMethodException| InvocationTargetException e) {
            throw new IllegalArgumentException(
                    "Invalid altSigner or altSignerPath", e);
        }
    }

    static class SignatureFile {

        /**
         * SignatureFile
         */
        Manifest sf;

        /**
         * .SF base name
         */
        String baseName;

        public SignatureFile(MessageDigest digests[],
                             Manifest mf,
                             ManifestDigester md,
                             String baseName,
                             boolean sectionsonly) {

            this.baseName = baseName;

            String version = System.getProperty("java.version");
            String javaVendor = System.getProperty("java.vendor");

            sf = new Manifest();
            Attributes mattr = sf.getMainAttributes();

            mattr.putValue(Attributes.Name.SIGNATURE_VERSION.toString(), "1.0");
            mattr.putValue("Created-By", version + " (" + javaVendor + ")");

            if (!sectionsonly) {
                for (MessageDigest digest: digests) {
                    mattr.putValue(digest.getAlgorithm() + "-Digest-Manifest",
                            Base64.getEncoder().encodeToString(
                                    md.manifestDigest(digest)));
                }
            }

            // create digest of the manifest main attributes
            ManifestDigester.Entry mde = md.getMainAttsEntry(false);
            if (mde != null) {
                for (MessageDigest digest : digests) {
                    mattr.putValue(digest.getAlgorithm() + "-Digest-" +
                            ManifestDigester.MF_MAIN_ATTRS,
                            Base64.getEncoder().encodeToString(mde.digest(digest)));
                }
            } else {
                throw new IllegalStateException
                        ("ManifestDigester failed to create " +
                                "Manifest-Main-Attribute entry");
            }

            // go through the manifest entries and create the digests
            Map<String, Attributes> entries = sf.getEntries();
            for (String name: mf.getEntries().keySet()) {
                mde = md.get(name, false);
                if (mde != null) {
                    Attributes attr = new Attributes();
                    for (MessageDigest digest: digests) {
                        attr.putValue(digest.getAlgorithm() + "-Digest",
                                Base64.getEncoder().encodeToString(
                                        mde.digest(digest)));
                    }
                    entries.put(name, attr);
                }
            }
        }

        // Write .SF file
        public void write(OutputStream out) throws IOException {
            sf.write(out);
        }

        private static String getBaseSignatureFilesName(String baseName) {
            return "META-INF/" + baseName + ".";
        }

        // get .SF file name
        public String getMetaName() {
            return getBaseSignatureFilesName(baseName) + "SF";
        }

        // get .DSA (or .DSA, .EC) file name
        public String getBlockName(PrivateKey privateKey) {
            String type = SignatureFileVerifier.getBlockExtension(privateKey);
            return getBaseSignatureFilesName(baseName) + type;
        }
    }

    @SuppressWarnings("removal")
    @Deprecated(since="16", forRemoval=true)
    class JarSignerParameters implements ContentSignerParameters {

        private String[] args;
        private URI tsa;
        private byte[] signature;
        private String signatureAlgorithm;
        private X509Certificate[] signerCertificateChain;
        private byte[] content;
        private ZipFile source;
        private String tSAPolicyID;
        private String tSADigestAlg;

        JarSignerParameters(String[] args, URI tsa,
                            String tSAPolicyID, String tSADigestAlg,
                            byte[] signature, String signatureAlgorithm,
                            X509Certificate[] signerCertificateChain,
                            byte[] content, ZipFile source) {

            Objects.requireNonNull(signature);
            Objects.requireNonNull(signatureAlgorithm);
            Objects.requireNonNull(signerCertificateChain);

            this.args = args;
            this.tsa = tsa;
            this.tSAPolicyID = tSAPolicyID;
            this.tSADigestAlg = tSADigestAlg;
            this.signature = signature;
            this.signatureAlgorithm = signatureAlgorithm;
            this.signerCertificateChain = signerCertificateChain;
            this.content = content;
            this.source = source;
        }

        public String[] getCommandLine() {
            return args;
        }

        public URI getTimestampingAuthority() {
            return tsa;
        }

        public X509Certificate getTimestampingAuthorityCertificate() {
            // We don't use this param. Always provide tsaURI.
            return null;
        }

        public String getTSAPolicyID() {
            return tSAPolicyID;
        }

        public String getTSADigestAlg() {
            return tSADigestAlg;
        }

        public byte[] getSignature() {
            return signature;
        }

        public String getSignatureAlgorithm() {
            return signatureAlgorithm;
        }

        public X509Certificate[] getSignerCertificateChain() {
            return signerCertificateChain;
        }

        public byte[] getContent() {
            return content;
        }

        public ZipFile getSource() {
            return source;
        }
    }
}
