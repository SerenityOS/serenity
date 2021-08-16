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

package jdk.internal.module;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * The result of hashing the contents of a number of module artifacts.
 */

public final class ModuleHashes {

    /**
     * A supplier of a message digest.
     */
    public static interface HashSupplier {
        byte[] generate(String algorithm);
    }

    private final String algorithm;
    private final Map<String, byte[]> nameToHash;

    /**
     * Creates a {@code ModuleHashes}.
     *
     * @param algorithm   the algorithm used to create the hashes
     * @param nameToHash  the map of module name to hash value
     */
    ModuleHashes(String algorithm, Map<String, byte[]> nameToHash) {
        this.algorithm = Objects.requireNonNull(algorithm);
        this.nameToHash = Collections.unmodifiableMap(nameToHash);
    }

    /**
     * Returns the algorithm used to hash the modules ("SHA-256" for example).
     */
    public String algorithm() {
        return algorithm;
    }

    /**
     * Returns the set of module names for which hashes are recorded.
     */
    public Set<String> names() {
        return nameToHash.keySet();
    }

    /**
     * Returns the hash for the given module name, {@code null}
     * if there is no hash recorded for the module.
     */
    public byte[] hashFor(String mn) {
        return nameToHash.get(mn);
    }

    /**
     * Returns unmodifiable map of module name to hash
     */
    public Map<String, byte[]> hashes() {
        return nameToHash;
    }

    /**
     * Computes a hash from the names and content of a module.
     *
     * @param reader the module reader to access the module content
     * @param algorithm the name of the message digest algorithm to use
     * @return the hash
     * @throws IllegalArgumentException if digest algorithm is not supported
     * @throws UncheckedIOException if an I/O error occurs
     */
    private static byte[] computeHash(ModuleReader reader, String algorithm) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        try {
            byte[] buf = new byte[32*1024];
            reader.list().sorted().forEach(rn -> {
                md.update(rn.getBytes(StandardCharsets.UTF_8));
                try (InputStream in = reader.open(rn).orElseThrow()) {
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        md.update(buf, 0, n);
                    }
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            });
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return md.digest();
    }

    /**
     * Computes a hash from the names and content of a module.
     *
     * @param supplier supplies the module reader to access the module content
     * @param algorithm the name of the message digest algorithm to use
     * @return the hash
     * @throws IllegalArgumentException if digest algorithm is not supported
     * @throws UncheckedIOException if an I/O error occurs
     */
    static byte[] computeHash(Supplier<ModuleReader> supplier, String algorithm) {
        try (ModuleReader reader = supplier.get()) {
            return computeHash(reader, algorithm);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Computes the hash from the names and content of a set of modules. Returns
     * a {@code ModuleHashes} to encapsulate the result.
     *
     * @param mrefs the set of modules
     * @param algorithm the name of the message digest algorithm to use
     * @return ModuleHashes that encapsulates the hashes
     * @throws IllegalArgumentException if digest algorithm is not supported
     * @throws UncheckedIOException if an I/O error occurs
     */
    static ModuleHashes generate(Set<ModuleReference> mrefs, String algorithm) {
        Map<String, byte[]> nameToHash = new TreeMap<>();
        for (ModuleReference mref : mrefs) {
            try (ModuleReader reader = mref.open()) {
                byte[] hash = computeHash(reader, algorithm);
                nameToHash.put(mref.descriptor().name(), hash);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
        return new ModuleHashes(algorithm, nameToHash);
    }

    @Override
    public int hashCode() {
        int h = algorithm.hashCode();
        for (Map.Entry<String, byte[]> e : nameToHash.entrySet()) {
            h = h * 31 + e.getKey().hashCode();
            h = h * 31 + Arrays.hashCode(e.getValue());
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModuleHashes))
            return false;
        ModuleHashes other = (ModuleHashes) obj;
        if (!algorithm.equals(other.algorithm)
                || nameToHash.size() != other.nameToHash.size())
            return false;
        for (Map.Entry<String, byte[]> e : nameToHash.entrySet()) {
            String name = e.getKey();
            byte[] hash = e.getValue();
            if (!Arrays.equals(hash, other.nameToHash.get(name)))
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(algorithm);
        sb.append(" ");
        nameToHash.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    sb.append(e.getKey());
                    sb.append("=");
                    byte[] ba = e.getValue();
                    for (byte b : ba) {
                        sb.append(String.format("%02x", b & 0xff));
                    }
                });
        return sb.toString();
    }

    /**
     * This is used by jdk.internal.module.SystemModules class
     * generated at link time.
     */
    public static class Builder {
        final String algorithm;
        final Map<String, byte[]> nameToHash;

        Builder(String algorithm, int initialCapacity) {
            this.nameToHash = new HashMap<>(initialCapacity);
            this.algorithm =  Objects.requireNonNull(algorithm);
        }

        /**
         * Sets the module hash for the given module name
         */
        public Builder hashForModule(String mn, byte[] hash) {
            nameToHash.put(mn, hash);
            return this;
        }

        /**
         * Builds a {@code ModuleHashes}.
         */
        public ModuleHashes build() {
            if (!nameToHash.isEmpty()) {
                return new ModuleHashes(algorithm, nameToHash);
            } else {
                return null;
            }
        }
    }
}
