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

package java.util.jar;

import jdk.internal.access.SharedSecrets;
import jdk.internal.access.JavaUtilZipFileAccess;
import sun.security.action.GetPropertyAction;
import sun.security.util.ManifestEntryVerifier;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * The {@code JarFile} class is used to read the contents of a jar file
 * from any file that can be opened with {@code java.io.RandomAccessFile}.
 * It extends the class {@code java.util.zip.ZipFile} with support
 * for reading an optional {@code Manifest} entry, and support for
 * processing multi-release jar files.  The {@code Manifest} can be used
 * to specify meta-information about the jar file and its entries.
 *
 * <p><a id="multirelease">A multi-release jar file</a> is a jar file that
 * contains a manifest with a main attribute named "Multi-Release",
 * a set of "base" entries, some of which are public classes with public
 * or protected methods that comprise the public interface of the jar file,
 * and a set of "versioned" entries contained in subdirectories of the
 * "META-INF/versions" directory.  The versioned entries are partitioned by the
 * major version of the Java platform.  A versioned entry, with a version
 * {@code n}, {@code 8 < n}, in the "META-INF/versions/{n}" directory overrides
 * the base entry as well as any entry with a version number {@code i} where
 * {@code 8 < i < n}.
 *
 * <p>By default, a {@code JarFile} for a multi-release jar file is configured
 * to process the multi-release jar file as if it were a plain (unversioned) jar
 * file, and as such an entry name is associated with at most one base entry.
 * The {@code JarFile} may be configured to process a multi-release jar file by
 * creating the {@code JarFile} with the
 * {@link JarFile#JarFile(File, boolean, int, Runtime.Version)} constructor.  The
 * {@code Runtime.Version} object sets a maximum version used when searching for
 * versioned entries.  When so configured, an entry name
 * can correspond with at most one base entry and zero or more versioned
 * entries. A search is required to associate the entry name with the latest
 * versioned entry whose version is less than or equal to the maximum version
 * (see {@link #getEntry(String)}).
 *
 * <p>Class loaders that utilize {@code JarFile} to load classes from the
 * contents of {@code JarFile} entries should construct the {@code JarFile}
 * by invoking the {@link JarFile#JarFile(File, boolean, int, Runtime.Version)}
 * constructor with the value {@code Runtime.version()} assigned to the last
 * argument.  This assures that classes compatible with the major
 * version of the running JVM are loaded from multi-release jar files.
 *
 * <p> If the {@code verify} flag is on when opening a signed jar file, the content
 * of the jar entry is verified against the signature embedded inside the manifest
 * that is associated with its {@link JarEntry#getRealName() path name}. For a
 * multi-release jar file, the content of a versioned entry is verfieid against
 * its own signature and {@link JarEntry#getCodeSigners()} returns its own signers.
 *
 * Please note that the verification process does not include validating the
 * signer's certificate. A caller should inspect the return value of
 * {@link JarEntry#getCodeSigners()} to further determine if the signature
 * can be trusted.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @implNote
 * <div class="block">
 * If the API can not be used to configure a {@code JarFile} (e.g. to override
 * the configuration of a compiled application or library), two {@code System}
 * properties are available.
 * <ul>
 * <li>
 * {@code jdk.util.jar.version} can be assigned a value that is the
 * {@code String} representation of a non-negative integer
 * {@code <= Runtime.version().feature()}.  The value is used to set the effective
 * runtime version to something other than the default value obtained by
 * evaluating {@code Runtime.version().feature()}. The effective runtime version
 * is the version that the {@link JarFile#JarFile(File, boolean, int, Runtime.Version)}
 * constructor uses when the value of the last argument is
 * {@code JarFile.runtimeVersion()}.
 * </li>
 * <li>
 * {@code jdk.util.jar.enableMultiRelease} can be assigned one of the three
 * {@code String} values <em>true</em>, <em>false</em>, or <em>force</em>.  The
 * value <em>true</em>, the default value, enables multi-release jar file
 * processing.  The value <em>false</em> disables multi-release jar processing,
 * ignoring the "Multi-Release" manifest attribute, and the versioned
 * directories in a multi-release jar file if they exist.  Furthermore,
 * the method {@link JarFile#isMultiRelease()} returns <em>false</em>. The value
 * <em>force</em> causes the {@code JarFile} to be initialized to runtime
 * versioning after construction.  It effectively does the same as this code:
 * {@code (new JarFile(File, boolean, int, JarFile.runtimeVersion())}.
 * </li>
 * </ul>
 * </div>
 *
 * @author  David Connelly
 * @see     Manifest
 * @see     java.util.zip.ZipFile
 * @see     java.util.jar.JarEntry
 * @since   1.2
 */
public class JarFile extends ZipFile {
    private static final Runtime.Version BASE_VERSION;
    private static final int BASE_VERSION_FEATURE;
    private static final Runtime.Version RUNTIME_VERSION;
    private static final boolean MULTI_RELEASE_ENABLED;
    private static final boolean MULTI_RELEASE_FORCED;
    private static final ThreadLocal<Boolean> isInitializing = new ThreadLocal<>();
    // The maximum size of array to allocate. Some VMs reserve some header words in an array.
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private SoftReference<Manifest> manRef;
    private JarEntry manEntry;
    private JarVerifier jv;
    private boolean jvInitialized;
    private boolean verify;
    private final Runtime.Version version;  // current version
    private final int versionFeature;       // version.feature()
    private boolean isMultiRelease;         // is jar multi-release?

    // indicates if Class-Path attribute present
    private boolean hasClassPathAttribute;
    // true if manifest checked for special attributes
    private volatile boolean hasCheckedSpecialAttributes;

    private static final JavaUtilZipFileAccess JUZFA;

    static {
        // Set up JavaUtilJarAccess in SharedSecrets
        SharedSecrets.setJavaUtilJarAccess(new JavaUtilJarAccessImpl());
        // Get JavaUtilZipFileAccess from SharedSecrets
        JUZFA = SharedSecrets.getJavaUtilZipFileAccess();
        // multi-release jar file versions >= 9
        BASE_VERSION = Runtime.Version.parse(Integer.toString(8));
        BASE_VERSION_FEATURE = BASE_VERSION.feature();
        String jarVersion = GetPropertyAction.privilegedGetProperty("jdk.util.jar.version");
        int runtimeVersion = Runtime.version().feature();
        if (jarVersion != null) {
            int jarVer = Integer.parseInt(jarVersion);
            runtimeVersion = (jarVer > runtimeVersion)
                    ? runtimeVersion
                    : Math.max(jarVer, BASE_VERSION_FEATURE);
        }
        RUNTIME_VERSION = Runtime.Version.parse(Integer.toString(runtimeVersion));
        String enableMultiRelease = GetPropertyAction
                .privilegedGetProperty("jdk.util.jar.enableMultiRelease", "true");
        switch (enableMultiRelease) {
            case "false" -> {
                MULTI_RELEASE_ENABLED = false;
                MULTI_RELEASE_FORCED = false;
            }
            case "force" -> {
                MULTI_RELEASE_ENABLED = true;
                MULTI_RELEASE_FORCED = true;
            }
            default -> {
                MULTI_RELEASE_ENABLED = true;
                MULTI_RELEASE_FORCED = false;
            }
        }
    }

    private static final String META_INF = "META-INF/";

    private static final String META_INF_VERSIONS = META_INF + "versions/";

    /**
     * The JAR manifest file name.
     */
    public static final String MANIFEST_NAME = META_INF + "MANIFEST.MF";

    /**
     * Returns the version that represents the unversioned configuration of a
     * multi-release jar file.
     *
     * @return the version that represents the unversioned configuration
     *
     * @since 9
     */
    public static Runtime.Version baseVersion() {
        return BASE_VERSION;
    }

    /**
     * Returns the version that represents the effective runtime versioned
     * configuration of a multi-release jar file.
     * <p>
     * By default the feature version number of the returned {@code Version} will
     * be equal to the feature version number of {@code Runtime.version()}.
     * However, if the {@code jdk.util.jar.version} property is set, the
     * returned {@code Version} is derived from that property and feature version
     * numbers may not be equal.
     *
     * @return the version that represents the runtime versioned configuration
     *
     * @since 9
     */
    public static Runtime.Version runtimeVersion() {
        return RUNTIME_VERSION;
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * file {@code name}. The {@code JarFile} will be verified if
     * it is signed.
     * @param name the name of the jar file to be opened for reading
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(String name) throws IOException {
        this(new File(name), true, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * file {@code name}.
     * @param name the name of the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(String name, boolean verify) throws IOException {
        this(new File(name), verify, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object. The {@code JarFile} will be verified if
     * it is signed.
     * @param file the jar file to be opened for reading
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     */
    public JarFile(File file) throws IOException {
        this(file, true, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object.
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager.
     */
    public JarFile(File file, boolean verify) throws IOException {
        this(file, verify, ZipFile.OPEN_READ);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object in the specified mode.  The mode argument
     * must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     *
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @param mode the mode in which the file is to be opened
     * @throws IOException if an I/O error has occurred
     * @throws IllegalArgumentException
     *         if the {@code mode} argument is invalid
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     * @since 1.3
     */
    public JarFile(File file, boolean verify, int mode) throws IOException {
        this(file, verify, mode, BASE_VERSION);
    }

    /**
     * Creates a new {@code JarFile} to read from the specified
     * {@code File} object in the specified mode.  The mode argument
     * must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     * The version argument, after being converted to a canonical form, is
     * used to configure the {@code JarFile} for processing
     * multi-release jar files.
     * <p>
     * The canonical form derived from the version parameter is
     * {@code Runtime.Version.parse(Integer.toString(n))} where {@code n} is
     * {@code Math.max(version.feature(), JarFile.baseVersion().feature())}.
     *
     * @param file the jar file to be opened for reading
     * @param verify whether or not to verify the jar file if
     * it is signed.
     * @param mode the mode in which the file is to be opened
     * @param version specifies the release version for a multi-release jar file
     * @throws IOException if an I/O error has occurred
     * @throws IllegalArgumentException
     *         if the {@code mode} argument is invalid
     * @throws SecurityException if access to the file is denied
     *         by the SecurityManager
     * @throws NullPointerException if {@code version} is {@code null}
     * @since 9
     */
    public JarFile(File file, boolean verify, int mode, Runtime.Version version) throws IOException {
        super(file, mode);
        this.verify = verify;
        Objects.requireNonNull(version);
        if (MULTI_RELEASE_FORCED || version.feature() == RUNTIME_VERSION.feature()) {
            // This deals with the common case where the value from JarFile.runtimeVersion() is passed
            this.version = RUNTIME_VERSION;
        } else if (version.feature() <= BASE_VERSION_FEATURE) {
            // This also deals with the common case where the value from JarFile.baseVersion() is passed
            this.version = BASE_VERSION;
        } else {
            // Canonicalize
            this.version = Runtime.Version.parse(Integer.toString(version.feature()));
        }
        this.versionFeature = this.version.feature();
    }

    /**
     * Returns the maximum version used when searching for versioned entries.
     * <p>
     * If this {@code JarFile} is not a multi-release jar file or is not
     * configured to be processed as such, then the version returned will be the
     * same as that returned from {@link #baseVersion()}.
     *
     * @return the maximum version
     * @since 9
     */
    public final Runtime.Version getVersion() {
        return isMultiRelease() ? this.version : BASE_VERSION;
    }

    /**
     * Indicates whether or not this jar file is a multi-release jar file.
     *
     * @return true if this JarFile is a multi-release jar file
     * @since 9
     */
    public final boolean isMultiRelease() {
        if (isMultiRelease) {
            return true;
        }
        if (MULTI_RELEASE_ENABLED) {
            try {
                checkForSpecialAttributes();
            } catch (IOException io) {
                isMultiRelease = false;
            }
        }
        return isMultiRelease;
    }

    /**
     * Returns the jar file manifest, or {@code null} if none.
     *
     * @return the jar file manifest, or {@code null} if none
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     * @throws IOException  if an I/O error has occurred
     */
    public Manifest getManifest() throws IOException {
        return getManifestFromReference();
    }

    private Manifest getManifestFromReference() throws IOException {
        Manifest man = manRef != null ? manRef.get() : null;

        if (man == null) {

            JarEntry manEntry = getManEntry();

            // If found then load the manifest
            if (manEntry != null) {
                if (verify) {
                    byte[] b = getBytes(manEntry);
                    if (!jvInitialized) {
                        if (JUZFA.getManifestNum(this) == 1) {
                            jv = new JarVerifier(manEntry.getName(), b);
                        } else {
                            if (JarVerifier.debug != null) {
                                JarVerifier.debug.println("Multiple MANIFEST.MF found. Treat JAR file as unsigned");
                            }
                        }
                    }
                    man = new Manifest(jv, new ByteArrayInputStream(b), getName());
                } else {
                    try (InputStream is = super.getInputStream(manEntry)) {
                        man = new Manifest(is, getName());
                    }
                }
                manRef = new SoftReference<>(man);
            }
        }
        return man;
    }

    /**
     * Returns the {@code JarEntry} for the given base entry name or
     * {@code null} if not found.
     *
     * <p>If this {@code JarFile} is a multi-release jar file and is configured
     * to be processed as such, then a search is performed to find and return
     * a {@code JarEntry} that is the latest versioned entry associated with the
     * given entry name.  The returned {@code JarEntry} is the versioned entry
     * corresponding to the given base entry name prefixed with the string
     * {@code "META-INF/versions/{n}/"}, for the largest value of {@code n} for
     * which an entry exists.  If such a versioned entry does not exist, then
     * the {@code JarEntry} for the base entry is returned, otherwise
     * {@code null} is returned if no entries are found.  The initial value for
     * the version {@code n} is the maximum version as returned by the method
     * {@link JarFile#getVersion()}.
     *
     * @param name the jar file entry name
     * @return the {@code JarEntry} for the given entry name, or
     *         the versioned entry name, or {@code null} if not found
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     *
     * @see java.util.jar.JarEntry
     *
     * @implSpec
     * <div class="block">
     * This implementation invokes {@link JarFile#getEntry(String)}.
     * </div>
     */
    public JarEntry getJarEntry(String name) {
        return (JarEntry)getEntry(name);
    }

    /**
     * Returns the {@code ZipEntry} for the given base entry name or
     * {@code null} if not found.
     *
     * <p>If this {@code JarFile} is a multi-release jar file and is configured
     * to be processed as such, then a search is performed to find and return
     * a {@code ZipEntry} that is the latest versioned entry associated with the
     * given entry name.  The returned {@code ZipEntry} is the versioned entry
     * corresponding to the given base entry name prefixed with the string
     * {@code "META-INF/versions/{n}/"}, for the largest value of {@code n} for
     * which an entry exists.  If such a versioned entry does not exist, then
     * the {@code ZipEntry} for the base entry is returned, otherwise
     * {@code null} is returned if no entries are found.  The initial value for
     * the version {@code n} is the maximum version as returned by the method
     * {@link JarFile#getVersion()}.
     *
     * @param name the jar file entry name
     * @return the {@code ZipEntry} for the given entry name or
     *         the versioned entry name or {@code null} if not found
     *
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     *
     * @see java.util.zip.ZipEntry
     *
     * @implSpec
     * <div class="block">
     * This implementation may return a versioned entry for the requested name
     * even if there is not a corresponding base entry.  This can occur
     * if there is a private or package-private versioned entry that matches.
     * If a subclass overrides this method, assure that the override method
     * invokes {@code super.getEntry(name)} to obtain all versioned entries.
     * </div>
     */
    public ZipEntry getEntry(String name) {
        if (isMultiRelease()) {
            JarEntry je = getVersionedEntry(name, null);
            if (je == null) {
                je = (JarEntry)super.getEntry(name);
            }
            return je;
        } else {
            return super.getEntry(name);
        }
    }

    /**
     * Returns an enumeration of the jar file entries.
     *
     * @return an enumeration of the jar file entries
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     */
    public Enumeration<JarEntry> entries() {
        return JUZFA.entries(this);
    }

    /**
     * Returns an ordered {@code Stream} over the jar file entries.
     * Entries appear in the {@code Stream} in the order they appear in
     * the central directory of the jar file.
     *
     * @return an ordered {@code Stream} of entries in this jar file
     * @throws IllegalStateException if the jar file has been closed
     * @since 1.8
     */
    public Stream<JarEntry> stream() {
        return JUZFA.stream(this);
    }

    /**
     * Returns a {@code Stream} of the versioned jar file entries.
     *
     * <p>If this {@code JarFile} is a multi-release jar file and is configured to
     * be processed as such, then an entry in the stream is the latest versioned entry
     * associated with the corresponding base entry name. The maximum version of the
     * latest versioned entry is the version returned by {@link #getVersion()}.
     * The returned stream may include an entry that only exists as a versioned entry.
     *
     * If the jar file is not a multi-release jar file or the {@code JarFile} is not
     * configured for processing a multi-release jar file, this method returns the
     * same stream that {@link #stream()} returns.
     *
     * @return stream of versioned entries
     * @since 10
     */
    public Stream<JarEntry> versionedStream() {

        if (isMultiRelease()) {
            return JUZFA.entryNameStream(this).map(this::getBasename)
                                              .filter(Objects::nonNull)
                                              .distinct()
                                              .map(this::getJarEntry)
                                              .filter(Objects::nonNull);
        }
        return stream();
    }

    /**
     * Creates a ZipEntry suitable for the given ZipFile.
     */
    JarEntry entryFor(String name) {
        return new JarFileEntry(name);
    }

    private String getBasename(String name) {
        if (name.startsWith(META_INF_VERSIONS)) {
            int off = META_INF_VERSIONS.length();
            int index = name.indexOf('/', off);
            try {
                // filter out dir META-INF/versions/ and META-INF/versions/*/
                // and any entry with version > 'version'
                if (index == -1 || index == (name.length() - 1) ||
                    Integer.parseInt(name, off, index, 10) > versionFeature) {
                    return null;
                }
            } catch (NumberFormatException x) {
                return null; // remove malformed entries silently
            }
            // map to its base name
            return name.substring(index + 1);
        }
        return name;
    }

    private JarEntry getVersionedEntry(String name, JarEntry defaultEntry) {
        if (!name.startsWith(META_INF)) {
            int[] versions = JUZFA.getMetaInfVersions(this);
            if (BASE_VERSION_FEATURE < versionFeature && versions.length > 0) {
                // search for versioned entry
                for (int i = versions.length - 1; i >= 0; i--) {
                    int version = versions[i];
                    // skip versions above versionFeature
                    if (version > versionFeature) {
                        continue;
                    }
                    // skip versions below base version
                    if (version < BASE_VERSION_FEATURE) {
                        break;
                    }
                    JarFileEntry vje = (JarFileEntry)super.getEntry(
                            META_INF_VERSIONS + version + "/" + name);
                    if (vje != null) {
                        return vje.withBasename(name);
                    }
                }
            }
        }
        return defaultEntry;
    }

    // placeholder for now
    String getRealName(JarEntry entry) {
        return entry.getRealName();
    }

    private class JarFileEntry extends JarEntry {
        private String basename;

        JarFileEntry(String name) {
            super(name);
            this.basename = name;
        }

        JarFileEntry(String name, ZipEntry vze) {
            super(vze);
            this.basename = name;
        }

        @Override
        public Attributes getAttributes() throws IOException {
            Manifest man = JarFile.this.getManifest();
            if (man != null) {
                return man.getAttributes(super.getName());
            } else {
                return null;
            }
        }

        @Override
        public Certificate[] getCertificates() {
            try {
                maybeInstantiateVerifier();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (certs == null && jv != null) {
                certs = jv.getCerts(JarFile.this, realEntry());
            }
            return certs == null ? null : certs.clone();
        }

        @Override
        public CodeSigner[] getCodeSigners() {
            try {
                maybeInstantiateVerifier();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (signers == null && jv != null) {
                signers = jv.getCodeSigners(JarFile.this, realEntry());
            }
            return signers == null ? null : signers.clone();
        }

        @Override
        public String getRealName() {
            return super.getName();
        }

        @Override
        public String getName() {
            return basename;
        }

        JarFileEntry realEntry() {
            if (isMultiRelease() && versionFeature != BASE_VERSION_FEATURE) {
                String entryName = super.getName();
                return entryName == basename || entryName.equals(basename) ?
                        this : new JarFileEntry(entryName, this);
            }
            return this;
        }

        // changes the basename, returns "this"
        JarFileEntry withBasename(String name) {
            basename = name;
            return this;
        }
    }

    /*
     * Ensures that the JarVerifier has been created if one is
     * necessary (i.e., the jar appears to be signed.) This is done as
     * a quick check to avoid processing of the manifest for unsigned
     * jars.
     */
    private void maybeInstantiateVerifier() throws IOException {
        if (jv != null) {
            return;
        }

        if (verify) {
            // Gets the manifest name, but only if there are
            // signature-related files. If so we can assume
            // that the jar is signed and that we therefore
            // need a JarVerifier and Manifest
            String name = JUZFA.getManifestName(this, true);
            if (name != null) {
                getManifest();
                return;
            }
            // No signature-related files; don't instantiate a
            // verifier
            verify = false;
        }
    }

    /*
     * Initializes the verifier object by reading all the manifest
     * entries and passing them to the verifier.
     */
    private void initializeVerifier() {
        ManifestEntryVerifier mev = null;

        // Verify "META-INF/" entries...
        try {
            List<String> names = JUZFA.getManifestAndSignatureRelatedFiles(this);
            for (String name : names) {
                JarEntry e = getJarEntry(name);
                byte[] b;
                if (e == null) {
                    throw new JarException("corrupted jar file");
                }
                if (mev == null) {
                    mev = new ManifestEntryVerifier
                        (getManifestFromReference());
                }
                if (name.equalsIgnoreCase(MANIFEST_NAME)) {
                    b = jv.manifestRawBytes;
                } else {
                    b = getBytes(e);
                }
                if (b != null && b.length > 0) {
                    jv.beginEntry(e, mev);
                    jv.update(b.length, b, 0, b.length, mev);
                    jv.update(-1, null, 0, 0, mev);
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            // if we had an error parsing any blocks, just
            // treat the jar file as being unsigned
            jv = null;
            verify = false;
            if (JarVerifier.debug != null) {
                JarVerifier.debug.println("jarfile parsing error!");
                ex.printStackTrace();
            }
        }

        // if after initializing the verifier we have nothing
        // signed, we null it out.

        if (jv != null) {

            jv.doneWithMeta();
            if (JarVerifier.debug != null) {
                JarVerifier.debug.println("done with meta!");
            }

            if (jv.nothingToVerify()) {
                if (JarVerifier.debug != null) {
                    JarVerifier.debug.println("nothing to verify!");
                }
                jv = null;
                verify = false;
            }
        }
    }

    /*
     * Reads all the bytes for a given entry. Used to process the
     * META-INF files.
     */
    private byte[] getBytes(ZipEntry ze) throws IOException {
        try (InputStream is = super.getInputStream(ze)) {
            long uncompressedSize = ze.getSize();
            if (uncompressedSize > MAX_ARRAY_SIZE) {
                throw new OutOfMemoryError("Required array size too large");
            }
            int len = (int)uncompressedSize;
            int bytesRead;
            byte[] b;
            // trust specified entry sizes when reasonably small
            if (len != -1 && len <= 65535) {
                b = new byte[len];
                bytesRead = is.readNBytes(b, 0, len);
            } else {
                b = is.readAllBytes();
                bytesRead = b.length;
            }
            if (len != -1 && len != bytesRead) {
                throw new EOFException("Expected:" + len + ", read:" + bytesRead);
            }
            return b;
        }
    }

    /**
     * Returns an input stream for reading the contents of the specified
     * zip file entry.
     * @param ze the zip file entry
     * @return an input stream for reading the contents of the specified
     *         zip file entry
     * @throws ZipException if a zip file format error has occurred
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if any of the jar file entries
     *         are incorrectly signed.
     * @throws IllegalStateException
     *         may be thrown if the jar file has been closed
     */
    public synchronized InputStream getInputStream(ZipEntry ze)
        throws IOException
    {
        maybeInstantiateVerifier();
        if (jv == null) {
            return super.getInputStream(ze);
        }
        if (!jvInitialized) {
            initializeVerifier();
            jvInitialized = true;
            // could be set to null after a call to
            // initializeVerifier if we have nothing to
            // verify
            if (jv == null)
                return super.getInputStream(ze);
        }

        // wrap a verifier stream around the real stream
        return new JarVerifier.VerifierStream(
            getManifestFromReference(),
            verifiableEntry(ze),
            super.getInputStream(ze),
            jv);
    }

    private JarEntry verifiableEntry(ZipEntry ze) {
        if (ze instanceof JarFileEntry) {
            // assure the name and entry match for verification
            return ((JarFileEntry)ze).realEntry();
        }
        ze = getJarEntry(ze.getName());
        if (ze instanceof JarFileEntry) {
            return ((JarFileEntry)ze).realEntry();
        }
        return (JarEntry)ze;
    }

    // Statics for hand-coded Boyer-Moore search
    private static final byte[] CLASSPATH_CHARS =
            {'C','L','A','S','S','-','P','A','T','H', ':', ' '};

    // The bad character shift for "class-path: "
    private static final byte[] CLASSPATH_LASTOCC;

    // The good suffix shift for "class-path: "
    private static final byte[] CLASSPATH_OPTOSFT;

    private static final byte[] MULTIRELEASE_CHARS =
            {'M','U','L','T','I','-','R','E','L','E', 'A', 'S', 'E', ':',
                    ' ', 'T', 'R', 'U', 'E'};

    // The bad character shift for "multi-release: true"
    private static final byte[] MULTIRELEASE_LASTOCC;

    // The good suffix shift for "multi-release: true"
    private static final byte[] MULTIRELEASE_OPTOSFT;

    static {
        CLASSPATH_LASTOCC = new byte[65];
        CLASSPATH_OPTOSFT = new byte[12];
        CLASSPATH_LASTOCC[(int)'C' - 32] = 1;
        CLASSPATH_LASTOCC[(int)'L' - 32] = 2;
        CLASSPATH_LASTOCC[(int)'S' - 32] = 5;
        CLASSPATH_LASTOCC[(int)'-' - 32] = 6;
        CLASSPATH_LASTOCC[(int)'P' - 32] = 7;
        CLASSPATH_LASTOCC[(int)'A' - 32] = 8;
        CLASSPATH_LASTOCC[(int)'T' - 32] = 9;
        CLASSPATH_LASTOCC[(int)'H' - 32] = 10;
        CLASSPATH_LASTOCC[(int)':' - 32] = 11;
        CLASSPATH_LASTOCC[(int)' ' - 32] = 12;
        for (int i = 0; i < 11; i++) {
            CLASSPATH_OPTOSFT[i] = 12;
        }
        CLASSPATH_OPTOSFT[11] = 1;

        MULTIRELEASE_LASTOCC = new byte[65];
        MULTIRELEASE_OPTOSFT = new byte[19];
        MULTIRELEASE_LASTOCC[(int)'M' - 32] = 1;
        MULTIRELEASE_LASTOCC[(int)'I' - 32] = 5;
        MULTIRELEASE_LASTOCC[(int)'-' - 32] = 6;
        MULTIRELEASE_LASTOCC[(int)'L' - 32] = 9;
        MULTIRELEASE_LASTOCC[(int)'A' - 32] = 11;
        MULTIRELEASE_LASTOCC[(int)'S' - 32] = 12;
        MULTIRELEASE_LASTOCC[(int)':' - 32] = 14;
        MULTIRELEASE_LASTOCC[(int)' ' - 32] = 15;
        MULTIRELEASE_LASTOCC[(int)'T' - 32] = 16;
        MULTIRELEASE_LASTOCC[(int)'R' - 32] = 17;
        MULTIRELEASE_LASTOCC[(int)'U' - 32] = 18;
        MULTIRELEASE_LASTOCC[(int)'E' - 32] = 19;
        for (int i = 0; i < 17; i++) {
            MULTIRELEASE_OPTOSFT[i] = 19;
        }
        MULTIRELEASE_OPTOSFT[17] = 6;
        MULTIRELEASE_OPTOSFT[18] = 1;
    }

    private JarEntry getManEntry() {
        if (manEntry == null) {
            // The manifest entry position is resolved during
            // initialization
            String name = JUZFA.getManifestName(this, false);
            if (name != null) {
                this.manEntry = (JarEntry)super.getEntry(name);
            }
        }
        return manEntry;
    }

   /**
    * Returns {@code true} iff this JAR file has a manifest with the
    * Class-Path attribute
    */
    boolean hasClassPathAttribute() throws IOException {
        checkForSpecialAttributes();
        return hasClassPathAttribute;
    }

    /**
     * Returns true if the pattern {@code src} is found in {@code b}.
     * The {@code lastOcc} array is the precomputed bad character shifts.
     * Since there are no repeated substring in our search strings,
     * the good suffix shifts can be replaced with a comparison.
     */
    private int match(byte[] src, byte[] b, byte[] lastOcc, byte[] optoSft) {
        int len = src.length;
        int last = b.length - len;
        int i = 0;
        next:
        while (i <= last) {
            for (int j = (len - 1); j >= 0; j--) {
                byte c = b[i + j];
                if (c >= ' ' && c <= 'z') {
                    if (c >= 'a') c -= 32; // Canonicalize

                    if (c != src[j]) {
                        // no match
                        int badShift = lastOcc[c - 32];
                        i += Math.max(j + 1 - badShift, optoSft[j]);
                        continue next;
                    }
                } else {
                    // no match, character not valid for name
                    i += len;
                    continue next;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * On first invocation, check if the JAR file has the Class-Path
     * and the Multi-Release attribute. A no-op on subsequent calls.
     */
    private void checkForSpecialAttributes() throws IOException {
        if (hasCheckedSpecialAttributes) {
            return;
        }
        synchronized (this) {
            if (hasCheckedSpecialAttributes) {
                return;
            }
            JarEntry manEntry = getManEntry();
            if (manEntry != null) {
                byte[] b = getBytes(manEntry);
                hasClassPathAttribute = match(CLASSPATH_CHARS, b,
                        CLASSPATH_LASTOCC, CLASSPATH_OPTOSFT) != -1;
                // is this a multi-release jar file
                if (MULTI_RELEASE_ENABLED) {
                    int i = match(MULTIRELEASE_CHARS, b, MULTIRELEASE_LASTOCC,
                            MULTIRELEASE_OPTOSFT);
                    if (i != -1) {
                        // Read the main attributes of the manifest
                        byte[] lbuf = new byte[512];
                        Attributes attr = new Attributes();
                        attr.read(new Manifest.FastInputStream(
                                new ByteArrayInputStream(b)), lbuf);
                        isMultiRelease = Boolean.parseBoolean(
                            attr.getValue(Attributes.Name.MULTI_RELEASE));
                    }
                }
            }
            hasCheckedSpecialAttributes = true;
        }
    }

    synchronized void ensureInitialization() {
        try {
            maybeInstantiateVerifier();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (jv != null && !jvInitialized) {
            isInitializing.set(Boolean.TRUE);
            try {
                initializeVerifier();
                jvInitialized = true;
            } finally {
                isInitializing.set(Boolean.FALSE);
            }
        }
    }

    static boolean isInitializing() {
        Boolean value = isInitializing.get();
        return (value == null) ? false : value;
    }

    /*
     * Returns a versioned {@code JarFileEntry} for the given entry,
     * if there is one. Otherwise returns the original entry. This
     * is invoked by the {@code entries2} for verifier.
     */
    JarEntry newEntry(JarEntry je) {
        if (isMultiRelease()) {
            return getVersionedEntry(je.getName(), je);
        }
        return je;
    }

    /*
     * Returns a versioned {@code JarFileEntry} for the given entry
     * name, if there is one. Otherwise returns a {@code JarFileEntry}
     * with the given name. It is invoked from JarVerifier's entries2
     * for {@code singers}.
     */
    JarEntry newEntry(String name) {
        if (isMultiRelease()) {
            JarEntry vje = getVersionedEntry(name, null);
            if (vje != null) {
                return vje;
            }
        }
        return new JarFileEntry(name);
    }

    Enumeration<String> entryNames(CodeSource[] cs) {
        ensureInitialization();
        if (jv != null) {
            return jv.entryNames(this, cs);
        }

        /*
         * JAR file has no signed content. Is there a non-signing
         * code source?
         */
        boolean includeUnsigned = false;
        for (CodeSource c : cs) {
            if (c.getCodeSigners() == null) {
                includeUnsigned = true;
                break;
            }
        }
        if (includeUnsigned) {
            return unsignedEntryNames();
        } else {
            return Collections.emptyEnumeration();
        }
    }

    /**
     * Returns an enumeration of the zip file entries
     * excluding internal JAR mechanism entries and including
     * signed entries missing from the ZIP directory.
     */
    Enumeration<JarEntry> entries2() {
        ensureInitialization();
        if (jv != null) {
            return jv.entries2(this, JUZFA.entries(JarFile.this));
        }

        // screen out entries which are never signed
        final var unfilteredEntries = JUZFA.entries(JarFile.this);

        return new Enumeration<>() {

            JarEntry entry;

            public boolean hasMoreElements() {
                if (entry != null) {
                    return true;
                }
                while (unfilteredEntries.hasMoreElements()) {
                    JarEntry je = unfilteredEntries.nextElement();
                    if (JarVerifier.isSigningRelated(je.getName())) {
                        continue;
                    }
                    entry = je;
                    return true;
                }
                return false;
            }

            public JarEntry nextElement() {
                if (hasMoreElements()) {
                    JarEntry je = entry;
                    entry = null;
                    return newEntry(je);
                }
                throw new NoSuchElementException();
            }
        };
    }

    CodeSource[] getCodeSources(URL url) {
        ensureInitialization();
        if (jv != null) {
            return jv.getCodeSources(this, url);
        }

        /*
         * JAR file has no signed content. Is there a non-signing
         * code source?
         */
        Enumeration<String> unsigned = unsignedEntryNames();
        if (unsigned.hasMoreElements()) {
            return new CodeSource[]{JarVerifier.getUnsignedCS(url)};
        } else {
            return null;
        }
    }

    private Enumeration<String> unsignedEntryNames() {
        final Enumeration<JarEntry> entries = entries();
        return new Enumeration<>() {

            String name;

            /*
             * Grab entries from ZIP directory but screen out
             * metadata.
             */
            public boolean hasMoreElements() {
                if (name != null) {
                    return true;
                }
                while (entries.hasMoreElements()) {
                    String value;
                    ZipEntry e = entries.nextElement();
                    value = e.getName();
                    if (e.isDirectory() || JarVerifier.isSigningRelated(value)) {
                        continue;
                    }
                    name = value;
                    return true;
                }
                return false;
            }

            public String nextElement() {
                if (hasMoreElements()) {
                    String value = name;
                    name = null;
                    return value;
                }
                throw new NoSuchElementException();
            }
        };
    }

    CodeSource getCodeSource(URL url, String name) {
        ensureInitialization();
        if (jv != null) {
            if (jv.eagerValidation) {
                CodeSource cs;
                JarEntry je = getJarEntry(name);
                if (je != null) {
                    cs = jv.getCodeSource(url, this, je);
                } else {
                    cs = jv.getCodeSource(url, name);
                }
                return cs;
            } else {
                return jv.getCodeSource(url, name);
            }
        }

        return JarVerifier.getUnsignedCS(url);
    }

    void setEagerValidation(boolean eager) {
        try {
            maybeInstantiateVerifier();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (jv != null) {
            jv.setEagerValidation(eager);
        }
    }

    List<Object> getManifestDigests() {
        ensureInitialization();
        if (jv != null) {
            return jv.getManifestDigests();
        }
        return new ArrayList<>();
    }
}
