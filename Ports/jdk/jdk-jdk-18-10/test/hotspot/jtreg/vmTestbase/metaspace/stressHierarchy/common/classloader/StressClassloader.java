/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package metaspace.stressHierarchy.common.classloader;

import java.net.URLClassLoader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import metaspace.stressHierarchy.common.generateHierarchy.NodeDescriptor;

/**
 * This classloader tries to load each class itself before forwarding to parent.
 * Also it stores loaded classes and class names.
 *
 */
public class StressClassloader extends URLClassLoader {

    private final Set<Class<?>> loadedClasses = new HashSet<Class<?>>();

    private final Set<String> loadedClassesNames = new HashSet<String>();

    private String className;

    private byte[] bytecode;

    public StressClassloader(NodeDescriptor nodeDescriptor,
            StressClassloader parentClassLoader) {
      super(new URL[] {}, parentClassLoader);
      this.className = nodeDescriptor.className;
      this.bytecode = nodeDescriptor.bytecode;
    }

    public Set<Class<?>> getLoadedClasses() {
        return loadedClasses;
    }

    public Set<String> getLoadedClassNames() {
        return loadedClassesNames;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (loadedClassesNames.contains(name)) {
            throw new RuntimeException("Classloader " + toString() + " loads class " + name + " second time! ");
        }
        Class<?> alreadyLoaded = findLoadedClass(name);
        if (alreadyLoaded != null) {
            return alreadyLoaded;
        }
        if (className.equals(name)) {
            Class<?> clazz = defineClass(name, bytecode, 0, bytecode.length);
            loadedClasses.add(clazz);
            loadedClassesNames.add(clazz.getName());
            return clazz;
        } else {
            return super.loadClass(name);
        }
    }

    @Override
    public String toString() {
        return "StressClassloader@" + className;
    }



}
