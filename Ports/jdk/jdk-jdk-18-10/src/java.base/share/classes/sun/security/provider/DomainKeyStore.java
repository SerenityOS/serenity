/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.util.PolicyUtil;

/**
 * This class provides the domain keystore type identified as "DKS".
 * DKS presents a collection of separate keystores as a single logical keystore.
 * The collection of keystores is specified in a domain configuration file which
 * is passed to DKS in a {@link DomainLoadStoreParameter}.
 * <p>
 * The following properties are supported:
 * <dl>
 * <dt> {@code keystoreType="<type>"} </dt>
 *     <dd> The keystore type. </dd>
 * <dt> {@code keystoreURI="<url>"} </dt>
 *     <dd> The keystore location. </dd>
 * <dt> {@code keystoreProviderName="<name>"} </dt>
 *     <dd> The name of the keystore's JCE provider. </dd>
 * <dt> {@code keystorePasswordEnv="<environment-variable>"} </dt>
 *     <dd> The environment variable that stores a keystore password.
 * <dt> {@code entryNameSeparator="<separator>"} </dt>
 *     <dd> The separator between a keystore name prefix and an entry name.
 *          When specified, it applies to all the entries in a domain.
 *          Its default value is a space. </dd>
 * </dl>
 *
 * @since 1.8
 */

abstract class DomainKeyStore extends KeyStoreSpi {

    // regular DKS
    public static final class DKS extends DomainKeyStore {
        String convertAlias(String alias) {
            return alias.toLowerCase(Locale.ENGLISH);
        }
    }

    // DKS property names
    private static final String ENTRY_NAME_SEPARATOR = "entrynameseparator";
    private static final String KEYSTORE_PROVIDER_NAME = "keystoreprovidername";
    private static final String KEYSTORE_TYPE = "keystoretype";
    private static final String KEYSTORE_URI = "keystoreuri";
    private static final String KEYSTORE_PASSWORD_ENV = "keystorepasswordenv";

    // RegEx meta characters
    private static final String REGEX_META = ".$|()[{^?*+\\";

    // Default prefix for keystores loaded-by-stream
    private static final String DEFAULT_STREAM_PREFIX = "iostream";
    private int streamCounter = 1;
    private String entryNameSeparator = " ";
    private String entryNameSeparatorRegEx = " ";

    // Default keystore type
    private static final String DEFAULT_KEYSTORE_TYPE =
        KeyStore.getDefaultType();

    // Domain keystores
    private final Map<String, KeyStore> keystores = new HashMap<>();

    DomainKeyStore() {
    }

    // convert an alias to internal form, overridden in subclasses:
    // lower case for regular DKS
    abstract String convertAlias(String alias);

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
        AbstractMap.SimpleEntry<String, Collection<KeyStore>> pair =
            getKeystoresForReading(alias);
        Key key = null;

