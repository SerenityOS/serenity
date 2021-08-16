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

package jdk.dynalink.linker;

import java.security.Permission;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import jdk.dynalink.DynamicLinkerFactory;

/**
 * A class acting as a supplier of guarding dynamic linkers that can be
 * automatically loaded by other language runtimes. Language runtimes wishing
 * to export their own linkers should subclass this class and implement the
 * {@link #get()} method to return a list of exported linkers and declare the
 * subclass in
 * {@code /META-INF/services/jdk.dynalink.linker.GuardingDynamicLinkerExporter}
 * resource of their distribution (typically, JAR file) so that dynamic linker
 * factories can discover them using the {@link ServiceLoader} mechanism. Note
 * that instantiating this class is tied to a security check for the
 * {@code RuntimePermission("dynalink.exportLinkersAutomatically")} when a
 * security manager is present, to ensure that only trusted runtimes can
 * automatically export their linkers into other runtimes.
 * @see DynamicLinkerFactory#setClassLoader(ClassLoader)
 */
public abstract class GuardingDynamicLinkerExporter implements Supplier<List<GuardingDynamicLinker>> {
    /**
     * The name of the runtime permission for creating instances of this class.
     * Granting this permission to a language runtime allows it to export its
     * linkers for automatic loading into other language runtimes.
     */
    public static final String AUTOLOAD_PERMISSION_NAME = "dynalink.exportLinkersAutomatically";

    private static final Permission AUTOLOAD_PERMISSION = new RuntimePermission(AUTOLOAD_PERMISSION_NAME);

    /**
     * Creates a new linker exporter. If there is a security manager installed
     * checks for the
     * {@code RuntimePermission("dynalink.exportLinkersAutomatically")} runtime
     * permission. This ensures only language runtimes granted this permission
     * will be allowed to export their linkers for automatic loading.
     * @throws SecurityException if the necessary runtime permission is not
     * granted.
     */
    protected GuardingDynamicLinkerExporter() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AUTOLOAD_PERMISSION);
        }
    }
}
