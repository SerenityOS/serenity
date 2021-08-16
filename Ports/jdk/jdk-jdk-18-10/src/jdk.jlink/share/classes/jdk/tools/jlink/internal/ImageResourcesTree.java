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
package jdk.tools.jlink.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A class to build a sorted tree of Resource paths as a tree of ImageLocation.
 *
 */
// XXX Public only due to the JImageTask / JImageTask code duplication
public final class ImageResourcesTree {
    public static boolean isTreeInfoResource(String path) {
        return path.startsWith("/packages") || path.startsWith("/modules");
    }

    /**
     * Path item tree node.
     */
    private static class Node {

        private final String name;
        private final Map<String, Node> children = new TreeMap<>();
        private final Node parent;
        private ImageLocationWriter loc;

        private Node(String name, Node parent) {
            this.name = name;
            this.parent = parent;

            if (parent != null) {
                parent.children.put(name, this);
            }
        }

        public String getPath() {
            if (parent == null) {
                return "/";
            }
            return buildPath(this);
        }

        public String getName() {
            return name;
        }

        public Node getChildren(String name) {
            Node item = children.get(name);
            return item;
        }

        private static String buildPath(Node item) {
            if (item == null) {
                return null;
            }
            String path = buildPath(item.parent);
            if (path == null) {
                return item.getName();
            } else {
                return path + "/" + item.getName();
            }
        }
    }

    private static final class ResourceNode extends Node {

        public ResourceNode(String name, Node parent) {
            super(name, parent);
        }
    }

    private static class PackageNode extends Node {
        /**
         * A reference to a package. Empty packages can be located inside one or
         * more modules. A package with classes exist in only one module.
         */
        final static class PackageReference {

            private final String name;
            private final boolean isEmpty;

            PackageReference(String name, boolean isEmpty) {
                this.name = Objects.requireNonNull(name);
                this.isEmpty = isEmpty;
            }

            @Override
            public String toString() {
                return name + "[empty:" + isEmpty + "]";
            }
        }

        private final Map<String, PackageReference> references = new TreeMap<>();

        PackageNode(String name, Node parent) {
            super(name, parent);
        }

        private void addReference(String name, boolean isEmpty) {
            PackageReference ref = references.get(name);
            if (ref == null || ref.isEmpty) {
                references.put(name, new PackageReference(name, isEmpty));
            }
        }

        private void validate() {
            boolean exists = false;
            for (PackageReference ref : references.values()) {
                if (!ref.isEmpty) {
                    if (exists) {
                        throw new RuntimeException("Multiple modules to contain package "
                                + getName());
                    } else {
                        exists = true;
                    }
                }
            }
        }
    }

    /**
     * Tree of nodes.
     */
    private static final class Tree {

        private final Map<String, Node> directAccess = new HashMap<>();
        private final List<String> paths;
        private final Node root;
        private Node modules;
        private Node packages;

        private Tree(List<String> paths) {
            this.paths = paths;
            root = new Node("", null);
            buildTree();
        }

