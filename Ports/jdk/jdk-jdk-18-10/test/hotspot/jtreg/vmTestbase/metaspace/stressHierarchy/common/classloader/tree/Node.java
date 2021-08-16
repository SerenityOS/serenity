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
package metaspace.stressHierarchy.common.classloader.tree;

import java.util.HashSet;
import java.util.Set;

import metaspace.stressHierarchy.common.classloader.StressClassloader;

/**
 * Node of tree
 *
 */
public class Node {

    private int level;

    private Set<String> loadedClassesNames;

    private int number;

    private Set<Object> objects = new HashSet<Object>();

    private Node parent = null;

    private StressClassloader classloader;

    public Node(int level, int number) {
        this.level = level;
        this.number = number;
    }

    public void cleanup() {
        loadedClassesNames = getClassLoader().getLoadedClassNames();
        objects.clear();
        getClassLoader().getLoadedClasses().clear();
        classloader = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Node) {
            Node other = (Node) obj;
            return other.getLevel() == level && other.getNumber() == number;
        } else {
            return false;
        }
    }

    StressClassloader getClassLoader() {     //package access level
        return classloader;
    }

    public int getLevel() {
        return level;
    }

    public Set<Class<?>> getLoadedClasses() {
        return getClassLoader() == null ? null : getClassLoader().getLoadedClasses();
    }

    public Set<String> getLoadedClassesNames() {
        if (loadedClassesNames != null) {
            return loadedClassesNames;
        } else if (getClassLoader() != null) {
            return getClassLoader().getLoadedClassNames();
        }
        return null;
    }

    public int getNumber() {
        return number;
    }

    public Node getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        return level + number * 999;
    }

    public void instantiateObjects() throws InstantiationException, IllegalAccessException {
        for (Class<?> c : getClassLoader().getLoadedClasses()) {
            if (!c.isInterface()) {
                objects.add(c.newInstance());
            }
        }
    }

    public boolean isRoot() {
        return (parent == null);
    }

    public void setClassLoader(StressClassloader classLoader) {
        classloader = classLoader;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "Node@level=" + level + "_num=" + number;
    }

    public void loadClasses() throws ClassNotFoundException {
        String className = "package_level" + getLevel() + "_num" + getNumber() + ".Dummy";
        getClassLoader().loadClass(className);
    }

}
