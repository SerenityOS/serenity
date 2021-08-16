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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import jdk.internal.joptsimple.ValueConversionException;
import jdk.internal.joptsimple.ValueConverter;

/**
 * Converts command line options to {@link Path} objects and checks the status of the underlying file.
 */
public class PathConverter implements ValueConverter<Path> {
    private final PathProperties[] pathProperties;

    public PathConverter( PathProperties... pathProperties ) {
        this.pathProperties = pathProperties;
    }

    @Override
    public Path convert( String value ) {
        Path path = Paths.get(value);

        if ( pathProperties != null ) {
            for ( PathProperties each : pathProperties ) {
                if ( !each.accept( path ) )
                    throw new ValueConversionException( message( each.getMessageKey(), path.toString() ) );
            }
        }

        return path;
    }

    @Override
    public Class<Path> valueType() {
        return Path.class;
    }

    @Override
    public String valuePattern() {
        return null;
    }

    private String message( String errorKey, String value ) {
        ResourceBundle bundle = ResourceBundle.getBundle( "jdk.internal.joptsimple.ExceptionMessages" );
        Object[] arguments = new Object[] { value, valuePattern() };
        String template = bundle.getString( PathConverter.class.getName() + "." + errorKey + ".message" );
        return new MessageFormat( template ).format( arguments );
    }
}