        private void buildTree() {
            modules = new Node("modules", root);
            directAccess.put(modules.getPath(), modules);

            Map<String, Set<String>> moduleToPackage = new TreeMap<>();
            Map<String, Set<String>> packageToModule = new TreeMap<>();

            for (String p : paths) {
                if (!p.startsWith("/")) {
                    continue;
                }
                String[] split = p.split("/");
                // minimum length is 3 items: /<mod>/<pkg>
                if (split.length < 3) {
                    System.err.println("Resources tree, invalid data structure, "
                            + "skipping " + p);
                    continue;
                }
                Node current = modules;
                String module = null;
                for (int i = 0; i < split.length; i++) {
                    // When a non terminal node is marked as being a resource, something is wrong.
                    // It has been observed some badly created jar file to contain
                    // invalid directory entry marled as not directory (see 8131762)
                    if (current instanceof ResourceNode) {
                        System.err.println("Resources tree, invalid data structure, "
                                + "skipping " + p);
                        continue;
                    }
                    String s = split[i];
                    if (!s.isEmpty()) {
                        // First item, this is the module, simply add a new node to the
                        // tree.
                        if (module == null) {
                            module = s;
                        }
                        Node n = current.children.get(s);
                        if (n == null) {
                            if (i == split.length - 1) { // Leaf
                                n = new ResourceNode(s, current);
                                String pkg = toPackageName(n.parent);
                                //System.err.println("Adding a resource node. pkg " + pkg + ", name " + s);
                                if (pkg != null && !pkg.startsWith("META-INF")) {
                                    Set<String> pkgs = moduleToPackage.get(module);
                                    if (pkgs == null) {
                                        pkgs = new TreeSet<>();
                                        moduleToPackage.put(module, pkgs);
                                    }
                                    pkgs.add(pkg);
                                }
                            } else { // put only sub trees, no leaf
                                n = new Node(s, current);
                                directAccess.put(n.getPath(), n);
                                String pkg = toPackageName(n);
                                if (pkg != null && !pkg.startsWith("META-INF")) {
                                    Set<String> mods = packageToModule.get(pkg);
                                    if (mods == null) {
                                        mods = new TreeSet<>();
                                        packageToModule.put(pkg, mods);
                                    }
                                    mods.add(module);
                                }
                            }
                        }
                        current = n;
                    }
                }
            }
            packages = new Node("packages", root);
            directAccess.put(packages.getPath(), packages);
            // The subset of package nodes that have some content.
            // These packages exist only in a single module.
            for (Map.Entry<String, Set<String>> entry : moduleToPackage.entrySet()) {
                for (String pkg : entry.getValue()) {
                    PackageNode pkgNode = new PackageNode(pkg, packages);
                    pkgNode.addReference(entry.getKey(), false);
                    directAccess.put(pkgNode.getPath(), pkgNode);
                }
            }

            // All packages
            for (Map.Entry<String, Set<String>> entry : packageToModule.entrySet()) {
                // Do we already have a package node?
                PackageNode pkgNode = (PackageNode) packages.getChildren(entry.getKey());
                if (pkgNode == null) {
                    pkgNode = new PackageNode(entry.getKey(), packages);
                }
                for (String module : entry.getValue()) {
                    pkgNode.addReference(module, true);
                }
                directAccess.put(pkgNode.getPath(), pkgNode);
            }
            // Validate that the packages are well formed.
            for (Node n : packages.children.values()) {
                ((PackageNode)n).validate();
            }

        }

        public String toResourceName(Node node) {
            if (!node.children.isEmpty()) {
                throw new RuntimeException("Node is not a resource");
            }
            return removeRadical(node);
        }

        public String getModule(Node node) {
            if (node.parent == null || node.getName().equals("modules")
                    || node.getName().startsWith("packages")) {
                return null;
            }
            String path = removeRadical(node);
            // "/xxx/...";
            path = path.substring(1);
            int i = path.indexOf("/");
            if (i == -1) {
                return path;
            } else {
                return path.substring(0, i);
            }
        }

        public String toPackageName(Node node) {
            if (node.parent == null) {
                return null;
            }
            String path = removeRadical(node.getPath(), "/modules/");
            String module = getModule(node);
            if (path.equals(module)) {
                return null;
            }
            String pkg = removeRadical(path, module + "/");
            return pkg.replaceAll("/", ".");
        }

        public String removeRadical(Node node) {
            return removeRadical(node.getPath(), "/modules");
        }

        private String removeRadical(String path, String str) {
            if (!(path.length() < str.length())) {
                path = path.substring(str.length());
            }
            return path;
        }

        public Node getRoot() {
            return root;
        }

        public Map<String, Node> getMap() {
            return directAccess;
        }
    }

    private static final class LocationsAdder {

        private long offset;
        private final List<byte[]> content = new ArrayList<>();
        private final BasicImageWriter writer;
        private final Tree tree;

