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


import com.sun.tools.jdeps.DepsAnalyzer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.sun.tools.jdeps.DepsAnalyzer.Info.*;
import static java.lang.module.ModuleDescriptor.Requires.Modifier.*;
import static java.lang.module.ModuleDescriptor.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class ModuleMetaData {
    public static final String JAVA_BASE = "java.base";

    static final String INTERNAL = "(internal)";
    static final String QUALIFIED = "(qualified)";
    static final String JDK_INTERNAL = "JDK internal API";
    static final String REMOVED_JDK_INTERNAL = "JDK removed internal API";

    final String moduleName;
    final boolean isNamed;
    final Map<String, ModuleRequires> requires = new LinkedHashMap<>();
    final Map<String, Dependence> references = new LinkedHashMap<>();
    final Map<String, Set<String>> exports = new LinkedHashMap<>();

    ModuleMetaData(String name) {
        this(name, true);
    }

    ModuleMetaData(String name, boolean isNamed) {
        this.moduleName = name;
        this.isNamed = isNamed;
        requires(JAVA_BASE);  // implicit requires
    }

    String name() {
        return moduleName;
    }

    ModuleMetaData requires(String name) {
        requires.put(name, new ModuleRequires(name));
        return this;
    }

    ModuleMetaData requiresTransitive(String name) {
        requires.put(name, new ModuleRequires(name, TRANSITIVE));
        return this;
    }

    // for unnamed module
    ModuleMetaData depends(String name) {
        requires.put(name, new ModuleRequires(name));
        return this;
    }

    ModuleMetaData reference(String origin, String target, String module) {
        return dependence(origin, target, module, "");
    }

    ModuleMetaData internal(String origin, String target, String module) {
        return dependence(origin, target, module, INTERNAL);
    }

    ModuleMetaData qualified(String origin, String target, String module) {
        return dependence(origin, target, module, QUALIFIED);
    }

    ModuleMetaData jdkInternal(String origin, String target, String module) {
        return dependence(origin, target, module, JDK_INTERNAL);
    }
    ModuleMetaData removedJdkInternal(String origin, String target) {
        return dependence(origin, target, REMOVED_JDK_INTERNAL, REMOVED_JDK_INTERNAL);
    }

    ModuleMetaData exports(String pn, Set<String> targets) {
        exports.put(pn, targets);
        return this;
    }

    private ModuleMetaData dependence(String origin, String target, String module, String access) {
        references.put(key(origin, target), new Dependence(origin, target, module, access));
        return this;
    }

    String key(String origin, String target) {
        return origin + ":" + target;
    }

    void checkRequires(String name, Set<DepsAnalyzer.Node> adjacentNodes) {
        // System.err.format("%s: Expected %s Found %s %n", name, requires, adjacentNodes);
        adjacentNodes.stream()
            .forEach(v -> checkRequires(v.name));
        assertEquals(adjacentNodes.size(), requires.size());
    }

    void checkRequires(String name) {
        ModuleRequires req = requires.get(name);
        if (req == null)
            System.err.println(moduleName + ": unexpected requires " + name);
        assertTrue(requires.containsKey(name));
    }

    void checkRequires(Requires require) {
        String name = require.name();
        if (name.equals(JAVA_BASE))
            return;

        ModuleRequires req = requires.get(name);
        if (req == null)
            System.err.format("%s: unexpected dependence %s%n", moduleName, name);

        assertTrue(requires.containsKey(name));

        assertEquals(require.modifiers(), req.modifiers());
    }

    void checkDependences(String name, Set<DepsAnalyzer.Node> adjacentNodes) {
        // System.err.format("%s: Expected %s Found %s %n", name, references, adjacentNodes);

        adjacentNodes.stream()
            .forEach(v -> checkDependence(name, v.name, v.source, v.info));
        assertEquals(adjacentNodes.size(), references.size());
    }

    void checkDependence(String origin, String target, String module, DepsAnalyzer.Info info) {
        String key = key(origin, target);
        Dependence dep = references.get(key);
        String access = "";
        if (info == QUALIFIED_EXPORTED_API)
            access = QUALIFIED;
        else if (info == JDK_INTERNAL_API)
            access = JDK_INTERNAL;
        else if (info == JDK_REMOVED_INTERNAL_API)
            access = REMOVED_JDK_INTERNAL;
        else if (info == INTERNAL_API)
            access = INTERNAL;

        assertTrue(references.containsKey(key));

        assertEquals(dep.access, access);
        assertEquals(dep.module, module);
    }


    public static class ModuleRequires {
        final String name;
        final Requires.Modifier mod;

        ModuleRequires(String name) {
            this.name = name;
            this.mod = null;
        }

        ModuleRequires(String name, Requires.Modifier mod) {
            this.name = name;
            this.mod = mod;
        }

        Set<Requires.Modifier> modifiers() {
            return mod != null ? Set.of(mod) : Collections.emptySet();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Dependence {
        final String origin;
        final String target;
        final String module;
        final String access;

        Dependence(String origin, String target, String module, String access) {
            this.origin = origin;
            this.target = target;
            this.module = module;
            this.access = access;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s (%s) %s", origin, target, module, access);
        }
    }
}
