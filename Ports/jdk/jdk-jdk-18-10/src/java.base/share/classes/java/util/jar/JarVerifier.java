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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.zip.ZipEntry;

import jdk.internal.util.jar.JarIndex;
import sun.security.util.ManifestDigester;
import sun.security.util.ManifestEntryVerifier;
import sun.security.util.SignatureFileVerifier;
import sun.security.util.Debug;

/**
 *
 * @author      Roland Schemers
 */
class JarVerifier {

    /* Are we debugging ? */
    static final Debug debug = Debug.getInstance("jar");

    /* a table mapping names to code signers, for jar entries that have
       had their actual hashes verified */
    private Hashtable<String, CodeSigner[]> verifiedSigners;

    /* a table mapping names to code signers, for jar entries that have
       passed the .SF/.DSA/.EC -> MANIFEST check */
    private Hashtable<String, CodeSigner[]> sigFileSigners;

    /* a hash table to hold .SF bytes */
    private Hashtable<String, byte[]> sigFileData;

    /** "queue" of pending PKCS7 blocks that we couldn't parse
     *  until we parsed the .SF file */
    private ArrayList<SignatureFileVerifier> pendingBlocks;

    /* cache of CodeSigner objects */
    private ArrayList<CodeSigner[]> signerCache;

    /* Are we parsing a block? */
    private boolean parsingBlockOrSF = false;

    /* Are we done parsing META-INF entries? */
    private boolean parsingMeta = true;

    /* Are there are files to verify? */
    private boolean anyToVerify = true;

    /* The output stream to use when keeping track of files we are interested
       in */
    private ByteArrayOutputStream baos;

    /** The ManifestDigester object */
    private volatile ManifestDigester manDig;

    /** the bytes for the manDig object */
    byte manifestRawBytes[] = null;

    /** the manifest name this JarVerifier is created upon */
    final String manifestName;

    /** controls eager signature validation */
    boolean eagerValidation;

    /** makes code source singleton instances unique to us */
    private Object csdomain = new Object();

    /** collect -DIGEST-MANIFEST values for deny list */
    private List<Object> manifestDigests;

    public JarVerifier(String name, byte rawBytes[]) {
        manifestName = name;
        manifestRawBytes = rawBytes;
        sigFileSigners = new Hashtable<>();
        verifiedSigners = new Hashtable<>();
        sigFileData = new Hashtable<>(11);
        pendingBlocks = new ArrayList<>();
        baos = new ByteArrayOutputStream();
        manifestDigests = new ArrayList<>();
    }

    /**
     * This method scans to see which entry we're parsing and
     * keeps various state information depending on what type of
     * file is being parsed.
     */
    public void beginEntry(JarEntry je, ManifestEntryVerifier mev)
        throws IOException
    {
        if (je == null)
            return;

        if (debug != null) {
            debug.println("beginEntry "+je.getName());
        }

        String name = je.getName();

        /*
         * Assumptions:
         * 1. The manifest should be the first entry in the META-INF directory.
         * 2. The .SF/.DSA/.EC files follow the manifest, before any normal entries
         * 3. Any of the following will throw a SecurityException:
         *    a. digest mismatch between a manifest section and
         *       the SF section.
         *    b. digest mismatch between the actual jar entry and the manifest
         */

        if (parsingMeta) {
            String uname = name.toUpperCase(Locale.ENGLISH);
            if ((uname.startsWith("META-INF/") ||
                 uname.startsWith("/META-INF/"))) {

                if (je.isDirectory()) {
                    mev.setEntry(null, je);
                    return;
                }

                if (uname.equals(JarFile.MANIFEST_NAME) ||
                        uname.equals(JarIndex.INDEX_NAME)) {
                    return;
                }

                if (SignatureFileVerifier.isBlockOrSF(uname)) {
                    /* We parse only DSA, RSA or EC PKCS7 blocks. */
                    parsingBlockOrSF = true;
                    baos.reset();
                    mev.setEntry(null, je);
                    return;
                }

                // If a META-INF entry is not MF or block or SF, they should
                // be normal entries. According to 2 above, no more block or
                // SF will appear. Let's doneWithMeta.
            }
        }

        if (parsingMeta) {
            doneWithMeta();
        }

        if (je.isDirectory()) {
            mev.setEntry(null, je);
            return;
        }

        // be liberal in what you accept. If the name starts with ./, remove
        // it as we internally canonicalize it with out the ./.
        if (name.startsWith("./"))
            name = name.substring(2);

        // be liberal in what you accept. If the name starts with /, remove
        // it as we internally canonicalize it with out the /.
        if (name.startsWith("/"))
            name = name.substring(1);

        // only set the jev object for entries that have a signature
        // (either verified or not)
        if (!name.equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
            if (sigFileSigners.get(name) != null ||
                    verifiedSigners.get(name) != null) {
                mev.setEntry(name, je);
                return;
            }
        }

        // don't compute the digest for this entry
        mev.setEntry(null, je);

        return;
    }

