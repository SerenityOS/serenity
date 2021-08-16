/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package selectionresolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class ByteCodeClassLoader extends ClassLoader {
    ArrayList<ClassConstruct> classes = new ArrayList<>();
    HashMap<String, Class> loadedClasses = new HashMap<>();

    public void addClasses(ClassConstruct... classes) {
        this.classes.addAll(Arrays.asList(classes));
    }

    public void loadAll() throws ClassNotFoundException {
        for (ClassConstruct clazz : classes) {
            findClass(clazz.getDottedName());
        }
    }


    @Override
    public Class findClass(String name) throws ClassNotFoundException {

        Class cls = loadedClasses.get(name);

        if (cls != null) {
            return cls;
        }

        for (ClassConstruct clazz : classes) {
            if (clazz.getDottedName().equals(name)) {
                return load(clazz);
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name);
        }
    }

    private Class load(ClassConstruct clazz) {
        byte[] bytecode = clazz.generateBytes();
        Class loadedClass = defineClass(clazz.getDottedName(), bytecode, 0, bytecode.length);
        loadedClasses.put(clazz.getDottedName(), loadedClass);
        return loadedClass;
    }
}
