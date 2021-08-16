/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs12;

import java.io.*;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.security.KeyStoreException;
import java.security.PKCS12Attribute;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.x500.X500Principal;

import jdk.internal.access.SharedSecrets;
import sun.security.action.GetPropertyAction;
import sun.security.tools.KeyStoreUtil;
import sun.security.util.*;
import sun.security.pkcs.ContentInfo;
import sun.security.x509.AlgorithmId;
import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.provider.JavaKeyStore.JKS;
import sun.security.x509.AuthorityKeyIdentifierExtension;


/**
 * This class provides the keystore implementation referred to as "PKCS12".
 * Implements the PKCS#12 PFX protected using the Password privacy mode.
 * The contents are protected using Password integrity mode.
 *
 * NOTE: In a PKCS12 keystore, entries are identified by the alias, and
 * a localKeyId is required to match the private key with the certificate.
 * Trusted certificate entries are identified by the presence of an
 * trustedKeyUsage attribute.
 *
 * @author Seema Malkani
 * @author Jeff Nisewanger
 * @author Jan Luehe
 *
 * @see java.security.KeyStoreSpi
 */
public final class PKCS12KeyStore extends KeyStoreSpi {

    // Hardcoded defaults. They should be the same with commented out
    // lines inside the java.security file.
    private static final String DEFAULT_CERT_PBE_ALGORITHM
            = "PBEWithHmacSHA256AndAES_256";
    private static final String DEFAULT_KEY_PBE_ALGORITHM
            = "PBEWithHmacSHA256AndAES_256";
    private static final String DEFAULT_MAC_ALGORITHM = "HmacPBESHA256";
    private static final int DEFAULT_CERT_PBE_ITERATION_COUNT = 10000;
    private static final int DEFAULT_KEY_PBE_ITERATION_COUNT = 10000;
    private static final int DEFAULT_MAC_ITERATION_COUNT = 10000;

    // Legacy settings. Used when "keystore.pkcs12.legacy" is set.
    private static final String LEGACY_CERT_PBE_ALGORITHM
            = "PBEWithSHA1AndRC2_40";
    private static final String LEGACY_KEY_PBE_ALGORITHM
            = "PBEWithSHA1AndDESede";
    private static final String LEGACY_MAC_ALGORITHM = "HmacPBESHA1";
    private static final int LEGACY_PBE_ITERATION_COUNT = 50000;
    private static final int LEGACY_MAC_ITERATION_COUNT = 100000;

    // Big switch. When this system property is set. Legacy settings
    // are used no matter what other keystore.pkcs12.* properties are set.
    // Note: This is only a system property, there's no same-name
    // security property defined.
    private static final String USE_LEGACY_PROP = "keystore.pkcs12.legacy";

    // special PKCS12 keystore that supports PKCS12 and JKS file formats
    public static final class DualFormatPKCS12 extends KeyStoreDelegator {
        public DualFormatPKCS12() {
            super("PKCS12", PKCS12KeyStore.class, "JKS", JKS.class);
        }
    }

    public static final int VERSION_3 = 3;

    private static final int MAX_ITERATION_COUNT = 5000000;
    private static final int SALT_LEN = 20;

    private static final KnownOIDs[] CORE_ATTRIBUTES = {
        KnownOIDs.FriendlyName,
        KnownOIDs.LocalKeyID,
        KnownOIDs.ORACLE_TrustedKeyUsage
    };

    private static final Debug debug = Debug.getInstance("pkcs12");

    private static final ObjectIdentifier PKCS8ShroudedKeyBag_OID =
            ObjectIdentifier.of(KnownOIDs.PKCS8ShroudedKeyBag);
    private static final ObjectIdentifier CertBag_OID =
            ObjectIdentifier.of(KnownOIDs.CertBag);
    private static final ObjectIdentifier SecretBag_OID =
            ObjectIdentifier.of(KnownOIDs.SecretBag);

    private static final ObjectIdentifier PKCS9FriendlyName_OID =
            ObjectIdentifier.of(KnownOIDs.FriendlyName);
    private static final ObjectIdentifier PKCS9LocalKeyId_OID =
            ObjectIdentifier.of(KnownOIDs.LocalKeyID);
    private static final ObjectIdentifier PKCS9CertType_OID =
            ObjectIdentifier.of(KnownOIDs.CertTypeX509);
    private static final ObjectIdentifier pbes2_OID =
            ObjectIdentifier.of(KnownOIDs.PBES2);

    /*
     * Temporary Oracle OID
     *
     * {joint-iso-itu-t(2) country(16) us(840) organization(1)
     *  oracle(113894) jdk(746875) crypto(1) id-at-trustedKeyUsage(1)}
     */
    private static final ObjectIdentifier TrustedKeyUsage_OID =
            ObjectIdentifier.of(KnownOIDs.ORACLE_TrustedKeyUsage);

    private static final ObjectIdentifier[] AnyUsage = new ObjectIdentifier[] {
                ObjectIdentifier.of(KnownOIDs.anyExtendedKeyUsage)
            };

    private int counter = 0;

    // private key count
    // Note: This is a workaround to allow null localKeyID attribute
    // in pkcs12 with one private key entry and associated cert-chain
    private int privateKeyCount = 0;

    // secret key count
    private int secretKeyCount = 0;

    // certificate count
    private int certificateCount = 0;

    // Alg/params used for *this* keystore. Initialized as -1 for ic and
    // null for algorithm names. When an existing file is read, they will be
    // assigned inside engineLoad() so storing an existing keystore uses the
    // old alg/params. This makes sure if a keystore is created password-less
    // it will be password-less forever. Otherwise, engineStore() will read
    // the default values. These fields are always reset when load() is called.
    private String certProtectionAlgorithm = null;
    private int certPbeIterationCount = -1;
    private String macAlgorithm = null;
    private int macIterationCount = -1;

    // the source of randomness
    private SecureRandom random;

    // A keystore entry and associated attributes
    private static class Entry {
        Date date; // the creation date of this entry
        String alias;
        byte[] keyId;
        Set<KeyStore.Entry.Attribute> attributes;
    }

    // A key entry
    private static class KeyEntry extends Entry {
    }

    // A private key entry and its supporting certificate chain
    private static class PrivateKeyEntry extends KeyEntry {
        byte[] protectedPrivKey;
        Certificate[] chain;
    };

    // A secret key
    private static class SecretKeyEntry extends KeyEntry {
        byte[] protectedSecretKey;
    };

    // A certificate entry
    private static class CertEntry extends Entry {
        final X509Certificate cert;
        ObjectIdentifier[] trustedKeyUsage;

        CertEntry(X509Certificate cert, byte[] keyId, String alias) {
            this(cert, keyId, alias, null, null);
        }

        CertEntry(X509Certificate cert, byte[] keyId, String alias,
                ObjectIdentifier[] trustedKeyUsage,
                Set<? extends KeyStore.Entry.Attribute> attributes) {
            this.date = new Date();
            this.cert = cert;
            this.keyId = keyId;
            this.alias = alias;
            this.trustedKeyUsage = trustedKeyUsage;
            this.attributes = new HashSet<>();
            if (attributes != null) {
                this.attributes.addAll(attributes);
            }
        }
    }

    /**
     * Retries an action with password "\0" if "" fails.
     * @param <T> the return type
     */
    @FunctionalInterface
    private interface RetryWithZero<T> {

        T tryOnce(char[] password) throws Exception;

        static <S> S run(RetryWithZero<S> f, char[] password) throws Exception {
            try {
                return f.tryOnce(password);
            } catch (Exception e) {
                if (password.length == 0) {
                    // Retry using an empty password with a NUL terminator.
                    if (debug != null) {
                        debug.println("Retry with a NUL password");
                    }
                    return f.tryOnce(new char[1]);
                }
                throw e;
            }
        }
    }

    /**
     * Private keys and certificates are stored in a map.
     * Map entries are keyed by alias names.
     */
    private Map<String, Entry> entries =
        Collections.synchronizedMap(new LinkedHashMap<String, Entry>());

    private ArrayList<KeyEntry> keyList = new ArrayList<KeyEntry>();
    private List<X509Certificate> allCerts = new ArrayList<>();
    private ArrayList<CertEntry> certEntries = new ArrayList<CertEntry>();

