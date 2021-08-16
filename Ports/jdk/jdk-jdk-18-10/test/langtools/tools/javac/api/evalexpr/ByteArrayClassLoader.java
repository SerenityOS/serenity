/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package evalexpr;

import java.util.Map;

/**
 * A class loader which loads classes from byte arrays.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 * @author Peter von der Ah&eacute;
 */
public class ByteArrayClassLoader extends ClassLoader {
    /**
     * Maps binary class names to class files stored as byte arrays.
     */
    private Map<String, byte[]> classes;

    /**
     * Creates a new instance of ByteArrayClassLoader
     * @param classes a map from binary class names to class files stored as byte arrays
     */
    public ByteArrayClassLoader(Map<String, byte[]> classes) {
        this.classes = classes;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            byte[] classData = classes.get(name);
            return defineClass(name, classData, 0, classData.length);
        }
    }
}
