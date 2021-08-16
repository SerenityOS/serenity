/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A ModuleReference implementation that supports referencing a module that
 * is patched and/or can be tied to other modules by means of hashes.
 */

public class ModuleReferenceImpl extends ModuleReference {

    // location of module
    private final URI location;

    // the module reader
    private final Supplier<ModuleReader> readerSupplier;

    // non-null if the module is patched
    private final ModulePatcher patcher;

    // ModuleTarget if the module is OS/architecture specific
    private final ModuleTarget target;

    // the hashes of other modules recorded in this module
    private final ModuleHashes recordedHashes;

    // the function that computes the hash of this module
    private final ModuleHashes.HashSupplier hasher;

    // ModuleResolution flags
    private final ModuleResolution moduleResolution;

    // cached hash of this module to avoid needing to compute it many times
    private byte[] cachedHash;

    /**
     * Constructs a new instance of this class.
     */
    public ModuleReferenceImpl(ModuleDescriptor descriptor,
                               URI location,
                               Supplier<ModuleReader> readerSupplier,
                               ModulePatcher patcher,
                               ModuleTarget target,
                               ModuleHashes recordedHashes,
                               ModuleHashes.HashSupplier hasher,
                               ModuleResolution moduleResolution)
    {
        super(descriptor, Objects.requireNonNull(location));
        this.location = location;
        this.readerSupplier = readerSupplier;
        this.patcher = patcher;
        this.target = target;
        this.recordedHashes = recordedHashes;
        this.hasher = hasher;
        this.moduleResolution = moduleResolution;
    }

    @Override
    public ModuleReader open() throws IOException {
        try {
            return readerSupplier.get();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Returns {@code true} if this module has been patched via --patch-module.
     */
    public boolean isPatched() {
        return (patcher != null);
    }

    /**
     * Returns the ModuleTarget or {@code null} if the no target platform.
     */
    public ModuleTarget moduleTarget() {
        return target;
    }

    /**
     * Returns the hashes recorded in this module or {@code null} if there
     * are no hashes recorded.
     */
    public ModuleHashes recordedHashes() {
        return recordedHashes;
    }

    /**
     * Returns the supplier that computes the hash of this module.
     */
    ModuleHashes.HashSupplier hasher() {
        return hasher;
    }

    /**
     * Returns the ModuleResolution flags.
     */
    public ModuleResolution moduleResolution() {
        return moduleResolution;
    }

    /**
     * Computes the hash of this module. Returns {@code null} if the hash
     * cannot be computed.
     *
     * @throws java.io.UncheckedIOException if an I/O error occurs
     */
    public byte[] computeHash(String algorithm) {
        byte[] result = cachedHash;
        if (result != null)
            return result;
        if (hasher == null)
            return null;
        cachedHash = result = hasher.generate(algorithm);
        return result;
    }

    @Override
    public int hashCode() {
        int hc = hash;
        if (hc == 0) {
            hc = descriptor().hashCode();
            hc = 43 * hc + Objects.hashCode(location);
            hc = 43 * hc + Objects.hashCode(patcher);
            if (hc == 0)
                hc = -1;
            hash = hc;
        }
        return hc;
    }

    private int hash;

    @Override
    public boolean equals(Object ob) {
        if (!(ob instanceof ModuleReferenceImpl))
            return false;
        ModuleReferenceImpl that = (ModuleReferenceImpl)ob;

        // assume module content, recorded hashes, etc. are the same
        // when the modules have equal module descriptors, are at the
        // same location, and are patched by the same patcher.
        return Objects.equals(this.descriptor(), that.descriptor())
                && Objects.equals(this.location, that.location)
                && Objects.equals(this.patcher, that.patcher);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[module ");
        sb.append(descriptor().name());
        sb.append(", location=");
        sb.append(location);
        if (isPatched()) sb.append(" (patched)");
        sb.append("]");
        return sb.toString();
    }

}