    /**
     * update a single byte.
     */

    public void update(int b, ManifestEntryVerifier mev)
        throws IOException
    {
        if (b != -1) {
            if (parsingBlockOrSF) {
                baos.write(b);
            } else {
                mev.update((byte)b);
            }
        } else {
            processEntry(mev);
        }
    }

    /**
     * update an array of bytes.
     */

    public void update(int n, byte[] b, int off, int len,
                       ManifestEntryVerifier mev)
        throws IOException
    {
        if (n != -1) {
            if (parsingBlockOrSF) {
                baos.write(b, off, n);
            } else {
                mev.update(b, off, n);
            }
        } else {
            processEntry(mev);
        }
    }

    /**
     * called when we reach the end of entry in one of the read() methods.
     */
    private void processEntry(ManifestEntryVerifier mev)
        throws IOException
    {
        if (!parsingBlockOrSF) {
            JarEntry je = mev.getEntry();
            if ((je != null) && (je.signers == null)) {
                je.signers = mev.verify(verifiedSigners, sigFileSigners);
                je.certs = mapSignersToCertArray(je.signers);
            }
        } else {

            try {
                parsingBlockOrSF = false;

                if (debug != null) {
                    debug.println("processEntry: processing block");
                }

                String uname = mev.getEntry().getName()
                                             .toUpperCase(Locale.ENGLISH);

                if (uname.endsWith(".SF")) {
                    String key = uname.substring(0, uname.length()-3);
                    byte bytes[] = baos.toByteArray();
                    // add to sigFileData in case future blocks need it
                    sigFileData.put(key, bytes);
                    // check pending blocks, we can now process
                    // anyone waiting for this .SF file
                    for (SignatureFileVerifier sfv : pendingBlocks) {
                        if (sfv.needSignatureFile(key)) {
                            if (debug != null) {
                                debug.println(
                                 "processEntry: processing pending block");
                            }

                            sfv.setSignatureFile(bytes);
                            sfv.process(sigFileSigners, manifestDigests,
                                    manifestName);
                        }
                    }
                    return;
                }

                // now we are parsing a signature block file

                String key = uname.substring(0, uname.lastIndexOf('.'));

                if (signerCache == null)
                    signerCache = new ArrayList<>();

                if (manDig == null) {
                    synchronized(manifestRawBytes) {
                        if (manDig == null) {
                            manDig = new ManifestDigester(manifestRawBytes);
                            manifestRawBytes = null;
                        }
                    }
                }

                SignatureFileVerifier sfv =
                  new SignatureFileVerifier(signerCache,
                                            manDig, uname, baos.toByteArray());

                if (sfv.needSignatureFileBytes()) {
                    // see if we have already parsed an external .SF file
                    byte[] bytes = sigFileData.get(key);

                    if (bytes == null) {
                        // put this block on queue for later processing
                        // since we don't have the .SF bytes yet
                        // (uname, block);
                        if (debug != null) {
                            debug.println("adding pending block");
                        }
                        pendingBlocks.add(sfv);
                        return;
                    } else {
                        sfv.setSignatureFile(bytes);
                    }
                }
                sfv.process(sigFileSigners, manifestDigests, manifestName);

            } catch (IOException | CertificateException |
                    NoSuchAlgorithmException | SignatureException e) {
                if (debug != null) debug.println("processEntry caught: "+e);
                // ignore and treat as unsigned
            }
        }
    }

