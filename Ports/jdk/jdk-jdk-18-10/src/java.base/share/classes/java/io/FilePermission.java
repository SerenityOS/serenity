/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.nio.file.*;
import java.security.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import jdk.internal.access.JavaIOFilePermissionAccess;
import jdk.internal.access.SharedSecrets;
import sun.nio.fs.DefaultFileSystemProvider;
import sun.security.action.GetPropertyAction;
import sun.security.util.FilePermCompat;
import sun.security.util.SecurityConstants;

/**
 * This class represents access to a file or directory.  A FilePermission consists
 * of a pathname and a set of actions valid for that pathname.
 * <P>
 * Pathname is the pathname of the file or directory granted the specified
 * actions. A pathname that ends in "/*" (where "/" is
 * the file separator character, {@code File.separatorChar}) indicates
 * all the files and directories contained in that directory. A pathname
 * that ends with "/-" indicates (recursively) all files
 * and subdirectories contained in that directory. Such a pathname is called
 * a wildcard pathname. Otherwise, it's a simple pathname.
 * <P>
 * A pathname consisting of the special token {@literal "<<ALL FILES>>"}
 * matches <b>any</b> file.
 * <P>
 * Note: A pathname consisting of a single "*" indicates all the files
 * in the current directory, while a pathname consisting of a single "-"
 * indicates all the files in the current directory and
 * (recursively) all files and subdirectories contained in the current
 * directory.
 * <P>
 * The actions to be granted are passed to the constructor in a string containing
 * a list of one or more comma-separated keywords. The possible keywords are
 * "read", "write", "execute", "delete", and "readlink". Their meaning is
 * defined as follows:
 *
 * <DL>
 *    <DT> read <DD> read permission
 *    <DT> write <DD> write permission
 *    <DT> execute
 *    <DD> execute permission. Allows {@code Runtime.exec} to
 *         be called. Corresponds to {@code SecurityManager.checkExec}.
 *    <DT> delete
 *    <DD> delete permission. Allows {@code File.delete} to
 *         be called. Corresponds to {@code SecurityManager.checkDelete}.
 *    <DT> readlink
 *    <DD> read link permission. Allows the target of a
 *         <a href="../nio/file/package-summary.html#links">symbolic link</a>
 *         to be read by invoking the {@link java.nio.file.Files#readSymbolicLink
 *         readSymbolicLink } method.
 * </DL>
 * <P>
 * The actions string is converted to lowercase before processing.
 * <P>
 * Be careful when granting FilePermissions. Think about the implications
 * of granting read and especially write access to various files and
 * directories. The {@literal "<<ALL FILES>>"} permission with write action is
 * especially dangerous. This grants permission to write to the entire
 * file system. One thing this effectively allows is replacement of the
 * system binary, including the JVM runtime environment.
 * <P>
 * Please note: Code can always read a file from the same
 * directory it's in (or a subdirectory of that directory); it does not
 * need explicit permission to do so.
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @since 1.2
 *
 * @serial exclude
 */

public final class FilePermission extends Permission implements Serializable {

    /**
     * Execute action.
     */
    private static final int EXECUTE = 0x1;
    /**
     * Write action.
     */
    private static final int WRITE   = 0x2;
    /**
     * Read action.
     */
    private static final int READ    = 0x4;
    /**
     * Delete action.
     */
    private static final int DELETE  = 0x8;
    /**
     * Read link action.
     */
    private static final int READLINK    = 0x10;

    /**
     * All actions (read,write,execute,delete,readlink)
     */
    private static final int ALL     = READ|WRITE|EXECUTE|DELETE|READLINK;
    /**
     * No actions.
     */
    private static final int NONE    = 0x0;

    // the actions mask
    private transient int mask;

    // does path indicate a directory? (wildcard or recursive)
    private transient boolean directory;

    // is it a recursive directory specification?
    private transient boolean recursive;

    /**
     * the actions string.
     *
     * @serial
     */
    private String actions; // Left null as long as possible, then
                            // created and re-used in the getAction function.

    // canonicalized dir path. used by the "old" behavior (nb == false).
    // In the case of directories, it is the name "/blah/*" or "/blah/-"
    // without the last character (the "*" or "-").

    private transient String cpath;

    // Following fields used by the "new" behavior (nb == true), in which
    // input path is not canonicalized. For compatibility (so that granting
    // FilePermission on "x" allows reading "`pwd`/x", an alternative path
    // can be added so that both can be used in an implies() check. Please note
    // the alternative path only deals with absolute/relative path, and does
    // not deal with symlink/target.

