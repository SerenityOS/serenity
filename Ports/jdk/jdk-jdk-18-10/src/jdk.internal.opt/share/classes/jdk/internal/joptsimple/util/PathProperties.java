/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.joptsimple.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Enum for checking common conditions of files and directories.
 *
 * @see jdk.internal.joptsimple.util.PathConverter
 */
public enum PathProperties {
    FILE_EXISTING( "file.existing" ) {
        @Override
        boolean accept( Path path ) {
            return Files.isRegularFile( path );
        }
    },
    DIRECTORY_EXISTING( "directory.existing" ) {
        @Override
        boolean accept( Path path ) {
            return Files.isDirectory( path );
        }
    },
    NOT_EXISTING( "file.not.existing" ) {
        @Override
        boolean accept( Path path ) {
            return Files.notExists( path );
        }
    },
    FILE_OVERWRITABLE( "file.overwritable" ) {
        @Override
        boolean accept( Path path ) {
            return FILE_EXISTING.accept( path ) && WRITABLE.accept( path );
        }
    },
    READABLE( "file.readable" ) {
        @Override
        boolean accept( Path path ) {
            return Files.isReadable( path );
        }
    },
    WRITABLE( "file.writable" ) {
        @Override
        boolean accept( Path path ) {
            return Files.isWritable( path );
        }
    };

    private final String messageKey;

    private PathProperties( String messageKey ) {
        this.messageKey = messageKey;
    }

    abstract boolean accept( Path path );

    String getMessageKey() {
        return messageKey;
    }
}
