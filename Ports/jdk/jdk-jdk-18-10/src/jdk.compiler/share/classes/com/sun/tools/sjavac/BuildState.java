/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * The build state class captures the source code and generated artifacts
 * from a build. There are usually two build states, the previous one (prev),
 * loaded from the javac_state file, and the current one (now).
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class BuildState {
    private Map<String,Module> modules = new HashMap<>();
    private Map<String,Package> packages = new HashMap<>();
    private Map<String,Source> sources = new HashMap<>();
    private Map<String,File> artifacts = new HashMap<>();
    // Map from package to a set of packages that depend on said package.
    private Map<String,Set<String>> dependents = new HashMap<>();

    public  Map<String,Module> modules() { return modules; }
    public  Map<String,Package> packages() { return packages; }
    public  Map<String,Source> sources() { return sources; }
    public  Map<String,File> artifacts() { return artifacts; }
    public  Map<String,Set<String>> dependents() { return dependents; }

    /**
     * Lookup a module from a name. Create the module if it does
     * not exist yet.
     */
    public Module lookupModule(String mod) {
        Module m = modules.get(mod);
        if (m == null) {
            m = new Module(mod, "???");
            modules.put(mod, m);
        }
        return m;
    }

    /**
     * Find a module from a given package name. For example:
     * The package name "base:java.lang" will fetch the module named "base".
     * The package name ":java.net" will fetch the default module.
     */
    Module findModuleFromPackageName(String pkg) {
        int cp = pkg.indexOf(':');
        Assert.check(cp != -1, "Could not find package name");
        String mod = pkg.substring(0, cp);
        return lookupModule(mod);
    }

    /**
     * Store references to all packages, sources and artifacts for all modules
     * into the build state. I.e. flatten the module tree structure
     * into global maps stored in the BuildState for easy access.
     *
     * @param m The set of modules.
     */
    public void flattenPackagesSourcesAndArtifacts(Map<String,Module> m) {
        modules = m;
        // Extract all the found packages.
        for (Module i : modules.values()) {
            for (Map.Entry<String,Package> j : i.packages().entrySet()) {
                Package p = packages.get(j.getKey());
                // Check that no two different packages are stored under same name.
                Assert.check(p == null || p == j.getValue());
                if (p == null) {
                    p = j.getValue();
                    packages.put(j.getKey(),j.getValue());
                }
                for (Map.Entry<String,Source> k : p.sources().entrySet()) {
                    Source s = sources.get(k.getKey());
                    // Check that no two different sources are stored under same name.
                    Assert.check(s == null || s == k.getValue());
                    if (s == null) {
                        s = k.getValue();
                        sources.put(k.getKey(), k.getValue());
                    }
                }
                for (Map.Entry<String,File> g : p.artifacts().entrySet()) {
                    File f = artifacts.get(g.getKey());
                    // Check that no two artifacts are stored under the same file.
                    Assert.check(f == null || f == g.getValue());
                    if (f == null) {
                        f = g.getValue();
                        artifacts.put(g.getKey(), g.getValue());
                    }
                }
            }
        }
    }

    /**
     * Store references to all artifacts found in the module tree into the maps
     * stored in the build state.
     *
     * @param m The set of modules.
     */
    public void flattenArtifacts(Map<String,Module> m) {
        modules = m;
        // Extract all the found packages.
        for (Module i : modules.values()) {
            for (Map.Entry<String,Package> j : i.packages().entrySet()) {
                Package p = packages.get(j.getKey());
                // Check that no two different packages are stored under same name.
                Assert.check(p == null || p == j.getValue());
                p = j.getValue();
                packages.put(j.getKey(),j.getValue());
                for (Map.Entry<String,File> g : p.artifacts().entrySet()) {
                    File f = artifacts.get(g.getKey());
                    // Check that no two artifacts are stored under the same file.
                    Assert.check(f == null || f == g.getValue());
                    artifacts.put(g.getKey(), g.getValue());
                }
            }
        }
    }

    /**
     * Calculate the package dependents (ie the reverse of the dependencies).
     */
    public void calculateDependents() {
        dependents = new HashMap<>();

        for (String s : packages.keySet()) {
            Package p = packages.get(s);

            // Collect all dependencies of the classes in this package
            Set<String> deps = p.typeDependencies()  // maps fqName -> set of dependencies
                                .values()
                                .stream()
                                .reduce(Collections.emptySet(), Util::union);

            // Now reverse the direction

            for (String dep : deps) {
                // Add the dependent information to the global dependent map.
                String depPkgStr = ":" + dep.substring(0, dep.lastIndexOf('.'));
                dependents.merge(depPkgStr, Collections.singleton(s), Util::union);

                // Also add the dependent information to the package specific map.
                // Normally, you do not compile java.lang et al. Therefore
                // there are several packages that p depends upon that you
                // do not have in your state database. This is perfectly fine.
                Package dp = packages.get(depPkgStr);
                if (dp != null) {
                    // But this package did exist in the state database.
                    dp.addDependent(p.name());
                }
            }
        }
    }

    /**
     * Verify that the setModules method above did the right thing when
     * running through the {@literal module->package->source} structure.
     */
    public void checkInternalState(String msg, boolean linkedOnly, Map<String,Source> srcs) {
        boolean baad = false;
        Map<String,Source> original = new HashMap<>();
        Map<String,Source> calculated = new HashMap<>();

        for (String s : sources.keySet()) {
            Source ss = sources.get(s);
            if (ss.isLinkedOnly() == linkedOnly) {
                calculated.put(s,ss);
            }
        }
        for (String s : srcs.keySet()) {
            Source ss = srcs.get(s);
            if (ss.isLinkedOnly() == linkedOnly) {
                original.put(s,ss);
            }
        }
        if (original.size() != calculated.size()) {
            Log.error("INTERNAL ERROR "+msg+" original and calculated are not the same size!");
            baad = true;
        }
        if (!original.keySet().equals(calculated.keySet())) {
            Log.error("INTERNAL ERROR "+msg+" original and calculated do not have the same domain!");
            baad = true;
        }
        if (!baad) {
            for (String s : original.keySet()) {
                Source s1 = original.get(s);
                Source s2 = calculated.get(s);
                if (s1 == null || s2 == null || !s1.equals(s2)) {
                    Log.error("INTERNAL ERROR "+msg+" original and calculated have differing elements for "+s);
                }
                baad = true;
            }
        }
        if (baad) {
            for (String s : original.keySet()) {
                Source ss = original.get(s);
                Source sss = calculated.get(s);
                if (sss == null) {
                    Log.error("The file "+s+" does not exist in calculated tree of sources.");
                }
            }
            for (String s : calculated.keySet()) {
                Source ss = calculated.get(s);
                Source sss = original.get(s);
                if (sss == null) {
                    Log.error("The file "+s+" does not exist in original set of found sources.");
                }
            }
        }
    }

    /**
     * Load a module from the javac state file.
     */
    public Module loadModule(String l) {
        Module m = Module.load(l);
        modules.put(m.name(), m);
        return m;
    }

    /**
     * Load a package from the javac state file.
     */
    public Package loadPackage(Module lastModule, String l) {
        Package p = Package.load(lastModule, l);
        lastModule.addPackage(p);
        packages.put(p.name(), p);
        return p;
    }

    /**
     * Load a source from the javac state file.
     */
    public Source loadSource(Package lastPackage, String l, boolean is_generated) {
        Source s = Source.load(lastPackage, l, is_generated);
        lastPackage.addSource(s);
        sources.put(s.name(), s);
        return s;
    }

    /**
     * During an incremental compile we need to copy the old javac state
     * information about packages that were not recompiled.
     */
    public void copyPackagesExcept(BuildState prev, Set<String> recompiled, Set<String> removed) {
        for (String pkg : prev.packages().keySet()) {
            // Do not copy recompiled or removed packages.
            if (recompiled.contains(pkg) || removed.contains(pkg))
                continue;

            Module mnew = findModuleFromPackageName(pkg);
            Package pprev = prev.packages().get(pkg);

            // Even though we haven't recompiled this package, we may have
            // information about its public API: It may be a classpath dependency
            if (packages.containsKey(pkg)) {
                pprev.setPubapi(PubApi.mergeTypes(pprev.getPubApi(),
                                                  packages.get(pkg).getPubApi()));
            }

            mnew.addPackage(pprev);
            // Do not forget to update the flattened data. (See JDK-8071904)
            packages.put(pkg, pprev);
        }
    }
}