    private transient Path npath;       // normalized dir path.
    private transient Path npath2;      // alternative normalized dir path.
    private transient boolean allFiles; // whether this is <<ALL FILES>>
    private transient boolean invalid;  // whether input path is invalid

    // static Strings used by init(int mask)
    private static final char RECURSIVE_CHAR = '-';
    private static final char WILD_CHAR = '*';

//    public String toString() {
//        StringBuffer sb = new StringBuffer();
//        sb.append("*** FilePermission on " + getName() + " ***");
//        for (Field f : FilePermission.class.getDeclaredFields()) {
//            if (!Modifier.isStatic(f.getModifiers())) {
//                try {
//                    sb.append(f.getName() + " = " + f.get(this));
//                } catch (Exception e) {
//                    sb.append(f.getName() + " = " + e.toString());
//                }
//                sb.append('\n');
//            }
//        }
//        sb.append("***\n");
//        return sb.toString();
//    }

    @java.io.Serial
    private static final long serialVersionUID = 7930732926638008763L;

    /**
     * Use the platform's default file system to avoid recursive initialization
     * issues when the VM is configured to use a custom file system provider.
     */
    private static final java.nio.file.FileSystem builtInFS =
        DefaultFileSystemProvider.theFileSystem();

    private static final Path here = builtInFS.getPath(
            GetPropertyAction.privilegedGetProperty("user.dir"));

    private static final Path EMPTY_PATH = builtInFS.getPath("");
    private static final Path DASH_PATH = builtInFS.getPath("-");
    private static final Path DOTDOT_PATH = builtInFS.getPath("..");

    /**
     * A private constructor that clones some and updates some,
     * always with a different name.
     * @param input
     */
    private FilePermission(String name,
                           FilePermission input,
                           Path npath,
                           Path npath2,
                           int mask,
                           String actions) {
        super(name);
        // Customizables
        this.npath = npath;
        this.npath2 = npath2;
        this.actions = actions;
        this.mask = mask;
        // Cloneds
        this.allFiles = input.allFiles;
        this.invalid = input.invalid;
        this.recursive = input.recursive;
        this.directory = input.directory;
        this.cpath = input.cpath;
    }

