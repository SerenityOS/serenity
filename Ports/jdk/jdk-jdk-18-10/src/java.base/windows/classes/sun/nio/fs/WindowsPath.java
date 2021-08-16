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
import java.io.*;
import java.net.URI;
import java.util.*;
import java.lang.ref.WeakReference;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * Windows implementation of Path
 */

class WindowsPath implements Path {

    // The maximum path that does not require long path prefix. On Windows
    // the maximum path is 260 minus 1 (NUL) but for directories it is 260
    // minus 12 minus 1 (to allow for the creation of a 8.3 file in the
    // directory).
    private static final int MAX_PATH = 247;

    // Maximum extended-length path
    private static final int MAX_LONG_PATH = 32000;

    // FIXME - eliminate this reference to reduce space
    private final WindowsFileSystem fs;

    // path type
    private final WindowsPathType type;
    // root component (may be empty)
    private final String root;
    // normalized path
    private final String path;

    // the path to use in Win32 calls. This differs from path for relative
    // paths and has a long path prefix for all paths longer than MAX_PATH.
    private volatile WeakReference<String> pathForWin32Calls;

    // offsets into name components (computed lazily)
    private volatile Integer[] offsets;

    // computed hash code (computed lazily, no need to be volatile)
    private int hash;


    /**
     * Initializes a new instance of this class.
     */
    private WindowsPath(WindowsFileSystem fs,
                        WindowsPathType type,
                        String root,
                        String path)
    {
        this.fs = fs;
        this.type = type;
        this.root = root;
        this.path = path;
    }

    /**
     * Creates a Path by parsing the given path.
     */
    static WindowsPath parse(WindowsFileSystem fs, String path) {
        WindowsPathParser.Result result = WindowsPathParser.parse(path);
        return new WindowsPath(fs, result.type(), result.root(), result.path());
    }

    /**
     * Creates a Path from a given path that is known to be normalized.
     */
    static WindowsPath createFromNormalizedPath(WindowsFileSystem fs,
                                                String path,
                                                BasicFileAttributes attrs)
    {
        try {
            WindowsPathParser.Result result =
                WindowsPathParser.parseNormalizedPath(path);
            if (attrs == null) {
                return new WindowsPath(fs,
                                       result.type(),
                                       result.root(),
                                       result.path());
            } else {
                return new WindowsPathWithAttributes(fs,
                                                     result.type(),
                                                     result.root(),
                                                     result.path(),
                                                     attrs);
            }
        } catch (InvalidPathException x) {
            throw new AssertionError(x.getMessage());
        }
    }

    /**
     * Creates a WindowsPath from a given path that is known to be normalized.
     */
    static WindowsPath createFromNormalizedPath(WindowsFileSystem fs,
                                                String path)
    {
        return createFromNormalizedPath(fs, path, null);
    }

    /**
     * Special implementation with attached/cached attributes (used to quicken
     * file tree traversal)
     */
    private static class WindowsPathWithAttributes
        extends WindowsPath implements BasicFileAttributesHolder
    {
        final WeakReference<BasicFileAttributes> ref;

        WindowsPathWithAttributes(WindowsFileSystem fs,
                                  WindowsPathType type,
                                  String root,
                                  String path,
                                  BasicFileAttributes attrs)
        {
            super(fs, type, root, path);
            ref = new WeakReference<BasicFileAttributes>(attrs);
        }

        @Override
        public BasicFileAttributes get() {
            return ref.get();
        }

        @Override
        public void invalidate() {
            ref.clear();
        }

        // no need to override equals/hashCode.
    }

    // use this message when throwing exceptions
    String getPathForExceptionMessage() {
        return path;
    }

    // use this path for permission checks
    String getPathForPermissionCheck() {
        return path;
    }

