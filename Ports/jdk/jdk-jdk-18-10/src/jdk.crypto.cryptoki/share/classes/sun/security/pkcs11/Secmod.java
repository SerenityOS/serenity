/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.KeyStore.*;
import java.security.cert.X509Certificate;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;


/**
 * The Secmod class defines the interface to the native NSS
 * library and the configuration information it stores in its
 * secmod.db file.
 *
 * <p>Example code:
 * <pre>
 *   Secmod secmod = Secmod.getInstance();
 *   if (secmod.isInitialized() == false) {
 *       secmod.initialize("/home/myself/.mozilla");
 *   }
 *
 *   Provider p = secmod.getModule(ModuleType.KEYSTORE).getProvider();
 *   KeyStore ks = KeyStore.getInstance("PKCS11", p);
 *   ks.load(null, password);
 * </pre>
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
public final class Secmod {

    private static final boolean DEBUG = false;

    private static final Secmod INSTANCE;

    static {
        sun.security.pkcs11.wrapper.PKCS11.loadNative();
        INSTANCE = new Secmod();
    }

    private static final String NSS_LIB_NAME = "nss3";

    private static final String SOFTTOKEN_LIB_NAME = "softokn3";

    private static final String TRUST_LIB_NAME = "nssckbi";

    // Slot IDs - defined in j2secmod.h on the native side
    // Values obtained from NSS's pkcs11i.h header

    private final static int NETSCAPE_SLOT_ID = 0x1;

    private final static int PRIVATE_KEY_SLOT_ID = 0x2;

    private final static int FIPS_SLOT_ID = 0x3;

    // handle to be passed to the native code, 0 means not initialized
    private long nssHandle;

    // whether this is a supported version of NSS
    private boolean supported;

    // list of the modules
    private List<Module> modules;

    private String configDir;

    private String nssLibDir;

    private Secmod() {
        // empty
    }

    /**
     * Return the singleton Secmod instance.
     */
    public static Secmod getInstance() {
        return INSTANCE;
    }

    private boolean isLoaded() {
        if (nssHandle == 0) {
            nssHandle = nssGetLibraryHandle(System.mapLibraryName(NSS_LIB_NAME));
            if (nssHandle != 0) {
                fetchVersions();
            }
        }
        return (nssHandle != 0);
    }

    private void fetchVersions() {
        supported = nssVersionCheck(nssHandle, "3.7");
    }

    /**
     * Test whether this Secmod has been initialized. Returns true
     * if NSS has been initialized using either the initialize() method
     * or by directly calling the native NSS APIs. The latter may be
     * the case if the current process contains components that use
     * NSS directly.
     *
     * @throws IOException if an incompatible version of NSS
     *   has been loaded
     */
    public synchronized boolean isInitialized() throws IOException {
        // NSS does not allow us to check if it is initialized already
        // assume that if it is loaded it is also initialized
        if (isLoaded() == false) {
            return false;
        }
        if (supported == false) {
            throw new IOException
                ("An incompatible version of NSS is already loaded, "
                + "3.7 or later required");
        }
        return true;
    }

    String getConfigDir() {
        return configDir;
    }

    String getLibDir() {
        return nssLibDir;
    }

    /**
     * Initialize this Secmod.
     *
     * @param configDir the directory containing the NSS configuration
     *   files such as secmod.db
     * @param nssLibDir the directory containing the NSS libraries
     *   (libnss3.so or nss3.dll) or null if the library is on
     *   the system default shared library path
     *
     * @throws IOException if NSS has already been initialized,
     *   the specified directories are invalid, or initialization
     *   fails for any other reason
     */
    public void initialize(String configDir, String nssLibDir)
            throws IOException {
        initialize(DbMode.READ_WRITE, configDir, nssLibDir, false);
    }

    public void initialize(DbMode dbMode, String configDir, String nssLibDir)
            throws IOException {
        initialize(dbMode, configDir, nssLibDir, false);
    }

    public synchronized void initialize(DbMode dbMode, String configDir,
        String nssLibDir, boolean nssOptimizeSpace) throws IOException {

        if (isInitialized()) {
            throw new IOException("NSS is already initialized");
        }

        if (dbMode == null) {
            throw new NullPointerException();
        }
        if ((dbMode != DbMode.NO_DB) && (configDir == null)) {
            throw new NullPointerException();
        }
        String platformLibName = System.mapLibraryName("nss3");
        String platformPath;
        if (nssLibDir == null) {
            platformPath = platformLibName;
        } else {
            File base = new File(nssLibDir);
            if (base.isDirectory() == false) {
                throw new IOException("nssLibDir must be a directory:" + nssLibDir);
            }
            File platformFile = new File(base, platformLibName);
            if (platformFile.isFile() == false) {
                throw new FileNotFoundException(platformFile.getPath());
            }
            platformPath = platformFile.getPath();
        }

        if (configDir != null) {
            String configDirPath = null;
            String sqlPrefix = "sql:";
            if (!configDir.startsWith(sqlPrefix)) {
                configDirPath = configDir;
            } else {
                StringBuilder configDirPathSB = new StringBuilder(configDir);
                configDirPath = configDirPathSB.substring(sqlPrefix.length());
            }
            File configBase = new File(configDirPath);
            if (configBase.isDirectory() == false ) {
                throw new IOException("configDir must be a directory: " + configDirPath);
            }
            if (!configDir.startsWith(sqlPrefix)) {
                File secmodFile = new File(configBase, "secmod.db");
                if (secmodFile.isFile() == false) {
                    throw new FileNotFoundException(secmodFile.getPath());
                }
            }
        }

        if (DEBUG) System.out.println("lib: " + platformPath);
        nssHandle = nssLoadLibrary(platformPath);
        if (DEBUG) System.out.println("handle: " + nssHandle);
        fetchVersions();
        if (supported == false) {
            throw new IOException
                ("The specified version of NSS is incompatible, "
                + "3.7 or later required");
        }

        if (DEBUG) System.out.println("dir: " + configDir);
        boolean initok = nssInitialize(dbMode.functionName, nssHandle,
            configDir, nssOptimizeSpace);
        if (DEBUG) System.out.println("init: " + initok);
        if (initok == false) {
            throw new IOException("NSS initialization failed");
        }

        this.configDir = configDir;
        this.nssLibDir = nssLibDir;
    }

    /**
     * Return an immutable list of all available modules.
     *
     * @throws IllegalStateException if this Secmod is misconfigured
     *   or not initialized
     */
    public synchronized List<Module> getModules() {
        try {
            if (isInitialized() == false) {
                throw new IllegalStateException("NSS not initialized");
            }
        } catch (IOException e) {
            // IOException if misconfigured
            throw new IllegalStateException(e);
        }
        if (modules == null) {
            @SuppressWarnings("unchecked")
            List<Module> modules = (List<Module>)nssGetModuleList(nssHandle,
                nssLibDir);
            this.modules = Collections.unmodifiableList(modules);
        }
        return modules;
    }

    private static byte[] getDigest(X509Certificate cert, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(cert.getEncoded());
        } catch (GeneralSecurityException e) {
            throw new ProviderException(e);
        }
    }

    boolean isTrusted(X509Certificate cert, TrustType trustType) {
        Bytes bytes = new Bytes(getDigest(cert, "SHA-1"));
        TrustAttributes attr = getModuleTrust(ModuleType.KEYSTORE, bytes);
        if (attr == null) {
            attr = getModuleTrust(ModuleType.FIPS, bytes);
            if (attr == null) {
                attr = getModuleTrust(ModuleType.TRUSTANCHOR, bytes);
            }
        }
        return (attr == null) ? false : attr.isTrusted(trustType);
    }

    private TrustAttributes getModuleTrust(ModuleType type, Bytes bytes) {
        Module module = getModule(type);
        TrustAttributes t = (module == null) ? null : module.getTrust(bytes);
        return t;
    }

    /**
     * Constants describing the different types of NSS modules.
     * For this API, NSS modules are classified as either one
     * of the internal modules delivered as part of NSS or
     * as an external module provided by a 3rd party.
     */
    public static enum ModuleType {
        /**
         * The NSS Softtoken crypto module. This is the first
         * slot of the softtoken object.
         * This module provides
         * implementations for cryptographic algorithms but no KeyStore.
         */
        CRYPTO,
        /**
         * The NSS Softtoken KeyStore module. This is the second
         * slot of the softtoken object.
         * This module provides
         * implementations for cryptographic algorithms (after login)
         * and the KeyStore.
         */
        KEYSTORE,
        /**
         * The NSS Softtoken module in FIPS mode. Note that in FIPS mode the
         * softtoken presents only one slot, not separate CRYPTO and KEYSTORE
         * slots as in non-FIPS mode.
         */
        FIPS,
        /**
         * The NSS builtin trust anchor module. This is the
         * NSSCKBI object. It provides no crypto functions.
         */
        TRUSTANCHOR,
        /**
         * An external module.
         */
        EXTERNAL,
    }

    /**
     * Returns the first module of the specified type. If no such
     * module exists, this method returns null.
     *
     * @throws IllegalStateException if this Secmod is misconfigured
     *   or not initialized
     */
    public Module getModule(ModuleType type) {
        for (Module module : getModules()) {
            if (module.getType() == type) {
                return module;
            }
        }
        return null;
    }

    static final String TEMPLATE_EXTERNAL =
        "library = %s\n"
        + "name = \"%s\"\n"
        + "slotListIndex = %d\n";

    static final String TEMPLATE_TRUSTANCHOR =
        "library = %s\n"
        + "name = \"NSS Trust Anchors\"\n"
        + "slotListIndex = 0\n"
        + "enabledMechanisms = { KeyStore }\n"
        + "nssUseSecmodTrust = true\n";

    static final String TEMPLATE_CRYPTO =
        "library = %s\n"
        + "name = \"NSS SoftToken Crypto\"\n"
        + "slotListIndex = 0\n"
        + "disabledMechanisms = { KeyStore }\n";

    static final String TEMPLATE_KEYSTORE =
        "library = %s\n"
        + "name = \"NSS SoftToken KeyStore\"\n"
        + "slotListIndex = 1\n"
        + "nssUseSecmodTrust = true\n";

    static final String TEMPLATE_FIPS =
        "library = %s\n"
        + "name = \"NSS FIPS SoftToken\"\n"
        + "slotListIndex = 0\n"
        + "nssUseSecmodTrust = true\n";

    /**
     * A representation of one PKCS#11 slot in a PKCS#11 module.
     */
    public static final class Module {
        // path of the native library
        final String libraryName;
        // descriptive name used by NSS
        final String commonName;
        final int slot;
        final ModuleType type;

        private String config;
        private SunPKCS11 provider;

        // trust attributes. Used for the KEYSTORE and TRUSTANCHOR modules only
        private Map<Bytes,TrustAttributes> trust;

        Module(String libraryDir, String libraryName, String commonName,
                int slotIndex, int slotId) {
            ModuleType type;

            if ((libraryName == null) || (libraryName.length() == 0)) {
                // must be softtoken
                libraryName = System.mapLibraryName(SOFTTOKEN_LIB_NAME);
                if (slotId == NETSCAPE_SLOT_ID) {
                    type = ModuleType.CRYPTO;
                } else if (slotId == PRIVATE_KEY_SLOT_ID) {
                    type = ModuleType.KEYSTORE;
                } else if (slotId == FIPS_SLOT_ID) {
                    type = ModuleType.FIPS;
                } else {
                    throw new RuntimeException("Unexpected slot ID " + slotId +
                            " in the NSS Internal Module");
                }
            } else {
                if (libraryName.endsWith(System.mapLibraryName(TRUST_LIB_NAME))
                        || commonName.equals("Builtin Roots Module")) {
                    type = ModuleType.TRUSTANCHOR;
                } else {
                    type = ModuleType.EXTERNAL;
                }
            }
            // On Ubuntu the libsoftokn3 library is located in a subdirectory
            // of the system libraries directory. (Since Ubuntu 11.04.)
            File libraryFile = new File(libraryDir, libraryName);
            if (!libraryFile.isFile()) {
               File failover = new File(libraryDir, "nss/" + libraryName);
               if (failover.isFile()) {
                   libraryFile = failover;
               }
            }
            this.libraryName = libraryFile.getPath();
            this.commonName = commonName;
            this.slot = slotIndex;
            this.type = type;
            initConfiguration();
        }

        private void initConfiguration() {
            switch (type) {
            case EXTERNAL:
                config = String.format(TEMPLATE_EXTERNAL, libraryName,
                                            commonName + " " + slot, slot);
                break;
            case CRYPTO:
                config = String.format(TEMPLATE_CRYPTO, libraryName);
                break;
            case KEYSTORE:
                config = String.format(TEMPLATE_KEYSTORE, libraryName);
                break;
            case FIPS:
                config = String.format(TEMPLATE_FIPS, libraryName);
                break;
            case TRUSTANCHOR:
                config = String.format(TEMPLATE_TRUSTANCHOR, libraryName);
                break;
            default:
                throw new RuntimeException("Unknown module type: " + type);
            }
        }

        /**
         * Get the configuration for this module. This is a string
         * in the SunPKCS11 configuration format. It can be
         * customized with additional options and then made
         * current using the setConfiguration() method.
         */
        @Deprecated
        public synchronized String getConfiguration() {
            return config;
        }

        /**
         * Set the configuration for this module.
         *
         * @throws IllegalStateException if the associated provider
         *   instance has already been created.
         */
        @Deprecated
        public synchronized void setConfiguration(String config) {
            if (provider != null) {
                throw new IllegalStateException("Provider instance already created");
            }
            this.config = config;
        }

        /**
         * Return the pathname of the native library that implements
         * this module. For example, /usr/lib/libpkcs11.so.
         */
        public String getLibraryName() {
            return libraryName;
        }

        /**
         * Returns the type of this module.
         */
        public ModuleType getType() {
            return type;
        }

        /**
         * Returns the provider instance that is associated with this
         * module. The first call to this method creates the provider
         * instance.
         */
        @Deprecated
        public synchronized Provider getProvider() {
            if (provider == null) {
                provider = newProvider();
            }
            return provider;
        }

        synchronized boolean hasInitializedProvider() {
            return provider != null;
        }

        void setProvider(SunPKCS11 p) {
            if (provider != null) {
                throw new ProviderException("Secmod provider already initialized");
            }
            provider = p;
        }

        private SunPKCS11 newProvider() {
            try {
                return new SunPKCS11(new Config("--" + config));
            } catch (Exception e) {
                // XXX
                throw new ProviderException(e);
            }
        }

        synchronized void setTrust(Token token, X509Certificate cert) {
            Bytes bytes = new Bytes(getDigest(cert, "SHA-1"));
            TrustAttributes attr = getTrust(bytes);
            if (attr == null) {
                attr = new TrustAttributes(token, cert, bytes, CKT_NETSCAPE_TRUSTED_DELEGATOR);
                trust.put(bytes, attr);
            } else {
                // does it already have the correct trust settings?
                if (attr.isTrusted(TrustType.ALL) == false) {
                    // XXX not yet implemented
                    throw new ProviderException("Cannot change existing trust attributes");
                }
            }
        }

        TrustAttributes getTrust(Bytes hash) {
            if (trust == null) {
                // If provider is not set, create a temporary provider to
                // retrieve the trust information. This can happen if we need
                // to get the trust information for the trustanchor module
                // because we need to look for user customized settings in the
                // keystore module (which may not have a provider created yet).
                // Creating a temporary provider and then dropping it on the
                // floor immediately is flawed, but it's the best we can do
                // for now.
                synchronized (this) {
                    SunPKCS11 p = provider;
                    if (p == null) {
                        p = newProvider();
                    }
                    try {
                        trust = Secmod.getTrust(p);
                    } catch (PKCS11Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return trust.get(hash);
        }

        public String toString() {
            return
            commonName + " (" + type + ", " + libraryName + ", slot " + slot + ")";
        }

    }

    /**
     * Constants representing NSS trust categories.
     */
    public static enum TrustType {
        /** Trusted for all purposes */
        ALL,
        /** Trusted for SSL client authentication */
        CLIENT_AUTH,
        /** Trusted for SSL server authentication */
        SERVER_AUTH,
        /** Trusted for code signing */
        CODE_SIGNING,
        /** Trusted for email protection */
        EMAIL_PROTECTION,
    }

    public static enum DbMode {
        READ_WRITE("NSS_InitReadWrite"),
        READ_ONLY ("NSS_Init"),
        NO_DB     ("NSS_NoDB_Init");

        final String functionName;
        DbMode(String functionName) {
            this.functionName = functionName;
        }
    }

    /**
     * A LoadStoreParameter for use with the NSS Softtoken or
     * NSS TrustAnchor KeyStores.
     * <p>
     * It allows the set of trusted certificates that are returned by
     * the KeyStore to be specified.
     */
    public static final class KeyStoreLoadParameter implements LoadStoreParameter {
        final TrustType trustType;
        final ProtectionParameter protection;
        public KeyStoreLoadParameter(TrustType trustType, char[] password) {
            this(trustType, new PasswordProtection(password));

        }
        public KeyStoreLoadParameter(TrustType trustType, ProtectionParameter prot) {
            if (trustType == null) {
                throw new NullPointerException("trustType must not be null");
            }
            this.trustType = trustType;
            this.protection = prot;
        }
        public ProtectionParameter getProtectionParameter() {
            return protection;
        }
        public TrustType getTrustType() {
            return trustType;
        }
    }

    static class TrustAttributes {
        final long handle;
        final long clientAuth, serverAuth, codeSigning, emailProtection;
        final byte[] shaHash;
        TrustAttributes(Token token, X509Certificate cert, Bytes bytes, long trustValue) {
            Session session = null;
            try {
                session = token.getOpSession();
                // XXX use KeyStore TrustType settings to determine which
                // attributes to set
                CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                    new CK_ATTRIBUTE(CKA_TOKEN, true),
                    new CK_ATTRIBUTE(CKA_CLASS, CKO_NETSCAPE_TRUST),
                    new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_SERVER_AUTH, trustValue),
                    new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_CODE_SIGNING, trustValue),
                    new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_EMAIL_PROTECTION, trustValue),
                    new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_CLIENT_AUTH, trustValue),
                    new CK_ATTRIBUTE(CKA_NETSCAPE_CERT_SHA1_HASH, bytes.b),
                    new CK_ATTRIBUTE(CKA_NETSCAPE_CERT_MD5_HASH, getDigest(cert, "MD5")),
                    new CK_ATTRIBUTE(CKA_ISSUER, cert.getIssuerX500Principal().getEncoded()),
                    new CK_ATTRIBUTE(CKA_SERIAL_NUMBER, cert.getSerialNumber().toByteArray()),
                    // XXX per PKCS#11 spec, the serial number should be in ASN.1
                };
                handle = token.p11.C_CreateObject(session.id(), attrs);
                shaHash = bytes.b;
                clientAuth = trustValue;
                serverAuth = trustValue;
                codeSigning = trustValue;
                emailProtection = trustValue;
            } catch (PKCS11Exception e) {
                throw new ProviderException("Could not create trust object", e);
            } finally {
                token.releaseSession(session);
            }
        }
        TrustAttributes(Token token, Session session, long handle)
                        throws PKCS11Exception {
            this.handle = handle;
            CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_SERVER_AUTH),
                new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_CODE_SIGNING),
                new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_EMAIL_PROTECTION),
                new CK_ATTRIBUTE(CKA_NETSCAPE_CERT_SHA1_HASH),
            };

            token.p11.C_GetAttributeValue(session.id(), handle, attrs);
            serverAuth = attrs[0].getLong();
            codeSigning = attrs[1].getLong();
            emailProtection = attrs[2].getLong();
            shaHash = attrs[3].getByteArray();

            attrs = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_NETSCAPE_TRUST_CLIENT_AUTH),
            };
            long c;
            try {
                token.p11.C_GetAttributeValue(session.id(), handle, attrs);
                c = attrs[0].getLong();
            } catch (PKCS11Exception e) {
                // trust anchor module does not support this attribute
                c = serverAuth;
            }
            clientAuth = c;
        }
        Bytes getHash() {
            return new Bytes(shaHash);
        }
        boolean isTrusted(TrustType type) {
            switch (type) {
            case CLIENT_AUTH:
                return isTrusted(clientAuth);
            case SERVER_AUTH:
                return isTrusted(serverAuth);
            case CODE_SIGNING:
                return isTrusted(codeSigning);
            case EMAIL_PROTECTION:
                return isTrusted(emailProtection);
            case ALL:
                return isTrusted(TrustType.CLIENT_AUTH)
                    && isTrusted(TrustType.SERVER_AUTH)
                    && isTrusted(TrustType.CODE_SIGNING)
                    && isTrusted(TrustType.EMAIL_PROTECTION);
            default:
                return false;
            }
        }

        private boolean isTrusted(long l) {
            // XXX CKT_TRUSTED?
            return (l == CKT_NETSCAPE_TRUSTED_DELEGATOR);
        }

    }

    private static class Bytes {
        final byte[] b;
        Bytes(byte[] b) {
            this.b = b;
        }
        public int hashCode() {
            return Arrays.hashCode(b);
        }
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof Bytes == false) {
                return false;
            }
            Bytes other = (Bytes)o;
            return Arrays.equals(this.b, other.b);
        }
    }

    private static Map<Bytes,TrustAttributes> getTrust(SunPKCS11 provider)
            throws PKCS11Exception {
        Map<Bytes,TrustAttributes> trustMap = new HashMap<Bytes,TrustAttributes>();
        Token token = provider.getToken();
        Session session = null;
        boolean exceptionOccurred = true;
        try {
            session = token.getOpSession();
            int MAX_NUM = 8192;
            CK_ATTRIBUTE[] attrs = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_CLASS, CKO_NETSCAPE_TRUST),
            };
            token.p11.C_FindObjectsInit(session.id(), attrs);
            long[] handles = token.p11.C_FindObjects(session.id(), MAX_NUM);
            token.p11.C_FindObjectsFinal(session.id());
            if (DEBUG) System.out.println("handles: " + handles.length);

            for (long handle : handles) {
                try {
                    TrustAttributes trust = new TrustAttributes(token, session, handle);
                    trustMap.put(trust.getHash(), trust);
                } catch (PKCS11Exception e) {
                    // skip put on pkcs11 error
                }
            }
            exceptionOccurred = false;
        } finally {
            if (exceptionOccurred) {
                token.killSession(session);
            } else {
                token.releaseSession(session);
            }
        }
        return trustMap;
    }

    private static native long nssGetLibraryHandle(String libraryName);

    private static native long nssLoadLibrary(String name) throws IOException;

    private static native boolean nssVersionCheck(long handle, String minVersion);

    private static native boolean nssInitialize(String functionName, long handle, String configDir, boolean nssOptimizeSpace);

    private static native Object nssGetModuleList(long handle, String libDir);

}