    /**
     * Returns the alternative path as a Path object, i.e. absolute path
     * for a relative one, or vice versa.
     *
     * @param in a real path w/o "-" or "*" at the end, and not <<ALL FILES>>.
     * @return the alternative path, or null if cannot find one.
     */
    private static Path altPath(Path in) {
        try {
            if (!in.isAbsolute()) {
                return here.resolve(in).normalize();
            } else {
                return here.relativize(in).normalize();
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static {
        SharedSecrets.setJavaIOFilePermissionAccess(
            /**
             * Creates FilePermission objects with special internals.
             * See {@link FilePermCompat#newPermPlusAltPath(Permission)} and
             * {@link FilePermCompat#newPermUsingAltPath(Permission)}.
             */
            new JavaIOFilePermissionAccess() {
                public FilePermission newPermPlusAltPath(FilePermission input) {
                    if (!input.invalid && input.npath2 == null && !input.allFiles) {
                        Path npath2 = altPath(input.npath);
                        if (npath2 != null) {
                            // Please note the name of the new permission is
                            // different than the original so that when one is
                            // added to a FilePermissionCollection it will not
                            // be merged with the original one.
                            return new FilePermission(input.getName() + "#plus",
                                    input,
                                    input.npath,
                                    npath2,
                                    input.mask,
                                    input.actions);
                        }
                    }
                    return input;
                }
                public FilePermission newPermUsingAltPath(FilePermission input) {
                    if (!input.invalid && !input.allFiles) {
                        Path npath2 = altPath(input.npath);
                        if (npath2 != null) {
                            // New name, see above.
                            return new FilePermission(input.getName() + "#using",
                                    input,
                                    npath2,
                                    null,
                                    input.mask,
                                    input.actions);
                        }
                    }
                    return null;
                }
            }
        );
    }

    /**
     * initialize a FilePermission object. Common to all constructors.
     * Also called during de-serialization.
     *
     * @param mask the actions mask to use.
     *
     */
    @SuppressWarnings("removal")
    private void init(int mask) {
        if ((mask & ALL) != mask)
                throw new IllegalArgumentException("invalid actions mask");

        if (mask == NONE)
                throw new IllegalArgumentException("invalid actions mask");

        if (FilePermCompat.nb) {
            String name = getName();

            if (name == null)
                throw new NullPointerException("name can't be null");

            this.mask = mask;

            if (name.equals("<<ALL FILES>>")) {
                allFiles = true;
                npath = EMPTY_PATH;
                // other fields remain default
                return;
            }

            boolean rememberStar = false;
            if (name.endsWith("*")) {
                rememberStar = true;
                recursive = false;
                name = name.substring(0, name.length()-1) + "-";
            }

            try {
                // new File() can "normalize" some name, for example, "/C:/X" on
                // Windows. Some JDK codes generate such illegal names.
                npath = builtInFS.getPath(new File(name).getPath())
                        .normalize();
                // lastName should always be non-null now
                Path lastName = npath.getFileName();
                if (lastName != null && lastName.equals(DASH_PATH)) {
                    directory = true;
                    recursive = !rememberStar;
                    npath = npath.getParent();
                }
                if (npath == null) {
                    npath = EMPTY_PATH;
                }
                invalid = false;
            } catch (InvalidPathException ipe) {
                // Still invalid. For compatibility reason, accept it
                // but make this permission useless.
                npath = builtInFS.getPath("-u-s-e-l-e-s-s-");
                invalid = true;
            }

        } else {
            if ((cpath = getName()) == null)
                throw new NullPointerException("name can't be null");

            this.mask = mask;

            if (cpath.equals("<<ALL FILES>>")) {
                allFiles = true;
                directory = true;
                recursive = true;
                cpath = "";
                return;
            }

            // Validate path by platform's default file system
            try {
                String name = cpath.endsWith("*") ? cpath.substring(0, cpath.length() - 1) + "-" : cpath;
                builtInFS.getPath(new File(name).getPath());
            } catch (InvalidPathException ipe) {
                invalid = true;
                return;
            }

            // store only the canonical cpath if possible
            cpath = AccessController.doPrivileged(new PrivilegedAction<>() {
                public String run() {
                    try {
                        String path = cpath;
                        if (cpath.endsWith("*")) {
                            // call getCanonicalPath with a path with wildcard character
                            // replaced to avoid calling it with paths that are
                            // intended to match all entries in a directory
                            path = path.substring(0, path.length() - 1) + "-";
                            path = new File(path).getCanonicalPath();
                            return path.substring(0, path.length() - 1) + "*";
                        } else {
                            return new File(path).getCanonicalPath();
                        }
                    } catch (IOException ioe) {
                        return cpath;
                    }
                }
            });

            int len = cpath.length();
            char last = ((len > 0) ? cpath.charAt(len - 1) : 0);

            if (last == RECURSIVE_CHAR &&
                    cpath.charAt(len - 2) == File.separatorChar) {
                directory = true;
                recursive = true;
                cpath = cpath.substring(0, --len);
            } else if (last == WILD_CHAR &&
                    cpath.charAt(len - 2) == File.separatorChar) {
                directory = true;
                //recursive = false;
                cpath = cpath.substring(0, --len);
            } else {
                // overkill since they are initialized to false, but
                // commented out here to remind us...
                //directory = false;
                //recursive = false;
            }

            // XXX: at this point the path should be absolute. die if it isn't?
        }
    }

    /**
     * Creates a new FilePermission object with the specified actions.
     * <i>path</i> is the pathname of a file or directory, and <i>actions</i>
     * contains a comma-separated list of the desired actions granted on the
     * file or directory. Possible actions are
     * "read", "write", "execute", "delete", and "readlink".
     *
     * <p>A pathname that ends in "/*" (where "/" is
     * the file separator character, {@code File.separatorChar})
     * indicates all the files and directories contained in that directory.
     * A pathname that ends with "/-" indicates (recursively) all files and
     * subdirectories contained in that directory. The special pathname
     * {@literal "<<ALL FILES>>"} matches any file.
     *
     * <p>A pathname consisting of a single "*" indicates all the files
     * in the current directory, while a pathname consisting of a single "-"
     * indicates all the files in the current directory and
     * (recursively) all files and subdirectories contained in the current
     * directory.
     *
     * <p>A pathname containing an empty string represents an empty path.
     *
     * @implNote In this implementation, the
     * {@systemProperty jdk.io.permissionsUseCanonicalPath} system property
     * dictates how the {@code path} argument is processed and stored.
     * <P>
     * If the value of the system property is set to {@code true}, {@code path}
     * is canonicalized and stored as a String object named {@code cpath}.
     * This means a relative path is converted to an absolute path, a Windows
     * DOS-style 8.3 path is expanded to a long path, and a symbolic link is
     * resolved to its target, etc.
     * <P>
     * If the value of the system property is set to {@code false}, {@code path}
     * is converted to a {@link java.nio.file.Path} object named {@code npath}
     * after {@link Path#normalize() normalization}. No canonicalization is
     * performed which means the underlying file system is not accessed.
     * If an {@link InvalidPathException} is thrown during the conversion,
     * this {@code FilePermission} will be labeled as invalid.
     * <P>
     * In either case, the "*" or "-" character at the end of a wildcard
     * {@code path} is removed before canonicalization or normalization.
     * It is stored in a separate wildcard flag field.
     * <P>
     * The default value of the {@code jdk.io.permissionsUseCanonicalPath}
     * system property is {@code false} in this implementation.
     * <p>
     * The value can also be set with a security property using the same name,
     * but setting a system property will override the security property value.
     *
     * @param path the pathname of the file/directory.
     * @param actions the action string.
     *
     * @throws IllegalArgumentException if actions is {@code null}, empty,
     *         malformed or contains an action other than the specified
     *         possible actions
     */
    public FilePermission(String path, String actions) {
        super(path);
        init(getMask(actions));
    }

    /**
     * Creates a new FilePermission object using an action mask.
     * More efficient than the FilePermission(String, String) constructor.
     * Can be used from within
     * code that needs to create a FilePermission object to pass into the
     * {@code implies} method.
     *
     * @param path the pathname of the file/directory.
     * @param mask the action mask to use.
     */
    // package private for use by the FilePermissionCollection add method
    FilePermission(String path, int mask) {
        super(path);
        init(mask);
    }

    /**
     * Checks if this FilePermission object "implies" the specified permission.
     * <P>
     * More specifically, this method returns true if:
     * <ul>
     * <li> <i>p</i> is an instanceof FilePermission,
     * <li> <i>p</i>'s actions are a proper subset of this
     * object's actions, and
     * <li> <i>p</i>'s pathname is implied by this object's
     *      pathname. For example, "/tmp/*" implies "/tmp/foo", since
     *      "/tmp/*" encompasses all files in the "/tmp" directory,
     *      including the one named "foo".
     * </ul>
     * <P>
     * Precisely, a simple pathname implies another simple pathname
     * if and only if they are equal. A simple pathname never implies
     * a wildcard pathname. A wildcard pathname implies another wildcard
     * pathname if and only if all simple pathnames implied by the latter
     * are implied by the former. A wildcard pathname implies a simple
     * pathname if and only if
     * <ul>
     *     <li>if the wildcard flag is "*", the simple pathname's path
     *     must be right inside the wildcard pathname's path.
     *     <li>if the wildcard flag is "-", the simple pathname's path
     *     must be recursively inside the wildcard pathname's path.
     * </ul>
     * <P>
     * {@literal "<<ALL FILES>>"} implies every other pathname. No pathname,
     * except for {@literal "<<ALL FILES>>"} itself, implies
     * {@literal "<<ALL FILES>>"}.
     *
     * @implNote
     * If {@code jdk.io.permissionsUseCanonicalPath} is {@code true}, a
     * simple {@code cpath} is inside a wildcard {@code cpath} if and only if
     * after removing the base name (the last name in the pathname's name
     * sequence) from the former the remaining part is equal to the latter,
     * a simple {@code cpath} is recursively inside a wildcard {@code cpath}
     * if and only if the former starts with the latter.
     * <p>
     * If {@code jdk.io.permissionsUseCanonicalPath} is {@code false}, a
     * simple {@code npath} is inside a wildcard {@code npath} if and only if
     * {@code  simple_npath.relativize(wildcard_npath)} is exactly "..",
     * a simple {@code npath} is recursively inside a wildcard {@code npath}
     * if and only if {@code simple_npath.relativize(wildcard_npath)} is a
     * series of one or more "..". This means "/-" implies "/foo" but not "foo".
     * <p>
     * An invalid {@code FilePermission} does not imply any object except for
     * itself. An invalid {@code FilePermission} is not implied by any object
     * except for itself or a {@code FilePermission} on
     * {@literal "<<ALL FILES>>"} whose actions is a superset of this
     * invalid {@code FilePermission}. Even if two {@code FilePermission}
     * are created with the same invalid path, one does not imply the other.
     *
     * @param p the permission to check against.
     *
     * @return {@code true} if the specified permission is not
     *                  {@code null} and is implied by this object,
     *                  {@code false} otherwise.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof FilePermission that))
            return false;

        // we get the effective mask. i.e., the "and" of this and that.
        // They must be equal to that.mask for implies to return true.

        return ((this.mask & that.mask) == that.mask) && impliesIgnoreMask(that);
    }

    /**
     * Checks if the Permission's actions are a proper subset of the
     * this object's actions. Returns the effective mask iff the
     * this FilePermission's path also implies that FilePermission's path.
     *
     * @param that the FilePermission to check against.
     * @return the effective mask
     */
    boolean impliesIgnoreMask(FilePermission that) {
        if (this == that) {
            return true;
        }
        if (allFiles) {
            return true;
        }
        if (this.invalid || that.invalid) {
            return false;
        }
        if (that.allFiles) {
            return false;
        }
        if (FilePermCompat.nb) {
            // Left at least same level of wildness as right
            if ((this.recursive && that.recursive) != that.recursive
                    || (this.directory && that.directory) != that.directory) {
                return false;
            }
            // Same npath is good as long as both or neither are directories
            if (this.npath.equals(that.npath)
                    && this.directory == that.directory) {
                return true;
            }
            int diff = containsPath(this.npath, that.npath);
            // Right inside left is good if recursive
            if (diff >= 1 && recursive) {
                return true;
            }
            // Right right inside left if it is element in set
            if (diff == 1 && directory && !that.directory) {
                return true;
            }

            // Hack: if a npath2 field exists, apply the same checks
            // on it as a fallback.
            if (this.npath2 != null) {
                if (this.npath2.equals(that.npath)
                        && this.directory == that.directory) {
                    return true;
                }
                diff = containsPath(this.npath2, that.npath);
                if (diff >= 1 && recursive) {
                    return true;
                }
                if (diff == 1 && directory && !that.directory) {
                    return true;
                }
            }

            return false;
        } else {
            if (this.directory) {
                if (this.recursive) {
                    // make sure that.path is longer then path so
                    // something like /foo/- does not imply /foo
                    if (that.directory) {
                        return (that.cpath.length() >= this.cpath.length()) &&
                                that.cpath.startsWith(this.cpath);
                    } else {
                        return ((that.cpath.length() > this.cpath.length()) &&
                                that.cpath.startsWith(this.cpath));
                    }
                } else {
                    if (that.directory) {
                        // if the permission passed in is a directory
                        // specification, make sure that a non-recursive
                        // permission (i.e., this object) can't imply a recursive
                        // permission.
                        if (that.recursive)
                            return false;
                        else
                            return (this.cpath.equals(that.cpath));
                    } else {
                        int last = that.cpath.lastIndexOf(File.separatorChar);
                        if (last == -1)
                            return false;
                        else {
                            // this.cpath.equals(that.cpath.substring(0, last+1));
                            // Use regionMatches to avoid creating new string
                            return (this.cpath.length() == (last + 1)) &&
                                    this.cpath.regionMatches(0, that.cpath, 0, last + 1);
                        }
                    }
                }
            } else if (that.directory) {
                // if this is NOT recursive/wildcarded,
                // do not let it imply a recursive/wildcarded permission
                return false;
            } else {
                return (this.cpath.equals(that.cpath));
            }
        }
    }

    /**
     * Returns the depth between an outer path p1 and an inner path p2. -1
     * is returned if
     *
     * - p1 does not contains p2.
     * - this is not decidable. For example, p1="../x", p2="y".
     * - the depth is not decidable. For example, p1="/", p2="x".
     *
     * This method can return 2 if the depth is greater than 2.
     *
     * @param p1 the expected outer path, normalized
     * @param p2 the expected inner path, normalized
     * @return the depth in between
     */
    private static int containsPath(Path p1, Path p2) {

        // Two paths must have the same root. For example,
        // there is no contains relation between any two of
        // "/x", "x", "C:/x", "C:x", and "//host/share/x".
        if (!Objects.equals(p1.getRoot(), p2.getRoot())) {
            return -1;
        }

        // Empty path (i.e. "." or "") is a strange beast,
        // because its getNameCount()==1 but getName(0) is null.
        // It's better to deal with it separately.
        if (p1.equals(EMPTY_PATH)) {
            if (p2.equals(EMPTY_PATH)) {
                return 0;
            } else if (p2.getName(0).equals(DOTDOT_PATH)) {
                // "." contains p2 iff p2 has no "..". Since
                // a normalized path can only have 0 or more
                // ".." at the beginning. We only need to look
                // at the head.
                return -1;
            } else {
                // and the distance is p2's name count. i.e.
                // 3 between "." and "a/b/c".
                return p2.getNameCount();
            }
        } else if (p2.equals(EMPTY_PATH)) {
            int c1 = p1.getNameCount();
            if (!p1.getName(c1 - 1).equals(DOTDOT_PATH)) {
                // "." is inside p1 iff p1 is 1 or more "..".
                // For the same reason above, we only need to
                // look at the tail.
                return -1;
            }
            // and the distance is the count of ".."
            return c1;
        }

        // Good. No more empty paths.

        // Common heads are removed

        int c1 = p1.getNameCount();
        int c2 = p2.getNameCount();

        int n = Math.min(c1, c2);
        int i = 0;
        while (i < n) {
            if (!p1.getName(i).equals(p2.getName(i)))
                break;
            i++;
        }

        // for p1 containing p2, p1 must be 0-or-more "..",
        // and p2 cannot have "..". For the same reason, we only
        // check tail of p1 and head of p2.
        if (i < c1 && !p1.getName(c1 - 1).equals(DOTDOT_PATH)) {
            return -1;
        }

        if (i < c2 && p2.getName(i).equals(DOTDOT_PATH)) {
            return -1;
        }

        // and the distance is the name counts added (after removing
        // the common heads).

        // For example: p1 = "../../..", p2 = "../a".
        // After removing the common heads, they become "../.." and "a",
        // and the distance is (3-1)+(2-1) = 3.
        return c1 - i + c2 - i;
    }

    /**
     * Checks two FilePermission objects for equality. Checks that <i>obj</i> is
     * a FilePermission, and has the same pathname and actions as this object.
     *
     * @implNote More specifically, two pathnames are the same if and only if
     * they have the same wildcard flag and their {@code cpath}
     * (if {@code jdk.io.permissionsUseCanonicalPath} is {@code true}) or
     * {@code npath} (if {@code jdk.io.permissionsUseCanonicalPath}
     * is {@code false}) are equal. Or they are both {@literal "<<ALL FILES>>"}.
     * <p>
     * When {@code jdk.io.permissionsUseCanonicalPath} is {@code false}, an
     * invalid {@code FilePermission} does not equal to any object except
     * for itself, even if they are created using the same invalid path.
     *
     * @param obj the object we are testing for equality with this object.
     * @return {@code true} if obj is a FilePermission, and has the same
     *          pathname and actions as this FilePermission object,
     *          {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof FilePermission that))
            return false;

        if (this.invalid || that.invalid) {
            return false;
        }
        if (FilePermCompat.nb) {
            return (this.mask == that.mask) &&
                    (this.allFiles == that.allFiles) &&
                    this.npath.equals(that.npath) &&
                    Objects.equals(npath2, that.npath2) &&
                    (this.directory == that.directory) &&
                    (this.recursive == that.recursive);
        } else {
            return (this.mask == that.mask) &&
                    (this.allFiles == that.allFiles) &&
                    this.cpath.equals(that.cpath) &&
                    (this.directory == that.directory) &&
                    (this.recursive == that.recursive);
        }
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        if (FilePermCompat.nb) {
            return Objects.hash(
                    mask, allFiles, directory, recursive, npath, npath2, invalid);
        } else {
            return 0;
        }
    }

    /**
     * Converts an actions String to an actions mask.
     *
     * @param actions the action string.
     * @return the actions mask.
     */
    private static int getMask(String actions) {
        int mask = NONE;

        // Null action valid?
        if (actions == null) {
            return mask;
        }

        // Use object identity comparison against known-interned strings for
        // performance benefit (these values are used heavily within the JDK).
        if (actions == SecurityConstants.FILE_READ_ACTION) {
            return READ;
        } else if (actions == SecurityConstants.FILE_WRITE_ACTION) {
            return WRITE;
        } else if (actions == SecurityConstants.FILE_EXECUTE_ACTION) {
            return EXECUTE;
        } else if (actions == SecurityConstants.FILE_DELETE_ACTION) {
            return DELETE;
        } else if (actions == SecurityConstants.FILE_READLINK_ACTION) {
            return READLINK;
        }

        char[] a = actions.toCharArray();

        int i = a.length - 1;
        if (i < 0)
            return mask;

        while (i != -1) {
            char c;

            // skip whitespace
            while ((i!=-1) && ((c = a[i]) == ' ' ||
                               c == '\r' ||
                               c == '\n' ||
                               c == '\f' ||
                               c == '\t'))
                i--;

            // check for the known strings
            int matchlen;

            if (i >= 3 && (a[i-3] == 'r' || a[i-3] == 'R') &&
                          (a[i-2] == 'e' || a[i-2] == 'E') &&
                          (a[i-1] == 'a' || a[i-1] == 'A') &&
                          (a[i] == 'd' || a[i] == 'D'))
            {
                matchlen = 4;
                mask |= READ;

            } else if (i >= 4 && (a[i-4] == 'w' || a[i-4] == 'W') &&
                                 (a[i-3] == 'r' || a[i-3] == 'R') &&
                                 (a[i-2] == 'i' || a[i-2] == 'I') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 5;
                mask |= WRITE;

            } else if (i >= 6 && (a[i-6] == 'e' || a[i-6] == 'E') &&
                                 (a[i-5] == 'x' || a[i-5] == 'X') &&
                                 (a[i-4] == 'e' || a[i-4] == 'E') &&
                                 (a[i-3] == 'c' || a[i-3] == 'C') &&
                                 (a[i-2] == 'u' || a[i-2] == 'U') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 7;
                mask |= EXECUTE;

            } else if (i >= 5 && (a[i-5] == 'd' || a[i-5] == 'D') &&
                                 (a[i-4] == 'e' || a[i-4] == 'E') &&
                                 (a[i-3] == 'l' || a[i-3] == 'L') &&
                                 (a[i-2] == 'e' || a[i-2] == 'E') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 6;
                mask |= DELETE;

            } else if (i >= 7 && (a[i-7] == 'r' || a[i-7] == 'R') &&
                                 (a[i-6] == 'e' || a[i-6] == 'E') &&
                                 (a[i-5] == 'a' || a[i-5] == 'A') &&
                                 (a[i-4] == 'd' || a[i-4] == 'D') &&
                                 (a[i-3] == 'l' || a[i-3] == 'L') &&
                                 (a[i-2] == 'i' || a[i-2] == 'I') &&
                                 (a[i-1] == 'n' || a[i-1] == 'N') &&
                                 (a[i] == 'k' || a[i] == 'K'))
            {
                matchlen = 8;
                mask |= READLINK;

            } else {
                // parse error
                throw new IllegalArgumentException(
                        "invalid permission: " + actions);
            }

            // make sure we didn't just match the tail of a word
            // like "ackbarfdelete".  Also, skip to the comma.
            boolean seencomma = false;
            while (i >= matchlen && !seencomma) {
                switch (c = a[i-matchlen]) {
                case ' ': case '\r': case '\n':
                case '\f': case '\t':
                    break;
                default:
                    if (c == ',' && i > matchlen) {
                        seencomma = true;
                        break;
                    }
                    throw new IllegalArgumentException(
                            "invalid permission: " + actions);
                }
                i--;
            }

            // point i at the location of the comma minus one (or -1).
            i -= matchlen;
        }

        return mask;
    }

    /**
     * Return the current action mask. Used by the FilePermissionCollection.
     *
     * @return the actions mask.
     */
    int getMask() {
        return mask;
    }

    /**
     * Return the canonical string representation of the actions.
     * Always returns present actions in the following order:
     * read, write, execute, delete, readlink.
     *
     * @return the canonical string representation of the actions.
     */
    private static String getActions(int mask) {
        StringJoiner sj = new StringJoiner(",");

        if ((mask & READ) == READ) {
            sj.add("read");
        }
        if ((mask & WRITE) == WRITE) {
            sj.add("write");
        }
        if ((mask & EXECUTE) == EXECUTE) {
            sj.add("execute");
        }
        if ((mask & DELETE) == DELETE) {
            sj.add("delete");
        }
        if ((mask & READLINK) == READLINK) {
            sj.add("readlink");
        }

        return sj.toString();
    }

    /**
     * Returns the "canonical string representation" of the actions.
     * That is, this method always returns present actions in the following order:
     * read, write, execute, delete, readlink. For example, if this FilePermission
     * object allows both write and read actions, a call to {@code getActions}
     * will return the string "read,write".
     *
     * @return the canonical string representation of the actions.
     */
    @Override
    public String getActions() {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }

    /**
     * Returns a new PermissionCollection object for storing FilePermission
     * objects.
     * <p>
     * FilePermission objects must be stored in a manner that allows them
     * to be inserted into the collection in any order, but that also enables the
     * PermissionCollection {@code implies}
     * method to be implemented in an efficient (and consistent) manner.
     *
     * <p>For example, if you have two FilePermissions:
     * <OL>
     * <LI>  {@code "/tmp/-", "read"}
     * <LI>  {@code "/tmp/scratch/foo", "write"}
     * </OL>
     *
     * <p>and you are calling the {@code implies} method with the FilePermission:
     *
     * <pre>
     *   "/tmp/scratch/foo", "read,write",
     * </pre>
     *
     * then the {@code implies} function must
     * take into account both the "/tmp/-" and "/tmp/scratch/foo"
     * permissions, so the effective permission is "read,write",
     * and {@code implies} returns true. The "implies" semantics for
     * FilePermissions are handled properly by the PermissionCollection object
     * returned by this {@code newPermissionCollection} method.
     *
     * @return a new PermissionCollection object suitable for storing
     * FilePermissions.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new FilePermissionCollection();
    }

    /**
     * WriteObject is called to save the state of the FilePermission
     * to a stream. The actions are serialized, and the superclass
     * takes care of the name.
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream s)
        throws IOException
    {
        // Write out the actions. The superclass takes care of the name
        // call getActions to make sure actions field is initialized
        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }

    /**
     * readObject is called to restore the state of the FilePermission from
     * a stream.
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the actions, then restore everything else by calling init.
        s.defaultReadObject();
        init(getMask(actions));
    }

    /**
     * Create a cloned FilePermission with a different actions.
     * @param effective the new actions
     * @return a new object
     */
    FilePermission withNewActions(int effective) {
        return new FilePermission(this.getName(),
                this,
                this.npath,
                this.npath2,
                effective,
                null);
    }
}

/**
 * A FilePermissionCollection stores a set of FilePermission permissions.
 * FilePermission objects
 * must be stored in a manner that allows them to be inserted in any
 * order, but enable the implies function to evaluate the implies
 * method.
 * For example, if you have two FilePermissions:
 * <OL>
 * <LI> "/tmp/-", "read"
 * <LI> "/tmp/scratch/foo", "write"
 * </OL>
 * And you are calling the implies function with the FilePermission:
 * "/tmp/scratch/foo", "read,write", then the implies function must
 * take into account both the /tmp/- and /tmp/scratch/foo
 * permissions, so the effective permission is "read,write".
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 *
 * @serial include
 *
 */

final class FilePermissionCollection extends PermissionCollection
    implements Serializable
{
    // Not serialized; see serialization section at end of class
    private transient ConcurrentHashMap<String, Permission> perms;

    /**
     * Create an empty FilePermissionCollection object.
     */
    public FilePermissionCollection() {
        perms = new ConcurrentHashMap<>();
    }

    /**
     * Adds a permission to the FilePermissionCollection. The key for the hash is
     * permission.path.
     *
     * @param permission the Permission object to add.
     *
     * @throws    IllegalArgumentException   if the permission is not a
     *                                       FilePermission
     *
     * @throws    SecurityException   if this FilePermissionCollection object
     *                                has been marked readonly
     */
    @Override
    public void add(Permission permission) {
        if (! (permission instanceof FilePermission fp))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        if (isReadOnly())
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");

        // Add permission to map if it is absent, or replace with new
        // permission if applicable.
        perms.merge(fp.getName(), fp,
            new java.util.function.BiFunction<>() {
                @Override
                public Permission apply(Permission existingVal,
                                        Permission newVal) {
                    int oldMask = ((FilePermission)existingVal).getMask();
                    int newMask = ((FilePermission)newVal).getMask();
                    if (oldMask != newMask) {
                        int effective = oldMask | newMask;
                        if (effective == newMask) {
                            return newVal;
                        }
                        if (effective != oldMask) {
                            return ((FilePermission)newVal)
                                    .withNewActions(effective);
                        }
                    }
                    return existingVal;
                }
            }
        );
    }

    /**
     * Check and see if this set of permissions implies the permissions
     * expressed in "permission".
     *
     * @param permission the Permission object to compare
     *
     * @return true if "permission" is a proper subset of a permission in
     * the set, false if not.
     */
    @Override
    public boolean implies(Permission permission) {
        if (! (permission instanceof FilePermission fperm))
            return false;

        int desired = fperm.getMask();
        int effective = 0;
        int needed = desired;

        for (Permission perm : perms.values()) {
            FilePermission fp = (FilePermission)perm;
            if (((needed & fp.getMask()) != 0) && fp.impliesIgnoreMask(fperm)) {
                effective |= fp.getMask();
                if ((effective & desired) == desired) {
                    return true;
                }
                needed = (desired & ~effective);
            }
        }
        return false;
    }

    /**
     * Returns an enumeration of all the FilePermission objects in the
     * container.
     *
     * @return an enumeration of all the FilePermission objects.
     */
    @Override
    public Enumeration<Permission> elements() {
        return perms.elements();
    }

    @java.io.Serial
    private static final long serialVersionUID = 2202956749081564585L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    //    private Vector permissions;

    /**
     * @serialField permissions java.util.Vector
     *     A list of FilePermission objects.
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Vector.class),
    };

    /**
     * @serialData "permissions" field (a Vector containing the FilePermissions).
     */
    /**
     * Writes the contents of the perms field out as a Vector for
     * serialization compatibility with earlier releases.
     *
     * @param  out the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Write out Vector
        Vector<Permission> permissions = new Vector<>(perms.values());

        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }

    /**
     * Reads in a Vector of FilePermissions and saves them in the perms field.
     *
     * @param  in the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get the one we want
        @SuppressWarnings("unchecked")
        Vector<Permission> permissions = (Vector<Permission>)gfields.get("permissions", null);
        perms = new ConcurrentHashMap<>(permissions.size());
        for (Permission perm : permissions) {
            perms.put(perm.getName(), perm);
        }
    }
}
