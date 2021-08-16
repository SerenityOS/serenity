/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share;

import java.io.IOException;

import vm.share.FileUtils;

public class CustomClassLoaders {

    public static ClassLoader makeClassBytesLoader(final byte[] classBytes,
            final String className) {
        return new ClassLoader() {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                Env.traceDebug("Custom loader: requested=" + name + "; can supply=" + className);

                if (!name.equals(className))
                    return super.loadClass(name, resolve);

                Env.traceDebug("Custom loader: defining " + className + " (" + classBytes.length + ") bytes");

                return defineClass(className, classBytes, 0, classBytes.length);
            }
        };
    }

    public static ClassLoader makeCustomClassLoader(final String forClassName) throws IOException {
        return makeClassBytesLoader(FileUtils.readClass(forClassName), forClassName);
    }
}
