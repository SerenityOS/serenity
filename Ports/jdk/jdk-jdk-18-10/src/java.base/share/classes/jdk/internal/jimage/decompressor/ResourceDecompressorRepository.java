/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.jimage.decompressor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * JLink Decompressors. All decompressors must be registered in the static
 * initializer of this class.
 *
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
public final class ResourceDecompressorRepository {

    private ResourceDecompressorRepository() {
    }

    private static final Map<String, ResourceDecompressorFactory> factories = new HashMap<>();

    static {
        registerReaderProvider(new ZipDecompressorFactory());
        registerReaderProvider(new StringSharingDecompressorFactory());
    }

    /**
     * Build a new decompressor for the passed name.
     * @param properties Contains plugin configuration.
     * @param name The plugin name to build.
     * @return A decompressor or null if not found
     * @throws IOException
     */
    public static ResourceDecompressor newResourceDecompressor(Properties properties,
            String name) throws IOException {

        ResourceDecompressorFactory fact = factories.get(name);
        if (fact != null) {
            return fact.newDecompressor(properties);
        }
        return null;
    }

    private static void registerReaderProvider(ResourceDecompressorFactory factory) {
        factories.put(factory.getName(), factory);
    }


}