    /**
     * Return an array of java.security.cert.Certificate objects for
     * the given file in the jar.
     * @deprecated
     */
    @Deprecated
    public java.security.cert.Certificate[] getCerts(String name)
    {
        return mapSignersToCertArray(getCodeSigners(name));
    }

    public java.security.cert.Certificate[] getCerts(JarFile jar, JarEntry entry)
    {
        return mapSignersToCertArray(getCodeSigners(jar, entry));
    }

    /**
     * return an array of CodeSigner objects for
     * the given file in the jar. this array is not cloned.
     *
     */
    public CodeSigner[] getCodeSigners(String name)
    {
        return verifiedSigners.get(name);
    }

    public CodeSigner[] getCodeSigners(JarFile jar, JarEntry entry)
    {
        String name = entry.getName();
        if (eagerValidation && sigFileSigners.get(name) != null) {
            /*
             * Force a read of the entry data to generate the
             * verification hash.
             */
            try {
                InputStream s = jar.getInputStream(entry);
                byte[] buffer = new byte[1024];
                int n = buffer.length;
                while (n != -1) {
                    n = s.read(buffer, 0, buffer.length);
                }
                s.close();
            } catch (IOException e) {
            }
        }
        return getCodeSigners(name);
    }

    /*
     * Convert an array of signers into an array of concatenated certificate
     * arrays.
     */
    private static java.security.cert.Certificate[] mapSignersToCertArray(
        CodeSigner[] signers) {

        if (signers != null) {
            ArrayList<java.security.cert.Certificate> certChains = new ArrayList<>();
            for (CodeSigner signer : signers) {
                certChains.addAll(
                    signer.getSignerCertPath().getCertificates());
            }

            // Convert into a Certificate[]
            return certChains.toArray(
                    new java.security.cert.Certificate[certChains.size()]);
        }
        return null;
    }

    /**
     * returns true if there no files to verify.
     * should only be called after all the META-INF entries
     * have been processed.
     */
    boolean nothingToVerify()
    {
        return (anyToVerify == false);
    }

    /**
     * called to let us know we have processed all the
     * META-INF entries, and if we re-read one of them, don't
     * re-process it. Also gets rid of any data structures
     * we needed when parsing META-INF entries.
     */
    void doneWithMeta()
    {
        parsingMeta = false;
        anyToVerify = !sigFileSigners.isEmpty();
        baos = null;
        sigFileData = null;
        pendingBlocks = null;
        signerCache = null;
        manDig = null;
        // MANIFEST.MF is always treated as signed and verified,
        // move its signers from sigFileSigners to verifiedSigners.
        if (sigFileSigners.containsKey(manifestName)) {
            CodeSigner[] codeSigners = sigFileSigners.remove(manifestName);
            verifiedSigners.put(manifestName, codeSigners);
        }
    }

    static class VerifierStream extends java.io.InputStream {

        private InputStream is;
        private JarVerifier jv;
        private ManifestEntryVerifier mev;
        private long numLeft;

        VerifierStream(Manifest man,
                       JarEntry je,
                       InputStream is,
                       JarVerifier jv) throws IOException
        {
            this.is = Objects.requireNonNull(is);
            this.jv = jv;
            this.mev = new ManifestEntryVerifier(man);
            this.jv.beginEntry(je, mev);
            this.numLeft = je.getSize();
            if (this.numLeft == 0)
                this.jv.update(-1, this.mev);
        }

