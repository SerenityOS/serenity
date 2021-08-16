/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package shared;

import java.util.HashMap;
import java.util.Map;

/*******************************************************************/
// Class loader which has local class file storage in memory
/*******************************************************************/

public class ByteArrayClassLoader extends ClassLoader {
    private Map<String, byte[]> classes;

    public ByteArrayClassLoader() {
        classes = new HashMap<String, byte[]>();
    }

    public ByteArrayClassLoader(Map<String, byte[]> classes) {
        this.classes = classes;
    }

    public void appendClass(String name, byte[] classFile) {
        classes.put(name, classFile);
    }

    public Class findClass (String name) throws ClassNotFoundException {
        if (classes.containsKey(name)) {
            byte[] classData = classes.get(name);
            return defineClass(name, classData, 0, classData.length);
        } else {
            throw new ClassNotFoundException("Can't find requested class: " + name);
        }
    }
}
