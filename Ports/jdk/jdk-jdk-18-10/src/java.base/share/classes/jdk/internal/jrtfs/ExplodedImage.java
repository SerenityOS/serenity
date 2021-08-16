/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.jrtfs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.internal.jimage.ImageReader.Node;

/**
 * A jrt file system built on $JAVA_HOME/modules directory ('exploded modules
 * build')
 *
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
class ExplodedImage extends SystemImage {

    private static final String MODULES = "/modules/";
    private static final String PACKAGES = "/packages/";
    private static final int PACKAGES_LEN = PACKAGES.length();

    private final FileSystem defaultFS;
    private final String separator;
    private final Map<String, PathNode> nodes = Collections.synchronizedMap(new HashMap<>());
    private final BasicFileAttributes modulesDirAttrs;

    ExplodedImage(Path modulesDir) throws IOException {
        defaultFS = FileSystems.getDefault();
        String str = defaultFS.getSeparator();
        separator = str.equals("/") ? null : str;
        modulesDirAttrs = Files.readAttributes(modulesDir, BasicFileAttributes.class);
        initNodes();
    }

    // A Node that is backed by actual default file system Path
    private final class PathNode extends Node {

        // Path in underlying default file system
        private Path path;
        private PathNode link;
        private List<Node> children;

        PathNode(String name, Path path, BasicFileAttributes attrs) {  // path
            super(name, attrs);
            this.path = path;
        }

        PathNode(String name, Node link) {              // link
            super(name, link.getFileAttributes());
            this.link = (PathNode)link;
        }

        PathNode(String name, List<Node> children) {    // dir
            super(name, modulesDirAttrs);
            this.children = children;
        }

        @Override
        public boolean isDirectory() {
            return children != null ||
                   (link == null && getFileAttributes().isDirectory());
        }

        @Override
        public boolean isLink() {
            return link != null;
        }

        @Override
        public PathNode resolveLink(boolean recursive) {
            if (link == null)
                return this;
            return recursive && link.isLink() ? link.resolveLink(true) : link;
        }

        byte[] getContent() throws IOException {
            if (!getFileAttributes().isRegularFile())
                throw new FileSystemException(getName() + " is not file");
            return Files.readAllBytes(path);
        }

        @Override
        public List<Node> getChildren() {
            if (!isDirectory())
                throw new IllegalArgumentException("not a directory: " + getNameString());
            if (children == null) {
                List<Node> list = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path p : stream) {
                        p = explodedModulesDir.relativize(p);
                        String pName = MODULES + nativeSlashToFrontSlash(p.toString());
                        Node node = findNode(pName);
                        if (node != null) {  // findNode may choose to hide certain files!
                            list.add(node);
                        }
                    }
                } catch (IOException x) {
                    return null;
                }
                children = list;
            }
            return children;
        }

        @Override
        public long size() {
            try {
                return isDirectory() ? 0 : Files.size(path);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    @Override
    public void close() throws IOException {
        nodes.clear();
    }

    @Override
    public byte[] getResource(Node node) throws IOException {
        return ((PathNode)node).getContent();
    }

    // find Node for the given Path
    @Override
    public synchronized Node findNode(String str) {
        Node node = findModulesNode(str);
        if (node != null) {
            return node;
        }
        // lazily created for paths like /packages/<package>/<module>/xyz
        // For example /packages/java.lang/java.base/java/lang/
        if (str.startsWith(PACKAGES)) {
            // pkgEndIdx marks end of <package> part
            int pkgEndIdx = str.indexOf('/', PACKAGES_LEN);
            if (pkgEndIdx != -1) {
                // modEndIdx marks end of <module> part
                int modEndIdx = str.indexOf('/', pkgEndIdx + 1);
                if (modEndIdx != -1) {
                    // make sure we have such module link!
                    // ie., /packages/<package>/<module> is valid
                    Node linkNode = nodes.get(str.substring(0, modEndIdx));
                    if (linkNode == null || !linkNode.isLink()) {
                        return null;
                    }
                    // map to "/modules/zyz" path and return that node
                    // For example, "/modules/java.base/java/lang" for
                    // "/packages/java.lang/java.base/java/lang".
                    String mod = MODULES + str.substring(pkgEndIdx + 1);
                    return findModulesNode(mod);
                }
            }
        }
        return null;
    }

    // find a Node for a path that starts like "/modules/..."
    Node findModulesNode(String str) {
        PathNode node = nodes.get(str);
        if (node != null) {
            return node;
        }
        // lazily created "/modules/xyz/abc/" Node
        // This is mapped to default file system path "<JDK_MODULES_DIR>/xyz/abc"
        Path p = underlyingPath(str);
        if (p != null) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                if (attrs.isRegularFile()) {
                    Path f = p.getFileName();
                    if (f.toString().startsWith("_the."))
                        return null;
                }
                node = new PathNode(str, p, attrs);
                nodes.put(str, node);
                return node;
            } catch (IOException x) {
                // does not exists or unable to determine
            }
        }
        return null;
    }

    Path underlyingPath(String str) {
        if (str.startsWith(MODULES)) {
            str = frontSlashToNativeSlash(str.substring("/modules".length()));
            return defaultFS.getPath(explodedModulesDir.toString(), str);
        }
        return null;
    }

    // convert "/" to platform path separator
    private String frontSlashToNativeSlash(String str) {
        return separator == null ? str : str.replace("/", separator);
    }

    // convert platform path separator to "/"
    private String nativeSlashToFrontSlash(String str) {
        return separator == null ? str : str.replace(separator, "/");
    }

    // convert "/"s to "."s
    private String slashesToDots(String str) {
        return str.replace(separator != null ? separator : "/", ".");
    }

    // initialize file system Nodes
    private void initNodes() throws IOException {
        // same package prefix may exist in mutliple modules. This Map
        // is filled by walking "jdk modules" directory recursively!
        Map<String, List<String>> packageToModules = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(explodedModulesDir)) {
            for (Path module : stream) {
                if (Files.isDirectory(module)) {
                    String moduleName = module.getFileName().toString();
                    // make sure "/modules/<moduleName>" is created
                    findModulesNode(MODULES + moduleName);
                    Files.walk(module).filter(Files::isDirectory).forEach((p) -> {
                        p = module.relativize(p);
                        String pkgName = slashesToDots(p.toString());
                        // skip META-INFO and empty strings
                        if (!pkgName.isEmpty() && !pkgName.startsWith("META-INF")) {
                            List<String> moduleNames = packageToModules.get(pkgName);
                            if (moduleNames == null) {
                                moduleNames = new ArrayList<>();
                                packageToModules.put(pkgName, moduleNames);
                            }
                            moduleNames.add(moduleName);
                        }
                    });
                }
            }
        }
        // create "/modules" directory
        // "nodes" map contains only /modules/<foo> nodes only so far and so add all as children of /modules
        PathNode modulesDir = new PathNode("/modules", new ArrayList<>(nodes.values()));
        nodes.put(modulesDir.getName(), modulesDir);

        // create children under "/packages"
        List<Node> packagesChildren = new ArrayList<>(packageToModules.size());
        for (Map.Entry<String, List<String>> entry : packageToModules.entrySet()) {
            String pkgName = entry.getKey();
            List<String> moduleNameList = entry.getValue();
            List<Node> moduleLinkNodes = new ArrayList<>(moduleNameList.size());
            for (String moduleName : moduleNameList) {
                Node moduleNode = findModulesNode(MODULES + moduleName);
                PathNode linkNode = new PathNode(PACKAGES + pkgName + "/" + moduleName, moduleNode);
                nodes.put(linkNode.getName(), linkNode);
                moduleLinkNodes.add(linkNode);
            }
            PathNode pkgDir = new PathNode(PACKAGES + pkgName, moduleLinkNodes);
            nodes.put(pkgDir.getName(), pkgDir);
            packagesChildren.add(pkgDir);
        }
        // "/packages" dir
        PathNode packagesDir = new PathNode("/packages", packagesChildren);
        nodes.put(packagesDir.getName(), packagesDir);

        // finally "/" dir!
        List<Node> rootChildren = new ArrayList<>();
        rootChildren.add(packagesDir);
        rootChildren.add(modulesDir);
        PathNode root = new PathNode("/", rootChildren);
        nodes.put(root.getName(), root);
    }
}