        LocationsAdder(Tree tree, long offset, BasicImageWriter writer) {
            this.tree = tree;
            this.offset = offset;
            this.writer = writer;
            addLocations(tree.getRoot());
        }

        private int addLocations(Node current) {
            if (current instanceof PackageNode) {
                PackageNode pkgNode = (PackageNode) current;
                int size = pkgNode.references.size() * 8;
                writer.addLocation(current.getPath(), offset, 0, size);
                offset += size;
            } else {
                int[] ret = new int[current.children.size()];
                int i = 0;
                for (java.util.Map.Entry<String, Node> entry : current.children.entrySet()) {
                    ret[i] = addLocations(entry.getValue());
                    i += 1;
                }
                if (current != tree.getRoot() && !(current instanceof ResourceNode)) {
                    int size = ret.length * 4;
                    writer.addLocation(current.getPath(), offset, 0, size);
                    offset += size;
                }
            }
            return 0;
        }

        private List<byte[]> computeContent() {
            // Map used to associate Tree item with locations offset.
            Map<String, ImageLocationWriter> outLocations = new HashMap<>();
            for (ImageLocationWriter wr : writer.getLocations()) {
                outLocations.put(wr.getFullName(), wr);
            }
            // Attach location to node
            for (Map.Entry<String, ImageLocationWriter> entry : outLocations.entrySet()) {
                Node item = tree.getMap().get(entry.getKey());
                if (item != null) {
                    item.loc = entry.getValue();
                }
            }
            computeContent(tree.getRoot(), outLocations);
            return content;
        }

        private int computeContent(Node current, Map<String, ImageLocationWriter> outLocations) {
            if (current instanceof PackageNode) {
                // /packages/<pkg name>
                PackageNode pkgNode = (PackageNode) current;
                int size = pkgNode.references.size() * 8;
                ByteBuffer buff = ByteBuffer.allocate(size);
                buff.order(writer.getByteOrder());
                for (PackageNode.PackageReference mod : pkgNode.references.values()) {
                    buff.putInt(mod.isEmpty ? 1 : 0);
                    buff.putInt(writer.addString(mod.name));
                }
                byte[] arr = buff.array();
                content.add(arr);
                current.loc = outLocations.get(current.getPath());
            } else {
                int[] ret = new int[current.children.size()];
                int i = 0;
                for (java.util.Map.Entry<String, Node> entry : current.children.entrySet()) {
                    ret[i] = computeContent(entry.getValue(), outLocations);
                    i += 1;
                }
                if (ret.length > 0) {
                    int size = ret.length * 4;
                    ByteBuffer buff = ByteBuffer.allocate(size);
                    buff.order(writer.getByteOrder());
                    for (int val : ret) {
                        buff.putInt(val);
                    }
                    byte[] arr = buff.array();
                    content.add(arr);
                } else {
                    if (current instanceof ResourceNode) {
                        // A resource location, remove "/modules"
                        String s = tree.toResourceName(current);
                        current.loc = outLocations.get(s);
                    } else {
                        // empty "/packages" or empty "/modules" paths
                        current.loc = outLocations.get(current.getPath());
                    }
                }
                if (current.loc == null && current != tree.getRoot()) {
                    System.err.println("Invalid path in metadata, skipping " + current.getPath());
                }
            }
            return current.loc == null ? 0 : current.loc.getLocationOffset();
        }
    }

    private final List<String> paths;
    private final LocationsAdder adder;

    public ImageResourcesTree(long offset, BasicImageWriter writer, List<String> paths) {
        this.paths = new ArrayList<>();
        this.paths.addAll(paths);
        Collections.sort(this.paths);
        Tree tree = new Tree(this.paths);
        adder = new LocationsAdder(tree, offset, writer);
    }

    public void addContent(DataOutputStream out) throws IOException {
        List<byte[]> content = adder.computeContent();
        for (byte[] c : content) {
            out.write(c, 0, c.length);
        }
    }
}
