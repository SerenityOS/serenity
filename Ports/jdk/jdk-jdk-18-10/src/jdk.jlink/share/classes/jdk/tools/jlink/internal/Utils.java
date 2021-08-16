/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.tools.jlink.internal;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jdk.tools.jlink.plugin.Plugin;

public class Utils {

    private Utils() {}

    // jrt-fs file system
    private static FileSystem JRT_FILE_SYSTEM;

    // current module
    private static final Module THIS_MODULE = Utils.class.getModule();

    public static List<String> parseList(String arguments) {
        return Arrays.stream(arguments.split(","))
                     .map((p) -> p.trim())
                     .filter((p) -> !p.isEmpty())
                     .toList();
    }


    public static List<Plugin> getSortedPlugins(List<Plugin> plugins) {
        List<Plugin> res = new ArrayList<>();
        res.addAll(plugins);
        res.sort(new Comparator<Plugin>() {
            @Override
            public int compare(Plugin o1, Plugin o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return res;
    }

    public static boolean isFunctional(Plugin prov) {
        return prov.getState().contains(Plugin.State.FUNCTIONAL);
    }

    public static boolean isAutoEnabled(Plugin prov) {
        return prov.getState().contains(Plugin.State.AUTO_ENABLED);
    }

    public static boolean isDisabled(Plugin prov) {
        return prov.getState().contains(Plugin.State.DISABLED);
    }

    // is this a builtin (jdk.jlink) plugin?
    public static boolean isBuiltin(Plugin prov) {
        return THIS_MODULE.equals(prov.getClass().getModule());
    }

    public static FileSystem jrtFileSystem() {
        if (JRT_FILE_SYSTEM == null) {
            JRT_FILE_SYSTEM = FileSystems.getFileSystem(URI.create("jrt:/"));
        }

        return JRT_FILE_SYSTEM;
    }

    public static PathMatcher getPathMatcher(FileSystem fs, String pattern) {
        if (!pattern.startsWith("glob:") && !pattern.startsWith("regex:")) {
            pattern = "glob:" + pattern;
        }

        return fs.getPathMatcher(pattern);
    }

    public static PathMatcher getJRTFSPathMatcher(String pattern) {
        return getPathMatcher(jrtFileSystem(), pattern);
    }

    public static Path getJRTFSPath(String first, String... more) {
        return jrtFileSystem().getPath(first, more);
    }
}
