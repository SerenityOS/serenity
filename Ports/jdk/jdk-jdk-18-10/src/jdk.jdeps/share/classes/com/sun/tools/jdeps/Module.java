/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Jdeps internal representation of module for dependency analysis.
 */
class Module extends Archive {
    static final Module UNNAMED_MODULE = new UnnamedModule();
    static final String JDK_UNSUPPORTED = "jdk.unsupported";

    static final boolean DEBUG = Boolean.getBoolean("jdeps.debug");
    static void trace(String fmt, Object... args) {
        trace(DEBUG, fmt, args);
    }

    static void trace(boolean traceOn, String fmt, Object... args) {
        if (traceOn) {
            System.err.format(fmt, args);
        }
    }

    private final ModuleDescriptor descriptor;
    private final Map<String, Set<String>> exports;
    private final Map<String, Set<String>> opens;
    private final boolean isSystem;
    private final URI location;

    protected Module(String name) {
        this(name, null, false);
    }

    protected Module(String name, ModuleDescriptor descriptor, boolean isSystem) {
        super(name);
        this.descriptor = descriptor;
        this.location = null;
        this.exports = Collections.emptyMap();
        this.opens = Collections.emptyMap();
        this.isSystem = isSystem;
    }

    private Module(String name,
                   URI location,
                   ModuleDescriptor descriptor,
                   Map<String, Set<String>> exports,
                   Map<String, Set<String>> opens,
                   boolean isSystem,
                   ClassFileReader reader) {
        super(name, location, reader);
        this.descriptor = descriptor;
        this.location = location;
        this.exports = Collections.unmodifiableMap(exports);
        this.opens = Collections.unmodifiableMap(opens);
        this.isSystem = isSystem;
    }

    /**
     * Returns module name
     */
    public String name() {
        return descriptor != null ? descriptor.name() : getName();
    }

    public boolean isNamed() {
        return descriptor != null;
    }

    public boolean isAutomatic() {
        return descriptor != null && descriptor.isAutomatic();
    }

    public Module getModule() {
        return this;
    }

    public ModuleDescriptor descriptor() {
        return descriptor;
    }

    public URI location() {
        return location;
    }

    public boolean isJDK() {
        String mn = name();
        return isSystem &&
            (mn.startsWith("java.") || mn.startsWith("jdk."));
    }

    public boolean isSystem() {
        return isSystem;
    }

    public Map<String, Set<String>> exports() {
        return exports;
    }

    public Set<String> packages() {
        return descriptor.packages();
    }

    public boolean isJDKUnsupported() {
        return JDK_UNSUPPORTED.equals(this.name());
    }

    /**
     * Converts this module to a normal module with the given dependences
     *
     * @throws IllegalArgumentException if this module is not an automatic module
     */
    public Module toNormalModule(Map<String, Boolean> requires) {
        if (!isAutomatic()) {
            throw new IllegalArgumentException(name() + " not an automatic module");
        }
        return new NormalModule(this, requires);
    }

    /**
     * Tests if the package of the given name is exported.
     */
    public boolean isExported(String pn) {
        return exports.containsKey(pn) && exports.get(pn).isEmpty();
    }

    /**
     * Tests if the package of the given name is exported to the target
     * in a qualified fashion.
     */
    public boolean isExported(String pn, String target) {
        return isExported(pn)
                || exports.containsKey(pn) && exports.get(pn).contains(target);
    }

    /**
     * Tests if the package of the given name is open.
     */
    public boolean isOpen(String pn) {
        return opens.containsKey(pn) && opens.get(pn).isEmpty();
    }

    /**
     * Tests if the package of the given name is open to the target
     * in a qualified fashion.
     */
    public boolean isOpen(String pn, String target) {
        return isOpen(pn)
            || opens.containsKey(pn) && opens.get(pn).contains(target);
    }

    @Override
    public String toString() {
        return name();
    }

    public final static class Builder {
        final String name;
        final ModuleDescriptor descriptor;
        final boolean isSystem;
        ClassFileReader reader;
        URI location;

        public Builder(ModuleDescriptor md) {
            this(md, false);
        }

        public Builder(ModuleDescriptor md, boolean isSystem) {
            this.name = md.name();
            this.descriptor = md;
            this.isSystem = isSystem;
        }

        public Builder location(URI location) {
            this.location = location;
            return this;
        }

        public Builder classes(ClassFileReader reader) {
            this.reader = reader;
            return this;
        }

        public Module build() {
            if (descriptor.isAutomatic() && isSystem) {
                throw new InternalError("JDK module: " + name + " can't be automatic module");
            }

            Map<String, Set<String>> exports = new HashMap<>();
            Map<String, Set<String>> opens = new HashMap<>();

            if (descriptor.isAutomatic()) {
                // ModuleDescriptor::exports and opens returns an empty set
                descriptor.packages().forEach(pn -> exports.put(pn, Collections.emptySet()));
                descriptor.packages().forEach(pn -> opens.put(pn, Collections.emptySet()));
            } else {
                descriptor.exports().stream()
                          .forEach(exp -> exports.computeIfAbsent(exp.source(), _k -> new HashSet<>())
                                                 .addAll(exp.targets()));
                descriptor.opens().stream()
                    .forEach(exp -> opens.computeIfAbsent(exp.source(), _k -> new HashSet<>())
                        .addAll(exp.targets()));
            }
            return new Module(name, location, descriptor, exports, opens, isSystem, reader);
        }
    }

    private static class UnnamedModule extends Module {
        private UnnamedModule() {
            super("unnamed", null, false);
        }

        @Override
        public String name() {
            return "unnamed";
        }

        @Override
        public boolean isExported(String pn) {
            return true;
        }
    }

    /**
     * A normal module has a module-info.class
     */
    private static class NormalModule extends Module {
        private final ModuleDescriptor md;

        /**
         * Converts the given automatic module to a normal module.
         *
         * Replace this module's dependences with the given requires and also
         * declare service providers, if specified in META-INF/services configuration file
         */
        private NormalModule(Module m, Map<String, Boolean> requires) {
            super(m.name(), m.location, m.descriptor, m.exports, m.opens, m.isSystem, m.reader());

            ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(m.name());
            requires.keySet().forEach(mn -> {
                if (requires.get(mn).equals(Boolean.TRUE)) {
                    builder.requires(Set.of(ModuleDescriptor.Requires.Modifier.TRANSITIVE), mn);
                } else {
                    builder.requires(mn);
                }
            });
            // exports all packages
            m.descriptor.packages().forEach(builder::exports);
            m.descriptor.uses().forEach(builder::uses);
            m.descriptor.provides().forEach(builder::provides);
            this.md = builder.build();
        }

        @Override
        public ModuleDescriptor descriptor() {
            return md;
        }
    }
}