        public int read() throws IOException
        {
            ensureOpen();
            if (numLeft > 0) {
                int b = is.read();
                jv.update(b, mev);
                numLeft--;
                if (numLeft == 0)
                    jv.update(-1, mev);
                return b;
            } else {
                return -1;
            }
        }

        public int read(byte b[], int off, int len) throws IOException {
            ensureOpen();
            if ((numLeft > 0) && (numLeft < len)) {
                len = (int)numLeft;
            }

            if (numLeft > 0) {
                int n = is.read(b, off, len);
                jv.update(n, b, off, len, mev);
                numLeft -= n;
                if (numLeft == 0)
                    jv.update(-1, b, off, len, mev);
                return n;
            } else {
                return -1;
            }
        }

        public void close()
            throws IOException
        {
            if (is != null)
                is.close();
            is = null;
            mev = null;
            jv = null;
        }

        public int available() throws IOException {
            ensureOpen();
            return is.available();
        }

        private void ensureOpen() throws IOException {
            if (is == null) {
                throw new IOException("stream closed");
            }
        }
    }

    // Extended JavaUtilJarAccess CodeSource API Support

    private Map<URL, Map<CodeSigner[], CodeSource>> urlToCodeSourceMap = new HashMap<>();
    private Map<CodeSigner[], CodeSource> signerToCodeSource = new HashMap<>();
    private URL lastURL;
    private Map<CodeSigner[], CodeSource> lastURLMap;

    /*
     * Create a unique mapping from codeSigner cache entries to CodeSource.
     * In theory, multiple URLs origins could map to a single locally cached
     * and shared JAR file although in practice there will be a single URL in use.
     */
    private synchronized CodeSource mapSignersToCodeSource(URL url, CodeSigner[] signers) {
        Map<CodeSigner[], CodeSource> map;
        if (url == lastURL) {
            map = lastURLMap;
        } else {
            map = urlToCodeSourceMap.get(url);
            if (map == null) {
                map = new HashMap<>();
                urlToCodeSourceMap.put(url, map);
            }
            lastURLMap = map;
            lastURL = url;
        }
        CodeSource cs = map.get(signers);
        if (cs == null) {
            cs = new VerifierCodeSource(csdomain, url, signers);
            signerToCodeSource.put(signers, cs);
        }
        return cs;
    }

    private CodeSource[] mapSignersToCodeSources(URL url, List<CodeSigner[]> signers, boolean unsigned) {
        List<CodeSource> sources = new ArrayList<>();

        for (CodeSigner[] signer : signers) {
            sources.add(mapSignersToCodeSource(url, signer));
        }
        if (unsigned) {
            sources.add(mapSignersToCodeSource(url, null));
        }
        return sources.toArray(new CodeSource[sources.size()]);
    }
    private CodeSigner[] emptySigner = new CodeSigner[0];

    /*
     * Match CodeSource to a CodeSigner[] in the signer cache.
     */
    private CodeSigner[] findMatchingSigners(CodeSource cs) {
        if (cs instanceof VerifierCodeSource vcs) {
            if (vcs.isSameDomain(csdomain)) {
                return vcs.getPrivateSigners();
            }
        }

        /*
         * In practice signers should always be optimized above
         * but this handles a CodeSource of any type, just in case.
         */
        CodeSource[] sources = mapSignersToCodeSources(cs.getLocation(), getJarCodeSigners(), true);
        List<CodeSource> sourceList = new ArrayList<>();
        for (CodeSource source : sources) {
            sourceList.add(source);
        }
        int j = sourceList.indexOf(cs);
        if (j != -1) {
            CodeSigner[] match;
            match = ((VerifierCodeSource) sourceList.get(j)).getPrivateSigners();
            if (match == null) {
                match = emptySigner;
            }
            return match;
        }
        return null;
    }

    /*
     * Instances of this class hold uncopied references to internal
     * signing data that can be compared by object reference identity.
     */
    private static class VerifierCodeSource extends CodeSource {
        @java.io.Serial
        private static final long serialVersionUID = -9047366145967768825L;

