/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import sun.security.action.GetPropertyAction;

/**
 * Base implementation of FileSystem for Unix-like implementations.
 */

abstract class UnixFileSystem
    extends FileSystem
{
    private final UnixFileSystemProvider provider;
    private final byte[] defaultDirectory;
    private final boolean needToResolveAgainstDefaultDirectory;
    private final UnixPath rootDirectory;

    // package-private
    UnixFileSystem(UnixFileSystemProvider provider, String dir) {
        this.provider = provider;
        this.defaultDirectory = Util.toBytes(UnixPath.normalizeAndCheck(dir));
        if (this.defaultDirectory[0] != '/') {
            throw new RuntimeException("default directory must be absolute");
        }

        // if process-wide chdir is allowed or default directory is not the
        // process working directory then paths must be resolved against the
        // default directory.
        String propValue = GetPropertyAction
                .privilegedGetProperty("sun.nio.fs.chdirAllowed", "false");
        boolean chdirAllowed = propValue.isEmpty() ? true : Boolean.parseBoolean(propValue);
        if (chdirAllowed) {
            this.needToResolveAgainstDefaultDirectory = true;
        } else {
            byte[] cwd = UnixNativeDispatcher.getcwd();
            boolean defaultIsCwd = (cwd.length == defaultDirectory.length);
            if (defaultIsCwd) {
                for (int i=0; i<cwd.length; i++) {
                    if (cwd[i] != defaultDirectory[i]) {
                        defaultIsCwd = false;
                        break;
                    }
                }
            }
            this.needToResolveAgainstDefaultDirectory = !defaultIsCwd;
        }

        // the root directory
        this.rootDirectory = new UnixPath(this, "/");
    }

    // package-private
    byte[] defaultDirectory() {
        return defaultDirectory;
    }

    boolean needToResolveAgainstDefaultDirectory() {
        return needToResolveAgainstDefaultDirectory;
    }

    UnixPath rootDirectory() {
        return rootDirectory;
    }

    static List<String> standardFileAttributeViews() {
        return Arrays.asList("basic", "posix", "unix", "owner");
    }

    @Override
    public final FileSystemProvider provider() {
        return provider;
    }

    @Override
    public final String getSeparator() {
        return "/";
    }

    @Override
    public final boolean isOpen() {
        return true;
    }

    @Override
    public final boolean isReadOnly() {
        return false;
    }

    @Override
    public final void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Copies non-POSIX attributes from the source to target file.
     *
     * Copying a file preserving attributes, or moving a file, will preserve
     * the file owner/group/permissions/timestamps but it does not preserve
     * other non-POSIX attributes. This method is invoked by the
     * copy or move operation to preserve these attributes. It should copy
     * extended attributes, ACLs, or other attributes.
     *
     * @param   sfd
     *          Open file descriptor to source file
     * @param   tfd
     *          Open file descriptor to target file
     */
    void copyNonPosixAttributes(int sfd, int tfd) {
        // no-op by default
    }

    /**
     * Unix systems only have a single root directory (/)
     */
    @Override
    public final Iterable<Path> getRootDirectories() {
        final List<Path> allowedList =
           Collections.unmodifiableList(Arrays.asList((Path)rootDirectory));
        return new Iterable<>() {
            public Iterator<Path> iterator() {
                try {
                    @SuppressWarnings("removal")
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null)
                        sm.checkRead(rootDirectory.toString());
                    return allowedList.iterator();
                } catch (SecurityException x) {
                    List<Path> disallowed = Collections.emptyList();
                    return disallowed.iterator();
                }
            }
        };
    }

    /**
     * Returns object to iterate over entries in mounttab or equivalent
     */
    abstract Iterable<UnixMountEntry> getMountEntries();

    /**
     * Returns a FileStore to represent the file system for the given mount
     * mount.
     */
    abstract FileStore getFileStore(UnixMountEntry entry) throws IOException;

    /**
     * Iterator returned by getFileStores method.
     */
    private class FileStoreIterator implements Iterator<FileStore> {
        private final Iterator<UnixMountEntry> entries;
        private FileStore next;

        FileStoreIterator() {
            this.entries = getMountEntries().iterator();
        }

        private FileStore readNext() {
            assert Thread.holdsLock(this);
            for (;;) {
                if (!entries.hasNext())
                    return null;
                UnixMountEntry entry = entries.next();

                // skip entries with the "ignore" option
                if (entry.isIgnored())
                    continue;

                // check permission to read mount point
                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    try {
                        sm.checkRead(Util.toString(entry.dir()));
                    } catch (SecurityException x) {
                        continue;
                    }
                }
                try {
                    return getFileStore(entry);
                } catch (IOException ignore) {
                    // ignore as per spec
                }
            }
        }

        @Override
        public synchronized boolean hasNext() {
            if (next != null)
                return true;
            next = readNext();
            return next != null;
        }

        @Override
        public synchronized FileStore next() {
            if (next == null)
                next = readNext();
            if (next == null) {
                throw new NoSuchElementException();
            } else {
                FileStore result = next;
                next = null;
                return result;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public final Iterable<FileStore> getFileStores() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkPermission(new RuntimePermission("getFileStoreAttributes"));
            } catch (SecurityException se) {
                return Collections.emptyList();
            }
        }
        return new Iterable<>() {
            public Iterator<FileStore> iterator() {
                return new FileStoreIterator();
            }
        };
    }

    @Override
    public final Path getPath(String first, String... more) {
        Objects.requireNonNull(first);
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment: more) {
                if (!segment.isEmpty()) {
                    if (sb.length() > 0)
                        sb.append('/');
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return new UnixPath(this, path);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndInput) {
        int pos = syntaxAndInput.indexOf(':');
        if (pos <= 0 || pos == syntaxAndInput.length())
            throw new IllegalArgumentException();
        String syntax = syntaxAndInput.substring(0, pos);
        String input = syntaxAndInput.substring(pos+1);

        String expr;
        if (syntax.equalsIgnoreCase(GLOB_SYNTAX)) {
            expr = Globs.toUnixRegexPattern(input);
        } else {
            if (syntax.equalsIgnoreCase(REGEX_SYNTAX)) {
                expr = input;
            } else {
                throw new UnsupportedOperationException("Syntax '" + syntax +
                    "' not recognized");
            }
        }

        // return matcher
        final Pattern pattern = compilePathMatchPattern(expr);

        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return pattern.matcher(path.toString()).matches();
            }
        };
    }

    private static final String GLOB_SYNTAX = "glob";
    private static final String REGEX_SYNTAX = "regex";

    @Override
    public final UserPrincipalLookupService getUserPrincipalLookupService() {
        return LookupService.instance;
    }

    private static class LookupService {
        static final UserPrincipalLookupService instance =
            new UserPrincipalLookupService() {
                @Override
                public UserPrincipal lookupPrincipalByName(String name)
                    throws IOException
                {
                    return UnixUserPrincipals.lookupUser(name);
                }

                @Override
                public GroupPrincipal lookupPrincipalByGroupName(String group)
                    throws IOException
                {
                    return UnixUserPrincipals.lookupGroup(group);
                }
            };
    }

    // Override if the platform has different path match requirement, such as
    // case insensitive or Unicode canonical equal on MacOSX
    Pattern compilePathMatchPattern(String expr) {
        return Pattern.compile(expr);
    }

    // Override if the platform uses different Unicode normalization form
    // for native file path. For example on MacOSX, the native path is stored
    // in Unicode NFD form.
    String normalizeNativePath(String path) {
        return path;
    }

    // Override if the native file path use non-NFC form. For example on MacOSX,
    // the native path is stored in Unicode NFD form, the path need to be
    // normalized back to NFC before passed back to Java level.
    String normalizeJavaPath(String path) {
        return path;
    }
}
