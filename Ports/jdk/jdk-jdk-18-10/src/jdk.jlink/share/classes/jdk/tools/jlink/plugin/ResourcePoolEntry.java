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
package jdk.tools.jlink.plugin;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import jdk.tools.jlink.internal.ResourcePoolEntryFactory;

/**
 * A ResourcePoolEntry is the elementary unit of data inside an image. It is
 * generally a file. e.g.: a java class file, a resource file, a shared library.
 * <br>
 * A ResourcePoolEntry is identified by a path of the form:
 * <ul>
 * <li>For jimage content: /{module name}/{package1}/.../{packageN}/{file
 * name}</li>
 * <li>For top-level files:/{module name}/{file name}</li>
 * <li>For other files (shared lib, launchers, config, ...):/{module name}/
 * {@literal bin|conf|native}/{dir1}/.../{dirN}/{file name}</li>
 * </ul>
 */
public interface ResourcePoolEntry {

    /**
     * Type of module data.
     * <li>
     * <ul>CLASS_OR_RESOURCE: A java class or resource file.</ul>
     * <ul>CONFIG: A configuration file.</ul>
     * <ul>HEADER_FILE: A header file.</ul>
     * <ul>LEGAL_NOTICE: A legal notice.</ul>
     * <ul>MAN_PAGE: A man page.</ul>
     * <ul>NATIVE_CMD: A native executable launcher.</ul>
     * <ul>NATIVE_LIB: A native library.</ul>
     * <ul>TOP: A top-level file in the jdk run-time image directory.</ul>
     * </li>
     */
    public enum Type {
        CLASS_OR_RESOURCE,
        CONFIG,
        HEADER_FILE,
        LEGAL_NOTICE,
        MAN_PAGE,
        NATIVE_CMD,
        NATIVE_LIB,
        TOP
    }

    /**
     * The module name of this ResourcePoolEntry.
     *
     * @return The module name.
     */
    public String moduleName();

    /**
     * The path of this ResourcePoolEntry.
     *
     * @return The module path.
     */
    public String path();

    /**
     * The ResourcePoolEntry's type.
     *
     * @return The data type.
     */
    public Type type();

    /**
     * The ResourcePoolEntry content length.
     *
     * @return The content length.
     */
    public long contentLength();

    /**
     * The ResourcePoolEntry content as an InputStream.
     *
     * @return The resource content as an InputStream.
     */
    public InputStream content();

    /**
     * Returns a target linked with this entry.
     *
     * @implSpec The default implementation returns {@code null}.
     *
     * @return the target ResourcePoolEntry linked with this entry.
     */
    public default ResourcePoolEntry linkedTarget() {
        return null;
    }

    /**
     * The ResourcePoolEntry content as an array of bytes.
     *
     * @return An Array of bytes.
     */
    public default byte[] contentBytes() {
        try (InputStream is = content()) {
            return is.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Write the content of this ResourcePoolEntry to an OutputStream.
     *
     * @param out the output stream
     */
    public default void write(OutputStream out) {
        try {
            out.write(contentBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Create a ResourcePoolEntry with new content but other information
     * copied from this ResourcePoolEntry.
     *
     * @param content The new resource content.
     * @return A new ResourcePoolEntry.
     */
    public default ResourcePoolEntry copyWithContent(byte[] content) {
        return ResourcePoolEntryFactory.create(this, content);
    }

    /**
     * Create a ResourcePoolEntry with new content but other information
     * copied from this ResourcePoolEntry.
     *
     * @param file The new resource content.
     * @return A new ResourcePoolEntry.
     */
    public default ResourcePoolEntry copyWithContent(Path file) {
        return ResourcePoolEntryFactory.create(this, file);
    }

    /**
     * Create a ResourcePoolEntry for a resource of the given type.
     *
     * @param path The resource path.
     * @param type The ResourcePoolEntry type.
     * @param content The resource content.
     * @return A new ResourcePoolEntry.
     */
    public static ResourcePoolEntry create(String path,
            ResourcePoolEntry.Type type, byte[] content) {
        return ResourcePoolEntryFactory.create(path, type, content);
    }

    /**
     * Create a ResourcePoolEntry for a resource of type {@link Type#CLASS_OR_RESOURCE}.
     *
     * @param path The resource path.
     * @param content The resource content.
     * @return A new ResourcePoolEntry.
     */
    public static ResourcePoolEntry create(String path, byte[] content) {
        return create(path, Type.CLASS_OR_RESOURCE, content);
    }

    /**
     * Create a ResourcePoolEntry for a resource of the given type.
     *
     * @param path The resource path.
     * @param type The ResourcePoolEntry type.
     * @param file The resource file.
     * @return A new ResourcePoolEntry.
     */
    public static ResourcePoolEntry create(String path,
            ResourcePoolEntry.Type type, Path file) {
        return ResourcePoolEntryFactory.create(path, type, file);
    }

    /**
     * Create a ResourcePoolEntry for a resource of type {@link Type#CLASS_OR_RESOURCE}.
     *
     * @param path The resource path.
     * @param file The resource file.
     * @return A new ResourcePoolEntry.
     */
    public static ResourcePoolEntry create(String path, Path file) {
        return create(path, Type.CLASS_OR_RESOURCE, file);
    }

    /**
     * Create a ResourcePoolEntry for a resource the given path and type.
     * If the target platform supports symbolic links, it will be created
     * as a symbolic link to the given target entry; otherwise, the
     * ResourcePoolEntry contains the relative path to the target entry.
     *
     * @param path The resource path.
     * @param type The ResourcePoolEntry type.
     * @param target The target entry
     * @return A new ResourcePoolEntry
     */
    public static ResourcePoolEntry createSymLink(String path,
                                                  ResourcePoolEntry.Type type,
                                                  ResourcePoolEntry target) {
        return ResourcePoolEntryFactory.createSymbolicLink(path, type, target);
    }
}