    // use this path for Win32 calls
    // This method will prefix long paths with \\?\ or \\?\UNC as required.
    String getPathForWin32Calls() throws WindowsException {
        // short absolute paths can be used directly
        if (isAbsolute() && path.length() <= MAX_PATH)
            return path;

        // return cached values if available
        WeakReference<String> ref = pathForWin32Calls;
        String resolved = (ref != null) ? ref.get() : null;
        if (resolved != null) {
            // Win32 path already available
            return resolved;
        }

        // resolve against default directory
        resolved = getAbsolutePath();

        // Long paths need to have "." and ".." removed and be prefixed with
        // "\\?\". Note that it is okay to remove ".." even when it follows
        // a link - for example, it is okay for foo/link/../bar to be changed
        // to foo/bar. The reason is that Win32 APIs to access foo/link/../bar
        // will access foo/bar anyway (which differs to Unix systems)
        if (resolved.length() > MAX_PATH) {
            if (resolved.length() > MAX_LONG_PATH) {
                throw new WindowsException("Cannot access file with path exceeding "
                    + MAX_LONG_PATH + " characters");
            }
            resolved = addPrefixIfNeeded(GetFullPathName(resolved));
        }

        // cache the resolved path (except drive relative paths as the working
        // directory on removal media devices can change during the lifetime
        // of the VM)
        if (type != WindowsPathType.DRIVE_RELATIVE) {
            synchronized (this) {
                pathForWin32Calls = new WeakReference<String>(resolved);
            }
        }
        return resolved;
    }

    // return this path resolved against the file system's default directory
    private String getAbsolutePath() throws WindowsException {
        if (isAbsolute())
            return path;

        // Relative path ("foo" for example)
        if (type == WindowsPathType.RELATIVE) {
            String defaultDirectory = getFileSystem().defaultDirectory();
            if (isEmpty())
                return defaultDirectory;
            if (defaultDirectory.endsWith("\\")) {
                return defaultDirectory + path;
            } else {
                StringBuilder sb =
                    new StringBuilder(defaultDirectory.length() + path.length() + 1);
                return sb.append(defaultDirectory).append('\\').append(path).toString();
            }
        }

        // Directory relative path ("\foo" for example)
        if (type == WindowsPathType.DIRECTORY_RELATIVE) {
            String defaultRoot = getFileSystem().defaultRoot();
            return defaultRoot + path.substring(1);
        }

        // Drive relative path ("C:foo" for example).
        if (isSameDrive(root, getFileSystem().defaultRoot())) {
            // relative to default directory
            String remaining = path.substring(root.length());
            String defaultDirectory = getFileSystem().defaultDirectory();
            if (remaining.isEmpty()) {
                return defaultDirectory;
            } else if (defaultDirectory.endsWith("\\")) {
                 return defaultDirectory + remaining;
            } else {
                return defaultDirectory + "\\" + remaining;
            }
        } else {
            // relative to some other drive
            String wd;
            try {
                int dt = GetDriveType(root + "\\");
                if (dt == DRIVE_UNKNOWN || dt == DRIVE_NO_ROOT_DIR)
                    throw new WindowsException("");
                wd = GetFullPathName(root + ".");
            } catch (WindowsException x) {
                throw new WindowsException("Unable to get working directory of drive '" +
                    Character.toUpperCase(root.charAt(0)) + "'");
            }
            String result = wd;
            if (wd.endsWith("\\")) {
                result += path.substring(root.length());
            } else {
                if (path.length() > root.length())
                    result += "\\" + path.substring(root.length());
            }
            return result;
        }
    }

    // returns true if same drive letter
    private static boolean isSameDrive(String root1, String root2) {
        return Character.toUpperCase(root1.charAt(0)) ==
               Character.toUpperCase(root2.charAt(0));
    }

    // Add long path prefix to path if required
    static String addPrefixIfNeeded(String path) {
        if (path.length() > MAX_PATH) {
            if (path.startsWith("\\\\")) {
                path = "\\\\?\\UNC" + path.substring(1, path.length());
            } else {
                path = "\\\\?\\" + path;
            }
        }
        return path;
    }

    @Override
    public WindowsFileSystem getFileSystem() {
        return fs;
    }

    // -- Path operations --

    private boolean isEmpty() {
        return path.isEmpty();
    }

    private WindowsPath emptyPath() {
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", "");
    }

