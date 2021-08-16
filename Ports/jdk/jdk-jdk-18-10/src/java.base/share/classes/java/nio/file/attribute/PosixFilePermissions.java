/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import static java.nio.file.attribute.PosixFilePermission.*;
import java.util.*;

/**
 * This class consists exclusively of static methods that operate on sets of
 * {@link PosixFilePermission} objects.
 *
 * @since 1.7
 */

public final class PosixFilePermissions {
    private PosixFilePermissions() { }

    // Write string representation of permission bits to {@code sb}.
    private static void writeBits(StringBuilder sb, boolean r, boolean w, boolean x) {
        if (r) {
            sb.append('r');
        } else {
            sb.append('-');
        }
        if (w) {
            sb.append('w');
        } else {
            sb.append('-');
        }
        if (x) {
            sb.append('x');
        } else {
            sb.append('-');
        }
    }

    /**
     * Returns the {@code String} representation of a set of permissions. It
     * is guaranteed that the returned {@code String} can be parsed by the
     * {@link #fromString} method.
     *
     * <p> If the set contains {@code null} or elements that are not of type
     * {@code PosixFilePermission} then these elements are ignored.
     *
     * @param   perms
     *          the set of permissions
     *
     * @return  the string representation of the permission set
     */
    public static String toString(Set<PosixFilePermission> perms) {
        StringBuilder sb = new StringBuilder(9);
        writeBits(sb, perms.contains(OWNER_READ), perms.contains(OWNER_WRITE),
          perms.contains(OWNER_EXECUTE));
        writeBits(sb, perms.contains(GROUP_READ), perms.contains(GROUP_WRITE),
          perms.contains(GROUP_EXECUTE));
        writeBits(sb, perms.contains(OTHERS_READ), perms.contains(OTHERS_WRITE),
          perms.contains(OTHERS_EXECUTE));
        return sb.toString();
    }

    private static boolean isSet(char c, char setValue) {
        if (c == setValue)
            return true;
        if (c == '-')
            return false;
        throw new IllegalArgumentException("Invalid mode");
    }
    private static boolean isR(char c) { return isSet(c, 'r'); }
    private static boolean isW(char c) { return isSet(c, 'w'); }
    private static boolean isX(char c) { return isSet(c, 'x'); }

    /**
     * Returns the set of permissions corresponding to a given {@code String}
     * representation.
     *
     * <p> The {@code perms} parameter is a {@code String} representing the
     * permissions. It has 9 characters that are interpreted as three sets of
     * three. The first set refers to the owner's permissions; the next to the
     * group permissions and the last to others. Within each set, the first
     * character is {@code 'r'} to indicate permission to read, the second
     * character is {@code 'w'} to indicate permission to write, and the third
     * character is {@code 'x'} for execute permission. Where a permission is
     * not set then the corresponding character is set to {@code '-'}.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we require the set of permissions that indicate the owner has read,
     * write, and execute permissions, the group has read and execute permissions
     * and others have none.
     * <pre>
     *   Set&lt;PosixFilePermission&gt; perms = PosixFilePermissions.fromString("rwxr-x---");
     * </pre>
     *
     * @param   perms
     *          string representing a set of permissions
     *
     * @return  the resulting set of permissions
     *
     * @throws  IllegalArgumentException
     *          if the string cannot be converted to a set of permissions
     *
     * @see #toString(Set)
     */
    public static Set<PosixFilePermission> fromString(String perms) {
        if (perms.length() != 9)
            throw new IllegalArgumentException("Invalid mode");
        Set<PosixFilePermission> result = EnumSet.noneOf(PosixFilePermission.class);
        if (isR(perms.charAt(0))) result.add(OWNER_READ);
        if (isW(perms.charAt(1))) result.add(OWNER_WRITE);
        if (isX(perms.charAt(2))) result.add(OWNER_EXECUTE);
        if (isR(perms.charAt(3))) result.add(GROUP_READ);
        if (isW(perms.charAt(4))) result.add(GROUP_WRITE);
        if (isX(perms.charAt(5))) result.add(GROUP_EXECUTE);
        if (isR(perms.charAt(6))) result.add(OTHERS_READ);
        if (isW(perms.charAt(7))) result.add(OTHERS_WRITE);
        if (isX(perms.charAt(8))) result.add(OTHERS_EXECUTE);
        return result;
    }

    /**
     * Creates a {@link FileAttribute}, encapsulating a copy of the given file
     * permissions, suitable for passing to the {@link java.nio.file.Files#createFile
     * createFile} or {@link java.nio.file.Files#createDirectory createDirectory}
     * methods.
     *
     * @param   perms
     *          the set of permissions
     *
     * @return  an attribute encapsulating the given file permissions with
     *          {@link FileAttribute#name name} {@code "posix:permissions"}
     *
     * @throws  ClassCastException
     *          if the set contains elements that are not of type {@code
     *          PosixFilePermission}
     */
    public static FileAttribute<Set<PosixFilePermission>>
        asFileAttribute(Set<PosixFilePermission> perms)
    {
        // copy set and check for nulls (CCE will be thrown if an element is not
        // a PosixFilePermission)
        perms = new HashSet<>(perms);
        for (PosixFilePermission p: perms) {
            if (p == null)
                throw new NullPointerException();
        }
        final Set<PosixFilePermission> value = perms;
        return new FileAttribute<>() {
            @Override
            public String name() {
                return "posix:permissions";
            }
            @Override
            public Set<PosixFilePermission> value() {
                return Collections.unmodifiableSet(value);
            }
        };
    }
}
