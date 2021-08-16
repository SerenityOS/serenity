/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net.spi;

import java.net.URLStreamHandlerFactory;

/**
 * URL stream handler service-provider class.
 *
 * <p> A URL stream handler provider is a concrete subclass of this class that
 * has a zero-argument constructor. URL stream handler providers may be
 * installed in an instance of the Java platform by adding them to the
 * application class path.
 *
 * <p> A URL stream handler provider identifies itself with a
 * provider-configuration file named java.net.spi.URLStreamHandlerProvider in
 * the resource directory META-INF/services. The file should contain a list of
 * fully-qualified concrete URL stream handler provider class names, one per
 * line.
 *
 * <p> URL stream handler providers are located at runtime, as specified in the
 * {@linkplain java.net.URL#URL(String,String,int,String) URL constructor}.
 *
 * @since 9
 */
public abstract class URLStreamHandlerProvider
    implements URLStreamHandlerFactory
{
    private static Void checkPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("setFactory"));
        return null;
    }
    private URLStreamHandlerProvider(Void ignore) { }

    /**
     * Initializes a new URL stream handler provider.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link RuntimePermission}{@code ("setFactory")}.
     */
    protected URLStreamHandlerProvider() {
        this(checkPermission());
    }
}