        URL vlocation;
        CodeSigner[] vsigners;
        java.security.cert.Certificate[] vcerts;
        @SuppressWarnings("serial") // Not statically typed as Serializable
        Object csdomain;

        VerifierCodeSource(Object csdomain, URL location, CodeSigner[] signers) {
            super(location, signers);
            this.csdomain = csdomain;
            vlocation = location;
            vsigners = signers; // from signerCache
        }

        VerifierCodeSource(Object csdomain, URL location, java.security.cert.Certificate[] certs) {
            super(location, certs);
            this.csdomain = csdomain;
            vlocation = location;
            vcerts = certs; // from signerCache
        }

        /*
         * All VerifierCodeSource instances are constructed based on
         * singleton signerCache or signerCacheCert entries for each unique signer.
         * No CodeSigner<->Certificate[] conversion is required.
         * We use these assumptions to optimize equality comparisons.
         */
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof VerifierCodeSource that) {

                /*
                 * Only compare against other per-signer singletons constructed
                 * on behalf of the same JarFile instance. Otherwise, compare
                 * things the slower way.
                 */
                if (isSameDomain(that.csdomain)) {
                    if (that.vsigners != this.vsigners
                            || that.vcerts != this.vcerts) {
                        return false;
                    }
                    if (that.vlocation != null) {
                        return that.vlocation.equals(this.vlocation);
                    } else if (this.vlocation != null) {
                        return this.vlocation.equals(that.vlocation);
                    } else { // both null
                        return true;
                    }
                }
            }
            return super.equals(obj);
        }

        boolean isSameDomain(Object csdomain) {
            return this.csdomain == csdomain;
        }

        private CodeSigner[] getPrivateSigners() {
            return vsigners;
        }

        private java.security.cert.Certificate[] getPrivateCertificates() {
            return vcerts;
        }
    }
    private Map<String, CodeSigner[]> signerMap;

    private synchronized Map<String, CodeSigner[]> signerMap() {
        if (signerMap == null) {
            /*
             * Snapshot signer state so it doesn't change on us. We care
             * only about the asserted signatures. Verification of
             * signature validity happens via the JarEntry apis.
             */
            signerMap = new HashMap<>(verifiedSigners.size() + sigFileSigners.size());
            signerMap.putAll(verifiedSigners);
            signerMap.putAll(sigFileSigners);
        }
        return signerMap;
    }

    public synchronized Enumeration<String> entryNames(JarFile jar, final CodeSource[] cs) {
        final Map<String, CodeSigner[]> map = signerMap();
        final Iterator<Map.Entry<String, CodeSigner[]>> itor = map.entrySet().iterator();
        boolean matchUnsigned = false;

        /*
         * Grab a single copy of the CodeSigner arrays. Check
         * to see if we can optimize CodeSigner equality test.
         */
        List<CodeSigner[]> req = new ArrayList<>(cs.length);
        for (CodeSource c : cs) {
            CodeSigner[] match = findMatchingSigners(c);
            if (match != null) {
                if (match.length > 0) {
                    req.add(match);
                } else {
                    matchUnsigned = true;
                }
            } else {
                matchUnsigned = true;
            }
        }

        final List<CodeSigner[]> signersReq = req;
        final Enumeration<String> enum2 = matchUnsigned ? unsignedEntryNames(jar) : Collections.emptyEnumeration();

        return new Enumeration<>() {

            String name;

            public boolean hasMoreElements() {
                if (name != null) {
                    return true;
                }

                while (itor.hasNext()) {
                    Map.Entry<String, CodeSigner[]> e = itor.next();
                    if (signersReq.contains(e.getValue())) {
                        name = e.getKey();
                        return true;
                    }
                }
                while (enum2.hasMoreElements()) {
                    name = enum2.nextElement();
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

    /*
     * Like entries() but screens out internal JAR mechanism entries
     * and includes signed entries with no ZIP data.
     */
    public Enumeration<JarEntry> entries2(final JarFile jar, Enumeration<JarEntry> e) {
        final Map<String, CodeSigner[]> map = new HashMap<>();
        map.putAll(signerMap());
        final Enumeration<JarEntry> enum_ = e;
        return new Enumeration<>() {

            Enumeration<String> signers = null;
            JarEntry entry;

            public boolean hasMoreElements() {
                if (entry != null) {
                    return true;
                }
                while (enum_.hasMoreElements()) {
                    JarEntry je = enum_.nextElement();
                    if (JarVerifier.isSigningRelated(je.getName())) {
                        continue;
                    }
                    entry = jar.newEntry(je);
                    return true;
                }
                if (signers == null) {
                    signers = Collections.enumeration(map.keySet());
                }
                while (signers.hasMoreElements()) {
                    String name = signers.nextElement();
                    entry = jar.newEntry(name);
                    return true;
                }

                // Any map entries left?
                return false;
            }

            public JarEntry nextElement() {
                if (hasMoreElements()) {
                    JarEntry je = entry;
                    map.remove(je.getName());
                    entry = null;
                    return je;
                }
                throw new NoSuchElementException();
            }
        };
    }

    // true if file is part of the signature mechanism itself
    static boolean isSigningRelated(String name) {
        return SignatureFileVerifier.isSigningRelated(name);
    }

    private Enumeration<String> unsignedEntryNames(JarFile jar) {
        final Map<String, CodeSigner[]> map = signerMap();
        final Enumeration<JarEntry> entries = jar.entries();
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
                    if (e.isDirectory() || isSigningRelated(value)) {
                        continue;
                    }
                    if (map.get(value) == null) {
                        name = value;
                        return true;
                    }
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
    private List<CodeSigner[]> jarCodeSigners;

    private synchronized List<CodeSigner[]> getJarCodeSigners() {
        CodeSigner[] signers;
        if (jarCodeSigners == null) {
            HashSet<CodeSigner[]> set = new HashSet<>();
            set.addAll(signerMap().values());
            jarCodeSigners = new ArrayList<>();
            jarCodeSigners.addAll(set);
        }
        return jarCodeSigners;
    }

    public synchronized CodeSource[] getCodeSources(JarFile jar, URL url) {
        boolean hasUnsigned = unsignedEntryNames(jar).hasMoreElements();

        return mapSignersToCodeSources(url, getJarCodeSigners(), hasUnsigned);
    }

    public CodeSource getCodeSource(URL url, String name) {
        CodeSigner[] signers;

        signers = signerMap().get(name);
        return mapSignersToCodeSource(url, signers);
    }

    public CodeSource getCodeSource(URL url, JarFile jar, JarEntry je) {
        CodeSigner[] signers;

        return mapSignersToCodeSource(url, getCodeSigners(jar, je));
    }

    public void setEagerValidation(boolean eager) {
        eagerValidation = eager;
    }

    public synchronized List<Object> getManifestDigests() {
        return Collections.unmodifiableList(manifestDigests);
    }

    static CodeSource getUnsignedCS(URL url) {
        return new VerifierCodeSource(null, url, (java.security.cert.Certificate[]) null);
    }

    /**
     * Returns whether the name is trusted. Used by
     * {@link Manifest#getTrustedAttributes(String)}.
     */
    boolean isTrustedManifestEntry(String name) {
        // How many signers? MANIFEST.MF is always verified
        CodeSigner[] forMan = verifiedSigners.get(manifestName);
        if (forMan == null) {
            return true;
        }
        // Check sigFileSigners first, because we are mainly dealing with
        // non-file entries which will stay in sigFileSigners forever.
        CodeSigner[] forName = sigFileSigners.get(name);
        if (forName == null) {
            forName = verifiedSigners.get(name);
        }
        // Returns trusted if all signers sign the entry
        return forName != null && forName.length == forMan.length;
    }
}