    @Override
    public Path getFileName() {
        int len = path.length();
        // represents empty path
        if (len == 0)
            return this;
        // represents root component only
        if (root.length() == len)
            return null;
        int off = path.lastIndexOf('\\');
        if (off < root.length())
            off = root.length();
        else
            off++;
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", path.substring(off));
    }

    @Override
    public WindowsPath getParent() {
        // represents root component only
        if (root.length() == path.length())
            return null;
        int off = path.lastIndexOf('\\');
        if (off < root.length())
            return getRoot();
        else
            return new WindowsPath(getFileSystem(),
                                   type,
                                   root,
                                   path.substring(0, off));
    }

    @Override
    public WindowsPath getRoot() {
        if (root.isEmpty())
            return null;
        return new WindowsPath(getFileSystem(), type, root, root);
    }

    // package-private
    WindowsPathType type() {
        return type;
    }

    // package-private
    boolean isUnc() {
        return type == WindowsPathType.UNC;
    }

    boolean needsSlashWhenResolving() {
        if (path.endsWith("\\"))
            return false;
        return path.length() > root.length();
    }

    @Override
    public boolean isAbsolute() {
        return type == WindowsPathType.ABSOLUTE || type == WindowsPathType.UNC;
    }

    static WindowsPath toWindowsPath(Path path) {
        if (path == null)
            throw new NullPointerException();
        if (!(path instanceof WindowsPath)) {
            throw new ProviderMismatchException();
        }
        return (WindowsPath)path;
    }

    // return true if this path has "." or ".."
    private boolean hasDotOrDotDot() {
        int n = getNameCount();
        for (int i=0; i<n; i++) {
            String name = elementAsString(i);
            if (name.length() == 1 && name.charAt(0) == '.')
                return true;
            if (name.length() == 2
                    && name.charAt(0) == '.' && name.charAt(1) == '.')
                return true;
        }
        return false;
    }

    @Override
    public WindowsPath relativize(Path obj) {
        WindowsPath child = toWindowsPath(obj);
        if (this.equals(child))
            return emptyPath();

        // can only relativize paths of the same type
        if (this.type != child.type)
            throw new IllegalArgumentException("'other' is different type of Path");

        // can only relativize paths if root component matches
        if (!this.root.equalsIgnoreCase(child.root))
            throw new IllegalArgumentException("'other' has different root");

        // this path is the empty path
        if (this.isEmpty())
            return child;


        WindowsPath base = this;
        if (base.hasDotOrDotDot() || child.hasDotOrDotDot()) {
            base = base.normalize();
            child = child.normalize();
        }

        int baseCount = base.getNameCount();
        int childCount = child.getNameCount();

        // skip matching names
        int n = Math.min(baseCount, childCount);
        int i = 0;
        while (i < n) {
            if (!base.getName(i).equals(child.getName(i)))
                break;
            i++;
        }

        // remaining elements in child
        WindowsPath childRemaining;
        boolean isChildEmpty;
        if (i == childCount) {
            childRemaining = emptyPath();
            isChildEmpty = true;
        } else {
            childRemaining = child.subpath(i, childCount);
            isChildEmpty = childRemaining.isEmpty();
        }

        // matched all of base
        if (i == baseCount) {
            return childRemaining;
        }

        // the remainder of base cannot contain ".."
        WindowsPath baseRemaining = base.subpath(i, baseCount);
        if (baseRemaining.hasDotOrDotDot()) {
            throw new IllegalArgumentException("Unable to compute relative "
                    + " path from " + this + " to " + obj);
        }
        if (baseRemaining.isEmpty())
            return childRemaining;

        // number of ".." needed
        int dotdots = baseRemaining.getNameCount();
        if (dotdots == 0) {
            return childRemaining;
        }

        StringBuilder result = new StringBuilder();
        for (int j=0; j<dotdots; j++) {
            result.append("..\\");
        }

        // append remaining names in child
        if (!isChildEmpty) {
            for (int j=0; j<childRemaining.getNameCount(); j++) {
                result.append(childRemaining.getName(j).toString());
                result.append("\\");
            }
        }

        // drop trailing slash
        result.setLength(result.length()-1);
        return createFromNormalizedPath(getFileSystem(), result.toString());
    }

