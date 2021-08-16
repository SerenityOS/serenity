/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * The module is the root of a set of packages/sources/artifacts.
 * At the moment there is only one module in use, the empty/no-name/default module.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Module implements Comparable<Module> {
    private String name;
    private String dirname;
    private Map<String,Package> packages = new HashMap<>();
    private Map<String,Source> sources = new HashMap<>();
    private Map<String,File> artifacts = new HashMap<>();

    public Module(String n, String dn) {
        name = n;
        dirname = n;
    }

    public String name() { return name; }
    public String dirname() { return dirname; }
    public Map<String,Package> packages() { return packages; }
    public Map<String,Source> sources() { return sources; }
    public Map<String,File> artifacts() { return artifacts; }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Module module) && name.equals(module.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(Module o) {
        return name.compareTo(o.name);
    }

    public void save(StringBuilder b) {
        b.append("M ").append(name).append(":").append("\n");
        Package.savePackages(packages, b);
    }

    public static Module load(String l) {
        int cp = l.indexOf(':',2);
        if (cp == -1) return null;
        String name = l.substring(2,cp);
        return new Module(name, "");
    }

    public static void saveModules(Map<String,Module> ms, StringBuilder b) {
        for (Module m : ms.values()) {
            m.save(b);
        }
    }

    public void addPackage(Package p) {
        packages.put(p.name(), p);
    }

    public Package lookupPackage(String pkg) {
        // See JDK-8071904
        Package p = packages.get(pkg);
        if (p == null) {
            p = new Package(this, pkg);
            packages.put(pkg, p);
        }
        return p;
    }

    public void addSource(String pkg, Source src) {
        Package p = lookupPackage(pkg);
        src.setPackage(p);
        p.addSource(src);
        sources.put(src.file().getPath(), src);
    }

    public Source lookupSource(String path) {
        return sources.get(path);
    }

    public void addArtifacts(String pkg, Set<URI> as) {
        Package p = lookupPackage(pkg);
        for (URI u : as) {
            p.addArtifact(new File(u));
        }
    }

    public void setDependencies(String pkg, Map<String, Set<String>> deps, boolean cp) {
        lookupPackage(pkg).setDependencies(deps, cp);
    }

    public void setPubapi(String pkg, PubApi ps) {
        Package p = lookupPackage(pkg);
        p.setPubapi(ps);
    }

    public boolean hasPubapiChanged(String pkg, PubApi newPubApi) {
        Package p = lookupPackage(pkg);
        return p.hasPubApiChanged(newPubApi);
    }
}