    /**
     * Returns the key associated with the given alias, using the given
     * password to recover it.
     *
     * @param alias the alias name
     * @param password the password for recovering the key
     *
     * @return the requested key, or null if the given alias does not exist
     * or does not identify a <i>key entry</i>.
     *
     * @exception NoSuchAlgorithmException if the algorithm for recovering the
     * key cannot be found
     * @exception UnrecoverableKeyException if the key cannot be recovered
     * (e.g., the given password is wrong).
     */
    public Key engineGetKey(String alias, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException
    {
        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        Key key = null;

        if (entry == null || (!(entry instanceof KeyEntry))) {
            return null;
        }

        // get the encoded private key or secret key
        byte[] encrBytes = null;
        if (entry instanceof PrivateKeyEntry) {
            encrBytes = ((PrivateKeyEntry) entry).protectedPrivKey;
        } else if (entry instanceof SecretKeyEntry) {
            encrBytes = ((SecretKeyEntry) entry).protectedSecretKey;
        } else {
            throw new UnrecoverableKeyException("Error locating key");
        }

        byte[] encryptedKey;
        AlgorithmParameters algParams;
        ObjectIdentifier algOid;

        try {
            // get the encrypted private key
            EncryptedPrivateKeyInfo encrInfo =
                        new EncryptedPrivateKeyInfo(encrBytes);
            encryptedKey = encrInfo.getEncryptedData();

            // parse Algorithm parameters
            DerValue val = new DerValue(encrInfo.getAlgorithm().encode());
            DerInputStream in = val.toDerInputStream();
            algOid = in.getOID();
            algParams = parseAlgParameters(algOid, in);

        } catch (IOException ioe) {
            UnrecoverableKeyException uke =
                new UnrecoverableKeyException("Private key not stored as "
                                 + "PKCS#8 EncryptedPrivateKeyInfo: " + ioe);
            uke.initCause(ioe);
            throw uke;
        }

       try {
            PBEParameterSpec pbeSpec;
            int ic;

            if (algParams != null) {
                try {
                    pbeSpec =
                        algParams.getParameterSpec(PBEParameterSpec.class);
                } catch (InvalidParameterSpecException ipse) {
                    throw new IOException("Invalid PBE algorithm parameters");
                }
                ic = pbeSpec.getIterationCount();

                if (ic > MAX_ITERATION_COUNT) {
                    throw new IOException("key PBE iteration count too large");
                }
            } else {
                ic = 0;
            }

            key = RetryWithZero.run(pass -> {
                // Use JCE
                Cipher cipher = Cipher.getInstance(
                        mapPBEParamsToAlgorithm(algOid, algParams));
                SecretKey skey = getPBEKey(pass);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, skey, algParams);
                } finally {
                    destroyPBEKey(skey);
                }
                byte[] keyInfo = cipher.doFinal(encryptedKey);
                /*
                 * Parse the key algorithm and then use a JCA key factory
                 * to re-create the key.
                 */
                DerValue val = new DerValue(keyInfo);
                try {
                    DerInputStream in = val.toDerInputStream();
                    int i = in.getInteger();
                    DerValue[] value = in.getSequence(2);
                    if (value.length < 1 || value.length > 2) {
                        throw new IOException("Invalid length for AlgorithmIdentifier");
                    }
                    AlgorithmId algId = new AlgorithmId(value[0].getOID());
                    String keyAlgo = algId.getName();

                    // decode private key
                    if (entry instanceof PrivateKeyEntry) {
                        KeyFactory kfac = KeyFactory.getInstance(keyAlgo);
                        PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(keyInfo);
                        try {
                            Key tmp = kfac.generatePrivate(kspec);

                            if (debug != null) {
                                debug.println("Retrieved a protected private key at alias" +
                                        " '" + alias + "' (" +
                                        mapPBEParamsToAlgorithm(algOid, algParams) +
                                        " iterations: " + ic + ")");
                            }
                            return tmp;
                        } finally {
                            SharedSecrets.getJavaSecuritySpecAccess()
                                    .clearEncodedKeySpec(kspec);
                        }
                        // decode secret key
                    } else {
                        byte[] keyBytes = in.getOctetString();
                        SecretKeySpec secretKeySpec =
                                new SecretKeySpec(keyBytes, keyAlgo);

                        try {
                            // Special handling required for PBE: needs a PBEKeySpec
                            Key tmp;
                            if (keyAlgo.startsWith("PBE")) {
                                SecretKeyFactory sKeyFactory =
                                        SecretKeyFactory.getInstance(keyAlgo);
                                KeySpec pbeKeySpec =
                                        sKeyFactory.getKeySpec(secretKeySpec, PBEKeySpec.class);
                                try {
                                    tmp = sKeyFactory.generateSecret(pbeKeySpec);
                                } finally {
                                    ((PBEKeySpec)pbeKeySpec).clearPassword();
                                    SharedSecrets.getJavaxCryptoSpecAccess()
                                            .clearSecretKeySpec(secretKeySpec);
                                }
                            } else {
                                tmp = secretKeySpec;
                            }

                            if (debug != null) {
                                debug.println("Retrieved a protected secret key at alias " +
                                        "'" + alias + "' (" +
                                        mapPBEParamsToAlgorithm(algOid, algParams) +
                                        " iterations: " + ic + ")");
                            }
                            return tmp;
                        } finally {
                            Arrays.fill(keyBytes, (byte)0);
                        }
                    }
                } finally {
                    val.clear();
                    Arrays.fill(keyInfo, (byte) 0);
                }
            }, password);

        } catch (Exception e) {
            UnrecoverableKeyException uke =
                new UnrecoverableKeyException("Get Key failed: " +
                                        e.getMessage());
            uke.initCause(e);
            throw uke;
        }
        return key;
    }

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @param alias the alias name
     *
     * @return the certificate chain (ordered with the user's certificate first
     * and the root certificate authority last), or null if the given alias
     * does not exist or does not contain a certificate chain (i.e., the given
     * alias identifies either a <i>trusted certificate entry</i> or a
     * <i>key entry</i> without a certificate chain).
     */
    public Certificate[] engineGetCertificateChain(String alias) {
        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry != null && entry instanceof PrivateKeyEntry) {
            if (((PrivateKeyEntry) entry).chain == null) {
                return null;
            } else {

                if (debug != null) {
                    debug.println("Retrieved a " +
                        ((PrivateKeyEntry) entry).chain.length +
                        "-certificate chain at alias '" + alias + "'");
                }

                return ((PrivateKeyEntry) entry).chain.clone();
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the certificate associated with the given alias.
     *
     * <p>If the given alias name identifies a
     * <i>trusted certificate entry</i>, the certificate associated with that
     * entry is returned. If the given alias name identifies a
     * <i>key entry</i>, the first element of the certificate chain of that
     * entry is returned, or null if that entry does not have a certificate
     * chain.
     *
     * @param alias the alias name
     *
     * @return the certificate, or null if the given alias does not exist or
     * does not contain a certificate.
     */
    public Certificate engineGetCertificate(String alias) {
        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry == null) {
            return null;
        }
        if (entry instanceof CertEntry &&
            ((CertEntry) entry).trustedKeyUsage != null) {

            if (debug != null) {
                if (Arrays.equals(AnyUsage,
                    ((CertEntry) entry).trustedKeyUsage)) {
                    debug.println("Retrieved a certificate at alias '" + alias +
                        "' (trusted for any purpose)");
                } else {
                    debug.println("Retrieved a certificate at alias '" + alias +
                        "' (trusted for limited purposes)");
                }
            }

            return ((CertEntry) entry).cert;

        } else if (entry instanceof PrivateKeyEntry) {
            if (((PrivateKeyEntry) entry).chain == null) {
                return null;
            } else {

                if (debug != null) {
                    debug.println("Retrieved a certificate at alias '" + alias +
                        "'");
                }

                return ((PrivateKeyEntry) entry).chain[0];
            }

        } else {
            return null;
        }
    }

    /**
     * Returns the creation date of the entry identified by the given alias.
     *
     * @param alias the alias name
     *
     * @return the creation date of this entry, or null if the given alias does
     * not exist
     */
    public Date engineGetCreationDate(String alias) {
        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry != null) {
            return new Date(entry.date.getTime());
        } else {
            return null;
        }
    }

    /**
     * Assigns the given key to the given alias, protecting it with the given
     * password.
     *
     * <p>If the given key is of type <code>java.security.PrivateKey</code>,
     * it must be accompanied by a certificate chain certifying the
     * corresponding public key.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key to be associated with the alias
     * @param password the password to protect the key
     * @param chain the certificate chain for the corresponding public
     * key (only required if the given key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if the given key cannot be protected, or
     * this operation fails for some other reason
     */
    public synchronized void engineSetKeyEntry(String alias, Key key,
                        char[] password, Certificate[] chain)
        throws KeyStoreException
    {
        KeyStore.PasswordProtection passwordProtection =
            new KeyStore.PasswordProtection(password);

        try {
            setKeyEntry(alias, key, passwordProtection, chain, null);

        } finally {
            try {
                passwordProtection.destroy();
            } catch (DestroyFailedException dfe) {
                // ignore
            }
        }
    }

    /*
     * Sets a key entry (with attributes, when present)
     */
    private void setKeyEntry(String alias, Key key,
        KeyStore.PasswordProtection passwordProtection, Certificate[] chain,
        Set<KeyStore.Entry.Attribute> attributes)
            throws KeyStoreException
    {
        try {
            Entry entry;

            if (key instanceof PrivateKey) {
                // Check that all the certs are X.509 certs
                checkX509Certs(chain);

                PrivateKeyEntry keyEntry = new PrivateKeyEntry();
                keyEntry.date = new Date();

                if ((key.getFormat().equals("PKCS#8")) ||
                    (key.getFormat().equals("PKCS8"))) {

                    if (debug != null) {
                        debug.println(
                            "Setting a protected private key at alias '" +
                            alias + "'");
                        }

                    // Encrypt the private key
                    byte[] encoded = key.getEncoded();
                    try {
                        keyEntry.protectedPrivKey =
                                encryptPrivateKey(encoded, passwordProtection);
                    } finally {
                        if (encoded != null) {
                            Arrays.fill(encoded, (byte) 0);
                        }
                    }
                } else {
                    throw new KeyStoreException("Private key is not encoded" +
                                "as PKCS#8");
                }

                // clone the chain
                if (chain != null) {
                    // validate cert-chain
                    if ((chain.length > 1) && (!validateChain(chain)))
                       throw new KeyStoreException("Certificate chain is " +
                                                "not valid");
                    keyEntry.chain = chain.clone();
                    certificateCount += chain.length;

                    if (debug != null) {
                        debug.println("Setting a " + chain.length +
                            "-certificate chain at alias '" + alias + "'");
                    }
                }
                privateKeyCount++;
                entry = keyEntry;

            } else if (key instanceof SecretKey) {
                SecretKeyEntry keyEntry = new SecretKeyEntry();
                keyEntry.date = new Date();

                // Encode secret key in a PKCS#8
                DerOutputStream secretKeyInfo = new DerOutputStream();
                secretKeyInfo.putInteger(0);
                AlgorithmId algId = AlgorithmId.get(key.getAlgorithm());
                algId.encode(secretKeyInfo);

                byte[] encoded = key.getEncoded();
                secretKeyInfo.putOctetString(encoded);
                Arrays.fill(encoded, (byte)0);

                DerValue pkcs8 = DerValue.wrap(DerValue.tag_Sequence, secretKeyInfo);
                byte[] p8Array = pkcs8.toByteArray();
                pkcs8.clear();
                try {
                    // Encrypt the secret key (using same PBE as for private keys)
                    keyEntry.protectedSecretKey =
                            encryptPrivateKey(p8Array, passwordProtection);
                } finally {
                    Arrays.fill(p8Array, (byte)0);
                }

                if (debug != null) {
                    debug.println("Setting a protected secret key at alias '" +
                        alias + "'");
                }
                secretKeyCount++;
                entry = keyEntry;

            } else {
                throw new KeyStoreException("Unsupported Key type");
            }

            entry.attributes = new HashSet<>();
            if (attributes != null) {
                entry.attributes.addAll(attributes);
            }
            // set the keyId to current date
            entry.keyId = ("Time " + (entry.date).getTime()).getBytes(UTF_8);
            // set the alias
            entry.alias = alias.toLowerCase(Locale.ENGLISH);
            // add the entry
            entries.put(alias.toLowerCase(Locale.ENGLISH), entry);

        } catch (KeyStoreException kse) {
            throw kse;
        } catch (Exception nsae) {
            throw new KeyStoreException("Key protection" +
                       " algorithm not found: " + nsae, nsae);
        }
    }

    /**
     * Assigns the given key (that has already been protected) to the given
     * alias.
     *
     * <p>If the protected key is of type
     * <code>java.security.PrivateKey</code>, it must be accompanied by a
     * certificate chain certifying the corresponding public key. If the
     * underlying keystore implementation is of type <code>jks</code>,
     * <code>key</code> must be encoded as an
     * <code>EncryptedPrivateKeyInfo</code> as defined in the PKCS #8 standard.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key (and possibly
     * certificate chain).
     *
     * @param alias the alias name
     * @param key the key (in protected format) to be associated with the alias
     * @param chain the certificate chain for the corresponding public
     * key (only useful if the protected key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if this operation fails.
     */
    public synchronized void engineSetKeyEntry(String alias, byte[] key,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        // Check that all the certs are X.509 certs
        checkX509Certs(chain);

        // Private key must be encoded as EncryptedPrivateKeyInfo
        // as defined in PKCS#8
        try {
            new EncryptedPrivateKeyInfo(key);
        } catch (IOException ioe) {
            throw new KeyStoreException("Private key is not stored"
                    + " as PKCS#8 EncryptedPrivateKeyInfo: " + ioe, ioe);
        }

        PrivateKeyEntry entry = new PrivateKeyEntry();
        entry.date = new Date();

        if (debug != null) {
            debug.println("Setting a protected private key at alias '" +
                alias + "'");
        }

        // set the keyId to current date
        entry.keyId = ("Time " + (entry.date).getTime()).getBytes(UTF_8);
        // set the alias
        entry.alias = alias.toLowerCase(Locale.ENGLISH);

        entry.protectedPrivKey = key.clone();
        if (chain != null) {
            // validate cert-chain
            if ((chain.length > 1) && (!validateChain(chain))) {
                throw new KeyStoreException("Certificate chain is "
                        + "not valid");
            }
            entry.chain = chain.clone();
            certificateCount += chain.length;

            if (debug != null) {
                debug.println("Setting a " + entry.chain.length +
                    "-certificate chain at alias '" + alias + "'");
            }
        }

        // add the entry
        privateKeyCount++;
        entries.put(alias.toLowerCase(Locale.ENGLISH), entry);
    }


    /*
     * Generate random salt
     */
    private byte[] getSalt()
    {
        // Generate a random salt.
        byte[] salt = new byte[SALT_LEN];
        if (random == null) {
           random = new SecureRandom();
        }
        random.nextBytes(salt);
        return salt;
    }

    /*
     * Generate PBE Algorithm Parameters
     */
    private AlgorithmParameters getPBEAlgorithmParameters(
            String algorithm, int iterationCount) throws IOException {
        AlgorithmParameters algParams;

        byte[] salt = getSalt();
        if (KnownOIDs.findMatch(algorithm) == KnownOIDs.PBEWithMD5AndDES) {
            // PBES1 scheme such as PBEWithMD5AndDES requires a 8-byte salt
            salt = Arrays.copyOf(salt, 8);
        }

        // create PBE parameters from salt and iteration count
        PBEParameterSpec paramSpec =
                new PBEParameterSpec(salt, iterationCount);
        try {
           algParams = AlgorithmParameters.getInstance(algorithm);
           algParams.init(paramSpec);
        } catch (Exception e) {
           throw new IOException("getPBEAlgorithmParameters failed: " +
                                 e.getMessage(), e);
        }
        return algParams;
    }

    /*
     * parse Algorithm Parameters
     */
    private AlgorithmParameters parseAlgParameters(ObjectIdentifier algorithm,
        DerInputStream in) throws IOException
    {
        AlgorithmParameters algParams = null;
        try {
            DerValue params;
            if (in.available() == 0) {
                params = null;
            } else {
                params = in.getDerValue();
                if (params.tag == DerValue.tag_Null) {
                   params = null;
                }
            }
            if (params != null) {
                if (algorithm.equals(pbes2_OID)) {
                    algParams = AlgorithmParameters.getInstance("PBES2");
                } else {
                    algParams = AlgorithmParameters.getInstance("PBE");
                }
                algParams.init(params.toByteArray());
            }
        } catch (Exception e) {
           throw new IOException("parseAlgParameters failed: " +
                                 e.getMessage(), e);
        }
        return algParams;
    }

    /*
     * Generate PBE key
     */
    private SecretKey getPBEKey(char[] password) throws IOException
    {
        SecretKey skey = null;

        try {
            PBEKeySpec keySpec = new PBEKeySpec(password);
            SecretKeyFactory skFac = SecretKeyFactory.getInstance("PBE");
            skey = skFac.generateSecret(keySpec);
            keySpec.clearPassword();
        } catch (Exception e) {
           throw new IOException("getSecretKey failed: " +
                                 e.getMessage(), e);
        }
        return skey;
    }

    /*
     * Destroy the key obtained from getPBEKey().
     */
    private void destroyPBEKey(SecretKey key) {
        try {
            key.destroy();
        } catch (DestroyFailedException e) {
            // Accept this
        }
    }

    /*
     * Encrypt private key or secret key using Password-based encryption (PBE)
     * as defined in PKCS#5.
     *
     * @return encrypted private key or secret key encoded as
     *         EncryptedPrivateKeyInfo
     */
    private byte[] encryptPrivateKey(byte[] data,
        KeyStore.PasswordProtection passwordProtection)
        throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException
    {
        byte[] key = null;

        try {
            String algorithm;
            AlgorithmParameters algParams;
            AlgorithmId algid;

            // Initialize PBE algorithm and parameters
            algorithm = passwordProtection.getProtectionAlgorithm();
            if (algorithm != null) {
                AlgorithmParameterSpec algParamSpec =
                    passwordProtection.getProtectionParameters();
                if (algParamSpec != null) {
                    algParams = AlgorithmParameters.getInstance(algorithm);
                    algParams.init(algParamSpec);
                } else {
                    algParams = getPBEAlgorithmParameters(algorithm,
                            defaultKeyPbeIterationCount());
                }
            } else {
                // Check default key protection algorithm for PKCS12 keystores
                algorithm = defaultKeyProtectionAlgorithm();
                algParams = getPBEAlgorithmParameters(algorithm,
                        defaultKeyPbeIterationCount());
            }

            ObjectIdentifier pbeOID = mapPBEAlgorithmToOID(algorithm);
            if (pbeOID == null) {
                    throw new IOException("PBE algorithm '" + algorithm +
                        " 'is not supported for key entry protection");
            }

            // Use JCE
            Cipher cipher = Cipher.getInstance(algorithm);
            SecretKey skey = getPBEKey(passwordProtection.getPassword());
            try {
                cipher.init(Cipher.ENCRYPT_MODE, skey, algParams);
            } finally {
                destroyPBEKey(skey);
            }
            byte[] encryptedKey = cipher.doFinal(data);
            algid = new AlgorithmId(pbeOID, cipher.getParameters());

            if (debug != null) {
                debug.println("  (Cipher algorithm: " + cipher.getAlgorithm() +
                    ")");
            }

            // wrap encrypted private key in EncryptedPrivateKeyInfo
            // as defined in PKCS#8
            EncryptedPrivateKeyInfo encrInfo =
                new EncryptedPrivateKeyInfo(algid, encryptedKey);
            key = encrInfo.getEncoded();
        } catch (Exception e) {
            UnrecoverableKeyException uke =
                new UnrecoverableKeyException("Encrypt Private Key failed: "
                                                + e.getMessage());
            uke.initCause(e);
            throw uke;
        }

        return key;
    }

    /*
     * Map a PBE algorithm name onto its object identifier
     */
    private static ObjectIdentifier mapPBEAlgorithmToOID(String algorithm)
        throws NoSuchAlgorithmException {
        // Check for PBES2 algorithms
        if (algorithm.toLowerCase(Locale.ENGLISH).startsWith("pbewithhmacsha")) {
            return pbes2_OID;
        }
        return AlgorithmId.get(algorithm).getOID();
    }

    /*
     * Map a PBE algorithm parameters onto its algorithm name
     */
    private static String mapPBEParamsToAlgorithm(ObjectIdentifier algorithm,
        AlgorithmParameters algParams) throws NoSuchAlgorithmException {
        // Check for PBES2 algorithms
        if (algorithm.equals(pbes2_OID) && algParams != null) {
            return algParams.toString();
        }
        return new AlgorithmId(algorithm).getName();
    }

    /**
     * Assigns the given certificate to the given alias.
     *
     * <p>If the given alias already exists in this keystore and identifies a
     * <i>trusted certificate entry</i>, the certificate associated with it is
     * overridden by the given certificate.
     *
     * @param alias the alias name
     * @param cert the certificate
     *
     * @exception KeyStoreException if the given alias already exists and does
     * not identify a <i>trusted certificate entry</i>, or this operation fails
     * for some other reason.
     */
    public synchronized void engineSetCertificateEntry(String alias,
        Certificate cert) throws KeyStoreException
    {
        setCertEntry(alias, cert, null);
    }

    /*
     * Sets a trusted cert entry (with attributes, when present)
     */
    private void setCertEntry(String alias, Certificate cert,
        Set<KeyStore.Entry.Attribute> attributes) throws KeyStoreException {

        // Check that the cert is an X.509 cert
        if (cert != null && (!(cert instanceof X509Certificate))) {
            throw new KeyStoreException(
                "Only X.509 certificates are supported - rejecting class: " +
                cert.getClass().getName());
        }

        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry != null && entry instanceof KeyEntry) {
            throw new KeyStoreException("Cannot overwrite own certificate");
        }

        CertEntry certEntry =
            new CertEntry((X509Certificate) cert, null, alias, AnyUsage,
                attributes);
        certificateCount++;
        entries.put(alias.toLowerCase(Locale.ENGLISH), certEntry);

        if (debug != null) {
            debug.println("Setting a trusted certificate at alias '" + alias +
                "'");
        }
    }

    /**
     * Deletes the entry identified by the given alias from this keystore.
     *
     * @param alias the alias name
     *
     * @exception KeyStoreException if the entry cannot be removed.
     */
    public synchronized void engineDeleteEntry(String alias)
        throws KeyStoreException
    {
        if (debug != null) {
            debug.println("Removing entry at alias '" + alias + "'");
        }

        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry instanceof PrivateKeyEntry) {
            PrivateKeyEntry keyEntry = (PrivateKeyEntry) entry;
            if (keyEntry.chain != null) {
                certificateCount -= keyEntry.chain.length;
            }
            privateKeyCount--;
        } else if (entry instanceof CertEntry) {
            certificateCount--;
        } else if (entry instanceof SecretKeyEntry) {
            secretKeyCount--;
        }
        entries.remove(alias.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Lists all the alias names of this keystore.
     *
     * @return enumeration of the alias names
     */
    public Enumeration<String> engineAliases() {
        return Collections.enumeration(entries.keySet());
    }

    /**
     * Checks if the given alias exists in this keystore.
     *
     * @param alias the alias name
     *
     * @return true if the alias exists, false otherwise
     */
    public boolean engineContainsAlias(String alias) {
        return entries.containsKey(alias.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Retrieves the number of entries in this keystore.
     *
     * @return the number of entries in this keystore
     */
    public int engineSize() {
        return entries.size();
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>key entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>key entry</i>, false otherwise.
     */
    public boolean engineIsKeyEntry(String alias) {
        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry != null && entry instanceof KeyEntry) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, false otherwise.
     */
    public boolean engineIsCertificateEntry(String alias) {
        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entry != null && entry instanceof CertEntry &&
            ((CertEntry) entry).trustedKeyUsage != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if the keystore {@code Entry} for the specified
     * {@code alias} is an instance or subclass of the specified
     * {@code entryClass}.
     *
     * @param alias the alias name
     * @param entryClass the entry class
     *
     * @return true if the keystore {@code Entry} for the specified
     *          {@code alias} is an instance or subclass of the
     *          specified {@code entryClass}, false otherwise
     *
     * @since 1.5
     */
    @Override
    public boolean
        engineEntryInstanceOf(String alias,
                              Class<? extends KeyStore.Entry> entryClass)
    {
        if (entryClass == KeyStore.TrustedCertificateEntry.class) {
            return engineIsCertificateEntry(alias);
        }

        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (entryClass == KeyStore.PrivateKeyEntry.class) {
            return (entry != null && entry instanceof PrivateKeyEntry);
        }
        if (entryClass == KeyStore.SecretKeyEntry.class) {
            return (entry != null && entry instanceof SecretKeyEntry);
        }
        return false;
    }

    /**
     * Returns the (alias) name of the first keystore entry whose certificate
     * matches the given certificate.
     *
     * <p>This method attempts to match the given certificate with each
     * keystore entry. If the entry being considered
     * is a <i>trusted certificate entry</i>, the given certificate is
     * compared to that entry's certificate. If the entry being considered is
     * a <i>key entry</i>, the given certificate is compared to the first
     * element of that entry's certificate chain (if a chain exists).
     *
     * @param cert the certificate to match with.
     *
     * @return the (alias) name of the first entry with matching certificate,
     * or null if no such entry exists in this keystore.
     */
    public String engineGetCertificateAlias(Certificate cert) {
        Certificate certElem = null;

        for (Enumeration<String> e = engineAliases(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            Entry entry = entries.get(alias);
            if (entry instanceof PrivateKeyEntry) {
                if (((PrivateKeyEntry) entry).chain != null) {
                    certElem = ((PrivateKeyEntry) entry).chain[0];
                }
            } else if (entry instanceof CertEntry &&
                    ((CertEntry) entry).trustedKeyUsage != null) {
                certElem = ((CertEntry) entry).cert;
            } else {
                continue;
            }
            if (certElem != null && certElem.equals(cert)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Stores this keystore to the given output stream, and protects its
     * integrity with the given password.
     *
     * @param stream the output stream to which this keystore is written.
     * @param password the password to generate the keystore integrity check
     *
     * @exception IOException if there was an I/O problem with data
     * @exception NoSuchAlgorithmException if the appropriate data integrity
     * algorithm could not be found
     * @exception CertificateException if any of the certificates included in
     * the keystore data could not be stored
     */
    public synchronized void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {

        // -- Create PFX
        DerOutputStream pfx = new DerOutputStream();

        // PFX version (always write the latest version)
        DerOutputStream version = new DerOutputStream();
        version.putInteger(VERSION_3);
        byte[] pfxVersion = version.toByteArray();
        pfx.write(pfxVersion);

        // -- Create AuthSafe
        DerOutputStream authSafe = new DerOutputStream();

        // -- Create ContentInfos
        DerOutputStream authSafeContentInfo = new DerOutputStream();

        // -- create safeContent Data ContentInfo
        if (privateKeyCount > 0 || secretKeyCount > 0) {

            if (debug != null) {
                debug.println("Storing " + (privateKeyCount + secretKeyCount) +
                    " protected key(s) in a PKCS#7 data");
            }

            byte[] safeContentData = createSafeContent();
            ContentInfo dataContentInfo = new ContentInfo(safeContentData);
            dataContentInfo.encode(authSafeContentInfo);
        }

        // -- create EncryptedContentInfo
        if (certificateCount > 0) {

            if (certProtectionAlgorithm == null) {
                certProtectionAlgorithm = defaultCertProtectionAlgorithm();
            }
            if (certPbeIterationCount < 0) {
                certPbeIterationCount = defaultCertPbeIterationCount();
            }

            if (debug != null) {
                debug.println("Storing " + certificateCount +
                    " certificate(s) in a PKCS#7 encryptedData");
            }

            byte[] encrData = createEncryptedData(password);
            if (!certProtectionAlgorithm.equalsIgnoreCase("NONE")) {
                ContentInfo encrContentInfo =
                        new ContentInfo(ContentInfo.ENCRYPTED_DATA_OID,
                                new DerValue(encrData));
                encrContentInfo.encode(authSafeContentInfo);
            } else {
                ContentInfo dataContentInfo = new ContentInfo(encrData);
                dataContentInfo.encode(authSafeContentInfo);
            }
        }

        // wrap as SequenceOf ContentInfos
        DerOutputStream cInfo = new DerOutputStream();
        cInfo.write(DerValue.tag_SequenceOf, authSafeContentInfo);
        byte[] authenticatedSafe = cInfo.toByteArray();

        // Create Encapsulated ContentInfo
        ContentInfo contentInfo = new ContentInfo(authenticatedSafe);
        contentInfo.encode(authSafe);
        byte[] authSafeData = authSafe.toByteArray();
        pfx.write(authSafeData);

        // -- MAC
        if (macAlgorithm == null) {
            macAlgorithm = defaultMacAlgorithm();
        }
        if (macIterationCount < 0) {
            macIterationCount = defaultMacIterationCount();
        }
        if (!macAlgorithm.equalsIgnoreCase("NONE")) {
            byte[] macData = calculateMac(password, authenticatedSafe);
            pfx.write(macData);
        }
        // write PFX to output stream
        DerOutputStream pfxout = new DerOutputStream();
        pfxout.write(DerValue.tag_Sequence, pfx);
        byte[] pfxData = pfxout.toByteArray();
        stream.write(pfxData);
        stream.flush();
    }

    /**
     * Gets a <code>KeyStore.Entry</code> for the specified alias
     * with the specified protection parameter.
     *
     * @param alias get the <code>KeyStore.Entry</code> for this alias
     * @param protParam the <code>ProtectionParameter</code>
     *          used to protect the <code>Entry</code>,
     *          which may be <code>null</code>
     *
     * @return the <code>KeyStore.Entry</code> for the specified alias,
     *          or <code>null</code> if there is no such entry
     *
     * @exception KeyStoreException if the operation failed
     * @exception NoSuchAlgorithmException if the algorithm for recovering the
     *          entry cannot be found
     * @exception UnrecoverableEntryException if the specified
     *          <code>protParam</code> were insufficient or invalid
     * @exception UnrecoverableKeyException if the entry is a
     *          <code>PrivateKeyEntry</code> or <code>SecretKeyEntry</code>
     *          and the specified <code>protParam</code> does not contain
     *          the information needed to recover the key (e.g. wrong password)
     *
     * @since 1.5
     */
    @Override
    public KeyStore.Entry engineGetEntry(String alias,
                        KeyStore.ProtectionParameter protParam)
                throws KeyStoreException, NoSuchAlgorithmException,
                UnrecoverableEntryException {

        if (!engineContainsAlias(alias)) {
            return null;
        }

        Entry entry = entries.get(alias.toLowerCase(Locale.ENGLISH));
        if (protParam == null) {
            if (engineIsCertificateEntry(alias)) {
                if (entry instanceof CertEntry &&
                    ((CertEntry) entry).trustedKeyUsage != null) {

                    if (debug != null) {
                        debug.println("Retrieved a trusted certificate at " +
                            "alias '" + alias + "'");
                    }

                    return new KeyStore.TrustedCertificateEntry(
                        ((CertEntry)entry).cert, getAttributes(entry));
                }
            } else {
                throw new UnrecoverableKeyException
                        ("requested entry requires a password");
            }
        }

        if (protParam instanceof KeyStore.PasswordProtection) {
            if (engineIsCertificateEntry(alias)) {
                throw new UnsupportedOperationException
                    ("trusted certificate entries are not password-protected");
            } else if (engineIsKeyEntry(alias)) {
                KeyStore.PasswordProtection pp =
                        (KeyStore.PasswordProtection)protParam;
                char[] password = pp.getPassword();

                Key key = engineGetKey(alias, password);
                if (key instanceof PrivateKey) {
                    Certificate[] chain = engineGetCertificateChain(alias);

                    return new KeyStore.PrivateKeyEntry((PrivateKey)key, chain,
                        getAttributes(entry));

                } else if (key instanceof SecretKey) {

                    return new KeyStore.SecretKeyEntry((SecretKey)key,
                        getAttributes(entry));
                }
            } else if (!engineIsKeyEntry(alias)) {
                throw new UnsupportedOperationException
                    ("untrusted certificate entries are not " +
                        "password-protected");
            }
        }

        throw new UnsupportedOperationException();
    }

    /**
     * Saves a <code>KeyStore.Entry</code> under the specified alias.
     * The specified protection parameter is used to protect the
     * <code>Entry</code>.
     *
     * <p> If an entry already exists for the specified alias,
     * it is overridden.
     *
     * @param alias save the <code>KeyStore.Entry</code> under this alias
     * @param entry the <code>Entry</code> to save
     * @param protParam the <code>ProtectionParameter</code>
     *          used to protect the <code>Entry</code>,
     *          which may be <code>null</code>
     *
     * @exception KeyStoreException if this operation fails
     *
     * @since 1.5
     */
    @Override
    public synchronized void engineSetEntry(String alias, KeyStore.Entry entry,
        KeyStore.ProtectionParameter protParam) throws KeyStoreException {

        // get password
        if (protParam != null &&
            !(protParam instanceof KeyStore.PasswordProtection)) {
            throw new KeyStoreException("unsupported protection parameter");
        }
        KeyStore.PasswordProtection pProtect = null;
        if (protParam != null) {
            pProtect = (KeyStore.PasswordProtection)protParam;
        }

        // set entry
        if (entry instanceof KeyStore.TrustedCertificateEntry) {
            if (protParam != null && pProtect.getPassword() != null) {
                // pre-1.5 style setCertificateEntry did not allow password
                throw new KeyStoreException
                    ("trusted certificate entries are not password-protected");
            } else {
                KeyStore.TrustedCertificateEntry tce =
                        (KeyStore.TrustedCertificateEntry)entry;
                setCertEntry(alias, tce.getTrustedCertificate(),
                    tce.getAttributes());

                return;
            }
        } else if (entry instanceof KeyStore.PrivateKeyEntry) {
            if (pProtect == null || pProtect.getPassword() == null) {
                // pre-1.5 style setKeyEntry required password
                throw new KeyStoreException
                    ("non-null password required to create PrivateKeyEntry");
            } else {
                KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry)entry;
                setKeyEntry(alias, pke.getPrivateKey(), pProtect,
                    pke.getCertificateChain(), pke.getAttributes());

                return;
            }
        } else if (entry instanceof KeyStore.SecretKeyEntry) {
            if (pProtect == null || pProtect.getPassword() == null) {
                // pre-1.5 style setKeyEntry required password
                throw new KeyStoreException
                    ("non-null password required to create SecretKeyEntry");
            } else {
                KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry)entry;
                setKeyEntry(alias, ske.getSecretKey(), pProtect,
                    (Certificate[])null, ske.getAttributes());

                return;
            }
        }

        throw new KeyStoreException
                ("unsupported entry type: " + entry.getClass().getName());
    }

    /*
     * Assemble the entry attributes
     */
    private Set<KeyStore.Entry.Attribute> getAttributes(Entry entry) {

        if (entry.attributes == null) {
            entry.attributes = new HashSet<>();
        }

        // friendlyName
        entry.attributes.add(new PKCS12Attribute(
            PKCS9FriendlyName_OID.toString(), entry.alias));

        // localKeyID
        byte[] keyIdValue = entry.keyId;
        if (keyIdValue != null) {
            entry.attributes.add(new PKCS12Attribute(
                PKCS9LocalKeyId_OID.toString(), Debug.toString(keyIdValue)));
        }

        // trustedKeyUsage
        if (entry instanceof CertEntry) {
            ObjectIdentifier[] trustedKeyUsageValue =
                ((CertEntry) entry).trustedKeyUsage;
            if (trustedKeyUsageValue != null) {
                if (trustedKeyUsageValue.length == 1) { // omit brackets
                    entry.attributes.add(new PKCS12Attribute(
                        TrustedKeyUsage_OID.toString(),
                        trustedKeyUsageValue[0].toString()));
                } else { // multi-valued
                    entry.attributes.add(new PKCS12Attribute(
                        TrustedKeyUsage_OID.toString(),
                        Arrays.toString(trustedKeyUsageValue)));
                }
            }
        }

        return entry.attributes;
    }

    /*
     * Calculate MAC using HMAC algorithm (required for password integrity)
     *
     * Hash-based MAC algorithm combines secret key with message digest to
     * create a message authentication code (MAC)
     */
    private byte[] calculateMac(char[] passwd, byte[] data)
        throws IOException
    {
        byte[] mData = null;
        String algName = macAlgorithm.substring(7);

        try {
            // Generate a random salt.
            byte[] salt = getSalt();

            // generate MAC (MAC key is generated within JCE)
            Mac m = Mac.getInstance(macAlgorithm);
            PBEParameterSpec params =
                        new PBEParameterSpec(salt, macIterationCount);
            SecretKey key = getPBEKey(passwd);
            try {
                m.init(key, params);
            } finally {
                destroyPBEKey(key);
            }
            m.update(data);
            byte[] macResult = m.doFinal();

            // encode as MacData
            MacData macData = new MacData(algName, macResult, salt,
                    macIterationCount);
            DerOutputStream bytes = new DerOutputStream();
            bytes.write(macData.getEncoded());
            mData = bytes.toByteArray();
        } catch (Exception e) {
            throw new IOException("calculateMac failed: " + e, e);
        }
        return mData;
    }


    /*
     * Validate Certificate Chain
     */
    private boolean validateChain(Certificate[] certChain)
    {
        for (int i = 0; i < certChain.length-1; i++) {
            X500Principal issuerDN =
                ((X509Certificate)certChain[i]).getIssuerX500Principal();
            X500Principal subjectDN =
                ((X509Certificate)certChain[i+1]).getSubjectX500Principal();
            if (!(issuerDN.equals(subjectDN)))
                return false;
        }

        // Check for loops in the chain. If there are repeated certs,
        // the Set of certs in the chain will contain fewer certs than
        // the chain
        Set<Certificate> set = new HashSet<>(Arrays.asList(certChain));
        return set.size() == certChain.length;
    }

    /*
     * Check that all the certificates are X.509 certificates
     */
    private static void checkX509Certs(Certificate[] certs)
            throws KeyStoreException {
        if (certs != null) {
            for (Certificate cert : certs) {
                if (!(cert instanceof X509Certificate)) {
                    throw new KeyStoreException(
                        "Only X.509 certificates are supported - " +
                        "rejecting class: " + cert.getClass().getName());
                }
            }
        }
    }

    /*
     * Create PKCS#12 Attributes, friendlyName, localKeyId and trustedKeyUsage.
     *
     * Although attributes are optional, they could be required.
     * For e.g. localKeyId attribute is required to match the
     * private key with the associated end-entity certificate.
     * The trustedKeyUsage attribute is used to denote a trusted certificate.
     *
     * PKCS8ShroudedKeyBags include unique localKeyID and friendlyName.
     * CertBags may or may not include attributes depending on the type
     * of Certificate. In end-entity certificates, localKeyID should be
     * unique, and the corresponding private key should have the same
     * localKeyID. For trusted CA certs in the cert-chain, localKeyID
     * attribute is not required, hence most vendors don't include it.
     * NSS/Netscape require it to be unique or null, where as IE/OpenSSL
     * ignore it.
     *
     * Here is a list of pkcs12 attribute values in CertBags.
     *
     * PKCS12 Attribute       NSS/Netscape    IE     OpenSSL    J2SE
     * --------------------------------------------------------------
     * LocalKeyId
     * (In EE cert only,
     *  NULL in CA certs)      true          true     true      true
     *
     * friendlyName            unique        same/    same/     unique
     *                                       unique   unique/
     *                                                null
     * trustedKeyUsage         -             -        -         true
     *
     * Note: OpenSSL adds friendlyName for end-entity cert only, and
     * removes the localKeyID and friendlyName for CA certs.
     * If the CertBag did not have a friendlyName, most vendors will
     * add it, and assign it to the DN of the cert.
     */
    private byte[] getBagAttributes(String alias, byte[] keyId,
        Set<KeyStore.Entry.Attribute> attributes) throws IOException {
        return getBagAttributes(alias, keyId, null, attributes);
    }

    private byte[] getBagAttributes(String alias, byte[] keyId,
        ObjectIdentifier[] trustedUsage,
        Set<KeyStore.Entry.Attribute> attributes) throws IOException {

        byte[] localKeyID = null;
        byte[] friendlyName = null;
        byte[] trustedKeyUsage = null;

        // return null if all three attributes are null
        if ((alias == null) && (keyId == null) && (trustedKeyUsage == null)) {
            return null;
        }

        // SafeBag Attributes
        DerOutputStream bagAttrs = new DerOutputStream();

        // Encode the friendlyname oid.
        if (alias != null) {
            DerOutputStream bagAttr1 = new DerOutputStream();
            bagAttr1.putOID(PKCS9FriendlyName_OID);
            DerOutputStream bagAttrContent1 = new DerOutputStream();
            DerOutputStream bagAttrValue1 = new DerOutputStream();
            bagAttrContent1.putBMPString(alias);
            bagAttr1.write(DerValue.tag_Set, bagAttrContent1);
            bagAttrValue1.write(DerValue.tag_Sequence, bagAttr1);
            friendlyName = bagAttrValue1.toByteArray();
        }

        // Encode the localkeyId oid.
        if (keyId != null) {
            DerOutputStream bagAttr2 = new DerOutputStream();
            bagAttr2.putOID(PKCS9LocalKeyId_OID);
            DerOutputStream bagAttrContent2 = new DerOutputStream();
            DerOutputStream bagAttrValue2 = new DerOutputStream();
            bagAttrContent2.putOctetString(keyId);
            bagAttr2.write(DerValue.tag_Set, bagAttrContent2);
            bagAttrValue2.write(DerValue.tag_Sequence, bagAttr2);
            localKeyID = bagAttrValue2.toByteArray();
        }

        // Encode the trustedKeyUsage oid.
        if (trustedUsage != null) {
            DerOutputStream bagAttr3 = new DerOutputStream();
            bagAttr3.putOID(TrustedKeyUsage_OID);
            DerOutputStream bagAttrContent3 = new DerOutputStream();
            DerOutputStream bagAttrValue3 = new DerOutputStream();
            for (ObjectIdentifier usage : trustedUsage) {
                bagAttrContent3.putOID(usage);
            }
            bagAttr3.write(DerValue.tag_Set, bagAttrContent3);
            bagAttrValue3.write(DerValue.tag_Sequence, bagAttr3);
            trustedKeyUsage = bagAttrValue3.toByteArray();
        }

        DerOutputStream attrs = new DerOutputStream();
        if (friendlyName != null) {
            attrs.write(friendlyName);
        }
        if (localKeyID != null) {
            attrs.write(localKeyID);
        }
        if (trustedKeyUsage != null) {
            attrs.write(trustedKeyUsage);
        }

        if (attributes != null) {
            for (KeyStore.Entry.Attribute attribute : attributes) {
                String attributeName = attribute.getName();
                // skip friendlyName, localKeyId and trustedKeyUsage
                if (CORE_ATTRIBUTES[0].value().equals(attributeName) ||
                    CORE_ATTRIBUTES[1].value().equals(attributeName) ||
                    CORE_ATTRIBUTES[2].value().equals(attributeName)) {
                    continue;
                }
                attrs.write(((PKCS12Attribute) attribute).getEncoded());
            }
        }

        bagAttrs.write(DerValue.tag_Set, attrs);
        return bagAttrs.toByteArray();
    }

    /*
     * Create EncryptedData content type, that contains EncryptedContentInfo.
     * Includes certificates in individual SafeBags of type CertBag.
     * Each CertBag may include pkcs12 attributes
     * (see comments in getBagAttributes)
     */
    private byte[] createEncryptedData(char[] password)
        throws CertificateException, IOException
    {
        DerOutputStream out = new DerOutputStream();
        for (Enumeration<String> e = engineAliases(); e.hasMoreElements(); ) {

            String alias = e.nextElement();
            Entry entry = entries.get(alias);

            // certificate chain
            Certificate[] certs;

            if (entry instanceof PrivateKeyEntry) {
                PrivateKeyEntry keyEntry = (PrivateKeyEntry) entry;
                if (keyEntry.chain != null) {
                    certs = keyEntry.chain;
                } else {
                    certs = new Certificate[0];
                }
            } else if (entry instanceof CertEntry) {
                certs = new Certificate[]{((CertEntry) entry).cert};
            } else {
                certs = new Certificate[0];
            }

            for (int i = 0; i < certs.length; i++) {
                // create SafeBag of Type CertBag
                DerOutputStream safeBag = new DerOutputStream();
                safeBag.putOID(CertBag_OID);

                // create a CertBag
                DerOutputStream certBag = new DerOutputStream();
                certBag.putOID(PKCS9CertType_OID);

                // write encoded certs in a context-specific tag
                DerOutputStream certValue = new DerOutputStream();
                X509Certificate cert = (X509Certificate) certs[i];
                certValue.putOctetString(cert.getEncoded());
                certBag.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                        true, (byte) 0), certValue);

                // wrap CertBag in a Sequence
                DerOutputStream certout = new DerOutputStream();
                certout.write(DerValue.tag_Sequence, certBag);
                byte[] certBagValue = certout.toByteArray();

                // Wrap the CertBag encoding in a context-specific tag.
                DerOutputStream bagValue = new DerOutputStream();
                bagValue.write(certBagValue);
                // write SafeBag Value
                safeBag.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                true, (byte) 0), bagValue);

                // write SafeBag Attributes
                // All Certs should have a unique friendlyName.
                // This change is made to meet NSS requirements.
                byte[] bagAttrs = null;
                if (i == 0) {
                    // Only End-Entity Cert should have a localKeyId.
                    if (entry instanceof KeyEntry) {
                        KeyEntry keyEntry = (KeyEntry) entry;
                        bagAttrs =
                            getBagAttributes(keyEntry.alias, keyEntry.keyId,
                                keyEntry.attributes);
                    } else {
                        CertEntry certEntry = (CertEntry) entry;
                        bagAttrs =
                            getBagAttributes(certEntry.alias, certEntry.keyId,
                                certEntry.trustedKeyUsage,
                                certEntry.attributes);
                    }
                } else {
                    // Trusted root CA certs and Intermediate CA certs do not
                    // need to have a localKeyId, and hence localKeyId is null
                    // This change is made to meet NSS/Netscape requirements.
                    // NSS pkcs12 library requires trusted CA certs in the
                    // certificate chain to have unique or null localKeyID.
                    // However, IE/OpenSSL do not impose this restriction.
                    bagAttrs = getBagAttributes(
                            cert.getSubjectX500Principal().getName(), null,
                            entry.attributes);
                }
                if (bagAttrs != null) {
                    safeBag.write(bagAttrs);
                }

                // wrap as Sequence
                out.write(DerValue.tag_Sequence, safeBag);
            } // for cert-chain
        }

        // wrap as SequenceOf SafeBag
        DerOutputStream safeBagValue = new DerOutputStream();
        safeBagValue.write(DerValue.tag_SequenceOf, out);
        byte[] safeBagData = safeBagValue.toByteArray();

        // encrypt the content (EncryptedContentInfo)
        if (!certProtectionAlgorithm.equalsIgnoreCase("NONE")) {
            byte[] encrContentInfo = encryptContent(safeBagData, password);

            // -- SEQUENCE of EncryptedData
            DerOutputStream encrData = new DerOutputStream();
            DerOutputStream encrDataContent = new DerOutputStream();
            encrData.putInteger(0);
            encrData.write(encrContentInfo);
            encrDataContent.write(DerValue.tag_Sequence, encrData);
            return encrDataContent.toByteArray();
        } else {
            return safeBagData;
        }
    }

    /*
     * Create SafeContent Data content type.
     * Includes encrypted secret key in a SafeBag of type SecretBag.
     * Includes encrypted private key in a SafeBag of type PKCS8ShroudedKeyBag.
     * Each PKCS8ShroudedKeyBag includes pkcs12 attributes
     * (see comments in getBagAttributes)
     */
    private byte[] createSafeContent()
        throws CertificateException, IOException {

        DerOutputStream out = new DerOutputStream();
        for (Enumeration<String> e = engineAliases(); e.hasMoreElements(); ) {

            String alias = e.nextElement();
            Entry entry = entries.get(alias);
            if (entry == null || (!(entry instanceof KeyEntry))) {
                continue;
            }
            DerOutputStream safeBag = new DerOutputStream();
            KeyEntry keyEntry = (KeyEntry) entry;

            // DER encode the private key
            if (keyEntry instanceof PrivateKeyEntry) {
                // Create SafeBag of type pkcs8ShroudedKeyBag
                safeBag.putOID(PKCS8ShroudedKeyBag_OID);

                // get the encrypted private key
                byte[] encrBytes = ((PrivateKeyEntry)keyEntry).protectedPrivKey;
                EncryptedPrivateKeyInfo encrInfo = null;
                try {
                    encrInfo = new EncryptedPrivateKeyInfo(encrBytes);

                } catch (IOException ioe) {
                    throw new IOException("Private key not stored as "
                            + "PKCS#8 EncryptedPrivateKeyInfo"
                            + ioe.getMessage());
                }

                // Wrap the EncryptedPrivateKeyInfo in a context-specific tag.
                DerOutputStream bagValue = new DerOutputStream();
                bagValue.write(encrInfo.getEncoded());
                safeBag.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                true, (byte) 0), bagValue);

            // DER encode the secret key
            } else if (keyEntry instanceof SecretKeyEntry) {
                // Create SafeBag of type SecretBag
                safeBag.putOID(SecretBag_OID);

                // Create a SecretBag
                DerOutputStream secretBag = new DerOutputStream();
                secretBag.putOID(PKCS8ShroudedKeyBag_OID);

                // Write secret key in a context-specific tag
                DerOutputStream secretKeyValue = new DerOutputStream();
                secretKeyValue.putOctetString(
                    ((SecretKeyEntry) keyEntry).protectedSecretKey);
                secretBag.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                        true, (byte) 0), secretKeyValue);

                // Wrap SecretBag in a Sequence
                DerOutputStream secretBagSeq = new DerOutputStream();
                secretBagSeq.write(DerValue.tag_Sequence, secretBag);
                byte[] secretBagValue = secretBagSeq.toByteArray();

                // Wrap the secret bag in a context-specific tag.
                DerOutputStream bagValue = new DerOutputStream();
                bagValue.write(secretBagValue);

                // Write SafeBag value
                safeBag.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                    true, (byte) 0), bagValue);
            } else {
                continue; // skip this entry
            }

            // write SafeBag Attributes
            byte[] bagAttrs =
                getBagAttributes(alias, entry.keyId, entry.attributes);
            safeBag.write(bagAttrs);

            // wrap as Sequence
            out.write(DerValue.tag_Sequence, safeBag);
        }

        // wrap as Sequence
        DerOutputStream safeBagValue = new DerOutputStream();
        safeBagValue.write(DerValue.tag_Sequence, out);
        return safeBagValue.toByteArray();
    }


    /*
     * Encrypt the contents using Password-based (PBE) encryption
     * as defined in PKCS #5.
     *
     * @return encrypted contents encoded as EncryptedContentInfo
     */
    private byte[] encryptContent(byte[] data, char[] password)
        throws IOException {

        byte[] encryptedData = null;


        try {
            // create AlgorithmParameters
            AlgorithmParameters algParams = getPBEAlgorithmParameters(
                    certProtectionAlgorithm, certPbeIterationCount);
            DerOutputStream bytes = new DerOutputStream();

            // Use JCE
            Cipher cipher = Cipher.getInstance(certProtectionAlgorithm);
            SecretKey skey = getPBEKey(password);
            try {
                cipher.init(Cipher.ENCRYPT_MODE, skey, algParams);
            } finally {
                destroyPBEKey(skey);
            }
            encryptedData = cipher.doFinal(data);

            AlgorithmId algId = new AlgorithmId(
                    mapPBEAlgorithmToOID(certProtectionAlgorithm),
                    cipher.getParameters());
                    // cipher.getParameters() now has IV
            algId.encode(bytes);
            byte[] encodedAlgId = bytes.toByteArray();

            if (debug != null) {
                debug.println("  (Cipher algorithm: " + cipher.getAlgorithm() +
                        ")");
            }

            // create EncryptedContentInfo
            DerOutputStream bytes2 = new DerOutputStream();
            bytes2.putOID(ContentInfo.DATA_OID);
            bytes2.write(encodedAlgId);

            // Wrap encrypted data in a context-specific tag.
            DerOutputStream tmpout2 = new DerOutputStream();
            tmpout2.putOctetString(encryptedData);
            bytes2.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT,
                    false, (byte) 0), tmpout2);

            // wrap EncryptedContentInfo in a Sequence
            DerOutputStream out = new DerOutputStream();
            out.write(DerValue.tag_Sequence, bytes2);
            return out.toByteArray();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new IOException("Failed to encrypt" +
                    " safe contents entry: " + e, e);
        }
    }

    /**
     * Loads the keystore from the given input stream.
     *
     * <p>If a password is given, it is used to check the integrity of the
     * keystore data. Otherwise, the integrity of the keystore is not checked.
     *
     * @param stream the input stream from which the keystore is loaded
     * @param password the (optional) password used to check the integrity of
     * the keystore.
     *
     * @exception IOException if there is an I/O or format problem with the
     * keystore data
     * @exception NoSuchAlgorithmException if the algorithm used to check
     * the integrity of the keystore cannot be found
     * @exception CertificateException if any of the certificates in the
     * keystore could not be loaded
     */
    public synchronized void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {

        // Reset config when loading a different keystore.
        certProtectionAlgorithm = null;
        certPbeIterationCount = -1;
        macAlgorithm = null;
        macIterationCount = -1;

        if (stream == null)
           return;

        // reset the counter
        counter = 0;

        DerValue val = new DerValue(stream);
        DerInputStream s = val.toDerInputStream();
        int version = s.getInteger();

        if (version != VERSION_3) {
           throw new IOException("PKCS12 keystore not in version 3 format");
        }

        entries.clear();

        /*
         * Read the authSafe.
         */
        byte[] authSafeData;
        ContentInfo authSafe = new ContentInfo(s);
        ObjectIdentifier contentType = authSafe.getContentType();

        if (contentType.equals(ContentInfo.DATA_OID)) {
           authSafeData = authSafe.getData();
        } else /* signed data */ {
           throw new IOException("public key protected PKCS12 not supported");
        }

        DerInputStream as = new DerInputStream(authSafeData);
        DerValue[] safeContentsArray = as.getSequence(2);
        int count = safeContentsArray.length;

        // reset the counters at the start
        privateKeyCount = 0;
        secretKeyCount = 0;
        certificateCount = 0;

        boolean seeEncBag = false;

        /*
         * Spin over the ContentInfos.
         */
        for (int i = 0; i < count; i++) {
            ContentInfo safeContents;
            DerInputStream sci;
            byte[] eAlgId = null;

            sci = new DerInputStream(safeContentsArray[i].toByteArray());
            safeContents = new ContentInfo(sci);
            contentType = safeContents.getContentType();
            if (contentType.equals(ContentInfo.DATA_OID)) {

                if (debug != null) {
                    debug.println("Loading PKCS#7 data");
                }

                loadSafeContents(new DerInputStream(safeContents.getData()));
            } else if (contentType.equals(ContentInfo.ENCRYPTED_DATA_OID)) {
                if (password == null) {

                    if (debug != null) {
                        debug.println("Warning: skipping PKCS#7 encryptedData" +
                            " - no password was supplied");
                    }
                    // No password to decrypt ENCRYPTED_DATA_OID. *Skip it*.
                    // This means user will see a PrivateKeyEntry without
                    // certificates and a whole TrustedCertificateEntry will
                    // be lost. This is not a perfect solution but alternative
                    // solutions are more disruptive:
                    //
                    // We cannot just fail, since KeyStore.load(is, null)
                    // has been known to never fail because of a null password.
                    //
                    // We cannot just throw away the whole PrivateKeyEntry,
                    // this is too silent and no one will notice anything.
                    //
                    // We also cannot fail when getCertificate() on such a
                    // PrivateKeyEntry is called, since the method has not
                    // specified this behavior.
                    continue;
                }

                DerInputStream edi =
                                safeContents.getContent().toDerInputStream();
                int edVersion = edi.getInteger();
                DerValue[] seq = edi.getSequence(3);
                if (seq.length != 3) {
                    // We require the encryptedContent field, even though
                    // it is optional
                    throw new IOException("Invalid length for EncryptedContentInfo");
                }
                ObjectIdentifier edContentType = seq[0].getOID();
                eAlgId = seq[1].toByteArray();
                if (!seq[2].isContextSpecific((byte)0)) {
                    throw new IOException("unsupported encrypted content type "
                                          + seq[2].tag);
                }
                byte newTag = DerValue.tag_OctetString;
                if (seq[2].isConstructed())
                   newTag |= 0x20;
                seq[2].resetTag(newTag);
                byte[] rawData = seq[2].getOctetString();

                // parse Algorithm parameters
                DerInputStream in = seq[1].toDerInputStream();
                ObjectIdentifier algOid = in.getOID();
                AlgorithmParameters algParams = parseAlgParameters(algOid, in);

                PBEParameterSpec pbeSpec;
                int ic = 0;

                if (algParams != null) {
                    try {
                        pbeSpec =
                            algParams.getParameterSpec(PBEParameterSpec.class);
                    } catch (InvalidParameterSpecException ipse) {
                        throw new IOException(
                            "Invalid PBE algorithm parameters");
                    }
                    ic = pbeSpec.getIterationCount();

                    if (ic > MAX_ITERATION_COUNT) {
                        throw new IOException("cert PBE iteration count too large");
                    }

                    certProtectionAlgorithm
                            = mapPBEParamsToAlgorithm(algOid, algParams);
                    certPbeIterationCount = ic;
                    seeEncBag = true;
                }

                if (debug != null) {
                    debug.println("Loading PKCS#7 encryptedData " +
                        "(" + mapPBEParamsToAlgorithm(algOid, algParams) +
                        " iterations: " + ic + ")");
                }

                try {
                    RetryWithZero.run(pass -> {
                        // Use JCE
                        Cipher cipher = Cipher.getInstance(
                                mapPBEParamsToAlgorithm(algOid, algParams));
                        SecretKey skey = getPBEKey(pass);
                        try {
                            cipher.init(Cipher.DECRYPT_MODE, skey, algParams);
                        } finally {
                            destroyPBEKey(skey);
                        }
                        loadSafeContents(new DerInputStream(cipher.doFinal(rawData)));
                        return null;
                    }, password);
                } catch (Exception e) {
                    throw new IOException("keystore password was incorrect",
                            new UnrecoverableKeyException(
                                    "failed to decrypt safe contents entry: " + e));
                }
            } else {
                throw new IOException("public key protected PKCS12" +
                                        " not supported");
            }
        }

        // No ENCRYPTED_DATA_OID but see certificate. Must be passwordless.
        if (!seeEncBag && certificateCount > 0) {
            certProtectionAlgorithm = "NONE";
        }

        // The MacData is optional.
        if (s.available() > 0) {
            // If there is no password, we cannot fail. KeyStore.load(is, null)
            // has been known to never fail because of a null password.
            if (password != null) {
                MacData macData = new MacData(s);
                int ic = macData.getIterations();

                try {
                    if (ic > MAX_ITERATION_COUNT) {
                        throw new InvalidAlgorithmParameterException(
                                "MAC iteration count too large: " + ic);
                    }

                    String algName =
                            macData.getDigestAlgName().toUpperCase(Locale.ENGLISH);

                    // Change SHA-1 to SHA1
                    algName = algName.replace("-", "");

                    macAlgorithm = "HmacPBE" + algName;
                    macIterationCount = ic;

                    // generate MAC (MAC key is created within JCE)
                    Mac m = Mac.getInstance(macAlgorithm);
                    PBEParameterSpec params =
                            new PBEParameterSpec(macData.getSalt(), ic);

                    RetryWithZero.run(pass -> {
                        SecretKey key = getPBEKey(pass);
                        try {
                            m.init(key, params);
                        } finally {
                            destroyPBEKey(key);
                        }
                        m.update(authSafeData);
                        byte[] macResult = m.doFinal();

                        if (debug != null) {
                            debug.println("Checking keystore integrity " +
                                    "(" + m.getAlgorithm() + " iterations: " + ic + ")");
                        }

                        if (!MessageDigest.isEqual(macData.getDigest(), macResult)) {
                            throw new UnrecoverableKeyException("Failed PKCS12" +
                                    " integrity checking");
                        }
                        return (Void) null;
                    }, password);
                } catch (Exception e) {
                    throw new IOException("Integrity check failed: " + e, e);
                }
            }
        } else {
            macAlgorithm = "NONE";
        }

        /*
         * Match up private keys with certificate chains.
         */
        PrivateKeyEntry[] list =
            keyList.toArray(new PrivateKeyEntry[keyList.size()]);
        for (int m = 0; m < list.length; m++) {
            PrivateKeyEntry entry = list[m];
            if (entry.keyId != null) {
                ArrayList<X509Certificate> chain =
                                new ArrayList<X509Certificate>();
                X509Certificate cert = findMatchedCertificate(entry);

                mainloop:
                while (cert != null) {
                    // Check for loops in the certificate chain
                    if (!chain.isEmpty()) {
                        for (X509Certificate chainCert : chain) {
                            if (cert.equals(chainCert)) {
                                if (debug != null) {
                                    debug.println("Loop detected in " +
                                        "certificate chain. Skip adding " +
                                        "repeated cert to chain. Subject: " +
                                        cert.getSubjectX500Principal()
                                            .toString());
                                }
                                break mainloop;
                            }
                        }
                    }
                    chain.add(cert);
                    if (KeyStoreUtil.isSelfSigned(cert)) {
                        break;
                    }
                    cert = findIssuer(cert);
                }
                /* Update existing KeyEntry in entries table */
                if (chain.size() > 0) {
                    entry.chain = chain.toArray(new Certificate[chain.size()]);
                } else {
                    // Remove private key entries where there is no associated
                    // certs. Most likely the keystore is loaded with a null
                    // password.
                    entries.remove(entry);
                }
            }
        }

        if (debug != null) {
            debug.println("PKCS12KeyStore load: private key count: " +
                    privateKeyCount + ". secret key count: " + secretKeyCount +
                    ". certificate count: " + certificateCount);
        }

        certEntries.clear();
        allCerts.clear();
        keyList.clear();
    }

    /**
     * Find the issuer of input in allCerts. If the input has an
     * AuthorityKeyIdentifier extension and the keyId inside matches
     * the keyId of the SubjectKeyIdentifier of a cert. This cert is
     * returned. Otherwise, a cert whose subjectDN is the same as the
     * input's issuerDN is returned.
     *
     * @param input the input certificate
     * @return the isssuer, or null if none matches
     */
    private X509Certificate findIssuer(X509Certificate input) {

        X509Certificate fallback = null; // the DN match
        X500Principal issuerPrinc = input.getIssuerX500Principal();

        // AuthorityKeyIdentifier value encoded as an OCTET STRING
        byte[] issuerIdExtension = input.getExtensionValue(
                KnownOIDs.AuthorityKeyID.value());
        byte[] issuerId = null;

        if (issuerIdExtension != null) {
            try {
                issuerId = new AuthorityKeyIdentifierExtension(
                            false,
                            new DerValue(issuerIdExtension).getOctetString())
                        .getEncodedKeyIdentifier();
            } catch (IOException e) {
                // ignored. issuerId is still null
            }
        }

        for (X509Certificate cert : allCerts) {
            if (cert.getSubjectX500Principal().equals(issuerPrinc)) {
                if (issuerId != null) {
                    // SubjectKeyIdentifier value encoded as an OCTET STRING
                    byte[] subjectIdExtension = cert.getExtensionValue(
                            KnownOIDs.SubjectKeyID.value());
                    byte[] subjectId = null;
                    if (subjectIdExtension != null) {
                        try {
                            subjectId = new DerValue(subjectIdExtension)
                                    .getOctetString();
                        } catch (IOException e) {
                            // ignored. issuerId is still null
                        }
                    }
                    if (subjectId != null) {
                        if (Arrays.equals(issuerId, subjectId)) {
                            // keyId exact match!
                            return cert;
                        } else {
                            // Different keyId. Not a fallback.
                            continue;
                        }
                    } else {
                        // A DN match with no subjectId
                        fallback = cert;
                    }
                } else { // if there is no issuerId, return the 1st DN match
                    return cert;
                }
            }
        }
        return fallback;
    }

    /**
     * Returns if a pkcs12 file is password-less. This means no cert is
     * encrypted and there is no Mac. Please note that the private key
     * can be encrypted.
     *
     * This is a simplified version of {@link #engineLoad} that only looks
     * at the ContentInfo types.
     *
     * @param f the pkcs12 file
     * @return if it's password-less
     * @throws IOException
     */
    public static boolean isPasswordless(File f) throws IOException {

        try (FileInputStream stream = new FileInputStream(f)) {
            DerValue val = new DerValue(stream);
            DerInputStream s = val.toDerInputStream();

            s.getInteger(); // skip version

            ContentInfo authSafe = new ContentInfo(s);
            DerInputStream as = new DerInputStream(authSafe.getData());
            for (DerValue seq : as.getSequence(2)) {
                DerInputStream sci = new DerInputStream(seq.toByteArray());
                ContentInfo safeContents = new ContentInfo(sci);
                if (safeContents.getContentType()
                        .equals(ContentInfo.ENCRYPTED_DATA_OID)) {
                    // Certificate encrypted
                    return false;
                }
            }

            if (s.available() > 0) {
                // The MacData exists.
                return false;
            }
        }
        return true;
    }

    /**
     * Locates a matched CertEntry from certEntries, and returns its cert.
     * @param entry the KeyEntry to match
     * @return a certificate, null if not found
     */
    private X509Certificate findMatchedCertificate(PrivateKeyEntry entry) {
        CertEntry keyIdMatch = null;
        CertEntry aliasMatch = null;
        for (CertEntry ce: certEntries) {
            if (Arrays.equals(entry.keyId, ce.keyId)) {
                keyIdMatch = ce;
                if (entry.alias.equalsIgnoreCase(ce.alias)) {
                    // Full match!
                    return ce.cert;
                }
            } else if (entry.alias.equalsIgnoreCase(ce.alias)) {
                aliasMatch = ce;
            }
        }
        // keyId match first, for compatibility
        if (keyIdMatch != null) return keyIdMatch.cert;
        else if (aliasMatch != null) return aliasMatch.cert;
        else return null;
    }

    private void loadSafeContents(DerInputStream stream)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        DerValue[] safeBags = stream.getSequence(2);
        int count = safeBags.length;

        /*
         * Spin over the SafeBags.
         */
        for (int i = 0; i < count; i++) {
            ObjectIdentifier bagId;
            DerInputStream sbi;
            DerValue bagValue;
            Object bagItem = null;

            sbi = safeBags[i].toDerInputStream();
            bagId = sbi.getOID();
            bagValue = sbi.getDerValue();
            if (!bagValue.isContextSpecific((byte)0)) {
                throw new IOException("unsupported PKCS12 bag value type "
                                        + bagValue.tag);
            }
            bagValue = bagValue.data.getDerValue();
            if (bagId.equals(PKCS8ShroudedKeyBag_OID)) {
                PrivateKeyEntry kEntry = new PrivateKeyEntry();
                kEntry.protectedPrivKey = bagValue.toByteArray();
                bagItem = kEntry;
                privateKeyCount++;
            } else if (bagId.equals(CertBag_OID)) {
                DerInputStream cs = new DerInputStream(bagValue.toByteArray());
                DerValue[] certValues = cs.getSequence(2);
                if (certValues.length != 2) {
                    throw new IOException("Invalid length for CertBag");
                }
                ObjectIdentifier certId = certValues[0].getOID();
                if (!certValues[1].isContextSpecific((byte)0)) {
                    throw new IOException("unsupported PKCS12 cert value type "
                                        + certValues[1].tag);
                }
                DerValue certValue = certValues[1].data.getDerValue();
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                X509Certificate cert;
                cert = (X509Certificate)cf.generateCertificate
                        (new ByteArrayInputStream(certValue.getOctetString()));
                bagItem = cert;
                certificateCount++;
            } else if (bagId.equals(SecretBag_OID)) {
                DerInputStream ss = new DerInputStream(bagValue.toByteArray());
                DerValue[] secretValues = ss.getSequence(2);
                if (secretValues.length != 2) {
                    throw new IOException("Invalid length for SecretBag");
                }
                ObjectIdentifier secretId = secretValues[0].getOID();
                if (!secretValues[1].isContextSpecific((byte)0)) {
                    throw new IOException(
                        "unsupported PKCS12 secret value type "
                                        + secretValues[1].tag);
                }
                DerValue secretValue = secretValues[1].data.getDerValue();
                SecretKeyEntry kEntry = new SecretKeyEntry();
                kEntry.protectedSecretKey = secretValue.getOctetString();
                bagItem = kEntry;
                secretKeyCount++;
            } else {

                if (debug != null) {
                    debug.println("Unsupported PKCS12 bag type: " + bagId);
                }
            }

            DerValue[] attrSet;
            try {
                attrSet = sbi.getSet(3);
            } catch (IOException e) {
                // entry does not have attributes
                // Note: CA certs can have no attributes
                // OpenSSL generates pkcs12 with no attr for CA certs.
                attrSet = null;
            }

            String alias = null;
            byte[] keyId = null;
            ObjectIdentifier[] trustedKeyUsage = null;
            Set<PKCS12Attribute> attributes = new HashSet<>();

            if (attrSet != null) {
                for (int j = 0; j < attrSet.length; j++) {
                    byte[] encoded = attrSet[j].toByteArray();
                    DerInputStream as = new DerInputStream(encoded);
                    DerValue[] attrSeq = as.getSequence(2);
                    if (attrSeq.length != 2) {
                        throw new IOException("Invalid length for Attribute");
                    }
                    ObjectIdentifier attrId = attrSeq[0].getOID();
                    DerInputStream vs =
                        new DerInputStream(attrSeq[1].toByteArray());
                    DerValue[] valSet;
                    try {
                        valSet = vs.getSet(1);
                    } catch (IOException e) {
                        throw new IOException("Attribute " + attrId +
                                " should have a value " + e.getMessage());
                    }
                    if (attrId.equals(PKCS9FriendlyName_OID)) {
                        alias = valSet[0].getBMPString();
                    } else if (attrId.equals(PKCS9LocalKeyId_OID)) {
                        keyId = valSet[0].getOctetString();
                    } else if
                        (attrId.equals(TrustedKeyUsage_OID)) {
                        trustedKeyUsage = new ObjectIdentifier[valSet.length];
                        for (int k = 0; k < valSet.length; k++) {
                            trustedKeyUsage[k] = valSet[k].getOID();
                        }
                    } else {
                        attributes.add(new PKCS12Attribute(encoded));
                    }
                }
            }

            /*
             * As per PKCS12 v1.0 friendlyname (alias) and localKeyId (keyId)
             * are optional PKCS12 bagAttributes. But entries in the keyStore
             * are identified by their alias. Hence we need to have an
             * Unfriendlyname in the alias, if alias is null. The keyId
             * attribute is required to match the private key with the
             * certificate. If we get a bagItem of type KeyEntry with a
             * null keyId, we should skip it entirely.
             */
            if (bagItem instanceof KeyEntry) {
                KeyEntry entry = (KeyEntry)bagItem;

                if (keyId == null) {
                    if (bagItem instanceof PrivateKeyEntry) {
                       // Insert a localKeyID for the privateKey
                       // Note: This is a workaround to allow null localKeyID
                       // attribute in pkcs12 with one private key entry and
                       // associated cert-chain
                       if (privateKeyCount == 1) {
                            keyId = "01".getBytes(UTF_8);
                       } else {
                            continue;
                       }
                    } else {
                        // keyId in a SecretKeyEntry is not significant
                        keyId = "00".getBytes(UTF_8);
                    }
                }
                entry.keyId = keyId;
                // restore date if it exists
                String keyIdStr = new String(keyId, UTF_8);
                Date date = null;
                if (keyIdStr.startsWith("Time ")) {
                    try {
                        date = new Date(
                                Long.parseLong(keyIdStr.substring(5)));
                    } catch (Exception e) {
                        date = null;
                    }
                }
                if (date == null) {
                    date = new Date();
                }
                entry.date = date;

                if (bagItem instanceof PrivateKeyEntry) {
                    keyList.add((PrivateKeyEntry) entry);
                }
                if (entry.attributes == null) {
                    entry.attributes = new HashSet<>();
                }
                entry.attributes.addAll(attributes);
                if (alias == null) {
                   alias = getUnfriendlyName();
                }
                entry.alias = alias;
                entries.put(alias.toLowerCase(Locale.ENGLISH), entry);

            } else if (bagItem instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate)bagItem;
                // Insert a localKeyID for the corresponding cert
                // Note: This is a workaround to allow null localKeyID
                // attribute in pkcs12 with one private key entry and
                // associated cert-chain
                if ((keyId == null) && (privateKeyCount == 1)) {
                    // insert localKeyID only for EE cert or self-signed cert
                    if (i == 0) {
                        keyId = "01".getBytes(UTF_8);
                    }
                }
                // Trusted certificate
                if (trustedKeyUsage != null) {
                    if (alias == null) {
                        alias = getUnfriendlyName();
                    }
                    CertEntry certEntry =
                        new CertEntry(cert, keyId, alias, trustedKeyUsage,
                            attributes);
                    entries.put(alias.toLowerCase(Locale.ENGLISH), certEntry);
                } else {
                    certEntries.add(new CertEntry(cert, keyId, alias));
                }
                allCerts.add(cert);
            }
        }
    }

    private String getUnfriendlyName() {
        counter++;
        return (String.valueOf(counter));
    }

    /*
     * PKCS12 permitted first 24 bytes:
     *
     * 30 80 02 01 03 30 80 06 09 2A 86 48 86 F7 0D 01 07 01 A0 80 24 80 04 --
     * 30 82 -- -- 02 01 03 30 82 -- -- 06 09 2A 86 48 86 F7 0D 01 07 01 A0 8-
     * 30 -- 02 01 03 30 -- 06 09 2A 86 48 86 F7 0D 01 07 01 A0 -- 04 -- -- --
     * 30 81 -- 02 01 03 30 81 -- 06 09 2A 86 48 86 F7 0D 01 07 01 A0 81 -- 04
     * 30 82 -- -- 02 01 03 30 81 -- 06 09 2A 86 48 86 F7 0D 01 07 01 A0 81 --
     * 30 83 -- -- -- 02 01 03 30 82 -- -- 06 09 2A 86 48 86 F7 0D 01 07 01 A0
     * 30 83 -- -- -- 02 01 03 30 83 -- -- -- 06 09 2A 86 48 86 F7 0D 01 07 01
     * 30 84 -- -- -- -- 02 01 03 30 83 -- -- -- 06 09 2A 86 48 86 F7 0D 01 07
     * 30 84 -- -- -- -- 02 01 03 30 84 -- -- -- -- 06 09 2A 86 48 86 F7 0D 01
     */

    private static final long[][] PKCS12_HEADER_PATTERNS = {
        { 0x3080020103308006L, 0x092A864886F70D01L, 0x0701A08024800400L },
        { 0x3082000002010330L, 0x82000006092A8648L, 0x86F70D010701A080L },
        { 0x3000020103300006L, 0x092A864886F70D01L, 0x0701A00004000000L },
        { 0x3081000201033081L, 0x0006092A864886F7L, 0x0D010701A0810004L },
        { 0x3082000002010330L, 0x810006092A864886L, 0xF70D010701A08100L },
        { 0x3083000000020103L, 0x3082000006092A86L, 0x4886F70D010701A0L },
        { 0x3083000000020103L, 0x308300000006092AL, 0x864886F70D010701L },
        { 0x3084000000000201L, 0x0330830000000609L, 0x2A864886F70D0107L },
        { 0x3084000000000201L, 0x0330840000000006L, 0x092A864886F70D01L }
    };

    private static final long[][] PKCS12_HEADER_MASKS = {
        { 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFF00L },
        { 0xFFFF0000FFFFFFFFL, 0xFF0000FFFFFFFFFFL, 0xFFFFFFFFFFFFFFF0L },
        { 0xFF00FFFFFFFF00FFL, 0xFFFFFFFFFFFFFFFFL, 0xFFFFFF00FF000000L },
        { 0xFFFF00FFFFFFFFFFL, 0x00FFFFFFFFFFFFFFL, 0xFFFFFFFFFFFF00FFL },
        { 0xFFFF0000FFFFFFFFL, 0xFF00FFFFFFFFFFFFL, 0xFFFFFFFFFFFFFF00L },
        { 0xFFFF000000FFFFFFL, 0xFFFF0000FFFFFFFFL, 0xFFFFFFFFFFFFFFFFL },
        { 0xFFFF000000FFFFFFL, 0xFFFF000000FFFFFFL, 0xFFFFFFFFFFFFFFFFL },
        { 0xFFFF00000000FFFFL, 0xFFFFFF000000FFFFL, 0xFFFFFFFFFFFFFFFFL },
        { 0xFFFF00000000FFFFL, 0xFFFFFF00000000FFL, 0xFFFFFFFFFFFFFFFFL }
    };

    /**
     * Probe the first few bytes of the keystore data stream for a valid
     * PKCS12 keystore encoding.
     */
    @Override
    public boolean engineProbe(InputStream stream) throws IOException {

        DataInputStream dataStream;
        if (stream instanceof DataInputStream) {
            dataStream = (DataInputStream)stream;
        } else {
            dataStream = new DataInputStream(stream);
        }

        long firstPeek = dataStream.readLong();
        long nextPeek = dataStream.readLong();
        long finalPeek = dataStream.readLong();
        boolean result = false;

        for (int i = 0; i < PKCS12_HEADER_PATTERNS.length; i++) {
            if (PKCS12_HEADER_PATTERNS[i][0] ==
                    (firstPeek & PKCS12_HEADER_MASKS[i][0]) &&
                (PKCS12_HEADER_PATTERNS[i][1] ==
                    (nextPeek & PKCS12_HEADER_MASKS[i][1])) &&
                (PKCS12_HEADER_PATTERNS[i][2] ==
                    (finalPeek & PKCS12_HEADER_MASKS[i][2]))) {
                result = true;
                break;
            }
        }

        return result;
    }

    // The following methods are related to customizing
    // the generation of a PKCS12 keystore or private/secret
    // key entries.

    private static boolean useLegacy() {
        return GetPropertyAction.privilegedGetProperty(
                USE_LEGACY_PROP) != null;
    }

    private static String defaultCertProtectionAlgorithm() {
        if (useLegacy()) {
            return LEGACY_CERT_PBE_ALGORITHM;
        }
        String result = SecurityProperties.privilegedGetOverridable(
                "keystore.pkcs12.certProtectionAlgorithm");
        return (result != null && !result.isEmpty())
                ? result : DEFAULT_CERT_PBE_ALGORITHM;
    }

    private static int defaultCertPbeIterationCount() {
        if (useLegacy()) {
            return LEGACY_PBE_ITERATION_COUNT;
        }
        String result = SecurityProperties.privilegedGetOverridable(
                "keystore.pkcs12.certPbeIterationCount");
        return (result != null && !result.isEmpty())
                ? string2IC("certPbeIterationCount", result)
                : DEFAULT_CERT_PBE_ITERATION_COUNT;
    }

    // Read both "keystore.pkcs12.keyProtectionAlgorithm" and
    // "keystore.PKCS12.keyProtectionAlgorithm" for compatibility.
    private static String defaultKeyProtectionAlgorithm() {
        if (useLegacy()) {
            return LEGACY_KEY_PBE_ALGORITHM;
        }
        @SuppressWarnings("removal")
        String result = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                String result;
                String name1 = "keystore.pkcs12.keyProtectionAlgorithm";
                String name2 = "keystore.PKCS12.keyProtectionAlgorithm";
                result = System.getProperty(name1);
                if (result != null) {
                    return result;
                }
                result = System.getProperty(name2);
                if (result != null) {
                    return result;
                }
                result = Security.getProperty(name1);
                if (result != null) {
                    return result;
                }
                return Security.getProperty(name2);
            }
        });
        return (result != null && !result.isEmpty())
                ? result : DEFAULT_KEY_PBE_ALGORITHM;
    }

    private static int defaultKeyPbeIterationCount() {
        if (useLegacy()) {
            return LEGACY_PBE_ITERATION_COUNT;
        }
        String result = SecurityProperties.privilegedGetOverridable(
                "keystore.pkcs12.keyPbeIterationCount");
        return (result != null && !result.isEmpty())
                ? string2IC("keyPbeIterationCount", result)
                : DEFAULT_KEY_PBE_ITERATION_COUNT;
    }

    private static String defaultMacAlgorithm() {
        if (useLegacy()) {
            return LEGACY_MAC_ALGORITHM;
        }
        String result = SecurityProperties.privilegedGetOverridable(
                "keystore.pkcs12.macAlgorithm");
        return (result != null && !result.isEmpty())
                ? result : DEFAULT_MAC_ALGORITHM;
    }

    private static int defaultMacIterationCount() {
        if (useLegacy()) {
            return LEGACY_MAC_ITERATION_COUNT;
        }
        String result = SecurityProperties.privilegedGetOverridable(
                "keystore.pkcs12.macIterationCount");
        return (result != null && !result.isEmpty())
                ? string2IC("macIterationCount", result)
                : DEFAULT_MAC_ITERATION_COUNT;
    }

    private static int string2IC(String type, String value) {
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("keystore.pkcs12." + type
                    + " is not a number: " + value);
        }
        if (number <= 0 || number > MAX_ITERATION_COUNT) {
            throw new IllegalArgumentException("Invalid keystore.pkcs12."
                    + type + ": " + value);
        }
        return number;
    }
}
