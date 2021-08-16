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

package com.sun.tools.jdeps;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class VersionHelper {
    private static final String META_INF_VERSIONS = "META-INF/versions/";
    private static final Map<String,String> nameToVersion = new ConcurrentHashMap<>();

    public static String get(String classname) {
        if (nameToVersion.containsKey(classname)) {
            return nameToVersion.get(classname) + "/" + classname;
        }
        return classname;
    }

    public static void add(JarFile jarfile, JarEntry e, ClassFile cf)
            throws ConstantPoolException
    {
        String realName = e.getRealName();
        if (realName.startsWith(META_INF_VERSIONS)) {
            int len = META_INF_VERSIONS.length();
            int n = realName.indexOf('/', len);
            if (n > 0) {
                String version = realName.substring(len, n);
                assert (Integer.parseInt(version) > 8);
                String name = cf.getName().replace('/', '.');
                if (nameToVersion.containsKey(name)) {
                    if (!version.equals(nameToVersion.get(name))) {
                        throw new MultiReleaseException(
                                "err.multirelease.version.associated",
                                name, nameToVersion.get(name), version
                        );
                    }
                } else {
                    nameToVersion.put(name, version);
                }
            } else {
                throw new MultiReleaseException("err.multirelease.jar.malformed",
                        jarfile.getName(), realName);
            }
        }
    }
}
