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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

/**
 * A representation of a class/interface hierarchy graph (just the
 * graph; the class data is represented elsewhere).
 */
public class HierarchyShape {
    public static final int OBJECT_CLASS = -1;

    protected int maxId;

    /**
     * The names of all the classes.
     */
    private final HashSet<Integer> classes;

    /**
     * The names of all the interfaces.
     */
    private final HashSet<Integer> interfaces;
    private final HashMap<Integer, HashSet<Integer>> extensions;

    /**
     * Create an empty hierarchy shape.
     */
    public HierarchyShape() {
        this(0, new HashSet<>(), new HashSet<>(), new HashMap<>());
    }

    private HierarchyShape(final int maxId,
                          final HashSet<Integer> classes,
                          final HashSet<Integer> interfaces,
                          final HashMap<Integer, HashSet<Integer>> extensions) {
        this.maxId = maxId;
        this.classes = classes;
        this.interfaces = interfaces;
        this.extensions = extensions;
    }

    /**
     * Make a copy of this hierarchy shape.
     */
    public HierarchyShape copy() {
        final HashMap<Integer, HashSet<Integer>> newextensions = new HashMap<>();

        for(final Map.Entry<Integer, HashSet<Integer>> entry :
                extensions.entrySet()) {
            newextensions.put(entry.getKey(),
                              (HashSet<Integer>)entry.getValue().clone());
        }

        return new HierarchyShape(maxId, (HashSet<Integer>) classes.clone(),
                                  (HashSet<Integer>) interfaces.clone(),
                                  newextensions);
    }

    /**
     * Add a class, and return its id.
     *
     * @return The new class id.
     */
    public int addClass() {
        final int id = maxId++;
        classes.add(id);
        return id;
    }

    /**
     * Add an interface, and return its id.
     *
     * @return The new interface id.
     */
    public int addInterface() {
        final int id = maxId++;
        interfaces.add(id);
        return id;
    }

    /**
     * Add an inheritance.
     *
     * @param sub The sub class/interface.
     * @param sup The super class/interface
     */
    public void addInherit(final int sub,
                           final int sup) {
        HashSet<Integer> ext = extensions.get(sub);

        if (ext == null) {
            ext = new HashSet<>();
            extensions.put(sub, ext);
        }

        ext.add(sup);
    }

    @Override
    public String toString() {
        String out = "";
        for(int i = maxId - 1; i >= 0; i--) {
            out += i + ": ";
            for(int j = 0; j < maxId; j++) {
                out += "[" + (inherits(i, j) ? "1" : "0") + "]";
            }
            out += "\n";
        }
        return out;
    }

    /**
     * Indicate whether the first class inherits from the second.
     *
     * @param sub The possible subtype.
     * @param sup The possible supertype.
     * @return Whether or not {@code sub} inherits from {@code sup}.
     */
    public boolean inherits(final int sub, final int sup) {
        final Set<Integer> ext = extensions.get(sub);
        if (ext != null) {
            return ext.contains(sup);
        } else {
            return false;
        }
    }

    /**
     * Indicate whether a given type name is a class.
     *
     * @param id The type in question.
     * @return Whether or not the type is a class.
     */
    public boolean isClass(final int id) {
        if (id == OBJECT_CLASS) {
            return true;
        }
        return classes.contains(id);
    }

    /**
     * Indicate whether a given type name is an interface.
     *
     * @param id The type in question.
     * @return Whether or not the type is an interface.
     */
    public boolean isInterface(final int id) {
        if (id == OBJECT_CLASS) {
            return false;
        }
        return interfaces.contains(id);
    }

    /**
     * Get an iterator over the classes.
     *
     * @return An iterator over classes.
     */
    public Collection<Integer> classes() {
        return classes;
    }

    /**
     * Get an iterator over the interfaces.
     *
     * @return An iterator over interfaces.
     */
    public Collection<Integer> interfaces() {
        return interfaces;
    }

    /**
     * Get an iterator over all types.
     *
     * @return An iterator over all types.
     */
    public Collection<Integer> types() {
        final Set<Integer> combined = new HashSet(classes);
        combined.addAll(interfaces);
        return combined;
    }

    public int numClasses() {
        return classes.size();
    }

    public int numInterfaces() {
        return interfaces.size();
    }

    public int numTypes() {
        return numClasses() + numInterfaces();
    }

}