    @Override
    public WindowsPath normalize() {
        final int count = getNameCount();
        if (count == 0 || isEmpty())
            return this;

        boolean[] ignore = new boolean[count];      // true => ignore name
        int remaining = count;                      // number of names remaining

        // multiple passes to eliminate all occurrences of "." and "name/.."
        int prevRemaining;
        do {
            prevRemaining = remaining;
            int prevName = -1;
            for (int i=0; i<count; i++) {
                if (ignore[i])
                    continue;

                String name = elementAsString(i);

                // not "." or ".."
                if (name.length() > 2) {
                    prevName = i;
                    continue;
                }

                // "." or something else
                if (name.length() == 1) {
                    // ignore "."
                    if (name.charAt(0) == '.') {
                        ignore[i] = true;
                        remaining--;
                    } else {
                        prevName = i;
                    }
                    continue;
                }

                // not ".."
                if (name.charAt(0) != '.' || name.charAt(1) != '.') {
                    prevName = i;
                    continue;
                }

                // ".." found
                if (prevName >= 0) {
                    // name/<ignored>/.. found so mark name and ".." to be
                    // ignored
                    ignore[prevName] = true;
                    ignore[i] = true;
                    remaining = remaining - 2;
                    prevName = -1;
                } else {
                    // Cases:
                    //    C:\<ignored>\..
                    //    \\server\\share\<ignored>\..
                    //    \<ignored>..
                    if (isAbsolute() || type == WindowsPathType.DIRECTORY_RELATIVE) {
                        boolean hasPrevious = false;
                        for (int j=0; j<i; j++) {
                            if (!ignore[j]) {
                                hasPrevious = true;
                                break;
                            }
                        }
                        if (!hasPrevious) {
                            // all proceeding names are ignored
                            ignore[i] = true;
                            remaining--;
                        }
                    }
                }
            }
        } while (prevRemaining > remaining);

        // no redundant names
        if (remaining == count)
            return this;

        // corner case - all names removed
        if (remaining == 0) {
            return root.isEmpty() ? emptyPath() : getRoot();
        }

        // re-constitute the path from the remaining names.
        StringBuilder result = new StringBuilder();
        if (root != null)
            result.append(root);
        for (int i=0; i<count; i++) {
            if (!ignore[i]) {
                result.append(getName(i));
                result.append("\\");
            }
        }

        // drop trailing slash in result
        result.setLength(result.length()-1);
        return createFromNormalizedPath(getFileSystem(), result.toString());
    }

    @Override
    public WindowsPath resolve(Path obj) {
        WindowsPath other = toWindowsPath(obj);
        if (other.isEmpty())
            return this;
        if (other.isAbsolute())
            return other;

        switch (other.type) {
            case RELATIVE: {
                String result;
                if (path.endsWith("\\") || (root.length() == path.length())) {
                    result = path + other.path;
                } else {
                    result = path + "\\" + other.path;
                }
                return new WindowsPath(getFileSystem(), type, root, result);
            }

            case DIRECTORY_RELATIVE: {
                String result;
                if (root.endsWith("\\")) {
                    result = root + other.path.substring(1);
                } else {
                    result = root + other.path;
                }
                return createFromNormalizedPath(getFileSystem(), result);
            }

            case DRIVE_RELATIVE: {
                if (!root.endsWith("\\"))
                    return other;
                // if different roots then return other
                String thisRoot = root.substring(0, root.length()-1);
                if (!thisRoot.equalsIgnoreCase(other.root))
                    return other;
                // same roots
                String remaining = other.path.substring(other.root.length());
                String result;
                if (path.endsWith("\\")) {
                    result = path + remaining;
                } else {
                    result = path + "\\" + remaining;
                }
                return createFromNormalizedPath(getFileSystem(), result);
            }

            default:
                throw new AssertionError();
        }
    }

