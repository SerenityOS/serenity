/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.jpackage.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * jpackage type traits.
 */
public enum PackageType {
    WIN_MSI(".msi",
            TKit.isWindows() ? "jdk.jpackage.internal.WinMsiBundler" : null),
    WIN_EXE(".exe",
            TKit.isWindows() ? "jdk.jpackage.internal.WinMsiBundler" : null),
    LINUX_DEB(".deb",
            TKit.isLinux() ? "jdk.jpackage.internal.LinuxDebBundler" : null),
    LINUX_RPM(".rpm",
            TKit.isLinux() ? "jdk.jpackage.internal.LinuxRpmBundler" : null),
    MAC_DMG(".dmg", TKit.isOSX() ? "jdk.jpackage.internal.MacDmgBundler" : null),
    MAC_PKG(".pkg", TKit.isOSX() ? "jdk.jpackage.internal.MacPkgBundler" : null),
    IMAGE("app-image", null, null);

    PackageType(String packageName, String bundleSuffix, String bundlerClass) {
        name  = packageName;
        suffix = bundleSuffix;
        if (bundlerClass != null && !Inner.DISABLED_PACKAGERS.contains(getName())) {
            supported = isBundlerSupported(bundlerClass);
        } else {
            supported = false;
        }

        if (suffix != null && supported) {
            TKit.trace(String.format("Bundler %s supported", getName()));
        }
    }

    PackageType(String bundleSuffix, String bundlerClass) {
        this(bundleSuffix.substring(1), bundleSuffix, bundlerClass);
    }

    void applyTo(JPackageCommand cmd) {
        cmd.setArgumentValue("--type", getName());
    }

    String getSuffix() {
        return suffix;
    }

    boolean isSupported() {
        return supported;
    }

    String getName() {
        return name;
    }

    static PackageType fromSuffix(String packageFilename) {
        if (packageFilename != null) {
            for (PackageType v : values()) {
                if (packageFilename.endsWith(v.getSuffix())) {
                    return v;
                }
            }
        }
        return null;
    }

    private static boolean isBundlerSupported(String bundlerClass) {
        try {
            Class clazz = Class.forName(bundlerClass);
            Method supported = clazz.getMethod("supported", boolean.class);
            return ((Boolean) supported.invoke(
                    clazz.getConstructor().newInstance(), true));
        } catch (ClassNotFoundException | IllegalAccessException ex) {
        } catch (InstantiationException | NoSuchMethodException
                | InvocationTargetException ex) {
            Functional.rethrowUnchecked(ex);
        }
        return false;
    }

    private final String name;
    private final String suffix;
    private final boolean supported;

    public final static Set<PackageType> LINUX = Set.of(LINUX_DEB, LINUX_RPM);
    public final static Set<PackageType> WINDOWS = Set.of(WIN_EXE, WIN_MSI);
    public final static Set<PackageType> MAC = Set.of(MAC_PKG, MAC_DMG);
    public final static Set<PackageType> NATIVE = Stream.concat(
            Stream.concat(LINUX.stream(), WINDOWS.stream()),
            MAC.stream()).collect(Collectors.toUnmodifiableSet());

    private final static class Inner {
        private final static Set<String> DISABLED_PACKAGERS = Optional.ofNullable(
                TKit.tokenizeConfigProperty("disabledPackagers")).orElse(
                TKit.isUbuntu() ? Set.of("rpm") : Collections.emptySet());
    }
}