        try {
            String entryAlias = pair.getKey();
            for (KeyStore keystore : pair.getValue()) {
                key = keystore.getKey(entryAlias, password);
                if (key != null) {
                    break;
                }
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
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

        AbstractMap.SimpleEntry<String, Collection<KeyStore>> pair =
            getKeystoresForReading(alias);
        Certificate[] chain = null;

        try {
            String entryAlias = pair.getKey();
            for (KeyStore keystore : pair.getValue()) {
                chain = keystore.getCertificateChain(entryAlias);
                if (chain != null) {
                    break;
                }
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return chain;
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

        AbstractMap.SimpleEntry<String, Collection<KeyStore>> pair =
            getKeystoresForReading(alias);
        Certificate cert = null;

        try {
            String entryAlias = pair.getKey();
            for (KeyStore keystore : pair.getValue()) {
                cert = keystore.getCertificate(entryAlias);
                if (cert != null) {
                    break;
                }
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return cert;
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

        AbstractMap.SimpleEntry<String, Collection<KeyStore>> pair =
            getKeystoresForReading(alias);
        Date date = null;

        try {
            String entryAlias = pair.getKey();
            for (KeyStore keystore : pair.getValue()) {
                date = keystore.getCreationDate(entryAlias);
                if (date != null) {
                    break;
                }
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return date;
    }

    /**
     * Assigns the given private key to the given alias, protecting
     * it with the given password as defined in PKCS8.
     *
     * <p>The given java.security.PrivateKey <code>key</code> must
     * be accompanied by a certificate chain certifying the
     * corresponding public key.
     *
     * <p>If the given alias already exists, the keystore information
     * associated with it is overridden by the given key and certificate
     * chain.
     *
     * @param alias the alias name
     * @param key the private key to be associated with the alias
     * @param password the password to protect the key
     * @param chain the certificate chain for the corresponding public
     * key (only required if the given key is of type
     * <code>java.security.PrivateKey</code>).
     *
     * @exception KeyStoreException if the given key is not a private key,
     * cannot be protected, or this operation fails for some other reason
     */
    public void engineSetKeyEntry(String alias, Key key, char[] password,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        AbstractMap.SimpleEntry<String,
            AbstractMap.SimpleEntry<String, KeyStore>> pair =
                getKeystoreForWriting(alias);

        if (pair == null) {
            throw new KeyStoreException("Error setting key entry for '" +
                alias + "'");
        }
        String entryAlias = pair.getKey();
        Map.Entry<String, KeyStore> keystore = pair.getValue();
        keystore.getValue().setKeyEntry(entryAlias, key, password, chain);
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
    public void engineSetKeyEntry(String alias, byte[] key,
                                  Certificate[] chain)
        throws KeyStoreException
    {
        AbstractMap.SimpleEntry<String,
            AbstractMap.SimpleEntry<String, KeyStore>> pair =
                getKeystoreForWriting(alias);

        if (pair == null) {
            throw new KeyStoreException(
                "Error setting protected key entry for '" + alias + "'");
        }
        String entryAlias = pair.getKey();
        Map.Entry<String, KeyStore> keystore = pair.getValue();
        keystore.getValue().setKeyEntry(entryAlias, key, chain);
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
     * not identify a <i>trusted certificate entry</i>, or this operation
     * fails for some other reason.
     */
    public void engineSetCertificateEntry(String alias, Certificate cert)
        throws KeyStoreException
    {
        AbstractMap.SimpleEntry<String,
            AbstractMap.SimpleEntry<String, KeyStore>> pair =
                getKeystoreForWriting(alias);

        if (pair == null) {
            throw new KeyStoreException("Error setting certificate entry for '"
                + alias + "'");
        }
        String entryAlias = pair.getKey();
        Map.Entry<String, KeyStore> keystore = pair.getValue();
        keystore.getValue().setCertificateEntry(entryAlias, cert);
    }

    /**
     * Deletes the entry identified by the given alias from this keystore.
     *
     * @param alias the alias name
     *
     * @exception KeyStoreException if the entry cannot be removed.
     */
    public void engineDeleteEntry(String alias) throws KeyStoreException
    {
        AbstractMap.SimpleEntry<String,
            AbstractMap.SimpleEntry<String, KeyStore>> pair =
                getKeystoreForWriting(alias);

        if (pair == null) {
            throw new KeyStoreException("Error deleting entry for '" + alias +
                "'");
        }
        String entryAlias = pair.getKey();
        Map.Entry<String, KeyStore> keystore = pair.getValue();
        keystore.getValue().deleteEntry(entryAlias);
    }

    /**
     * Lists all the alias names of this keystore.
     *
     * @return enumeration of the alias names
     */
    public Enumeration<String> engineAliases() {
        final Iterator<Map.Entry<String, KeyStore>> iterator =
            keystores.entrySet().iterator();

        return new Enumeration<String>() {
            private int index = 0;
            private Map.Entry<String, KeyStore> keystoresEntry = null;
            private String prefix = null;
            private Enumeration<String> aliases = null;

            public boolean hasMoreElements() {
                try {
                    if (aliases == null) {
                        if (iterator.hasNext()) {
                            keystoresEntry = iterator.next();
                            prefix = keystoresEntry.getKey() +
                                entryNameSeparator;
                            aliases = keystoresEntry.getValue().aliases();
                        } else {
                            return false;
                        }
                    }
                    if (aliases.hasMoreElements()) {
                        return true;
                    } else {
                        if (iterator.hasNext()) {
                            keystoresEntry = iterator.next();
                            prefix = keystoresEntry.getKey() +
                                entryNameSeparator;
                            aliases = keystoresEntry.getValue().aliases();
                        } else {
                            return false;
                        }
                    }
                } catch (KeyStoreException e) {
                    return false;
                }

                return aliases.hasMoreElements();
            }

            public String nextElement() {
                if (hasMoreElements()) {
                    return prefix + aliases.nextElement();
                }
                throw new NoSuchElementException();
            }
        };
    }

    /**
     * Checks if the given alias exists in this keystore.
     *
     * @param alias the alias name
     *
     * @return true if the alias exists, false otherwise
     */
    public boolean engineContainsAlias(String alias) {

        AbstractMap.SimpleEntry<String, Collection<KeyStore>> pair =
            getKeystoresForReading(alias);

        try {
            String entryAlias = pair.getKey();
            for (KeyStore keystore : pair.getValue()) {
                if (keystore.containsAlias(entryAlias)) {
                    return true;
                }
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return false;
    }

    /**
     * Retrieves the number of entries in this keystore.
     *
     * @return the number of entries in this keystore
     */
    public int engineSize() {

        int size = 0;
        try {
            for (KeyStore keystore : keystores.values()) {
                size += keystore.size();
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return size;
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>key entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>key entry</i>, false otherwise.
     */
    public boolean engineIsKeyEntry(String alias) {

        AbstractMap.SimpleEntry<String, Collection<KeyStore>> pair =
            getKeystoresForReading(alias);

        try {
            String entryAlias = pair.getKey();
            for (KeyStore keystore : pair.getValue()) {
                if (keystore.isKeyEntry(entryAlias)) {
                    return true;
                }
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return false;
    }

    /**
     * Returns true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, and false otherwise.
     *
     * @return true if the entry identified by the given alias is a
     * <i>trusted certificate entry</i>, false otherwise.
     */
    public boolean engineIsCertificateEntry(String alias) {

        AbstractMap.SimpleEntry<String, Collection<KeyStore>> pair =
            getKeystoresForReading(alias);

        try {
            String entryAlias = pair.getKey();
            for (KeyStore keystore : pair.getValue()) {
                if (keystore.isCertificateEntry(entryAlias)) {
                    return true;
                }
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        return false;
    }

    /*
     * Returns a keystore entry alias and a list of target keystores.
     * When the supplied alias prefix identifies a keystore then that single
     * keystore is returned. When no alias prefix is supplied then all the
     * keystores are returned.
     */
    private AbstractMap.SimpleEntry<String, Collection<KeyStore>>
        getKeystoresForReading(String alias) {

        String[] splits = alias.split(this.entryNameSeparatorRegEx, 2);
        if (splits.length == 2) { // prefixed alias
            KeyStore keystore = keystores.get(splits[0]);
            if (keystore != null) {
                return new AbstractMap.SimpleEntry<>(splits[1],
                    (Collection<KeyStore>) Collections.singleton(keystore));
            }
        } else if (splits.length == 1) { // unprefixed alias
            // Check all keystores for the first occurrence of the alias
            return new AbstractMap.SimpleEntry<>(alias, keystores.values());
        }
        return new AbstractMap.SimpleEntry<>("",
            (Collection<KeyStore>) Collections.<KeyStore>emptyList());
    }

    /*
     * Returns a keystore entry alias and a single target keystore.
     * An alias prefix must be supplied.
     */
    private
    AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, KeyStore>>
        getKeystoreForWriting(String alias) {

        String[] splits = alias.split(this.entryNameSeparator, 2);
        if (splits.length == 2) { // prefixed alias
            KeyStore keystore = keystores.get(splits[0]);
            if (keystore != null) {
                return new AbstractMap.SimpleEntry<>(splits[1],
                    new AbstractMap.SimpleEntry<>(splits[0], keystore));
            }
        }
        return null;
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

        try {

            String alias = null;
            for (KeyStore keystore : keystores.values()) {
                if ((alias = keystore.getCertificateAlias(cert)) != null) {
                    break;
                }
            }
            return alias;

        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }
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
    public void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        // Support storing to a stream only when a single keystore has been
        // configured
        try {
            if (keystores.size() == 1) {
                keystores.values().iterator().next().store(stream, password);
                return;
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

        throw new UnsupportedOperationException(
            "This keystore must be stored using a DomainLoadStoreParameter");
    }

    @Override
    public void engineStore(KeyStore.LoadStoreParameter param)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        if (param instanceof DomainLoadStoreParameter) {
            DomainLoadStoreParameter domainParameter =
                (DomainLoadStoreParameter) param;
            List<KeyStoreBuilderComponents> builders = getBuilders(
                domainParameter.getConfiguration(),
                    domainParameter.getProtectionParams());

            for (KeyStoreBuilderComponents builder : builders) {

                try {

                    KeyStore.ProtectionParameter pp = builder.protection;
                    if (!(pp instanceof KeyStore.PasswordProtection)) {
                        throw new KeyStoreException(
                            new IllegalArgumentException("ProtectionParameter" +
                                " must be a KeyStore.PasswordProtection"));
                    }
                    char[] password =
                        ((KeyStore.PasswordProtection) builder.protection)
                            .getPassword();

                    // Store the keystores
                    KeyStore keystore = keystores.get(builder.name);

                    try (FileOutputStream stream =
                        new FileOutputStream(builder.file)) {

                        keystore.store(stream, password);
                    }
                } catch (KeyStoreException e) {
                    throw new IOException(e);
                }
            }
        } else {
            throw new UnsupportedOperationException(
                "This keystore must be stored using a " +
                "DomainLoadStoreParameter");
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
    public void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        // Support loading from a stream only for a JKS or default type keystore
        try {
            KeyStore keystore = null;

            try {
                keystore = KeyStore.getInstance("JKS");
                keystore.load(stream, password);

            } catch (Exception e) {
                // Retry
                if (!"JKS".equalsIgnoreCase(DEFAULT_KEYSTORE_TYPE)) {
                    keystore = KeyStore.getInstance(DEFAULT_KEYSTORE_TYPE);
                    keystore.load(stream, password);
                } else {
                    throw e;
                }
            }
            String keystoreName = DEFAULT_STREAM_PREFIX + streamCounter++;
            keystores.put(keystoreName, keystore);

        } catch (Exception e) {
            throw new UnsupportedOperationException(
                "This keystore must be loaded using a " +
                "DomainLoadStoreParameter");
        }
    }

    @Override
    public void engineLoad(KeyStore.LoadStoreParameter param)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        if (param instanceof DomainLoadStoreParameter) {
            DomainLoadStoreParameter domainParameter =
                (DomainLoadStoreParameter) param;
            List<KeyStoreBuilderComponents> builders = getBuilders(
                domainParameter.getConfiguration(),
                    domainParameter.getProtectionParams());

            for (KeyStoreBuilderComponents builder : builders) {

                try {
                    // Load the keystores (file-based and non-file-based)
                    if (builder.file != null) {
                        keystores.put(builder.name,
                            KeyStore.Builder.newInstance(builder.type,
                                builder.provider, builder.file,
                                builder.protection)
                                    .getKeyStore());
                    } else {
                        keystores.put(builder.name,
                            KeyStore.Builder.newInstance(builder.type,
                                builder.provider, builder.protection)
                                    .getKeyStore());
                    }
                } catch (KeyStoreException e) {
                    throw new IOException(e);
                }
            }
        } else {
            throw new UnsupportedOperationException(
                "This keystore must be loaded using a " +
                "DomainLoadStoreParameter");
        }
    }

    /*
     * Parse a keystore domain configuration file and associated collection
     * of keystore passwords to create a collection of KeyStore.Builder.
     */
    private List<KeyStoreBuilderComponents> getBuilders(URI configuration,
        Map<String, KeyStore.ProtectionParameter> passwords)
            throws IOException {

        PolicyParser parser = new PolicyParser(true); // expand properties
        Collection<PolicyParser.DomainEntry> domains = null;
        List<KeyStoreBuilderComponents> builders = new ArrayList<>();
        String uriDomain = configuration.getFragment();

        try (InputStreamReader configurationReader =
            new InputStreamReader(
                PolicyUtil.getInputStream(configuration.toURL()), UTF_8)) {
            parser.read(configurationReader);
            domains = parser.getDomainEntries();

        } catch (MalformedURLException mue) {
            throw new IOException(mue);

        } catch (PolicyParser.ParsingException pe) {
            throw new IOException(pe);
        }

        for (PolicyParser.DomainEntry domain : domains) {
            Map<String, String> domainProperties = domain.getProperties();

            if (uriDomain != null &&
                (!uriDomain.equalsIgnoreCase(domain.getName()))) {
                continue; // skip this domain
            }

            if (domainProperties.containsKey(ENTRY_NAME_SEPARATOR)) {
                this.entryNameSeparator =
                    domainProperties.get(ENTRY_NAME_SEPARATOR);
                // escape any regex meta characters
                char ch = 0;
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < this.entryNameSeparator.length(); i++) {
                    ch = this.entryNameSeparator.charAt(i);
                    if (REGEX_META.indexOf(ch) != -1) {
                        s.append('\\');
                    }
                    s.append(ch);
                }
                this.entryNameSeparatorRegEx = s.toString();
            }

            Collection<PolicyParser.KeyStoreEntry> keystores =
                domain.getEntries();
            for (PolicyParser.KeyStoreEntry keystore : keystores) {
                String keystoreName = keystore.getName();
                Map<String, String> properties =
                    new HashMap<>(domainProperties);
                properties.putAll(keystore.getProperties());

                String keystoreType = DEFAULT_KEYSTORE_TYPE;
                if (properties.containsKey(KEYSTORE_TYPE)) {
                    keystoreType = properties.get(KEYSTORE_TYPE);
                }

                Provider keystoreProvider = null;
                if (properties.containsKey(KEYSTORE_PROVIDER_NAME)) {
                    String keystoreProviderName =
                        properties.get(KEYSTORE_PROVIDER_NAME);
                    keystoreProvider =
                        Security.getProvider(keystoreProviderName);
                    if (keystoreProvider == null) {
                        throw new IOException("Error locating JCE provider: " +
                            keystoreProviderName);
                    }
                }

                File keystoreFile = null;
                if (properties.containsKey(KEYSTORE_URI)) {
                    String uri = properties.get(KEYSTORE_URI);

                    try {
                        if (uri.startsWith("file://")) {
                            keystoreFile = new File(new URI(uri));
                        } else {
                            keystoreFile = new File(uri);
                        }

                    } catch (URISyntaxException | IllegalArgumentException e) {
                        throw new IOException(
                            "Error processing keystore property: " +
                                "keystoreURI=\"" + uri + "\"", e);
                    }
                }

                KeyStore.ProtectionParameter keystoreProtection = null;
                if (passwords.containsKey(keystoreName)) {
                    keystoreProtection = passwords.get(keystoreName);

                } else if (properties.containsKey(KEYSTORE_PASSWORD_ENV)) {
                    String env = properties.get(KEYSTORE_PASSWORD_ENV);
                    String pwd = System.getenv(env);
                    if (pwd != null) {
                        keystoreProtection =
                            new KeyStore.PasswordProtection(pwd.toCharArray());
                    } else {
                        throw new IOException(
                            "Error processing keystore property: " +
                                "keystorePasswordEnv=\"" + env + "\"");
                    }
                } else {
                    keystoreProtection = new KeyStore.PasswordProtection(null);
                }

                builders.add(new KeyStoreBuilderComponents(keystoreName,
                    keystoreType, keystoreProvider, keystoreFile,
                    keystoreProtection));
            }
            break; // skip other domains
        }
        if (builders.isEmpty()) {
            throw new IOException("Error locating domain configuration data " +
                "for: " + configuration);
        }

        return builders;
    }

/*
 * Utility class that holds the components used to construct a KeyStore.Builder
 */
static class KeyStoreBuilderComponents {
    String name;
    String type;
    Provider provider;
    File file;
    KeyStore.ProtectionParameter protection;

    KeyStoreBuilderComponents(String name, String type, Provider provider,
        File file, KeyStore.ProtectionParameter protection) {
        this.name = name;
        this.type = type;
        this.provider = provider;
        this.file = file;
        this.protection = protection;
    }
}
}