    // generate offset array
    private void initOffsets() {
        if (offsets == null) {
            ArrayList<Integer> list = new ArrayList<>();
            if (isEmpty()) {
                // empty path considered to have one name element
                list.add(0);
            } else {
                int start = root.length();
                int off = root.length();
                while (off < path.length()) {
                    if (path.charAt(off) != '\\') {
                        off++;
                    } else {
                        list.add(start);
                        start = ++off;
                    }
                }
                if (start != off)
                    list.add(start);
            }
            synchronized (this) {
                if (offsets == null)
                    offsets = list.toArray(new Integer[list.size()]);
            }
        }
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    private String elementAsString(int i) {
        initOffsets();
        if (i == (offsets.length-1))
            return path.substring(offsets[i]);
        return path.substring(offsets[i], offsets[i+1]-1);
    }

    @Override
    public WindowsPath getName(int index) {
        initOffsets();
        if (index < 0 || index >= offsets.length)
            throw new IllegalArgumentException();
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", elementAsString(index));
    }

    @Override
    public WindowsPath subpath(int beginIndex, int endIndex) {
        initOffsets();
        if (beginIndex < 0)
            throw new IllegalArgumentException();
        if (beginIndex >= offsets.length)
            throw new IllegalArgumentException();
        if (endIndex > offsets.length)
            throw new IllegalArgumentException();
        if (beginIndex >= endIndex)
            throw new IllegalArgumentException();

        StringBuilder sb = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++) {
            sb.append(elementAsString(i));
            if (i != (endIndex-1))
                sb.append("\\");
        }
        return new WindowsPath(getFileSystem(), WindowsPathType.RELATIVE, "", sb.toString());
    }

