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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * The Package class maintains meta information about a package.
 * For example its sources, dependents,its pubapi and its artifacts.
 *
 * It might look odd that we track dependents/pubapi/artifacts on
 * a package level, but it makes sense since recompiling a full package
 * takes as long as recompiling a single java file in that package,
 * if you take into account the startup time of the jvm.
 *
 * Also the dependency information will be much smaller (good for the javac_state file size)
 * and it simplifies tracking artifact generation, you do not always know from which
 * source a class file was generated, but you always know which package it belongs to.
 *
 * It is also educational to see package dependencies triggering recompilation of
 * other packages. Even though the recompilation was perhaps not necessary,
 * the visible recompilation of the dependent packages indicates how much circular
 * dependencies your code has.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Package implements Comparable<Package> {
    // The module this package belongs to. (There is a legacy module with an empty string name,
    // used for all legacy sources.)
    private Module mod;
    // Name of this package, module:pkg
    // ex1 jdk.base:java.lang
    // ex2 :java.lang (when in legacy mode)
    private String name;
    // The directory path to the package. If the package belongs to a module,
    // then that module's file system name is part of the path.
    private String dirname;
    // This package has the following dependents, that depend on this package.
    private Set<String> dependents = new HashSet<>();

    // Fully qualified name of class in this package -> fully qualified name of dependency
    private Map<String, Set<String>> dependencies = new TreeMap<>();
    // Fully qualified name of class in this package -> fully qualified name of dependency on class path
    private Map<String, Set<String>> cpDependencies = new TreeMap<>();

    // This is the public api of this package.
    private PubApi pubApi = new PubApi();
    // Map from source file name to Source info object.
    private Map<String,Source> sources = new HashMap<>();
    // This package generated these artifacts.
    private Map<String,File> artifacts = new HashMap<>();

    public Package(Module m, String n) {
        int c = n.indexOf(":");
        Assert.check(c != -1);
        Assert.check(m.name().equals(m.name()));
        name = n;
        dirname = n.replace('.', File.separatorChar);
        if (m.name().length() > 0) {
            // There is a module here, prefix the module dir name to the path.
            dirname = m.dirname()+File.separatorChar+dirname;
        }
    }

    public Module mod() { return mod; }
    public String name() { return name; }
    public String dirname() { return dirname; }
    public Map<String,Source> sources() { return sources; }
    public Map<String,File> artifacts() { return artifacts; }
    public PubApi getPubApi() { return pubApi; }

    public Map<String,Set<String>> typeDependencies() { return dependencies; }
    public Map<String,Set<String>> typeClasspathDependencies() { return cpDependencies; }

    public Set<String> dependents() { return dependents; }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Package pac) && name.equals(pac.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(Package o) {
        return name.compareTo(o.name);
    }

    public void addSource(Source s) {
        sources.put(s.file().getPath(), s);
    }

    private static Pattern DEP_PATTERN = Pattern.compile("(.*) -> (.*)");
    public void parseAndAddDependency(String d, boolean cp) {
        Matcher m = DEP_PATTERN.matcher(d);
        if (!m.matches())
            throw new IllegalArgumentException("Bad dependency string: " + d);
        addDependency(m.group(1), m.group(2), cp);
    }

    public void addDependency(String fullyQualifiedFrom,
                              String fullyQualifiedTo,
                              boolean cp) {
        Map<String, Set<String>> map = cp ? cpDependencies : dependencies;
        if (!map.containsKey(fullyQualifiedFrom))
            map.put(fullyQualifiedFrom, new HashSet<>());
        map.get(fullyQualifiedFrom).add(fullyQualifiedTo);
    }

    public void addDependent(String d) {
        dependents.add(d);
    }

    /**
     * Check if we have knowledge in the javac state that
     * describe the results of compiling this package before.
     */
    public boolean existsInJavacState() {
        return artifacts.size() > 0 || !pubApi.isEmpty();
    }

    public boolean hasPubApiChanged(PubApi newPubApi) {
        return !newPubApi.isBackwardCompatibleWith(pubApi);
    }

    public void setPubapi(PubApi newPubApi) {
        pubApi = newPubApi;
    }

    public void setDependencies(Map<String, Set<String>> ds, boolean cp) {
        (cp ? cpDependencies : dependencies).clear();
        for (String fullyQualifiedFrom : ds.keySet())
            for (String fullyQualifiedTo : ds.get(fullyQualifiedFrom))
                addDependency(fullyQualifiedFrom, fullyQualifiedTo, cp);
    }

    public void save(StringBuilder b) {
        b.append("P ").append(name).append("\n");
        Source.saveSources(sources, b);
        saveDependencies(b);
        savePubapi(b);
        saveArtifacts(b);
    }

    public static Package load(Module module, String l) {
        String name = l.substring(2);
        return new Package(module, name);
    }

    public void saveDependencies(StringBuilder b) {

        // Dependencies where *to* is among sources
        for (String fullyQualifiedFrom : dependencies.keySet()) {
            for (String fullyQualifiedTo : dependencies.get(fullyQualifiedFrom)) {
                b.append(String.format("D S %s -> %s%n", fullyQualifiedFrom, fullyQualifiedTo));
            }
        }

        // Dependencies where *to* is on class path
        for (String fullyQualifiedFrom : cpDependencies.keySet()) {
            for (String fullyQualifiedTo : cpDependencies.get(fullyQualifiedFrom)) {
                b.append(String.format("D C %s -> %s%n", fullyQualifiedFrom, fullyQualifiedTo));
            }
        }
    }

    public void savePubapi(StringBuilder b) {
        pubApi.asListOfStrings()
              .stream()
              .flatMap(l -> Stream.of("I ", l, "\n"))
              .forEach(b::append);
    }

    public static void savePackages(Map<String,Package> packages, StringBuilder b) {
        List<String> sorted_packages = new ArrayList<>();
        for (String key : packages.keySet() ) {
            sorted_packages.add(key);
        }
        Collections.sort(sorted_packages);
        for (String s : sorted_packages) {
            Package p = packages.get(s);
            p.save(b);
        }
    }

    public void addArtifact(String a) {
        artifacts.put(a, new File(a));
    }

    public void addArtifact(File f) {
        artifacts.put(f.getPath(), f);
    }

    public void addArtifacts(Set<URI> as) {
        for (URI u : as) {
            addArtifact(new File(u));
        }
    }

    public void setArtifacts(Set<URI> as) {
        Assert.check(!artifacts.isEmpty());
        artifacts = new HashMap<>();
        addArtifacts(as);
    }

    public void loadArtifact(String l) {
        // Find next space after "A ".
        int dp = l.indexOf(' ',2);
        String fn = l.substring(2,dp);
        long last_modified = Long.parseLong(l.substring(dp+1));
        File f = new File(fn);
        if (f.exists() && f.lastModified() != last_modified) {
            // Hmm, the artifact on disk does not have the same last modified
            // timestamp as the information from the build database.
            // We no longer trust the artifact on disk. Delete it.
            // The smart javac wrapper will then rebuild the artifact.
            Log.debug("Removing "+f.getPath()+" since its timestamp does not match javac_state.");
            f.delete();
        }
        artifacts.put(f.getPath(), f);
    }

    public void saveArtifacts(StringBuilder b) {
        List<File> sorted_artifacts = new ArrayList<>();
        for (File f : artifacts.values()) {
            sorted_artifacts.add(f);
        }
        Collections.sort(sorted_artifacts);
        for (File f : sorted_artifacts) {
            // The last modified information is only used
            // to detect tampering with the output dir.
            // If the outputdir has been modified, not by javac,
            // then a mismatch will be detected in the last modified
            // timestamps stored in the build database compared
            // to the timestamps on disk and the artifact will be deleted.

            b.append("A "+f.getPath()+" "+f.lastModified()+"\n");
        }
    }

    /**
     * Always clean out a tainted package before it is recompiled.
     */
    public void deleteArtifacts() {
        for (File a : artifacts.values()) {
            a.delete();
        }
    }
}
