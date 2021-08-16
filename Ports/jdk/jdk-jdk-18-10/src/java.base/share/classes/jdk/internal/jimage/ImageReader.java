/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.jimage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
public final class ImageReader implements AutoCloseable {
    private final SharedImageReader reader;

    private volatile boolean closed;

    private ImageReader(SharedImageReader reader) {
        this.reader = reader;
    }

    public static ImageReader open(Path imagePath, ByteOrder byteOrder) throws IOException {
        Objects.requireNonNull(imagePath);
        Objects.requireNonNull(byteOrder);

        return SharedImageReader.open(imagePath, byteOrder);
    }

    public static ImageReader open(Path imagePath) throws IOException {
        return open(imagePath, ByteOrder.nativeOrder());
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("image file already closed");
        }
        reader.close(this);
        closed = true;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("image file closed");
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("image file closed");
        }
    }

    // directory management interface
    public Directory getRootDirectory() throws IOException {
        ensureOpen();
        return reader.getRootDirectory();
    }


    public Node findNode(String name) throws IOException {
        ensureOpen();
        return reader.findNode(name);
    }

    public byte[] getResource(Node node) throws IOException {
        ensureOpen();
        return reader.getResource(node);
    }

    public byte[] getResource(Resource rs) throws IOException {
        ensureOpen();
        return reader.getResource(rs);
    }

    public ImageHeader getHeader() {
        requireOpen();
        return reader.getHeader();
    }

    public static void releaseByteBuffer(ByteBuffer buffer) {
        BasicImageReader.releaseByteBuffer(buffer);
    }

    public String getName() {
        requireOpen();
        return reader.getName();
    }

    public ByteOrder getByteOrder() {
        requireOpen();
        return reader.getByteOrder();
    }

    public Path getImagePath() {
        requireOpen();
        return reader.getImagePath();
    }

    public ImageStringsReader getStrings() {
        requireOpen();
        return reader.getStrings();
    }

    public ImageLocation findLocation(String mn, String rn) {
        requireOpen();
        return reader.findLocation(mn, rn);
    }

    public boolean verifyLocation(String mn, String rn) {
        requireOpen();
        return reader.verifyLocation(mn, rn);
    }

    public ImageLocation findLocation(String name) {
        requireOpen();
        return reader.findLocation(name);
    }

    public String[] getEntryNames() {
        requireOpen();
        return reader.getEntryNames();
    }

    public String[] getModuleNames() {
        requireOpen();
        int off = "/modules/".length();
        return reader.findNode("/modules")
                     .getChildren()
                     .stream()
                     .map(Node::getNameString)
                     .map(s -> s.substring(off, s.length()))
                     .toArray(String[]::new);
    }

    public long[] getAttributes(int offset) {
        requireOpen();
        return reader.getAttributes(offset);
    }

    public String getString(int offset) {
        requireOpen();
        return reader.getString(offset);
    }

    public byte[] getResource(String name) {
        requireOpen();
        return reader.getResource(name);
    }

    public byte[] getResource(ImageLocation loc) {
        requireOpen();
        return reader.getResource(loc);
    }

    public ByteBuffer getResourceBuffer(ImageLocation loc) {
        requireOpen();
        return reader.getResourceBuffer(loc);
    }

    public InputStream getResourceStream(ImageLocation loc) {
        requireOpen();
        return reader.getResourceStream(loc);
    }

    private static final class SharedImageReader extends BasicImageReader {
        static final int SIZE_OF_OFFSET = Integer.BYTES;

        static final Map<Path, SharedImageReader> OPEN_FILES = new HashMap<>();

        // List of openers for this shared image.
        final Set<ImageReader> openers;

        // attributes of the .jimage file. jimage file does not contain
        // attributes for the individual resources (yet). We use attributes
        // of the jimage file itself (creation, modification, access times).
        // Iniitalized lazily, see {@link #imageFileAttributes()}.
        BasicFileAttributes imageFileAttributes;

        // directory management implementation
        final HashMap<String, Node> nodes;
        volatile Directory rootDir;

        Directory packagesDir;
        Directory modulesDir;

        private SharedImageReader(Path imagePath, ByteOrder byteOrder) throws IOException {
            super(imagePath, byteOrder);
            this.openers = new HashSet<>();
            this.nodes = new HashMap<>();
        }

        public static ImageReader open(Path imagePath, ByteOrder byteOrder) throws IOException {
            Objects.requireNonNull(imagePath);
            Objects.requireNonNull(byteOrder);

            synchronized (OPEN_FILES) {
                SharedImageReader reader = OPEN_FILES.get(imagePath);

                if (reader == null) {
                    // Will fail with an IOException if wrong byteOrder.
                    reader =  new SharedImageReader(imagePath, byteOrder);
                    OPEN_FILES.put(imagePath, reader);
                } else if (reader.getByteOrder() != byteOrder) {
                    throw new IOException("\"" + reader.getName() + "\" is not an image file");
                }

                ImageReader image = new ImageReader(reader);
                reader.openers.add(image);

                return image;
            }
        }

        public void close(ImageReader image) throws IOException {
            Objects.requireNonNull(image);

            synchronized (OPEN_FILES) {
                if (!openers.remove(image)) {
                    throw new IOException("image file already closed");
                }

                if (openers.isEmpty()) {
                    close();
                    nodes.clear();
                    rootDir = null;

                    if (!OPEN_FILES.remove(this.getImagePath(), this)) {
                        throw new IOException("image file not found in open list");
                    }
                }
            }
        }

        void addOpener(ImageReader reader) {
            synchronized (OPEN_FILES) {
                openers.add(reader);
            }
        }

        boolean removeOpener(ImageReader reader) {
            synchronized (OPEN_FILES) {
                return openers.remove(reader);
            }
        }

        // directory management interface
        Directory getRootDirectory() {
            return buildRootDirectory();
        }

        /**
         * Lazily build a node from a name.
         */
        synchronized Node buildNode(String name) {
            Node n;
            boolean isPackages = name.startsWith("/packages");
            boolean isModules = !isPackages && name.startsWith("/modules");

            if (!(isModules || isPackages)) {
                return null;
            }

            ImageLocation loc = findLocation(name);

            if (loc != null) { // A sub tree node
                if (isPackages) {
                    n = handlePackages(name, loc);
                } else { // modules sub tree
                    n = handleModulesSubTree(name, loc);
                }
            } else { // Asking for a resource? /modules/java.base/java/lang/Object.class
                if (isModules) {
                    n = handleResource(name);
                } else {
                    // Possibly ask for /packages/java.lang/java.base
                    // although /packages/java.base not created
                    n = handleModuleLink(name);
                }
            }
            return n;
        }

        synchronized Directory buildRootDirectory() {
            Directory root = rootDir; // volatile read
            if (root != null) {
                return root;
            }

            root = newDirectory(null, "/");
            root.setIsRootDir();

            // /packages dir
            packagesDir = newDirectory(root, "/packages");
            packagesDir.setIsPackagesDir();

            // /modules dir
            modulesDir = newDirectory(root, "/modules");
            modulesDir.setIsModulesDir();

            root.setCompleted(true);
            return rootDir = root;
        }

        /**
         * To visit sub tree resources.
         */
        interface LocationVisitor {
            void visit(ImageLocation loc);
        }

        void visitLocation(ImageLocation loc, LocationVisitor visitor) {
            byte[] offsets = getResource(loc);
            ByteBuffer buffer = ByteBuffer.wrap(offsets);
            buffer.order(getByteOrder());
            IntBuffer intBuffer = buffer.asIntBuffer();
            for (int i = 0; i < offsets.length / SIZE_OF_OFFSET; i++) {
                int offset = intBuffer.get(i);
                ImageLocation pkgLoc = getLocation(offset);
                visitor.visit(pkgLoc);
            }
        }

        void visitPackageLocation(ImageLocation loc) {
            // Retrieve package name
            String pkgName = getBaseExt(loc);
            // Content is array of offsets in Strings table
            byte[] stringsOffsets = getResource(loc);
            ByteBuffer buffer = ByteBuffer.wrap(stringsOffsets);
            buffer.order(getByteOrder());
            IntBuffer intBuffer = buffer.asIntBuffer();
            // For each module, create a link node.
            for (int i = 0; i < stringsOffsets.length / SIZE_OF_OFFSET; i++) {
                // skip empty state, useless.
                intBuffer.get(i);
                i++;
                int offset = intBuffer.get(i);
                String moduleName = getString(offset);
                Node targetNode = findNode("/modules/" + moduleName);
                if (targetNode != null) {
                    String pkgDirName = packagesDir.getName() + "/" + pkgName;
                    Directory pkgDir = (Directory) nodes.get(pkgDirName);
                    newLinkNode(pkgDir, pkgDir.getName() + "/" + moduleName, targetNode);
                }
            }
        }

        Node handlePackages(String name, ImageLocation loc) {
            long size = loc.getUncompressedSize();
            Node n = null;
            // Only possiblities are /packages, /packages/package/module
            if (name.equals("/packages")) {
                visitLocation(loc, (childloc) -> {
                    findNode(childloc.getFullName());
                });
                packagesDir.setCompleted(true);
                n = packagesDir;
            } else {
                if (size != 0) { // children are offsets to module in StringsTable
                    String pkgName = getBaseExt(loc);
                    Directory pkgDir = newDirectory(packagesDir, packagesDir.getName() + "/" + pkgName);
                    visitPackageLocation(loc);
                    pkgDir.setCompleted(true);
                    n = pkgDir;
                } else { // Link to module
                    String pkgName = loc.getParent();
                    String modName = getBaseExt(loc);
                    Node targetNode = findNode("/modules/" + modName);
                    if (targetNode != null) {
                        String pkgDirName = packagesDir.getName() + "/" + pkgName;
                        Directory pkgDir = (Directory) nodes.get(pkgDirName);
                        Node linkNode = newLinkNode(pkgDir, pkgDir.getName() + "/" + modName, targetNode);
                        n = linkNode;
                    }
                }
            }
            return n;
        }

        // Asking for /packages/package/module although
        // /packages/<pkg>/ not yet created, need to create it
        // prior to return the link to module node.
        Node handleModuleLink(String name) {
            // eg: unresolved /packages/package/module
            // Build /packages/package node
            Node ret = null;
            String radical = "/packages/";
            String path = name;
            if (path.startsWith(radical)) {
                int start = radical.length();
                int pkgEnd = path.indexOf('/', start);
                if (pkgEnd != -1) {
                    String pkg = path.substring(start, pkgEnd);
                    String pkgPath = radical + pkg;
                    Node n = findNode(pkgPath);
                    // If not found means that this is a symbolic link such as:
                    // /packages/java.util/java.base/java/util/Vector.class
                    // and will be done by a retry of the filesystem
                    for (Node child : n.getChildren()) {
                        if (child.name.equals(name)) {
                            ret = child;
                            break;
                        }
                    }
                }
            }
            return ret;
        }

        Node handleModulesSubTree(String name, ImageLocation loc) {
            Node n;
            assert (name.equals(loc.getFullName()));
            Directory dir = makeDirectories(name);
            visitLocation(loc, (childloc) -> {
                String path = childloc.getFullName();
                if (path.startsWith("/modules")) { // a package
                    makeDirectories(path);
                } else { // a resource
                    makeDirectories(childloc.buildName(true, true, false));
                    newResource(dir, childloc);
                }
            });
            dir.setCompleted(true);
            n = dir;
            return n;
        }

        Node handleResource(String name) {
            Node n = null;
            if (!name.startsWith("/modules/")) {
                return null;
            }
            // Make sure that the thing that follows "/modules/" is a module name.
            int moduleEndIndex = name.indexOf('/', "/modules/".length());
            if (moduleEndIndex == -1) {
                return null;
            }
            ImageLocation moduleLoc = findLocation(name.substring(0, moduleEndIndex));
            if (moduleLoc == null || moduleLoc.getModuleOffset() == 0) {
                return null;
            }

            String locationPath = name.substring("/modules".length());
            ImageLocation resourceLoc = findLocation(locationPath);
            if (resourceLoc != null) {
                Directory dir = makeDirectories(resourceLoc.buildName(true, true, false));
                Resource res = newResource(dir, resourceLoc);
                n = res;
            }
            return n;
        }

        String getBaseExt(ImageLocation loc) {
            String base = loc.getBase();
            String ext = loc.getExtension();
            if (ext != null && !ext.isEmpty()) {
                base = base + "." + ext;
            }
            return base;
        }

        synchronized Node findNode(String name) {
            buildRootDirectory();
            Node n = nodes.get(name);
            if (n == null || !n.isCompleted()) {
                n = buildNode(name);
            }
            return n;
        }

        /**
         * Returns the file attributes of the image file.
         */
        BasicFileAttributes imageFileAttributes() {
            BasicFileAttributes attrs = imageFileAttributes;
            if (attrs == null) {
                try {
                    Path file = getImagePath();
                    attrs = Files.readAttributes(file, BasicFileAttributes.class);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
                imageFileAttributes = attrs;
            }
            return attrs;
        }

        Directory newDirectory(Directory parent, String name) {
            Directory dir = Directory.create(parent, name, imageFileAttributes());
            nodes.put(dir.getName(), dir);
            return dir;
        }

        Resource newResource(Directory parent, ImageLocation loc) {
            Resource res = Resource.create(parent, loc, imageFileAttributes());
            nodes.put(res.getName(), res);
            return res;
        }

        LinkNode newLinkNode(Directory dir, String name, Node link) {
            LinkNode linkNode = LinkNode.create(dir, name, link);
            nodes.put(linkNode.getName(), linkNode);
            return linkNode;
        }

        Directory makeDirectories(String parent) {
            Directory last = rootDir;
            for (int offset = parent.indexOf('/', 1);
                    offset != -1;
                    offset = parent.indexOf('/', offset + 1)) {
                String dir = parent.substring(0, offset);
                last = makeDirectory(dir, last);
            }
            return makeDirectory(parent, last);

        }

        Directory makeDirectory(String dir, Directory last) {
            Directory nextDir = (Directory) nodes.get(dir);
            if (nextDir == null) {
                nextDir = newDirectory(last, dir);
            }
            return nextDir;
        }

        byte[] getResource(Node node) throws IOException {
            if (node.isResource()) {
                return super.getResource(node.getLocation());
            }
            throw new IOException("Not a resource: " + node);
        }

        byte[] getResource(Resource rs) throws IOException {
            return super.getResource(rs.getLocation());
        }
    }

    // jimage file does not store directory structure. We build nodes
    // using the "path" strings found in the jimage file.
    // Node can be a directory or a resource
    public abstract static class Node {
        private static final int ROOT_DIR = 0b0000_0000_0000_0001;
        private static final int PACKAGES_DIR = 0b0000_0000_0000_0010;
        private static final int MODULES_DIR = 0b0000_0000_0000_0100;

        private int flags;
        private final String name;
        private final BasicFileAttributes fileAttrs;
        private boolean completed;

        protected Node(String name, BasicFileAttributes fileAttrs) {
            this.name = Objects.requireNonNull(name);
            this.fileAttrs = Objects.requireNonNull(fileAttrs);
        }

        /**
         * A node is completed when all its direct children have been built.
         *
         * @return
         */
        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public final void setIsRootDir() {
            flags |= ROOT_DIR;
        }

        public final boolean isRootDir() {
            return (flags & ROOT_DIR) != 0;
        }

        public final void setIsPackagesDir() {
            flags |= PACKAGES_DIR;
        }

        public final boolean isPackagesDir() {
            return (flags & PACKAGES_DIR) != 0;
        }

        public final void setIsModulesDir() {
            flags |= MODULES_DIR;
        }

        public final boolean isModulesDir() {
            return (flags & MODULES_DIR) != 0;
        }

        public final String getName() {
            return name;
        }

        public final BasicFileAttributes getFileAttributes() {
            return fileAttrs;
        }

        // resolve this Node (if this is a soft link, get underlying Node)
        public final Node resolveLink() {
            return resolveLink(false);
        }

        public Node resolveLink(boolean recursive) {
            return this;
        }

        // is this a soft link Node?
        public boolean isLink() {
            return false;
        }

        public boolean isDirectory() {
            return false;
        }

        public List<Node> getChildren() {
            throw new IllegalArgumentException("not a directory: " + getNameString());
        }

        public boolean isResource() {
            return false;
        }

        public ImageLocation getLocation() {
            throw new IllegalArgumentException("not a resource: " + getNameString());
        }

        public long size() {
            return 0L;
        }

        public long compressedSize() {
            return 0L;
        }

        public String extension() {
            return null;
        }

        public long contentOffset() {
            return 0L;
        }

        public final FileTime creationTime() {
            return fileAttrs.creationTime();
        }

        public final FileTime lastAccessTime() {
            return fileAttrs.lastAccessTime();
        }

        public final FileTime lastModifiedTime() {
            return fileAttrs.lastModifiedTime();
        }

        public final String getNameString() {
            return name;
        }

        @Override
        public final String toString() {
            return getNameString();
        }

        @Override
        public final int hashCode() {
            return name.hashCode();
        }

        @Override
        public final boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (other instanceof Node) {
                return name.equals(((Node) other).name);
            }

            return false;
        }
    }

    // directory node - directory has full path name without '/' at end.
    static final class Directory extends Node {
        private final List<Node> children;

        private Directory(String name, BasicFileAttributes fileAttrs) {
            super(name, fileAttrs);
            children = new ArrayList<>();
        }

        static Directory create(Directory parent, String name, BasicFileAttributes fileAttrs) {
            Directory d = new Directory(name, fileAttrs);
            if (parent != null) {
                parent.addChild(d);
            }
            return d;
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public List<Node> getChildren() {
            return Collections.unmodifiableList(children);
        }

        void addChild(Node node) {
            children.add(node);
        }

        public void walk(Consumer<? super Node> consumer) {
            consumer.accept(this);
            for (Node child : children) {
                if (child.isDirectory()) {
                    ((Directory)child).walk(consumer);
                } else {
                    consumer.accept(child);
                }
            }
        }
    }

    // "resource" is .class or any other resource (compressed/uncompressed) in a jimage.
    // full path of the resource is the "name" of the resource.
    static class Resource extends Node {
        private final ImageLocation loc;

        private Resource(ImageLocation loc, BasicFileAttributes fileAttrs) {
            super(loc.getFullName(true), fileAttrs);
            this.loc = loc;
        }

        static Resource create(Directory parent, ImageLocation loc, BasicFileAttributes fileAttrs) {
            Resource rs = new Resource(loc, fileAttrs);
            parent.addChild(rs);
            return rs;
        }

        @Override
        public boolean isCompleted() {
            return true;
        }

        @Override
        public boolean isResource() {
            return true;
        }

        @Override
        public ImageLocation getLocation() {
            return loc;
        }

        @Override
        public long size() {
            return loc.getUncompressedSize();
        }

        @Override
        public long compressedSize() {
            return loc.getCompressedSize();
        }

        @Override
        public String extension() {
            return loc.getExtension();
        }

        @Override
        public long contentOffset() {
            return loc.getContentOffset();
        }
    }

    // represents a soft link to another Node
    static class LinkNode extends Node {
        private final Node link;

        private LinkNode(String name, Node link) {
            super(name, link.getFileAttributes());
            this.link = link;
        }

        static LinkNode create(Directory parent, String name, Node link) {
            LinkNode ln = new LinkNode(name, link);
            parent.addChild(ln);
            return ln;
        }

        @Override
        public boolean isCompleted() {
            return true;
        }

        @Override
        public Node resolveLink(boolean recursive) {
            return (recursive && link instanceof LinkNode) ? ((LinkNode)link).resolveLink(true) : link;
        }

        @Override
        public boolean isLink() {
            return true;
        }
    }
}
