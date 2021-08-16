/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.security.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

import sun.security.jca.Providers;
import sun.security.util.DisabledAlgorithmConstraints;
import sun.security.util.JarConstraintsParameters;

/**
 * This class is used to verify each entry in a jar file with its
 * manifest value.
 */

public class ManifestEntryVerifier {

    private static final Debug debug = Debug.getInstance("jar");

    /**
     * Holder class to lazily load Sun provider. NOTE: if
     * Providers.getSunProvider returned a cached provider, we could avoid the
     * need for caching the provider with this holder class; we should try to
     * revisit this in JDK 8.
     */
    private static class SunProviderHolder {
        private static final Provider instance = Providers.getSunProvider();
    }

    /** the created digest objects */
    HashMap<String, MessageDigest> createdDigests;

    /** the digests in use for a given entry*/
    ArrayList<MessageDigest> digests;

    /** the manifest hashes for the digests in use */
    ArrayList<byte[]> manifestHashes;

    private String name = null;
    private Manifest man;

    private boolean skip = true;

    private JarEntry entry;

    private CodeSigner[] signers = null;

    /**
     * Create a new ManifestEntryVerifier object.
     */
    public ManifestEntryVerifier(Manifest man)
    {
        createdDigests = new HashMap<>(11);
        digests = new ArrayList<>();
        manifestHashes = new ArrayList<>();
        this.man = man;
    }

    /**
     * Find the hashes in the
     * manifest for this entry, save them, and set the MessageDigest
     * objects to calculate the hashes on the fly. If name is
     * null it signifies that update/verify should ignore this entry.
     */
    public void setEntry(String name, JarEntry entry)
        throws IOException
    {
        digests.clear();
        manifestHashes.clear();
        this.name = name;
        this.entry = entry;

        skip = true;
        signers = null;

        if (man == null || name == null) {
            return;
        }

        /* get the headers from the manifest for this entry */
        /* if there aren't any, we can't verify any digests for this entry */

        skip = false;

        Attributes attr = man.getAttributes(name);
        if (attr == null) {
            // ugh. we should be able to remove this at some point.
            // there are broken jars floating around with ./name and /name
            // in the manifest, and "name" in the zip/jar file.
            attr = man.getAttributes("./"+name);
            if (attr == null) {
                attr = man.getAttributes("/"+name);
                if (attr == null)
                    return;
            }
        }

        for (Map.Entry<Object,Object> se : attr.entrySet()) {
            String key = se.getKey().toString();

            if (key.toUpperCase(Locale.ENGLISH).endsWith("-DIGEST")) {
                // 7 is length of "-Digest"
                String algorithm = key.substring(0, key.length()-7);

                MessageDigest digest = createdDigests.get(algorithm);

                if (digest == null) {
                    try {

                        digest = MessageDigest.getInstance
                                        (algorithm, SunProviderHolder.instance);
                        createdDigests.put(algorithm, digest);
                    } catch (NoSuchAlgorithmException nsae) {
                        // ignore
                    }
                }

                if (digest != null) {
                    digest.reset();
                    digests.add(digest);
                    manifestHashes.add(
                                Base64.getMimeDecoder().decode((String)se.getValue()));
                }
            }
        }
    }

    /**
     * update the digests for the digests we are interested in
     */
    public void update(byte buffer) {
        if (skip) return;

        for (int i=0; i < digests.size(); i++) {
            digests.get(i).update(buffer);
        }
    }

    /**
     * update the digests for the digests we are interested in
     */
    public void update(byte[] buffer, int off, int len) {
        if (skip) return;

        for (int i=0; i < digests.size(); i++) {
            digests.get(i).update(buffer, off, len);
        }
    }

    /**
     * get the JarEntry for this object
     */
    public JarEntry getEntry()
    {
        return entry;
    }

    /**
     * go through all the digests, calculating the final digest
     * and comparing it to the one in the manifest. If this is
     * the first time we have verified this object, remove its
     * code signers from sigFileSigners and place in verifiedSigners.
     *
     *
     */
    public CodeSigner[] verify(Hashtable<String, CodeSigner[]> verifiedSigners,
                Hashtable<String, CodeSigner[]> sigFileSigners)
        throws JarException
    {
        if (skip) {
            return null;
        }

        if (digests.isEmpty()) {
            throw new SecurityException("digest missing for " + name);
        }

        if (signers != null) {
            return signers;
        }

        JarConstraintsParameters params =
            getParams(verifiedSigners, sigFileSigners);

        for (int i=0; i < digests.size(); i++) {

            MessageDigest digest = digests.get(i);
            if (params != null) {
                try {
                    params.setExtendedExceptionMsg(JarFile.MANIFEST_NAME,
                        name + " entry");
                    DisabledAlgorithmConstraints.jarConstraints()
                           .permits(digest.getAlgorithm(), params);
                } catch (GeneralSecurityException e) {
                    if (debug != null) {
                        debug.println("Digest algorithm is restricted: " + e);
                    }
                    return null;
                }
            }
            byte [] manHash = manifestHashes.get(i);
            byte [] theHash = digest.digest();

            if (debug != null) {
                debug.println("Manifest Entry: " +
                                   name + " digest=" + digest.getAlgorithm());
                debug.println("  manifest " + HexFormat.of().formatHex(manHash));
                debug.println("  computed " + HexFormat.of().formatHex(theHash));
                debug.println();
            }

            if (!MessageDigest.isEqual(theHash, manHash))
                throw new SecurityException(digest.getAlgorithm()+
                                            " digest error for "+name);
        }

        // take it out of sigFileSigners and put it in verifiedSigners...
        signers = sigFileSigners.remove(name);
        if (signers != null) {
            verifiedSigners.put(name, signers);
        }
        return signers;
    }

    /**
     * Get constraints parameters for JAR. The constraints should be
     * checked against all code signers. Returns the parameters,
     * or null if the signers for this entry have already been checked.
     */
    private JarConstraintsParameters getParams(
            Map<String, CodeSigner[]> verifiedSigners,
            Map<String, CodeSigner[]> sigFileSigners) {

        // verifiedSigners is usually preloaded with the Manifest's signers.
        // If verifiedSigners contains the Manifest, then it will have all of
        // the signers of the JAR. But if it doesn't then we need to fallback
        // and check verifiedSigners to see if the signers of this entry have
        // been checked already.
        if (verifiedSigners.containsKey(JarFile.MANIFEST_NAME)) {
            if (verifiedSigners.size() > 1) {
                // this means we already checked it previously
                return null;
            } else {
                return new JarConstraintsParameters(
                    verifiedSigners.get(JarFile.MANIFEST_NAME));
            }
        } else {
            CodeSigner[] signers = sigFileSigners.get(name);
            if (verifiedSigners.containsValue(signers)) {
                return null;
            } else {
                return new JarConstraintsParameters(signers);
            }
        }
    }
}