    @Override
    public boolean startsWith(Path obj) {
        if (!(Objects.requireNonNull(obj) instanceof WindowsPath))
            return false;
        WindowsPath other = (WindowsPath)obj;

        // if this path has a root component the given path's root must match
        if (!this.root.equalsIgnoreCase(other.root)) {
            return false;
        }

        // empty path starts with itself
        if (other.isEmpty())
            return this.isEmpty();

        // roots match so compare elements
        int thisCount = getNameCount();
        int otherCount = other.getNameCount();
        if (otherCount <= thisCount) {
            while (--otherCount >= 0) {
                String thisElement = this.elementAsString(otherCount);
                String otherElement = other.elementAsString(otherCount);
                // FIXME: should compare in uppercase
                if (!thisElement.equalsIgnoreCase(otherElement))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean endsWith(Path obj) {
        if (!(Objects.requireNonNull(obj) instanceof WindowsPath))
            return false;
        WindowsPath other = (WindowsPath)obj;

        // other path is longer
        if (other.path.length() > this.path.length()) {
            return false;
        }

        // empty path ends in itself
        if (other.isEmpty()) {
            return this.isEmpty();
        }

        int thisCount = this.getNameCount();
        int otherCount = other.getNameCount();

        // given path has more elements that this path
        if (otherCount > thisCount) {
            return false;
        }

        // compare roots
        if (other.root.length() > 0) {
            if (otherCount < thisCount)
                return false;
            // FIXME: should compare in uppercase
            if (!this.root.equalsIgnoreCase(other.root))
                return false;
        }

        // match last 'otherCount' elements
        int off = thisCount - otherCount;
        while (--otherCount >= 0) {
            String thisElement = this.elementAsString(off + otherCount);
            String otherElement = other.elementAsString(otherCount);
            // FIXME: should compare in uppercase
            if (!thisElement.equalsIgnoreCase(otherElement))
                return false;
        }
        return true;
    }

    @Override
    public int compareTo(Path obj) {
        if (obj == null)
            throw new NullPointerException();
        String s1 = path;
        String s2 = ((WindowsPath)obj).path;
        int n1 = s1.length();
        int n2 = s2.length();
        int min = Math.min(n1, n2);
        for (int i = 0; i < min; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
             if (c1 != c2) {
                 c1 = Character.toUpperCase(c1);
                 c2 = Character.toUpperCase(c2);
                 if (c1 != c2) {
                     return c1 - c2;
                 }
             }
        }
        return n1 - n2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WindowsPath path) {
            return compareTo(path) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // OK if two or more threads compute hash
        int h = hash;
        if (h == 0) {
            for (int i = 0; i< path.length(); i++) {
                h = 31*h + Character.toUpperCase(path.charAt(i));
            }
            hash = h;
        }
        return h;
    }

    @Override
    public String toString() {
        return path;
    }

    // -- file operations --

    // package-private
    long openForReadAttributeAccess(boolean followLinks)
        throws WindowsException
    {
        int flags = FILE_FLAG_BACKUP_SEMANTICS;
        if (!followLinks)
            flags |= FILE_FLAG_OPEN_REPARSE_POINT;
        try {
            return openFileForReadAttributeAccess(flags);
        } catch (WindowsException e) {
            if (followLinks && e.lastError() == ERROR_CANT_ACCESS_FILE) {
                // Object could be a Unix domain socket
                try {
                    return openSocketForReadAttributeAccess();
                } catch (WindowsException ignore) {}
            }
            throw e;
        }
    }

    private long openFileForReadAttributeAccess(int flags)
        throws WindowsException
    {
        return CreateFile(getPathForWin32Calls(),
                            FILE_READ_ATTRIBUTES,
                            (FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE),
                            0L,
                            OPEN_EXISTING,
                            flags);
    }

    /**
     * Returns a handle to the file if it is a socket.
     * Throws WindowsException if file is not a socket
     */
    private long openSocketForReadAttributeAccess()
        throws WindowsException
    {
        // needs to specify FILE_FLAG_OPEN_REPARSE_POINT if the file is a socket
        int flags = FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OPEN_REPARSE_POINT;

        long handle = openFileForReadAttributeAccess(flags);

        try {
            WindowsFileAttributes attrs = WindowsFileAttributes.readAttributes(handle);
            if (!attrs.isUnixDomainSocket()) {
                throw new WindowsException("not a socket");
            }
            return handle;
        } catch (WindowsException e) {
            CloseHandle(handle);
            throw e;
        }
    }

    void checkRead() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkRead(getPathForPermissionCheck());
        }
    }

    void checkWrite() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkWrite(getPathForPermissionCheck());
        }
    }

    void checkDelete() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkDelete(getPathForPermissionCheck());
        }
    }

    @Override
    public URI toUri() {
        return WindowsUriSupport.toUri(this);
    }

    @Override
    public WindowsPath toAbsolutePath() {
        if (isAbsolute())
            return this;

        // permission check as per spec
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPropertyAccess("user.dir");
        }

        try {
            return createFromNormalizedPath(getFileSystem(), getAbsolutePath());
        } catch (WindowsException x) {
            throw new IOError(new IOException(x.getMessage()));
        }
    }

    @Override
    public WindowsPath toRealPath(LinkOption... options) throws IOException {
        checkRead();
        String rp = WindowsLinkSupport.getRealPath(this, Util.followLinks(options));
        return createFromNormalizedPath(getFileSystem(), rp);
    }

    @Override
    public WatchKey register(WatchService watcher,
                             WatchEvent.Kind<?>[] events,
                             WatchEvent.Modifier... modifiers)
        throws IOException
    {
        if (watcher == null)
            throw new NullPointerException();
        if (!(watcher instanceof WindowsWatchService))
            throw new ProviderMismatchException();

        // When a security manager is set then we need to make a defensive
        // copy of the modifiers and check for the Windows specific FILE_TREE
        // modifier. When the modifier is present then check that permission
        // has been granted recursively.
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            boolean watchSubtree = false;
            final int ml = modifiers.length;
            if (ml > 0) {
                modifiers = Arrays.copyOf(modifiers, ml);
                int i=0;
                while (i < ml) {
                    if (ExtendedOptions.FILE_TREE.matches(modifiers[i++])) {
                        watchSubtree = true;
                        break;
                    }
                }
            }
            String s = getPathForPermissionCheck();
            sm.checkRead(s);
            if (watchSubtree)
                sm.checkRead(s + "\\-");
        }

        return ((WindowsWatchService)watcher).register(this, events, modifiers);
    }
}
